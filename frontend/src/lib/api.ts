const API_BASE = (
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
).replace(/\/$/, "");

export interface BookCopy {
  id: number;
  inscriptionNumber: string;
  barcode: string | null;
  status: string;
  shelfLocation: string | null;
  queuedForLabel: boolean;
}

export interface BibliographicTitle {
  id: number;
  isbn: string | null;
  title: string;
  subtitle: string | null;
  author: string | null;
  publisher: string | null;
  publicationYear: string | null;
  deweyCode: string | null;
  cutterCode: string | null;
  descriptors: string[];
  synopsis: string | null;
  coverUrl: string | null;
  copies: BookCopy[];
}

export interface IsbnLookupResult {
  isbn: string;
  found: boolean;
  source: string;
  alreadyInCatalog: boolean;
  existingTitleId: number | null;
  existingCopyCount: number;
  title: string | null;
  subtitle: string | null;
  author: string | null;
  publisher: string | null;
  publicationYear: string | null;
  descriptors: string[];
  synopsis: string | null;
  coverUrl: string | null;
}

export interface DashboardStats {
  totalBookCopies: number;
  totalTechResources: number;
  activePatrons: number;
  activeLoans: number;
  returnedLoans: number;
  roomConsultations: number;
}

export interface LoanRecord {
  id: number;
  loanCode: string;
  itemType: string;
  itemCode: string;
  itemTitle: string;
  patronIdentification: string;
  patronName: string;
  loanedAt: string;
  dueDate: string | null;
  returnedAt: string | null;
  status: string;
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers || {}),
    },
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export const biblioApi = {
  baseUrl: API_BASE,
  getStats: () => requestJson<DashboardStats>("/api/circulation/stats"),
  lookupIsbn: (isbn: string) =>
    requestJson<IsbnLookupResult>(
      `/api/catalog/isbn/${encodeURIComponent(isbn)}`
    ),
  searchTitles: (q = "") =>
    requestJson<BibliographicTitle[]>(
      `/api/catalog/titles?q=${encodeURIComponent(q)}`
    ),
  createTitle: (payload: {
    isbn?: string;
    title: string;
    subtitle?: string;
    author?: string;
    publisher?: string;
    publicationYear?: string;
    deweyCode?: string;
    cutterCode?: string;
    synopsis?: string;
    coverUrl?: string;
    inscriptionNumber?: string;
  }) =>
    requestJson<BibliographicTitle>("/api/catalog/titles", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  addCopy: (titleId: number, inscriptionNumber?: string) =>
    requestJson<BookCopy>(`/api/catalog/titles/${titleId}/copies`, {
      method: "POST",
      body: JSON.stringify({ inscriptionNumber: inscriptionNumber || null }),
    }),
  listActiveLoans: () =>
    requestJson<LoanRecord[]>("/api/circulation/loans/active"),
  checkout: (payload: {
    patronIdentification: string;
    itemCode: string;
    dueDate?: string;
  }) =>
    requestJson<LoanRecord>("/api/circulation/loans", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  returnItem: (itemCode: string) =>
    requestJson<LoanRecord>("/api/circulation/returns", {
      method: "POST",
      body: JSON.stringify({ itemCode }),
    }),
  registerRoomConsultation: (inscriptionNumber: string) =>
    requestJson("/api/circulation/room-consultations", {
      method: "POST",
      body: JSON.stringify({ inscriptionNumber }),
    }),
};