import { describe, expect, it } from "vitest";
import { createTranslator, messagesFor, resolveLocale } from "./core";

function keys(node: unknown, prefix = ""): string[] {
  return Object.entries(node as Record<string, unknown>).flatMap(([key, value]) =>
    typeof value === "string" ? [prefix + key] : keys(value, `${prefix}${key}.`),
  );
}

function placeholders(text: string) {
  return [...text.matchAll(/\{(\w+)\}/g)].map((match) => match[1]).sort();
}

describe("messages", () => {
  it("Portuguese has exactly the keys of English", () => {
    expect(keys(messagesFor("pt")).sort()).toEqual(keys(messagesFor("en")).sort());
  });

  it("each translation keeps the placeholders of the English text", () => {
    const en = createTranslator(messagesFor("en"));
    const pt = createTranslator(messagesFor("pt"));

    for (const key of keys(messagesFor("en"))) {
      expect({ key, placeholders: placeholders(pt(key as never)) }).toEqual({ key, placeholders: placeholders(en(key as never)) });
    }
  });
});

describe("createTranslator", () => {
  const t = createTranslator(messagesFor("en"));

  it("fills the placeholders and leaves unknown ones visible", () => {
    expect(t("story.subtaskCreated", { key: "PRU-7" })).toBe("PRU-7 created");
    expect(t("story.subtaskCreated")).toBe("{key} created");
  });

  it("falls back to the key itself when there is no text", () => {
    expect(t("story.doesNotExist" as never)).toBe("story.doesNotExist");
  });
});

describe("resolveLocale", () => {
  it("prefers the cookie, then the browser language, then English", () => {
    expect(resolveLocale("pt", "en-US")).toBe("pt");
    expect(resolveLocale(undefined, "pt-BR,pt;q=0.9")).toBe("pt");
    expect(resolveLocale("xx", null)).toBe("en");
  });
});
