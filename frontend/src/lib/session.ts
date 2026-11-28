export interface AdminSession {
  token: string;
  email: string;
  fullName: string;
  role: "SUPER_ADMIN" | "SCHOOL_ADMIN" | "LIBRARIAN";
  schoolId: number | null;
  schoolCode: string | null;
}

const ADMIN_SESSION_KEY = "bibliomesh_admin_session";
const SCHOOL_CODE_COOKIE = "bibliomesh_school_code";
const ONE_MONTH_SECONDS = 60 * 60 * 24 * 30;

export function getSavedSchoolCode(): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${SCHOOL_CODE_COOKIE}=`));
  if (!match) return null;
  const raw = decodeURIComponent(match.split("=").slice(1).join("=")).trim();
  return raw ? raw.toUpperCase() : null;
}

export function saveSchoolCodeCookie(code: string): void {
  if (typeof document === "undefined") return;
  const normalized = encodeURIComponent(code.trim().toUpperCase());
  const secure =
    typeof window !== "undefined" && window.location.protocol === "https:"
      ? "; Secure"
      : "";
  document.cookie = `${SCHOOL_CODE_COOKIE}=${normalized}; path=/; max-age=${ONE_MONTH_SECONDS}; SameSite=Lax${secure}`;
}

export function loadAdminSession(): AdminSession | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.sessionStorage.getItem(ADMIN_SESSION_KEY);
    return raw ? (JSON.parse(raw) as AdminSession) : null;
  } catch {
    return null;
  }
}

export function saveAdminSession(session: AdminSession | null): void {
  if (typeof window === "undefined") return;
  if (!session) {
    window.sessionStorage.removeItem(ADMIN_SESSION_KEY);
    return;
  }
  window.sessionStorage.setItem(ADMIN_SESSION_KEY, JSON.stringify(session));
}