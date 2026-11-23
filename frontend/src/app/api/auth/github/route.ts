import { NextResponse } from "next/server";
import { publicBackendUrl } from "@/lib/backend";

export function GET() {
  return NextResponse.redirect(`${publicBackendUrl}/oauth2/authorization/github`);
}
