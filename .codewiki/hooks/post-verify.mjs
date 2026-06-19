#!/usr/bin/env node
import path from "node:path";

import { appendLine, collectStrings, hookEvent, hookHost, listMarkdownBaseNames, logDebug, normalizedFileList, readTextIfExists, scrubVolatile, sha256, stableStringify, STATE_DIR, timestamp, topicCandidates } from "./codewiki-hook-lib.mjs";

const MAX_MATERIAL_PAYLOAD_BYTES = 256 * 1024;

function fallbackStrings(payload) {
  return [...payload.matchAll(/"([^"\\]*(?:\\.[^"\\]*)*)\.[A-Za-z0-9]+"/g)].map((match) => match[0].slice(1, -1));
}

async function readBoundedStdin(maxBytes) {
  if (process.stdin.isTTY) return { hasStdin: false, input: "", totalBytes: 0, truncated: false };

  const chunks = [];
  let totalBytes = 0;

  try {
    for await (const chunk of process.stdin) {
      const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      const remaining = maxBytes - totalBytes;
      if (remaining > 0) {
        chunks.push(buffer.subarray(0, remaining));
      }
      totalBytes += buffer.length;
    }
  } catch {
    return { hasStdin: true, input: "", totalBytes: 0, truncated: false };
  }

  return {
    hasStdin: true,
    input: Buffer.concat(chunks).toString("utf8"),
    totalBytes,
    truncated: totalBytes > maxBytes
  };
}

function materialPayload(payload, parsed) {
  if (parsed !== undefined) {
    return stableStringify(scrubVolatile(parsed));
  }

  const matches = [...payload.matchAll(/"(diff|patch|changes|content|oldString|newString|old_string|new_string|before|after|output|result)"\s*:\s*"([^"\\]|\\.)*"/g)].map((match) => match[0]);
  return matches.sort((left, right) => left.localeCompare(right)).join("\n");
}

async function main() {
  const host = hookHost();
  const event = hookEvent("post-verify");
  const debugBase = { stateDir: STATE_DIR, host, event };
  const { hasStdin, input, totalBytes, truncated } = await readBoundedStdin(MAX_MATERIAL_PAYLOAD_BYTES);

  if (!hasStdin) {
    logDebug({ ...debugBase, stage: "called", stdinPayload: "false", stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "called without stdin payload" });
    return;
  }

  if (!input) {
    logDebug({ ...debugBase, stage: "called", stdinPayload: "true", stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "called with empty stdin payload" });
    return;
  }

  let parsed;
  if (!truncated) {
    try {
      parsed = JSON.parse(input);
    } catch {
      parsed = undefined;
    }
  }

  const allStrings = parsed === undefined ? fallbackStrings(input) : collectStrings(parsed);
  if (allStrings.length === 0) {
    logDebug({ ...debugBase, stage: "parsed", stdinPayload: "true", stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "payload contained no file-like strings" });
    return;
  }

  const textHaystack = allStrings.join("\n").toLowerCase();
  const matchedEntities = listMarkdownBaseNames(path.join("wiki", "entities")).filter((name) => textHaystack.includes(name.toLowerCase()));
  const files = normalizedFileList(allStrings, 20);
  const candidates = topicCandidates(allStrings, 12);

  if (matchedEntities.length === 0 && candidates.length === 0) {
    logDebug({ ...debugBase, stage: "parsed", stdinPayload: "true", stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "no wiki-relevant file signal" });
    return;
  }

  const materialPrefix = truncated ? `truncated:${totalBytes}\n` : "";
  const material = `${materialPrefix}${materialPayload(input, parsed) || files.join("\n")}`;
  const payloadHash = sha256(`${material}\n`);
  const filesHash = sha256(`${files.join("\n")}\n`);
  const dedupeKey = sha256(`${host}\n${event}\n${filesHash}\n${payloadHash}\n`);
  const dedupePath = path.join(STATE_DIR, "pending-absorb-dedupe.txt");
  const dedupe = readTextIfExists(dedupePath).split(/\r?\n/).filter(Boolean);

  if (dedupe.includes(dedupeKey)) {
    logDebug({ ...debugBase, stage: "deduped", stdinPayload: "true", stdoutProduced: false, wrapperJson: "unknown", observableContext: "state", message: "duplicate pending absorb signal suppressed" });
    return;
  }

  const eventRecord = {
    timestamp: timestamp(),
    source: "hook",
    host,
    event,
    reason: "wiki-relevant file change",
    files: files.join("\n"),
    matched_entities: matchedEntities.join("\n"),
    topic_candidates: candidates.join("\n"),
    payload_hash: payloadHash
  };

  if (appendLine(path.join(STATE_DIR, "pending-absorb.jsonl"), JSON.stringify(eventRecord))) {
    appendLine(dedupePath, dedupeKey);
  }

  logDebug({ ...debugBase, stage: "recorded", stdinPayload: "true", stdoutProduced: false, wrapperJson: "unknown", observableContext: "state", message: "recorded pending absorb signal" });
}

main().catch(() => undefined);
