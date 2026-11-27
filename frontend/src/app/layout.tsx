import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "BiblioMesh — School Library & Federated OPAC",
  description:
    "Modular school library management system with ISBN lookup, barcode circulation, and federated interlibrary loans.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es">
      <body className="min-h-screen bg-slate-950 text-slate-100 antialiased">
        {children}
      </body>
    </html>
  );
}