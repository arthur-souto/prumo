import { describe, expect, it } from "vitest";
import { createTranslator, messagesFor, resolveLocale } from "./core";
import type { Messages, Plural, Text } from "./en";

function isPlural(value: unknown): value is Plural {
  // a group of messages may itself have a message called "other", so both forms must be texts
  return typeof value === "object" && value !== null && typeof (value as Plural).one === "string" && typeof (value as Plural).other === "string";
}

/** Every message with its key; a plural counts as one message. */
function entries(node: unknown, prefix = ""): [string, Text][] {
  return Object.entries(node as Record<string, unknown>).flatMap(([key, value]) =>
    typeof value === "string" || isPlural(value) ? [[prefix + key, value as Text]] : entries(value, `${prefix}${key}.`),
  );
}

const placeholdersOf = (text: string) => [...text.matchAll(/\{(\w+)\}/g)].map((match) => match[1]);

/** The placeholders a message can use, across its forms. */
function placeholders(text: Text) {
  const forms = typeof text === "string" ? [text] : [text.one, text.other];
  return [...new Set(forms.flatMap(placeholdersOf))].sort();
}

const catalog = (messages: Messages) => new Map(entries(messages));

describe("messages", () => {
  const en = catalog(messagesFor("en"));
  const pt = catalog(messagesFor("pt"));

  it("Portuguese has exactly the messages of English", () => {
    expect([...pt.keys()].sort()).toEqual([...en.keys()].sort());
  });

  it("each translation can fill the same placeholders as the English text", () => {
    for (const [key, text] of en) {
      expect({ key, placeholders: placeholders(pt.get(key)!) }).toEqual({ key, placeholders: placeholders(text) });
    }
  });

  it("a plural is chosen by a number the plural form actually shows", () => {
    for (const [key, text] of [...en, ...pt]) {
      if (isPlural(text)) {
        expect({ key, uses: placeholdersOf(text.other).includes(text.by ?? "count") }).toEqual({ key, uses: true });
      }
    }
  });
});

describe("createTranslator", () => {
  const t = createTranslator(messagesFor("en"));
  const pt = createTranslator(messagesFor("pt"));

  it("fills the placeholders and leaves unknown ones visible", () => {
    expect(t("story.subtaskCreated", { key: "PRU-7" })).toBe("PRU-7 created");
    expect(t("story.subtaskCreated")).toBe("{key} created");
  });

  it("falls back to the key itself when there is no text", () => {
    expect(t("story.doesNotExist" as never)).toBe("story.doesNotExist");
  });

  it("uses the singular only for exactly one, zero included in the plural", () => {
    expect(t("story.branchesCreated", { count: 1 })).toBe("1 branch created");
    expect(t("story.branchesCreated", { count: 2 })).toBe("2 branches created");
    expect(t("story.branchesCreated", { count: 0 })).toBe("0 branches created");
    expect(pt("backlog.deleted", { count: 1 })).toBe("1 história excluída");
    expect(pt("backlog.deleted", { count: 0 })).toBe("0 histórias excluídas");
  });

  it("chooses by the number named in the message when it is not the count", () => {
    expect(pt("backlog.partial", { failed: 1, count: 3 })).toBe("1 de 3 não pôde ser atualizada");
    expect(pt("backlog.partial", { failed: 2, count: 3 })).toBe("2 de 3 não puderam ser atualizadas");
    expect(t("board.subtaskProgress", { done: 0, total: 1 })).toBe("0 of 1 subtask done");
  });

  it("falls back to the plural when the number is missing", () => {
    expect(t("story.branchesCreated")).toBe("{count} branches created");
  });

  it("Portuguese agrees in number where English does not change", () => {
    expect(t("backlog.selected", { count: 1 })).toBe("1 selected");
    expect(pt("backlog.selected", { count: 1 })).toBe("1 selecionada");
    expect(pt("backlog.selected", { count: 4 })).toBe("4 selecionadas");
  });
});

describe("resolveLocale", () => {
  it("prefers the cookie, then the browser language, then English", () => {
    expect(resolveLocale("pt", "en-US")).toBe("pt");
    expect(resolveLocale(undefined, "pt-BR,pt;q=0.9")).toBe("pt");
    expect(resolveLocale("xx", null)).toBe("en");
  });
});
