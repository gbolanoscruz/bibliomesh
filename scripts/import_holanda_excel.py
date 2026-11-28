#!/usr/bin/env python3
"""
Importador idempotente del inventario de la Biblioteca Gabriela Mistral (Escuela Holanda)
hacia PostgreSQL (BiblioMesh V1-V3).

Uso:
  python scripts/import_holanda_excel.py --file "Inventatrio_ Escuela Holanda 1.xlsx" --dry-run
  python scripts/import_holanda_excel.py --file "Inventatrio_ Escuela Holanda 1.xlsx" --apply --db-url "$env:DATABASE_URL"
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from collections import Counter
from typing import Any

import openpyxl


SCHOOL_CODE = "HOLANDA"
SCHOOL_NAME = "Biblioteca Gabriela Mistral - Escuela Holanda"


def clean_str(val: Any, max_len: int | None = None) -> str | None:
    if val is None:
        return None
    s = str(val).replace("\xa0", " ").strip()
    if not s or s.lower() == "nan":
        return None
    return s[:max_len] if max_len else s


def normalize_isbn(val: Any) -> str | None:
    raw = clean_str(val)
    if not raw:
        return None
    digits = re.sub(r"[^0-9Xx]", "", raw).upper()
    if len(digits) in (10, 13):
        return digits
    return None


def map_condition(val: Any) -> str:
    s = (clean_str(val) or "").lower()
    if s.startswith("exc") or s.startswith("excl"):
        return "EXCELLENT"
    if s.startswith("buen"):
        return "GOOD"
    if s.startswith("mal"):
        return "POOR"
    return "FAIR"


def map_status(estado: Any, condicion: Any) -> tuple[str, str | None]:
    e = (clean_str(estado) or "").lower()
    c = (clean_str(condicion) or "").lower()
    if "descart" in e or "descart" in c:
        return "WITHDRAWN", "DISCARDED_IN_LEGACY_INVENTORY"
    return "AVAILABLE", None


def parse_bool_flag(val: Any) -> bool:
    if val is None:
        return False
    if isinstance(val, (int, float)):
        return val == 1
    s = str(val).strip().lower()
    return s in ("1", "1.0", "true", "si", "sí", "v")


def parse_call_number(clasif: str | None) -> tuple[str | None, str | None, str | None]:
    """Descompone ej. 'CR 863.4 L992C' -> ('CR', '863.4', 'L992C')."""
    if not clasif:
        return None, None, None
    tokens = clasif.strip().split()
    if len(tokens) >= 3:
        return tokens[0][:16], tokens[1][:32], " ".join(tokens[2:])[:32]
    if len(tokens) == 2:
        return None, tokens[0][:32], tokens[1][:32]
    return None, tokens[0][:32], None


def load_hoja3_classifications(wb: openpyxl.Workbook) -> dict[int, tuple[str | None, str | None, str | None]]:
    result: dict[int, tuple[str | None, str | None, str | None]] = {}
    if "Hoja3" not in wb.sheetnames:
        return result
    ws = wb["Hoja3"]
    for row in ws.iter_rows(min_row=2, values_only=True):
        ins, _fich, _title, _author, clasif, _year = (row + (None,) * 6)[:6]
        if isinstance(ins, (int, float)) and clasif:
            result[int(ins)] = parse_call_number(clean_str(clasif))
    return result


def parse_workbook(filepath: str) -> dict[str, Any]:
    wb = openpyxl.load_workbook(filepath, read_only=True, data_only=True)
    hoja3 = load_hoja3_classifications(wb)

    titles: dict[tuple[str, str, str], dict[str, Any]] = {}
    copies: list[dict[str, Any]] = []
    seen_inscriptions: Counter[str] = Counter()
    skipped_books = 0

    # --- Hoja: Libros ---
    ws_libros = wb["Libros"]
    for idx, row in enumerate(ws_libros.iter_rows(min_row=2, values_only=True), start=2):
        padded = (row + (None,) * 16)[:16]
        (
            n_desc, estado, _total, recreativa, recomendada,
            titulo, _u6, autor, editorial, lugar, anio,
            isbn_raw, condicion, origen, observaciones, en_biblio,
        ) = padded

        title_clean = clean_str(titulo, 255)
        if not title_clean:
            skipped_books += 1
            continue

        # Construir número de inscripción único
        ins_int: int | None = None
        if isinstance(n_desc, (int, float)):
            ins_int = int(n_desc)
            base_ins = f"{SCHOOL_CODE}-{ins_int:05d}"
        else:
            base_ins = f"{SCHOOL_CODE}-SINNUM-{idx}"

        seen_inscriptions[base_ins] += 1
        inscription = base_ins if seen_inscriptions[base_ins] == 1 else f"{base_ins}-DUP{seen_inscriptions[base_ins]}"

        author_clean = clean_str(autor, 200)
        isbn_clean = normalize_isbn(isbn_raw)
        prefix, dewey, cutter = hoja3.get(ins_int, (None, None, None)) if ins_int else (None, None, None)

        title_key = (
            title_clean.lower(),
            (author_clean or "").lower(),
            isbn_clean or "",
        )
        if title_key not in titles:
            titles[title_key] = {
                "isbn": isbn_clean,
                "title": title_clean,
                "author": author_clean,
                "publisher": clean_str(editorial, 150),
                "publication_place": clean_str(lugar, 100),
                "publication_year": clean_str(anio, 32),
                "collection_prefix": prefix,
                "dewey_code": dewey,
                "cutter_code": cutter,
                "is_recreational": parse_bool_flag(recreativa),
                "is_recommended": parse_bool_flag(recomendada),
            }
        else:
            # Enriquecer título si otra copia trae signatura o flags
            t = titles[title_key]
            t["collection_prefix"] = t["collection_prefix"] or prefix
            t["dewey_code"] = t["dewey_code"] or dewey
            t["cutter_code"] = t["cutter_code"] or cutter
            t["is_recreational"] = t["is_recreational"] or parse_bool_flag(recreativa)
            t["is_recommended"] = t["is_recommended"] or parse_bool_flag(recomendada)

        status, unavail_reason = map_status(estado, condicion)
        copies.append({
            "title_key": title_key,
            "inscription_number": inscription,
            "status": status,
            "unavailable_reason": unavail_reason,
            "physical_condition": map_condition(estado),
            "acquisition_source": clean_str(origen, 150),
            "in_library": parse_bool_flag(en_biblio),
            "observations": clean_str(observaciones),
        })

    # --- Hoja: Audiovisuales ---
    audiovisuals: list[dict[str, Any]] = []
    if "Audiovisuales" in wb.sheetnames:
        ws_av = wb["Audiovisuales"]
        for row in ws_av.iter_rows(min_row=2, values_only=True):
            padded = (row + (None,) * 7)[:7]
            ins_av, articulo, desc, autor_av, ed_av, pais, anio_av = padded
            ins_clean = clean_str(ins_av, 64)
            if not ins_clean:
                continue
            audiovisuals.append({
                "inscription_number": f"{SCHOOL_CODE}-{ins_clean}",
                "item_type": clean_str(articulo, 80) or "Material didáctico",
                "description": clean_str(desc),
                "author": clean_str(autor_av, 200),
                "publisher": clean_str(ed_av, 200),
                "country": clean_str(pais, 100),
                "publication_year": clean_str(anio_av, 32),
            })

    wb.close()
    return {
        "titles": titles,
        "copies": copies,
        "audiovisuals": audiovisuals,
        "skipped_books": skipped_books,
        "duplicate_inscriptions": {k: v for k, v in seen_inscriptions.items() if v > 1},
    }


def apply_to_postgres(data: dict[str, Any], db_url: str) -> None:
    import psycopg

    with psycopg.connect(db_url) as conn:
        with conn.cursor() as cur:
            # 1. Asegurar escuela HOLANDA
            cur.execute(
                """
                INSERT INTO schools (code, name)
                VALUES (%s, %s)
                ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
                RETURNING id
                """,
                (SCHOOL_CODE, SCHOOL_NAME),
            )
            school_id = cur.fetchone()[0]

            # 2. Insertar títulos en lote (pipelining)
            title_keys = list(data["titles"].keys())
            title_rows = [
                (
                    t["isbn"], t["title"], t["author"], t["publisher"],
                    t["publication_place"], t["publication_year"],
                    t["collection_prefix"], t["dewey_code"], t["cutter_code"],
                    t["is_recreational"], t["is_recommended"],
                )
                for t in data["titles"].values()
            ]
            print(f"Insertando {len(title_rows)} títulos...", flush=True)
            cur.executemany(
                """
                INSERT INTO bibliographic_titles (
                    isbn, title, author, publisher, publication_place,
                    publication_year, collection_prefix, dewey_code, cutter_code,
                    is_recreational, is_recommended
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                RETURNING id
                """,
                title_rows,
                returning=True,
            )
            title_id_by_key: dict[tuple[str, str, str], int] = {}
            idx = 0
            while True:
                row = cur.fetchone()
                if row is not None:
                    title_id_by_key[title_keys[idx]] = row[0]
                    idx += 1
                if not cur.nextset():
                    break

            # 3. Insertar ejemplares en lote
            copy_rows = [
                (
                    title_id_by_key[c["title_key"]],
                    school_id,
                    c["inscription_number"],
                    c["inscription_number"],
                    c["status"],
                    c["unavailable_reason"],
                    c["physical_condition"],
                    c["acquisition_source"],
                    c["in_library"],
                    c["observations"],
                )
                for c in data["copies"]
            ]
            print(f"Insertando {len(copy_rows)} ejemplares...", flush=True)
            cur.executemany(
                """
                INSERT INTO book_copies (
                    title_id, school_id, inscription_number, barcode,
                    status, unavailable_reason, physical_condition,
                    acquisition_source, in_library, observations
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (inscription_number) DO NOTHING
                """,
                copy_rows,
            )

            # 4. Insertar audiovisuales en lote
            av_rows = [
                (
                    school_id,
                    av["inscription_number"],
                    av["item_type"],
                    av["description"],
                    av["author"],
                    av["publisher"],
                    av["country"],
                    av["publication_year"],
                )
                for av in data["audiovisuals"]
            ]
            print(f"Insertando {len(av_rows)} audiovisuales...", flush=True)
            cur.executemany(
                """
                INSERT INTO audiovisual_resources (
                    school_id, inscription_number, item_type, description,
                    author, publisher, country, publication_year
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (inscription_number) DO NOTHING
                """,
                av_rows,
            )

        conn.commit()
    print(f"OK — Escuela {SCHOOL_CODE} (id={school_id}): {len(title_id_by_key)} títulos, {len(copy_rows)} ejemplares procesados, {len(av_rows)} audiovisuales procesados.")
    
def main() -> int:
    parser = argparse.ArgumentParser(description="Importador Excel Escuela Holanda -> BiblioMesh PostgreSQL")
    parser.add_argument("--file", required=True, help="Ruta al archivo .xlsx")
    parser.add_argument("--dry-run", action="store_true", help="Solo valida y muestra estadísticas")
    parser.add_argument("--apply", action="store_true", help="Inserta los datos en PostgreSQL")
    parser.add_argument("--db-url", default=os.getenv("DATABASE_URL"), help="URL postgresql://... (o variable DATABASE_URL)")
    args = parser.parse_args()

    if not args.dry_run and not args.apply:
        parser.error("Debes pasar --dry-run o --apply")

    data = parse_workbook(args.file)
    status_counts = Counter(c["status"] for c in data["copies"])
    cond_counts = Counter(c["physical_condition"] for c in data["copies"])

    print("=== Resumen de análisis del Excel ===")
    print(f"Títulos únicos agrupados : {len(data['titles'])}")
    print(f"Ejemplares válidos       : {len(data['copies'])}")
    print(f"Filas sin título omitidas: {data['skipped_books']}")
    print(f"Audiovisuales válidos    : {len(data['audiovisuals'])}")
    print(f"Duplicados renombrados   : {data['duplicate_inscriptions']}")
    print(f"Estados de ejemplares    : {dict(status_counts)}")
    print(f"Condición física         : {dict(cond_counts)}")

    if args.apply:
        if not args.db_url:
            print("ERROR: Falta --db-url o variable de entorno DATABASE_URL (formato postgresql://user:pass@host/db?sslmode=require)", file=sys.stderr)
            return 1
        apply_to_postgres(data, args.db_url)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())