# UltiCleaner — UAT Checklist

This document is the executable companion to `FEATURES.md`: one row per feature stating the
steps to exercise it and the observable truth that proves it works. It is an internal reference
for real-machine verification, not user-facing documentation.

> Batches are dispatched at 60 rows or fewer, and a batch never spans two repositories. There are
> exactly two legitimate exits to `human-uat-pending`: a row needing the pixel layer while the
> real-client harness is not ready, and a row needing personal credentials. Every other row must
> reach `pass`, `fail`, or `blocked`. **This repository does NOT share a dispatch batch with any
> other repository.** Its five scheduled rows each impose a real wait — every one of them lowers
> its own interval config key below the shipped default first, and even so the five rows together
> impose approximately 65 seconds of pure scheduled-wait time (15s + 15s + 5s + 30s + 0s — see
> each row's own Preconditions for the exact key and lowered value), on top of the setup time each
> row's Preconditions need (spawning items/entities, moving chunks out of range). Dispatching this
> repository alongside another would either force the other repository's rows to share that same
> wait window for no benefit, or force this repository's waits to serialize behind unrelated rows
> — plan 10-16 schedules `UltiCleaner` alone for exactly this reason.

## Conventions

- **Columns:** `ID`, `Preconditions`, `Steps`, `Expected`, `Layer`, `Covers`.
- **ID:** cites its `FEATURES.md` ID verbatim. A negative case suffixes the checklist ID only,
  as `.neg-<slug>` — a negative case still tests the same feature, so the base ID is unchanged.
- **Layer**, copied verbatim from Laojun's own `ultitools-real-client-uat` skill so no
  translation step exists at dispatch time: `protocol`, `java-client`, `os-input`, `pixel`,
  `server`, `human`.
- **Human-authenticated-session rows (D-27b):** a row whose Steps can only be exercised through
  the maintainer's own authenticated UltiCloud panel session carries the fixed Preconditions
  phrase `maintainer-authenticated UltiCloud panel session (personal credentials)` and Layer
  `human`. This repository has no such row — it has no panel-capability surface of its own — so
  none is currently affected; the convention is stated here for template consistency.
- **Expected** must name an observable truth — an exact chat line, a log line, a database row,
  an inventory slot — and never the words "it works". Where `FEATURES.md` documents a key or task
  as having no observable effect (`messages.prefix`, `tps-fallback` on Paper), the corresponding
  Expected states that absence explicitly, as its own observable claim, rather than being silently
  skipped.
- **Covers** back-references a Phase 9 GUI-excluded class name; left blank when no such class
  applies. This repository has NO entry in `.planning/phases/09-module-ecosystem-readiness-and-
  test-coverage/gui-exclusions/` at all (it ships no GUI page of any generation), so every row's
  `Covers` cell below is blank — a structural fact, not an oversight.
- A row whose Preconditions name a prior CHECKLIST row must appear after that row in file order —
  asserted mechanically: for every row, every checklist ID cited in its Preconditions cell must
  have a strictly smaller line number in this file than the row citing it (sweep class 8, D-27a).
- **Config-per-file rule (D-06):** one checklist row per `@ConfigEntity`-annotated class, never
  one row per key. This module ships exactly one `@ConfigEntity` (`CleanerConfig`,
  `config/cleaner.yml`), so exactly one config row exists below (ID suffixed `-yml`,
  `ulticleaner.config.cleaner-yml`). This module ships no Maven-filtered (build-time) config file,
  so the build-time-property clause of this rule does not apply to any row below.
- **This module's actual language behaviour (read `FEATURES.md`'s own Conventions note in full
  before any row below):** `language: en` has no effect on `CleanCommand`'s output at all (every
  string is a hardcoded Chinese literal) and no effect on `CleanerService`'s scheduled broadcasts
  either (they read `CleanerConfig`'s own Chinese-default message fields, not the lang files). No
  row below carries a `language: en` precondition; every Expected quotation is given as an English
  gloss of the actual (Chinese) text the handler emits. Filed as `UltiKits/UltiCleaner#17`.
- **Scheduled-row wait discipline:** every scheduled row's Preconditions name the exact
  `config/cleaner.yml` key that shortens its interval below the shipped default, and the value to
  set it to, so the executor never has to guess a wait. A row with no stated wait would be a row
  the executor cannot run without guessing, and a guessed wait produces a false `blocked` — see
  the plan's own instruction for this repository.

## Commands

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ulticleaner.clean.items | Sender holds `ulticleaner.clean`; at least 3 distinguishable dropped item stacks on the ground in a non-blacklisted world, each older than `item.ignore-recent` seconds (30, shipped default — wait that long after dropping, or lower the key first), none a whitelisted material (`item.whitelist`'s shipped default: DIAMOND, EMERALD, NETHER_STAR, BEACON, ELYTRA), none carrying a custom display name; no cleanup already in progress | Run `/clean items` | Chat line reads (gloss) "Started cleaning N ground items (processing in batches)..." (green) where N is at least 3; within `batch.size` (50, shipped default) ticks — effectively immediate for 3 items — all 3 stacks are removed from the world and a chat line reads (gloss) "Cleaned N ground items!" (green) | server | |
| ulticleaner.clean.items.neg-in-progress | A large cleanup is already in progress (drop at least `batch.size + 1` = 51 non-whitelisted, unnamed, aged items, then run `/clean items` once and immediately proceed to the next step before the multi-tick batch can finish) | Run `/clean items` again while the first batch is still processing | Chat line reads (gloss) "Cleanup already in progress, please wait..." (yellow); no second batch starts, no double-counting occurs | server | |
| ulticleaner.clean.entities | Sender holds `ulticleaner.clean`; at least 3 entities of a configured type (`entity.types`' shipped default includes ZOMBIE) present in a non-blacklisted world, none custom-named, none leashed, none tamed; no cleanup already in progress | Run `/clean entities` | Chat line reads (gloss) "Started cleaning N entities (processing in batches)..." (green) where N is at least 3; within a few ticks all 3 entities are removed from the world and a chat line reads (gloss) "Cleaned N entities!" (green) | server | |
| ulticleaner.clean.entities.neg-in-progress | A large cleanup is already in progress (same setup as `.items.neg-in-progress` above, using entities instead of items) | Run `/clean entities` while the first batch is still processing | Chat line reads (gloss) "Cleanup already in progress, please wait..." (yellow); no second batch starts | server | |
| ulticleaner.clean.all | Sender holds `ulticleaner.clean`; at least 1 eligible dropped item AND at least 1 eligible entity present (same eligibility rules as `.items`/`.entities` above); no cleanup already in progress | Run `/clean all` | Chat line reads (gloss) "Started cleaning M items and N entities (processing in batches)..." (green) with both M and N at least 1; the item(s) ARE removed from the world; the entity/entities are NOT removed — `isCleaningInProgress` was already `true` (set synchronously by the just-started item batch) by the time `forceCleanEntities()` ran in the same tick, so `cleanEntitiesWithBatch` returned immediately with no broadcast and no removal, even though N was reported as started. Known product defect, `UltiKits/UltiCleaner#15` | server | |
| ulticleaner.clean.chunks | Sender holds `ulticleaner.clean`; `chunk.enabled: true` (NOT the shipped default — set it first, and either re-run `/ul reload` or restart so the change is picked up); at least one loaded chunk in a non-blacklisted world more than `chunk.max-distance` chunks (lower to 5, its minimum, for testability) from every online player, not force-loaded, not in use, holding no player | Run `/clean chunks` | Chat line reads (gloss) "Unloaded N idle chunks!" (green) with N at least 1; `world.getLoadedChunks()` immediately reflects the chunk as no longer loaded (compare `ulticleaner.clean.check`'s "Loaded chunks" line, the English gloss of its printed Chinese label, before and after) | server | |
| ulticleaner.clean.chunks.neg-disabled | Sender holds `ulticleaner.clean`; `chunk.enabled: false` (the shipped default) | Run `/clean chunks` | Chat line reads (gloss) "Chunk unload service not enabled!" (red); no chunk is scanned or unloaded | server | |
| ulticleaner.clean.check | Sender holds `ulticleaner.clean`; at least one ground item and one configured-type mob present server-wide, so the printed counts are non-trivial | Run `/clean check` | Chat prints (gloss) "=== Server Entity Statistics ===" (gold) followed by "Ground items: N" (N matching a manual count), "Cleanable mobs: M" (M matching a manual count of configured types only, regardless of whitelist exemptions), "Total entities: T" (T at least N+M); if `chunk.enabled` is true, two further lines for loaded/unloadable chunk counts; a final line showing the current TPS reading (e.g. "20.00 (Normal)") | server | |
| ulticleaner.clean.status | Sender holds `ulticleaner.clean`; the scheduled item and entity cleanup tasks are both enabled (shipped default) | Run `/clean status` | Chat prints (gloss) "=== Cleanup Status ===" (gold), then "Next item cleanup: S1 seconds" and "Next entity cleanup: S2 seconds" (both counting down from their own configured interval), then "Cleanup status: Idle" (gray) when no batch is running or "In progress..." (green) when one is, then the current TPS reading; if TPS is below `tps.low-threshold`/`tps.critical-threshold`, an additional warning line names the applied percentage reduction | server | |
| ulticleaner.clean.help | Sender holds `ulticleaner.clean` | Run bare `/clean` (no arguments) | Chat prints (gloss) "=== UltiCleaner Help ===" (gold) followed by one line per sub-command (items/entities/all/chunks/check/status), each with its own one-line Chinese description | server | |

## Automatic Cleanup

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ulticleaner.scheduled.item-tick | `item.enabled: true` (shipped default); `item.interval: 15` (NOT the shipped default of 300 — lowered for testability, still above the highest configured `item.warn-times` entry that will fire, 10); `item.warn-times` at its shipped default (60, 30, 10, 5, 3, 2, 1); at least one eligible dropped item present (same eligibility as `ulticleaner.clean.items` above), placed BEFORE the countdown reaches 10 | Wait up to 15 seconds without running any `/clean` command | At the 10/5/3/2/1-second countdown marks, chat broadcasts the warning (gloss) "Ground items will be cleaned in {TIME} seconds!" (four times, once per mark reached, since 60 and 30 are above the lowered 15-second interval and are never reached); at 0 seconds, the eligible item(s) are removed and chat reads (gloss) "Cleaned N ground items!" (green); the countdown then restarts at 15 | server | |
| ulticleaner.scheduled.entity-tick | `entity.enabled: true` (shipped default); `entity.interval: 15` (NOT the shipped default of 600 — lowered for testability); `entity.warn-times` at its shipped default; at least one eligible entity present (same eligibility as `ulticleaner.clean.entities` above) | Wait up to 15 seconds without running any `/clean` command | At the 10/5/3/2/1-second countdown marks, chat broadcasts (gloss) "Entities will be cleaned in {TIME} seconds!"; at 0 seconds, the eligible entity is removed and (since the removed count is above zero) chat reads (gloss) "Cleaned N entities!" (green); the countdown then restarts at 15 | server | |
| ulticleaner.scheduled.smart-clean | `smart.enabled: true` (NOT the shipped default — set it first); `smart.item-threshold: 100` (its `@Range` minimum, NOT the shipped default of 2000 — lowered for testability); at least 101 non-whitelisted, unnamed, aged dropped items present server-wide (in a non-blacklisted world) BEFORE this row starts | Wait up to 5 seconds without running any `/clean` command | Within 5 seconds, chat broadcasts (gloss) "Detected too many entities, initiating smart cleanup..." (yellow), followed by the same item-cleanup broadcast sequence as `ulticleaner.scheduled.item-tick` above (batch removal, "Cleaned N ground items!"); the item count afterward is at or below `smart.item-threshold` | server | |
| ulticleaner.scheduled.chunk-unload | `chunk.enabled: true` (NOT the shipped default — set it first); `chunk.max-distance: 5` (its `@Range` minimum, NOT the shipped default of 20 — lowered for testability); at least one loaded, non-blacklisted-world chunk more than 5 chunks (Chebyshev) from every online player, not force-loaded, not in use, holding no player, present BEFORE this row starts; note `world.getLoadedChunks().length` beforehand (`ulticleaner.clean.check` above) | Wait up to 30 seconds without running `/clean chunks` | Within 30 seconds, the qualifying chunk(s) are unloaded — `world.getLoadedChunks().length` (re-read via `ulticleaner.clean.check` above) has decreased by at least 1; if `batch.show-progress` is true, an OP receives a progress message during a multi-chunk batch | server | |
| ulticleaner.scheduled.tps-fallback | None (this repository's real-machine target is Paper 1.21.11) | Watch the server console and `/clean status`'s own TPS line for 5 seconds | No new console output attributable to `TpsAwareScheduler#updateFallbackTps` appears, and `/clean status`'s TPS reading is unaffected by whether this task ran — `ServerTypeUtil#hasTpsMethod` returns `true` on Paper, so `fallbackMonitorEnabled` is `false` and every invocation of this task returns immediately with no side effect; this task has no positive effect to observe on this repository's pinned real-machine environment, and this row's PASS criterion is that absence, not a positive log line | server | |

## Configuration

One row per `@ConfigEntity` class (D-06's config-per-file rule), not per key: `CleanerConfig`
(`config/cleaner.yml`, 38 keys), matching `FEATURES.md`'s `## Configuration` section exactly.
This row confirms every key is present at its documented default, then flips one representative
interval-style key and observes the behaviour follow — **except `messages.prefix`, which
`FEATURES.md` documents as having no observable effect (`UltiKits/UltiCleaner#18`)**, which this
row deliberately does NOT attempt to exercise for an effect.

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ulticleaner.config.cleaner-yml | Fresh `config/cleaner.yml` at its shipped default | Load the file; confirm all 38 keys listed under `FEATURES.md`'s `## Configuration` section are present at their documented defaults; then set `item.interval: 15` (default 300) and repeat `ulticleaner.scheduled.item-tick` above, confirming the item cleanup now fires around 15 seconds later rather than 300. Do NOT vary `messages.prefix` expecting an observable effect — it has none (see its own `FEATURES.md` row and `UltiKits/UltiCleaner#18`) | All 38 keys present at their documented defaults before the change; after lowering `item.interval` to 15, the scheduled item cleanup fires (warning broadcasts, then removal) around 15 seconds later rather than 300, proving the lowered value took effect | server | |
