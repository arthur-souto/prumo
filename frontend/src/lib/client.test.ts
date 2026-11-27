import { describe, expect, it, vi } from "vitest";
import { mockFetch, sentBody } from "@/test/fetch";
import { sendJson } from "./client";

describe("sendJson", () => {
  it("sends JSON and returns the parsed answer", async () => {
    const fetch = mockFetch([{ method: "POST", path: "/api/things", status: 201, body: { id: "1" } }]);

    const result = await sendJson<{ id: string }>("/api/things", "POST", { name: "x" });

    expect(result).toMatchObject({ ok: true, status: 201, data: { id: "1" } });
    expect(sentBody(fetch)).toEqual({ name: "x" });
    expect(fetch.mock.calls[0][1]?.headers).toEqual({ "Content-Type": "application/json" });
  });

  it("sends no body nor content type without a payload and accepts an empty 204", async () => {
    const fetch = mockFetch([{ method: "DELETE", path: "/api/things/1", status: 204 }]);

    const result = await sendJson("/api/things/1", "DELETE");

    expect(result).toMatchObject({ ok: true, status: 204, data: null });
    expect(fetch.mock.calls[0][1]).toMatchObject({ headers: undefined, body: undefined });
  });

  it("keeps the error code, reason and field errors of a failed request", async () => {
    mockFetch([
      {
        method: "POST",
        path: "/api/things",
        status: 422,
        body: { code: "fixFields", reason: "BRANCH_EXISTS", fields: { name: "taken" } },
      },
    ]);

    const result = await sendJson("/api/things", "POST", {});

    expect(result).toEqual({ ok: false, status: 422, data: null, code: "fixFields", reason: "BRANCH_EXISTS", fields: { name: "taken" } });
  });

  it("reports the backend as unavailable when the request never gets an answer", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    const result = await sendJson("/api/things", "GET");

    expect(result).toMatchObject({ ok: false, status: 0, code: "unavailable" });
  });
});
