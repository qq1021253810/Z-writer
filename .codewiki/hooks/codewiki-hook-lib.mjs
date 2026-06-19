import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } from "node:fs";
import path from "node:path";

export const STATE_DIR = path.join(".codewiki", "state");

export function option(name) {
  const index = process.argv.indexOf(name);
  if (index === -1) return undefined;
  const value = process.argv[index + 1];
  return value && !value.startsWith("--") ? value : undefined;
}

export function hookHost() {
  return option("--host") ?? process.env.CODEWIKI_HOOK_HOST ?? "unknown";
}

export function hookEvent(defaultEvent) {
  return option("--event") ?? process.env.CODEWIKI_HOOK_EVENT ?? defaultEvent;
}

export function timestamp() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, "Z");
}

export function sha256(value) {
  return createHash("sha256").update(value).digest("hex");
}

export function readStdin() {
  if (process.stdin.isTTY) {
    return { hasStdin: false, input: "" };
  }

  try {
    return { hasStdin: true, input: readFileSync(0, "utf8") };
  } catch {
    return { hasStdin: true, input: "" };
  }
}

export function readTextIfExists(filePath) {
  try {
    return existsSync(filePath) ? readFileSync(filePath, "utf8") : "";
  } catch {
    return "";
  }
}

export function ensureDir(dirPath) {
  try {
    mkdirSync(dirPath, { recursive: true });
    return true;
  } catch {
    return false;
  }
}

export function appendLine(filePath, line) {
  try {
    ensureDir(path.dirname(filePath));
    writeFileSync(filePath, `${line}\n`, { flag: "a" });
    return true;
  } catch {
    return false;
  }
}

export function logDebug({ stateDir = STATE_DIR, host, event, stage, stdinPayload, stdoutProduced, wrapperJson, observableContext, message }) {
  if (process.env.CODEWIKI_HOOK_DEBUG !== "1") return;
  if (!ensureDir(stateDir)) return;

  appendLine(
    path.join(stateDir, "hooks-debug.jsonl"),
    JSON.stringify({
      timestamp: timestamp(),
      host,
      event,
      stage,
      stdin_payload: stdinPayload,
      stdout_produced: stdoutProduced,
      wrapper_json: wrapperJson,
      observable_context: observableContext,
      message
    })
  );
}

export function normalizeSlashes(value) {
  return value.replace(/\\/g, "/").replace(/^\.\//, "");
}

export function isCodeWikiStatePath(value) {
  const normalized = normalizeSlashes(value).replace(/^\.\//, "");
  return normalized === ".codewiki/state" || normalized.startsWith(".codewiki/state/");
}

export function nonEmptyLines(value) {
  return value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
}

export function uniqueSorted(values) {
  return [...new Set(values.filter(Boolean))].sort((left, right) => left.localeCompare(right));
}

export function firstLines(value, limit) {
  return nonEmptyLines(value).slice(0, limit).join("\n");
}

export function collectStrings(value, output = []) {
  if (typeof value === "string") {
    output.push(value);
    return output;
  }

  if (Array.isArray(value)) {
    for (const item of value) collectStrings(item, output);
    return output;
  }

  if (value && typeof value === "object") {
    for (const item of Object.values(value)) collectStrings(item, output);
  }

  return output;
}

export function stableStringify(value) {
  if (Array.isArray(value)) {
    return `[${value.map((item) => stableStringify(item)).join(",")}]`;
  }

  if (value && typeof value === "object") {
    return `{${Object.keys(value)
      .sort((left, right) => left.localeCompare(right))
      .map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`)
      .join(",")}}`;
  }

  return JSON.stringify(value);
}

export function scrubVolatile(value) {
  const volatileKey = /^(callId|call_id|toolCallId|tool_call_id|requestId|request_id|invocationId|invocation_id|sessionId|session_id|timestamp|durationMs|duration_ms)$/i;

  if (Array.isArray(value)) {
    return value.map((item) => scrubVolatile(item));
  }

  if (value && typeof value === "object") {
    const next = {};
    for (const [key, item] of Object.entries(value)) {
      if (!volatileKey.test(key)) next[key] = scrubVolatile(item);
    }
    return next;
  }

  return value;
}

export function looksLikeFilePath(value) {
  const normalized = normalizeSlashes(value.trim());
  if (/^https?:\/\//i.test(normalized)) return false;
  return /(?:^|\/)[^/\r\n]+\.[A-Za-z0-9]+$/.test(normalized);
}

export function topicCandidates(files, limit = 12) {
  return uniqueSorted(
    files
      .map((file) => normalizeSlashes(file.trim()))
      .filter(looksLikeFilePath)
      .map((file) => path.posix.basename(file).replace(/\.[^.]*$/, "").replace(/[_.\s]+/g, "-"))
  ).slice(0, limit);
}

export function normalizedFileList(values, limit = 20) {
  return uniqueSorted(
    values
      .map((value) => normalizeSlashes(value.trim()))
      .filter(looksLikeFilePath)
      .filter((value) => !isCodeWikiStatePath(value))
  ).slice(0, limit);
}

export function runGit(args, cwd = process.cwd()) {
  try {
    return execFileSync("git", args, { cwd, encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] }).trim();
  } catch {
    return "";
  }
}

export function findGitRoot(cwd = process.cwd()) {
  return runGit(["rev-parse", "--show-toplevel"], cwd) || cwd;
}

export function fileContentHash(filePath, maxBytes = Number.POSITIVE_INFINITY) {
  try {
    const stat = statSync(filePath);
    if (!stat.isFile()) return "";
    if (stat.size > maxBytes) {
      return sha256(`large-file:${stat.size}:${Math.trunc(stat.mtimeMs)}`);
    }
    return sha256(readFileSync(filePath));
  } catch {
    return "";
  }
}

export function listMarkdownBaseNames(dirPath) {
  try {
    return readdirSync(dirPath, { withFileTypes: true })
      .filter((entry) => entry.isFile() && entry.name.endsWith(".md"))
      .map((entry) => entry.name.replace(/\.md$/, ""));
  } catch {
    return [];
  }
}
