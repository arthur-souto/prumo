import type { NextConfig } from "next";

/**
 * Dev-only access from other machines on the network. Off by default (nothing here applies to `npm run dev`):
 * `npm run dev:lan` turns it on, and going back to `npm run dev` undoes it. DEV_ALLOWED_ORIGINS adds hostnames or
 * IPs on top (comma separated; `*` stands for exactly one label, e.g. "192.168.1.*").
 */
const PRIVATE_NETWORKS = ["192.168.*.*", "10.*.*.*", "172.*.*.*"];

const lanAccess = process.env.LAN_ACCESS === "1";
const extraOrigins = (process.env.DEV_ALLOWED_ORIGINS ?? "")
  .split(",")
  .map((origin) => origin.trim())
  .filter(Boolean);

const config: NextConfig = {
  poweredByHeader: false,
  reactStrictMode: true,
  // The GitHub login runs through this server so it keeps working from other machines while the backend stays local.
  async rewrites() {
    const backend = process.env.BACKEND_URL ?? "http://localhost:8080";

    return {
      beforeFiles: [
        { source: "/oauth2/:path*", destination: `${backend}/oauth2/:path*` },
        { source: "/login/oauth2/:path*", destination: `${backend}/login/oauth2/:path*` },
      ],
      afterFiles: [],
      fallback: [],
    };
  },
  allowedDevOrigins: [...(lanAccess ? PRIVATE_NETWORKS : []), ...extraOrigins],
};

export default config;
