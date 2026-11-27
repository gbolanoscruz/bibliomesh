"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  biblioApi,
  BibliographicTitle,
  DashboardStats,
  LoanRecord,
} from "@/lib/api";

type Tab = "catalog" | "circulation" | "labels";

export default function HomePage() {
  const [tab, setTab] = useState<Tab>("catalog");
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [titles, setTitles] = useState<BibliographicTitle[]>([]);
  const [activeLoans, setActiveLoans] = useState<LoanRecord[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [banner, setBanner] = useState<{ type: "ok" | "err"; msg: string } | null>(
    null
  );

  // Catalog form + ISBN lookup
  const [isbn, setIsbn] = useState("");
  const [title, setTitle] = useState("");
  const [author, setAuthor] = useState("");
  const [publisher, setPublisher] = useState("");
  const [publicationYear, setPublicationYear] = useState("");
  const [deweyCode, setDeweyCode] = useState("");
  const [cutterCode, setCutterCode] = useState("");
  const [inscriptionNumber, setInscriptionNumber] = useState("");
  const [coverUrl, setCoverUrl] = useState("");
  const [synopsis, setSynopsis] = useState("");
  const [lookingUpIsbn, setLookingUpIsbn] = useState(false);

  // Circulation & scanner inputs
  const [patronId, setPatronId] = useState("");
  const [itemCode, setItemCode] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [roomCode, setRoomCode] = useState("");

  // Label print queue
  const [labelQueue, setLabelQueue] = useState<
    { inscription: string; title: string; dewey: string; cutter: string }[]
  >([]);

  async function refreshAll(q = searchQuery) {
    try {
      const [statsRes, titlesRes, loansRes] = await Promise.allSettled([
        biblioApi.getStats(),
        biblioApi.searchTitles(q),
        biblioApi.listActiveLoans(),
      ]);
      if (statsRes.status === "fulfilled") setStats(statsRes.value);
      if (titlesRes.status === "fulfilled") setTitles(titlesRes.value);
      if (loansRes.status === "fulfilled") setActiveLoans(loansRes.value);
    } catch {
      // handled per-request
    }
  }

  useEffect(() => {
    refreshAll("");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleIsbnLookup() {
    if (!isbn.trim()) return;
    setLookingUpIsbn(true);
    setBanner(null);
    try {
      const res = await biblioApi.lookupIsbn(isbn.trim());
      if (!res.found) {
        setBanner({
          type: "err",
          msg: `No se encontraron metadatos para el ISBN ${res.isbn}. Puedes llenarlos manualmente.`,
        });
        return;
      }
      setTitle(res.title || "");
      setAuthor(res.author || "");
      setPublisher(res.publisher || "");
      setPublicationYear(res.publicationYear || "");
      setSynopsis(res.synopsis || "");
      setCoverUrl(res.coverUrl || "");
      setBanner({
        type: "ok",
        msg: res.alreadyInCatalog
          ? `Título ya existente en catálogo (${res.existingCopyCount} copia(s)). Si guardas, se creará un nuevo ejemplar.`
          : `Metadatos cargados desde ${res.source} para ISBN ${res.isbn}.`,
      });
    } catch (err) {
      setBanner({
        type: "err",
        msg: err instanceof Error ? err.message : "Error consultando ISBN",
      });
    } finally {
      setLookingUpIsbn(false);
    }
  }

  async function handleCreateTitle(e: FormEvent) {
    e.preventDefault();
    setBanner(null);
    try {
      const created = await biblioApi.createTitle({
        isbn: isbn || undefined,
        title,
        author: author || undefined,
        publisher: publisher || undefined,
        publicationYear: publicationYear || undefined,
        deweyCode: deweyCode || undefined,
        cutterCode: cutterCode || undefined,
        inscriptionNumber: inscriptionNumber || undefined,
        coverUrl: coverUrl || undefined,
        synopsis: synopsis || undefined,
      });
      const firstIns = created.copies[0]?.inscriptionNumber || "";
      setBanner({
        type: "ok",
        msg: `Guardado "${created.title}" (Inscripción: ${firstIns}).`,
      });
      setIsbn("");
      setTitle("");
      setAuthor("");
      setPublisher("");
      setPublicationYear("");
      setDeweyCode("");
      setCutterCode("");
      setInscriptionNumber("");
      setCoverUrl("");
      setSynopsis("");
      await refreshAll();
    } catch (err) {
      setBanner({
        type: "err",
        msg: err instanceof Error ? err.message : "No se pudo guardar",
      });
    }
  }

  async function handleCheckout(e: FormEvent) {
    e.preventDefault();
    setBanner(null);
    try {
      const loan = await biblioApi.checkout({
        patronIdentification: patronId,
        itemCode,
        dueDate: dueDate || undefined,
      });
      setBanner({
        type: "ok",
        msg: `Préstamo ${loan.loanCode} registrado: ${loan.itemTitle} (${loan.itemCode}) → ${loan.patronName}.`,
      });
      setItemCode("");
      await refreshAll();
    } catch (err) {
      setBanner({
        type: "err",
        msg: err instanceof Error ? err.message : "Error en préstamo",
      });
    }
  }

  async function handleQuickReturn(codeToReturn = itemCode) {
    if (!codeToReturn.trim()) return;
    setBanner(null);
    try {
      const returned = await biblioApi.returnItem(codeToReturn.trim());
      setBanner({
        type: "ok",
        msg: `Devolución registrada para ${returned.itemCode} (${returned.itemTitle}).`,
      });
      setItemCode("");
      await refreshAll();
    } catch (err) {
      setBanner({
        type: "err",
        msg: err instanceof Error ? err.message : "Error en devolución",
      });
    }
  }

  async function handleRoomConsultation(e: FormEvent) {
    e.preventDefault();
    if (!roomCode.trim()) return;
    setBanner(null);
    try {
      await biblioApi.registerRoomConsultation(roomCode.trim());
      setBanner({
        type: "ok",
        msg: `Consulta en sala registrada para ${roomCode.trim().toUpperCase()}.`,
      });
      setRoomCode("");
      await refreshAll();
    } catch (err) {
      setBanner({
        type: "err",
        msg: err instanceof Error ? err.message : "Error en consulta en sala",
      });
    }
  }

  function queueBookLabel(t: BibliographicTitle, inscription: string) {
    setLabelQueue((prev) => [
      ...prev,
      {
        inscription,
        title: t.title,
        dewey: t.deweyCode || "-",
        cutter: t.cutterCode || "-",
      },
    ]);
    setBanner({
      type: "ok",
      msg: `Etiqueta ${inscription} añadida a la cola de impresión.`,
    });
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <header className="mb-6 flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-emerald-400">
            📚 BiblioMesh
          </h1>
          <p className="text-sm text-slate-400">
            API: <span className="font-mono text-slate-300">{biblioApi.baseUrl}</span>
          </p>
        </div>

        <nav className="flex gap-2">
          {(
            [
              ["catalog", "Catálogo & ISBN"],
              ["circulation", "Circulación y Sala"],
              ["labels", `Etiquetas (${labelQueue.length})`],
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              onClick={() => setTab(id)}
              className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${
                tab === id
                  ? "bg-emerald-500 text-slate-950"
                  : "bg-slate-900 text-slate-300 hover:bg-slate-800"
              }`}
            >
              {label}
            </button>
          ))}
        </nav>
      </header>

      {/* KPI Strip */}
      <section className="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-4">
          <div className="text-2xl font-bold">{stats?.totalBookCopies ?? 0}</div>
          <div className="text-xs uppercase text-slate-400">Ejemplares</div>
        </div>
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-4">
          <div className="text-2xl font-bold">{stats?.activePatrons ?? 0}</div>
          <div className="text-xs uppercase text-slate-400">Lectores activos</div>
        </div>
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-4">
          <div className="text-2xl font-bold text-amber-400">
            {stats?.activeLoans ?? 0}
          </div>
          <div className="text-xs uppercase text-slate-400">Préstamos activos</div>
        </div>
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-4">
          <div className="text-2xl font-bold text-emerald-400">
            {stats?.roomConsultations ?? 0}
          </div>
          <div className="text-xs uppercase text-slate-400">Consultas en sala</div>
        </div>
      </section>

      {banner && (
        <div
          className={`mb-6 rounded-xl border px-4 py-3 text-sm font-medium ${
            banner.type === "ok"
              ? "border-emerald-700/60 bg-emerald-950/50 text-emerald-300"
              : "border-rose-700/60 bg-rose-950/50 text-rose-300"
          }`}
        >
          {banner.msg}
        </div>
      )}

      {tab === "catalog" && (
        <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
          <aside className="rounded-xl border border-slate-800 bg-slate-900 p-5">
            <h2 className="mb-4 text-lg font-semibold">Nuevo Libro / Autocompletar ISBN</h2>

            <div className="mb-4 flex gap-2">
              <input
                value={isbn}
                onChange={(e) => setIsbn(e.target.value)}
                placeholder="ISBN 10 o 13 (ej. 9780134685991)"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              />
              <button
                type="button"
                onClick={handleIsbnLookup}
                disabled={lookingUpIsbn}
                className="shrink-0 rounded-lg bg-sky-500 px-3 py-2 text-sm font-semibold text-slate-950 hover:bg-sky-400 disabled:opacity-50"
              >
                {lookingUpIsbn ? "..." : "Buscar ISBN"}
              </button>
            </div>

            <form onSubmit={handleCreateTitle} className="space-y-3">
              <input
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Título principal (*)"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              />
              <input
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                placeholder="Autor / Creador"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              />
              <div className="grid grid-cols-2 gap-2">
                <input
                  value={deweyCode}
                  onChange={(e) => setDeweyCode(e.target.value)}
                  placeholder="Dewey (ej. 005.13)"
                  className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
                />
                <input
                  value={cutterCode}
                  onChange={(e) => setCutterCode(e.target.value)}
                  placeholder="Cutter (ej. B651e)"
                  className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <input
                  value={publisher}
                  onChange={(e) => setPublisher(e.target.value)}
                  placeholder="Editorial"
                  className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
                />
                <input
                  value={publicationYear}
                  onChange={(e) => setPublicationYear(e.target.value)}
                  placeholder="Año"
                  className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
                />
              </div>
              <input
                value={inscriptionNumber}
                onChange={(e) => setInscriptionNumber(e.target.value)}
                placeholder="Inscripción (vacío = automático)"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              />
              <input
                value={coverUrl}
                onChange={(e) => setCoverUrl(e.target.value)}
                placeholder="URL de portada"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              />
              <textarea
                rows={3}
                value={synopsis}
                onChange={(e) => setSynopsis(e.target.value)}
                placeholder="Resumen o sinopsis"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              />
              <button
                type="submit"
                className="w-full rounded-lg bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-slate-950 hover:bg-emerald-400"
              >
                Guardar Título y Ejemplar
              </button>
            </form>
          </aside>

          <main className="space-y-4">
            <div className="flex gap-2">
              <input
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  refreshAll(e.target.value);
                }}
                placeholder="Buscar por título, autor, ISBN o inscripción..."
                className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-2.5 text-sm"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              {titles.map((t) => (
                <article
                  key={t.id}
                  className="flex flex-col justify-between rounded-xl border border-slate-800 bg-slate-900 p-4"
                >
                  <div>
                    <div className="mb-1 flex items-center justify-between gap-2 text-xs text-slate-400">
                      <span>
                        {t.deweyCode || "-"} {t.cutterCode || ""}
                      </span>
                      <span>{t.isbn || "Sin ISBN"}</span>
                    </div>
                    <h3 className="text-base font-semibold text-white">{t.title}</h3>
                    <p className="text-sm text-slate-400">
                      {t.author || "Autor no registrado"}
                      {t.publicationYear ? ` (${t.publicationYear})` : ""}
                    </p>
                  </div>

                  <div className="mt-4 border-t border-slate-800 pt-3">
                    <div className="mb-2 flex flex-wrap gap-1.5">
                      {t.copies.map((c) => (
                        <span
                          key={c.id}
                          className={`rounded px-2 py-0.5 font-mono text-xs ${
                            c.status === "AVAILABLE"
                              ? "bg-emerald-950 text-emerald-300"
                              : "bg-amber-950 text-amber-300"
                          }`}
                        >
                          {c.inscriptionNumber} · {c.status}
                        </span>
                      ))}
                    </div>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={async () => {
                          await biblioApi.addCopy(t.id);
                          await refreshAll();
                        }}
                        className="rounded-lg bg-slate-800 px-3 py-1.5 text-xs font-medium hover:bg-slate-700"
                      >
                        + Duplicar copia
                      </button>
                      {t.copies[0] && (
                        <button
                          type="button"
                          onClick={() =>
                            queueBookLabel(t, t.copies[0].inscriptionNumber)
                          }
                          className="rounded-lg bg-amber-500/15 px-3 py-1.5 text-xs font-medium text-amber-300 hover:bg-amber-500/25"
                        >
                          Etiqueta lomo
                        </button>
                      )}
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </main>
        </div>
      )}

      {tab === "circulation" && (
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="space-y-6">
            <form
              onSubmit={handleCheckout}
              className="space-y-3 rounded-xl border border-slate-800 bg-slate-900 p-5"
            >
              <h2 className="text-lg font-semibold">Préstamo y Devolución por Escáner</h2>
              <input
                value={patronId}
                onChange={(e) => setPatronId(e.target.value)}
                placeholder="Cédula / ID del lector (vacío + Enter en código = Devolución)"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              />
              <input
                required
                value={itemCode}
                onChange={(e) => setItemCode(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !patronId.trim()) {
                    e.preventDefault();
                    handleQuickReturn(itemCode);
                  }
                }}
                placeholder="Código de inscripción o placa de recurso"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm font-mono"
              />
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              />
              <div className="flex gap-2">
                <button
                  type="submit"
                  className="flex-1 rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-emerald-400"
                >
                  Registrar Préstamo
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickReturn(itemCode)}
                  className="flex-1 rounded-lg bg-sky-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-sky-400"
                >
                  Registrar Devolución
                </button>
              </div>
            </form>

            <form
              onSubmit={handleRoomConsultation}
              className="space-y-3 rounded-xl border border-slate-800 bg-slate-900 p-5"
            >
              <h2 className="text-lg font-semibold">Consulta en Sala (Escáner rápido)</h2>
              <div className="flex gap-2">
                <input
                  required
                  value={roomCode}
                  onChange={(e) => setRoomCode(e.target.value)}
                  placeholder="Escanear inscripción y presionar Enter"
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm font-mono"
                />
                <button
                  type="submit"
                  className="rounded-lg bg-amber-400 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-amber-300"
                >
                  Registrar
                </button>
              </div>
            </form>
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-900 p-5">
            <h2 className="mb-4 text-lg font-semibold">Préstamos Activos</h2>
            <div className="space-y-3">
              {activeLoans.length === 0 && (
                <p className="text-sm text-slate-400">No hay préstamos activos.</p>
              )}
              {activeLoans.map((l) => (
                <div
                  key={l.id}
                  className="flex items-center justify-between gap-3 rounded-lg border border-slate-800 bg-slate-950 p-3 text-sm"
                >
                  <div>
                    <div className="font-mono font-semibold text-emerald-400">
                      {l.itemCode} · {l.itemTitle}
                    </div>
                    <div className="text-xs text-slate-400">
                      Lector: {l.patronName} ({l.patronIdentification})
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleQuickReturn(l.itemCode)}
                    className="rounded-lg bg-slate-800 px-3 py-1.5 text-xs font-semibold hover:bg-slate-700"
                  >
                    Devolver
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {tab === "labels" && (
        <section className="rounded-xl border border-slate-800 bg-slate-900 p-5">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold">Cola de Impresión de Lomos</h2>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => window.print()}
                className="rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-slate-950"
              >
                Imprimir
              </button>
              <button
                type="button"
                onClick={() => setLabelQueue([])}
                className="rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white"
              >
                Vaciar
              </button>
            </div>
          </div>

          <div id="print-canvas" className="flex flex-wrap gap-3">
            {labelQueue.map((lbl, idx) => (
              <div
                key={`${lbl.inscription}-${idx}`}
                className="flex h-[2in] w-[1.5in] flex-col justify-center rounded border border-dashed border-slate-400 bg-white p-2.5 font-mono text-black"
              >
                <div className="text-sm font-bold">{lbl.dewey}</div>
                <div className="text-sm font-bold">{lbl.cutter}</div>
                <div className="mt-2 font-sans text-xs font-semibold">
                  {lbl.inscription}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}