<!-- codewiki:start -->
## CodeWiki

This project uses [CodeWiki](https://github.com/user/codewiki) for AI-maintained project knowledge.

CodeWiki is not query-time RAG. It maintains a persistent, human-reviewed markdown wiki that compounds project knowledge across sessions. Use it as durable project memory: read/query the wiki before answering questions that depend on project history, and keep the wiki current when sources or substantial code changes add durable knowledge.

### Operating Flow
- At session start for wiki work: read `.codewiki/config.yml`, `wiki/SCHEMA.md`, `wiki/index.md`, and recent `wiki/log.md` before ingest/query/lint/absorb.
- New external source in `wiki/raw/` or user asks to process docs: use `codewiki-ingest`. Raw sources are immutable; wiki edits are proposed for review.
- User asks how the project works, why a decision was made, or where knowledge lives: use `codewiki-query` and cite wiki pages rather than inventing answers.
- User asks what to do next, resumes a session, starts a feature, or needs routing across CodeWiki workflows: use `codewiki-flow` to choose the next focused skill.
- New feature or larger change: use `codewiki-prd`, then `codewiki-tasks`, then `codewiki-process` to work one task at a time inside the generated phases.
- After a substantial coding session: run `codewiki-absorb` deliberately to capture durable lessons, entities, decisions, and issues from recent changes.
- Periodically or when drift is suspected: run `codewiki-lint` and `codewiki-breakdown` to find contradictions, stale claims, orphan pages, and missing high-signal pages.
- When setting up or auditing Obsidian usage: use `codewiki-obsidian` to keep vault structure, attachments, wikilinks, Dataview-ready frontmatter, and graph navigation compatible with CodeWiki.
- When `.codewiki/state/pending-absorb.jsonl` exists, treat it as a pending follow-up signal: invoke `codewiki-wiki-updater` for durable wiki-relevant changes, or explicitly defer the same work to `codewiki-absorb` at the next completed phase or explicit user request.
- After `codewiki-wiki-updater` proposes a non-trivial wiki change, invoke `codewiki-verifier` for read-only contradiction, reference, frontmatter, index, log, and backlink review before applying approved wiki edits.
- Hooks provide optional context and persistent change signals; they do not replace deliberate ingest/query/absorb/lint work or human approval of wiki writes.

### Schema Discipline
- Treat `wiki/SCHEMA.md` as the routing contract for page types, frontmatter, tag taxonomy, page thresholds, archive policy, index metadata, and log format.
- Raw markdown sources should preserve provenance fields such as `source_url`, `ingested`, and `sha256`; use `codewiki-ingest`/`codewiki-lint` to detect unchanged sources or source drift.
- Wiki pages should make uncertainty visible with `confidence`, `contested`, `contradictions`, and `sources` frontmatter instead of presenting weak claims as settled facts.
- New tags must be added to the schema taxonomy before use. Do not create pages for passing mentions; prefer updating existing pages unless the schema thresholds justify a new page.
- Substantial query answers can be proposed as durable pages under `wiki/queries/` or `wiki/comparisons/`; trivial lookups should stay in chat.
- Use `codewiki-verifier` for read-only review of proposed wiki changes that touch frontmatter, tags, confidence, contradictions, archive moves, index/log updates, or backlinks.

### CodeWiki Skills
- `codewiki-ingest` — Digest a raw source into the wiki
- `codewiki-query` — Search the wiki and synthesize an answer
- `codewiki-lint` — Check the wiki for contradictions, orphan pages, stale claims, and structural drift
- `codewiki-absorb` — Extract durable knowledge from recent git changes
- `codewiki-breakdown` — Find undocumented entities ranked by backlink importance
- `codewiki-obsidian` — Configure and audit the wiki as an Obsidian-compatible vault
- `codewiki-prd` — Create a product requirements document
- `codewiki-tasks` — Generate tasks from a PRD
- `codewiki-process` — Process a phase plan one task at a time
- `codewiki-flow` — Choose the right CodeWiki skill for setup, ingest, query, feature work, change-context follow-up, absorb, breakdown, and lint

Claude Code discovers these from `.claude/skills/codewiki-<name>/SKILL.md` and can invoke them through its native skill system.

### Wiki Location
- Wiki pages: `wiki/`
- Backlinks index: `wiki/_backlinks.json`
- Schema: `wiki/SCHEMA.md`
- Raw sources: `wiki/raw/`
- PRD/task workflow: `.codewiki/tasks/`
- Config: `.codewiki/config.yml`
- Pending absorb state: `.codewiki/state/pending-absorb.jsonl`

### Hooks
Shared CodeWiki hook scripts are installed under `.codewiki/hooks/`. Claude Code wires them through `.claude/settings.json`.

- `PreToolUse` and `PostToolUse` run on `Write|Edit` for short optional context and persistent pending-absorb signals. Hook output is advisory and may not be delivered by every host/runtime; pending signals are deduped in shared state.
- `.codewiki/hooks/session-end.mjs` ships as a shared asset but is not wired automatically in v1. Use `codewiki-absorb` deliberately at the end of a substantial session.
<!-- codewiki:end -->
