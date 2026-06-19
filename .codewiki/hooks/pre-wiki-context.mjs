#!/usr/bin/env node
import path from "node:path";

import { appendLine, hookEvent, hookHost, logDebug, nonEmptyLines, readStdin, readTextIfExists, sha256, STATE_DIR, timestamp } from "./codewiki-hook-lib.mjs";

async function main() {
  const host = hookHost();
  const event = hookEvent("pre-wiki-context");
  const indexPath = path.join("wiki", "index.md");
  const debugBase = { stateDir: STATE_DIR, host, event };

  if (!readTextIfExists(indexPath)) {
    logDebug({ ...debugBase, stage: "called", stdinPayload: "unknown", stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "wiki index missing" });
    return;
  }

  const { hasStdin, input } = readStdin();
  if (!input) {
    logDebug({ ...debugBase, stage: "called", stdinPayload: String(hasStdin), stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "called without prompt payload" });
    return;
  }

  const intentMatches = [...input.matchAll(/codewiki|wiki|ingest|query|lint|absorb|obsidian|lesson|lessons/gi)].map((match) => match[0].toLowerCase());
  if (intentMatches.length === 0) {
    logDebug({ ...debugBase, stage: "filtered", stdinPayload: String(hasStdin), stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "prompt did not request wiki context" });
    return;
  }

  const indexText = readTextIfExists(indexPath);
  const indexLines = indexText.split(/\r?\n/);
  const terms = [...new Set([...input.matchAll(/[A-Za-z][A-Za-z0-9_-]+/g)].map((match) => match[0]))].sort((left, right) => left.localeCompare(right)).slice(0, 20);
  const matchedLines = [];
  let emitted = 0;

  for (const term of terms) {
    const lowerTerm = term.toLowerCase();
    const matches = indexLines.filter((line) => line.toLowerCase().includes(lowerTerm)).slice(0, 3);
    if (matches.length > 0) {
      matchedLines.push(...matches);
      emitted += 1;
    }
    if (emitted >= 5) break;
  }

  let context = "## CodeWiki Context\n\nRelevant wiki index entries only; hooks are advisory and may not be delivered by every host.\n\n";
  context += matchedLines.join("\n");

  const pending = readTextIfExists(path.join(STATE_DIR, "pending-absorb.jsonl"));
  const pendingCount = nonEmptyLines(pending).length;
  if (pendingCount !== 0) {
    context += `\nPending CodeWiki absorb signals: ${pendingCount}. Read .codewiki/state/pending-absorb.jsonl before absorb.\n`;
  }

  const intentTerms = [...new Set(intentMatches)].sort((left, right) => left.localeCompare(right)).join(" ");
  const fingerprint = sha256(`${host}\n${event}\n${intentTerms}\n${context}\n`);
  const cachePath = path.join(STATE_DIR, "pre-wiki-context-cache.txt");
  const cache = nonEmptyLines(readTextIfExists(cachePath));

  if (process.env.CODEWIKI_HOOK_CONTEXT_BYPASS !== "1" && cache.includes(fingerprint)) {
    logDebug({ ...debugBase, stage: "deduped", stdinPayload: String(hasStdin), stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "context fingerprint already emitted" });
    return;
  }

  if (process.env.CODEWIKI_HOOK_CONTEXT_BYPASS !== "1") {
    appendLine(cachePath, fingerprint);
  }

  process.stdout.write(`${context}\n`);
  logDebug({ ...debugBase, stage: "emitted", stdinPayload: String(hasStdin), stdoutProduced: true, wrapperJson: "unknown", observableContext: "advisory", message: `emitted short wiki context at ${timestamp()}` });
}

main().catch(() => undefined);
