#!/usr/bin/env node
import path from "node:path";

import { appendLine, fileContentHash, firstLines, hookEvent, hookHost, isCodeWikiStatePath, logDebug, normalizedFileList, readStdin, readTextIfExists, runGit, sha256, STATE_DIR, timestamp, topicCandidates, uniqueSorted } from "./codewiki-hook-lib.mjs";

const MAX_UNTRACKED_FILES = 100;
const MAX_UNTRACKED_FILE_BYTES = 1024 * 1024;

function gitDiff(args) {
  return runGit([...args, "--", ".", ":(exclude).codewiki/state/**"]);
}

function untrackedFiles() {
  return uniqueSorted(
    runGit(["ls-files", "--others", "--exclude-standard", "--", "."])
      .split(/\r?\n/)
      .map((file) => file.trim())
      .filter(Boolean)
      .filter((file) => !isCodeWikiStatePath(file))
  );
}

function untrackedStat(files) {
  return files.map((file) => `${file} | untracked`).join("\n");
}

function boundedUntracked(files) {
  return files.slice(0, MAX_UNTRACKED_FILES);
}

function diffHash(untracked, omittedUntrackedCount) {
  const untrackedMaterial = untracked
    .map((file) => `${file}\n${fileContentHash(path.resolve(file), MAX_UNTRACKED_FILE_BYTES)}`)
    .join("\n");

  return sha256([
    gitDiff(["diff", "--no-ext-diff"]),
    gitDiff(["diff", "--cached", "--no-ext-diff"]),
    untrackedMaterial,
    `omitted-untracked:${omittedUntrackedCount}`
  ].join("\n"));
}

async function main() {
  const host = hookHost();
  const event = hookEvent("session-end");
  const debugBase = { stateDir: STATE_DIR, host, event };
  const { hasStdin } = readStdin();

  const worktreeStat = gitDiff(["diff", "--stat"]);
  const worktreeFiles = gitDiff(["diff", "--name-only"]).split(/\r?\n/).filter(Boolean);
  const cachedStat = gitDiff(["diff", "--cached", "--stat"]);
  const cachedFiles = gitDiff(["diff", "--cached", "--name-only"]).split(/\r?\n/).filter(Boolean);
  const untracked = untrackedFiles();
  const includedUntracked = boundedUntracked(untracked);
  const omittedUntrackedCount = Math.max(0, untracked.length - includedUntracked.length);
  const omittedUntrackedStat = omittedUntrackedCount > 0 ? `${omittedUntrackedCount} additional untracked files omitted from hook summary` : "";
  const diffStat = [worktreeStat, cachedStat, untrackedStat(includedUntracked), omittedUntrackedStat].filter(Boolean).join("\n");

  if (!diffStat) {
    logDebug({ ...debugBase, stage: "called", stdinPayload: String(hasStdin), stdoutProduced: false, wrapperJson: "unknown", observableContext: "none", message: "no working tree, cached, or untracked diff" });
    return;
  }

  const changedFiles = normalizedFileList([...worktreeFiles, ...cachedFiles, ...includedUntracked], 20);
  const candidates = topicCandidates(changedFiles, 12);
  const hash = diffHash(includedUntracked, omittedUntrackedCount);
  const filesHash = sha256(`${changedFiles.join("\n")}\n`);
  const dedupeKey = sha256(`${host}\n${event}\n${filesHash}\n${hash}\n`);
  const dedupePath = path.join(STATE_DIR, "pending-absorb-dedupe.txt");
  const dedupe = readTextIfExists(dedupePath).split(/\r?\n/).filter(Boolean);

  if (dedupe.includes(dedupeKey)) {
    logDebug({ ...debugBase, stage: "deduped", stdinPayload: String(hasStdin), stdoutProduced: false, wrapperJson: "unknown", observableContext: "state", message: "duplicate session pending absorb signal suppressed" });
    return;
  }

  const eventRecord = {
    timestamp: timestamp(),
    source: "hook",
    host,
    event,
    reason: "session ended with uncommitted changes",
    files: changedFiles.join("\n"),
    topic_candidates: candidates.join("\n"),
    diff_stat: firstLines(diffStat, 20),
    diff_hash: hash
  };

  if (appendLine(path.join(STATE_DIR, "pending-absorb.jsonl"), JSON.stringify(eventRecord))) {
    appendLine(dedupePath, dedupeKey);
  }

  logDebug({ ...debugBase, stage: "recorded", stdinPayload: String(hasStdin), stdoutProduced: false, wrapperJson: "unknown", observableContext: "state", message: "recorded session pending absorb signal" });
}

main().catch(() => undefined);
