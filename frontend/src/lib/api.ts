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

export interface PagedTitlesResponse {
  items: BibliographicTitle[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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

function fetchPagedTitles(
  q = "",
  schoolCode?: string,
  page = 0,
  size = 24
): Promise<PagedTitlesResponse> {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (schoolCode) params.set("school", schoolCode);
  params.set("page", String(page));
  params.set("size", String(size));
  return requestJson<PagedTitlesResponse>(
    `/api/catalog/titles?${params.toString()}`
  );
}

export const biblioApi = {
  baseUrl: API_BASE,
  getStats: () => requestJson<DashboardStats>("/api/circulation/stats"),
  lookupIsbn: (isbn: string) =>
    requestJson<IsbnLookupResult>(
      `/api/catalog/isbn/${encodeURIComponent(isbn)}`
    ),
  searchTitlesPaged: (
    q = "",
    schoolCode?: string,
    page = 0,
    size = 24
  ) => fetchPagedTitles(q, schoolCode, page, size),

  searchTitles: (q = "") =>
    fetchPagedTitles(q, undefined, 0, 24).then((res) => res.items),

  searchTitlesBySchool: (q = "", schoolCode?: string) =>
    fetchPagedTitles(q, schoolCode, 0, 24).then((res) => res.items),

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
    listPatrons: (q = "") =>
    requestJson<PatronRecord[]>(`/api/patrons?q=${encodeURIComponent(q)}`),
  upsertPatron: (payload: {
    identificationNumber: string;
    firstName: string;
    firstSurname?: string;
    secondSurname?: string;
    patronType?: "STUDENT" | "TEACHER" | "STAFF" | "PARTNER_LIBRARY";
    gradeSection?: string;
    phone?: string;
    email?: string;
    observations?: string;
  }) =>
    requestJson<PatronRecord>("/api/patrons", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  listTechResources: (q = "") =>
    requestJson<TechResourceRecord[]>(
      `/api/catalog/tech-resources?q=${encodeURIComponent(q)}`
    ),
  getDigitalLibrary: () =>
    requestJson<DigitalLibraryResponse>("/api/opac/digital"),
  listPublicSchools: () =>
    requestJson<SchoolOption[]>("/api/public/schools"),

  loginAdmin: (payload: { email: string; password: string }) =>
    requestJson<AdminSession>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  updateCopyStatus: (copyId: number, payload: UpdateCopyStatusPayload) =>
    requestJson<BookCopy>(`/api/catalog/copies/${copyId}/status`, {
      method: "PATCH",
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    }),
};

export interface PatronRecord {
  id: number;
  identificationNumber: string;
  firstName: string;
  firstSurname: string | null;
  secondSurname: string | null;
  fullName: string;
  patronType: "STUDENT" | "TEACHER" | "STAFF" | "PARTNER_LIBRARY";
  gradeSection: string | null;
  phone: string | null;
  email: string | null;
  observations: string | null;
  active: boolean;
}

export interface TechResourceRecord {
  id: number;
  assetTag: string;
  name: string;
  resourceType: string;
  brand: string | null;
  model: string | null;
  serialNumber: string | null;
  status: string;
}

export interface DigitalResourceRecord {
  id: number;
  title: string;
  author: string | null;
  category: string | null;
  resourceUrl: string;
  description: string | null;
}

export interface DigitalLibraryResponse {
  libraryName: string;
  driveFolderId: string | null;
  resources: DigitalResourceRecord[];
}

export interface SchoolOption {
  id: number;
  code: string;
  name: string;
  seqPadding?: number;
  lastInscriptionSeq?: number;
}

export interface AdminSession {
  token: string;
  email: string;
  fullName: string;
  role: "SUPER_ADMIN" | "SCHOOL_ADMIN" | "LIBRARIAN";
  schoolId: number | null;
  schoolCode: string | null;
}

export interface UpdateCopyStatusPayload {
  status: "AVAILABLE" | "LOANED" | "RESERVED" | "LOST" | "DAMAGED" | "ERROR_ENTRY" | "WITHDRAWN";
  unavailableReason?: string;
  unavailableNotes?: string;
  physicalCondition?: "EXCELLENT" | "GOOD" | "FAIR" | "POOR";
  inLibrary?: boolean;
}

function getAuthHeaders(): Record<string, string> {
  if (typeof window === "undefined") return {};
  try {
    const raw = window.sessionStorage.getItem("bibliomesh_admin_session");
    if (!raw) return {};
    const parsed = JSON.parse(raw) as Partial<AdminSession>;
    return parsed.token ? { Authorization: `Bearer ${parsed.token}` } : {};
  } catch {
    return {};
  }
}