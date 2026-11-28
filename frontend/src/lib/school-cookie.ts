const SCHOOL_CODE_COOKIE = "bibliomesh_school_code";
const ONE_MONTH_SECONDS = 60 * 60 * 24 * 30;

export function getSavedSchoolCode(): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${SCHOOL_CODE_COOKIE}=`));
  if (!match) return null;
  const value = decodeURIComponent(match.split("=").slice(1).join("=")).trim();
  return value || null;
}

export function saveSchoolCodeCookie(code: string): void {
  if (typeof document === "undefined") return;
  const normalized = encodeURIComponent(code.trim().toUpperCase());
  const secureFlag =
    typeof window !== "undefined" && window.location.protocol === "https:"
      ? "; Secure"
      : "";
  document.cookie = `${SCHOOL_CODE_COOKIE}=${normalized}; path=/; max-age=${ONE_MONTH_SECONDS}; SameSite=Lax${secureFlag}`;
}

export function clearSchoolCodeCookie(): void {
  if (typeof document === "undefined") return;
  document.cookie = `${SCHOOL_CODE_COOKIE}=; path=/; max-age=0; SameSite=Lax`;
}