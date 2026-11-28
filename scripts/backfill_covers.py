#!/usr/bin/env python3
"""
Rellena bibliographic_titles.cover_url usando Open Library + Google Books
para todos los títulos con ISBN que aún no tienen portada.

Uso:
  python scripts/backfill_covers.py --db-url "postgresql://..." --limit 50 --dry-run
  python scripts/backfill_covers.py --db-url "postgresql://..." --apply
"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
import sys
import urllib.error
import urllib.request
from typing import Optional

import psycopg


USER_AGENT = "BiblioMesh-CoverBackfill/1.0"


def check_openlibrary(isbn: str) -> Optional[str]:
    url = f"https://covers.openlibrary.org/b/isbn/{isbn}-M.jpg?default=false"
    req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=6) as resp:
            if resp.status == 200:
                return f"https://covers.openlibrary.org/b/isbn/{isbn}-M.jpg"
    except Exception:
        return None
    return None


def check_google_books(isbn: str) -> Optional[str]:
    url = (
        f"https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}"
        "&fields=items(volumeInfo/imageLinks/thumbnail)"
    )
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=6) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            items = data.get("items") or []
            if not items:
                return None
            thumb = (
                items[0]
                .get("volumeInfo", {})
                .get("imageLinks", {})
                .get("thumbnail")
            )
            if thumb:
                return thumb.replace("http://", "https://")
    except Exception:
        return None
    return None


def find_cover(row: tuple[int, str]) -> tuple[int, str, Optional[str]]:
    title_id, isbn = row
    cover = check_openlibrary(isbn) or check_google_books(isbn)
    return title_id, isbn, cover


def main() -> int:
    parser = argparse.ArgumentParser(description="Backfill de portadas por ISBN en Neon")
    parser.add_argument("--db-url", default=os.getenv("DATABASE_URL"), help="URL postgresql://...")
    parser.add_argument("--limit", type=int, default=0, help="Máximo de títulos a procesar (0 = todos)")
    parser.add_argument("--workers", type=int, default=8, help="Hilos concurrentes (default 8)")
    parser.add_argument("--dry-run", action="store_true", help="Solo consulta APIs sin escribir en BD")
    parser.add_argument("--apply", action="store_true", help="Guarda cover_url en PostgreSQL")
    args = parser.parse_args()

    if not args.dry_run and not args.apply:
        parser.error("Debes pasar --dry-run o --apply")
    if not args.db_url:
        print("ERROR: Falta --db-url o DATABASE_URL", file=sys.stderr)
        return 1

    with psycopg.connect(args.db_url) as conn:
        with conn.cursor() as cur:
            sql = """
                SELECT id, isbn
                FROM bibliographic_titles
                WHERE cover_url IS NULL
                  AND isbn IS NOT NULL
                  AND TRIM(isbn) <> ''
                ORDER BY id ASC
            """
            if args.limit > 0:
                sql += f" LIMIT {args.limit}"
            cur.execute(sql)
            rows = cur.fetchall()

    print(f"Títulos con ISBN sin portada encontrados: {len(rows)}", flush=True)
    if not rows:
        return 0

    found: list[tuple[str, int]] = []
    processed = 0

    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        for title_id, isbn, cover_url in pool.map(find_cover, rows):
            processed += 1
            if cover_url:
                found.append((cover_url, title_id))
            if processed % 100 == 0 or processed == len(rows):
                print(f"Progreso: {processed}/{len(rows)} | Portadas encontradas: {len(found)}", flush=True)

    print(f"Resumen: {len(found)} portadas encontradas de {len(rows)} ISBNs consultados.")

    if args.apply and found:
        with psycopg.connect(args.db_url) as conn:
            with conn.cursor() as cur:
                cur.executemany(
                    "UPDATE bibliographic_titles SET cover_url = %s WHERE id = %s",
                    found,
                )
            conn.commit()
        print(f"OK — {len(found)} portadas actualizadas en Neon.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())