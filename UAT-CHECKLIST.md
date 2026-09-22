# UltiCleaner — UAT Checklist

This document is the executable companion to `FEATURES.md`: one row per feature stating the
steps to exercise it and the observable truth that proves it works. It is an internal reference
for real-machine verification, not user-facing documentation.

> Batches are dispatched at 60 rows or fewer, and a batch never spans two repositories. There are
> exactly two legitimate exits to `human-uat-pending`: a row needing the pixel layer while the
> real-client harness is not ready, and a row needing personal credentials. Every other row must
> reach `pass`, `fail`, or `blocked`. **This repository does NOT share a dispatch batch with any
> other repository.** Its four scheduled rows each impose a real wait — every one of them lowers
> its own interval config key below the shipped default first, and even so the four rows together
> impose approximately 35 seconds of pure scheduled-wait time (15s + 15s + 5s + 0s — see
> each row's own Preconditions for the exact key and lowered value), on top of the setup time each
> row's Preconditions need (spawning items and entities, moving players between worlds). Dispatching this
> repository alongside another would either force the other repository's rows to share that same
> wait window for no benefit, or force this repository's waits to serialize behind unrelated rows
> — plan 10-16 schedules `UltiCleaner` alone for exactly this reason. The one row under
> `## Lifecycle Hooks` adds roughly 25 more seconds of wait on top of those 35, plus up to about
> 40 more if its step 1 has to wait for the item countdown to restart.

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
| ulticleaner.clean.items.neg-in-progress | A large cleanup is already in progress, set up with ALL three of: (a) `batch.size: 10` in `config/cleaner.yml` (default 50, so the window a small population gives is only ~1 tick — too narrow for a human or real-client executor to reliably hit), set either before the server starts or on the running server followed by `/ul reload UltiCleaner` (which re-reads the file since `UltiKits/UltiCleaner#14`); (b) at least 200 non-whitelisted, unnamed, aged dropped items, giving roughly 20 ticks (1 second) of processing time at `batch.size: 10`; (c) `/clean items` run once, then immediately proceed to the next step | Run `/clean items` again while the first batch is still processing | Chat line reads (gloss) "Cleanup already in progress, please wait..." (yellow); no second batch starts, no double-counting occurs | server | |
| ulticleaner.clean.entities | Sender holds `ulticleaner.clean`; at least 3 entities of a configured type (`entity.types`' shipped default includes ZOMBIE) present in a non-blacklisted world, none custom-named, none leashed, none tamed; no cleanup already in progress | Run `/clean entities` | Chat line reads (gloss) "Started cleaning N entities (processing in batches)..." (green) where N is at least 3; within a few ticks all 3 entities are removed from the world and a chat line reads (gloss) "Cleaned N entities!" (green) | server | |
| ulticleaner.clean.entities.neg-in-progress | A large cleanup is already in progress (same setup as `.items.neg-in-progress` above — `batch.size: 10`, at least 200 eligible entities — using entities instead of items) | Run `/clean entities` while the first batch is still processing | Chat line reads (gloss) "Cleanup already in progress, please wait..." (yellow); no second batch starts | server | |
| ulticleaner.clean.all | Sender holds `ulticleaner.clean`; at least 1 eligible dropped item AND at least 1 eligible entity present (same eligibility rules as `.items`/`.entities` above); no cleanup already in progress | Run `/clean all` | Chat line reads (gloss) "Started cleaning M items and N entities (processing in batches)..." (green) with both M and N at least 1; the item(s) ARE removed from the world; the entity/entities are NOT removed — `isCleaningInProgress` was already `true` (set synchronously by the just-started item batch) by the time `forceCleanEntities()` ran in the same tick, so `cleanEntitiesWithBatch` returned immediately with no broadcast and no removal, even though N was reported as started. Known product defect, `UltiKits/UltiCleaner#15` | server | |
| ulticleaner.clean.check | Sender holds `ulticleaner.clean`; at least one ground item and one configured-type mob present server-wide, so the printed counts are non-trivial | Run `/clean check` | Chat prints (gloss) "=== Server Entity Statistics ===" (gold) followed by "Ground items: N" (N matching a manual count), "Cleanable mobs: M" (M matching a manual count of configured types only, regardless of whitelist exemptions), "Total entities: T" (T at least N+M), then exactly one chunk line, "Loaded chunks: C" (C matching the sum of `world.getLoadedChunks().length` over every world, printed unconditionally — it is a plain server statistic, not a cleanup readout), then a final line showing the current TPS reading (e.g. "20.00 (Normal)"). NO "Unloadable chunks" line appears: that readout went with the removed chunk-unload feature (`UltiKits/UltiCleaner#27`) | server | |
| ulticleaner.clean.status | Sender holds `ulticleaner.clean`; the scheduled item and entity cleanup tasks are both enabled (shipped default); `tps.low-reduction: 60` and `tps.critical-reduction: 70`, both set BEFORE the server starts and both deliberately NOT the shipped defaults of 30 and 50 — a run left at the defaults cannot tell the configured value from the literal this row exists to prove is gone | Run `/clean status`; then drive the server below `tps.low-threshold` (but at or above `tps.critical-threshold`) and run it again; then drive it below `tps.critical-threshold` and run it a third time | First run: chat prints (gloss) "=== Cleanup Status ===" (gold), then "Next item cleanup: S1 seconds" and "Next entity cleanup: S2 seconds" (both counting down from their own configured interval), then "Cleanup status: Idle" (gray) when no batch is running or "In progress..." (green) when one is, then the current TPS reading, and NO warning line. Second run: one further yellow warning line ending "...已降低60%" — the configured `tps.low-reduction`, not 30. Third run: one further red warning line ending "...已降低70%" — the configured `tps.critical-reduction`, not 50. Fixed, `UltiKits/UltiCleaner#21` | server | |
| ulticleaner.clean.help | Sender holds `ulticleaner.clean` | Run bare `/clean` (no arguments) | Chat prints (gloss) "=== UltiCleaner Help ===" (gold) followed by exactly five sub-command lines (items/entities/all/check/status), each with its own one-line Chinese description. NO `/clean chunks` line appears — that sub-command was removed with the chunk-unload feature (`UltiKits/UltiCleaner#27`) | server | |

## Automatic Cleanup

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ulticleaner.scheduled.item-tick | `item.enabled: true` (shipped default); `item.interval: 15` (NOT the shipped default of 300 — lowered for testability, still above the highest configured `item.warn-times` entry that will fire, 10); `item.warn-times` at its shipped default (60, 30, 10, 5, 3, 2, 1); `entity.interval` at its shipped default (600) or otherwise NOT lowered to a value that could reach zero during this row's own 15-second wait — a same-tick collision between item and entity cleanup silently drops one of them via the shared `isCleaningInProgress` flag, `UltiKits/UltiCleaner#15`; at least one eligible dropped item present (same eligibility as `ulticleaner.clean.items` above), placed BEFORE the countdown reaches 10 | Wait up to 15 seconds without running any `/clean` command | At the 10/5/3/2/1-second countdown marks, chat broadcasts the warning (gloss) "Ground items will be cleaned in {TIME} seconds!" (FIVE times — 60 and 30 are above the lowered 15-second interval and are never reached, but all of 10, 5, 3, 2 and 1 are); at 0 seconds, the eligible item(s) are removed and chat reads (gloss) "Cleaned N ground items!" (green); the countdown then restarts at 15 | server | |
| ulticleaner.scheduled.entity-tick | `entity.enabled: true` (shipped default); `entity.interval: 15` (NOT the shipped default of 600 — lowered for testability); `entity.warn-times` at its shipped default; `item.interval` RESET back to its shipped default (300), or otherwise confirmed not to reach zero during this row's own 15-second wait, if `ulticleaner.scheduled.item-tick` above lowered it in this same session — a same-tick collision between item and entity cleanup silently drops one of them via the shared `isCleaningInProgress` flag, `UltiKits/UltiCleaner#15`; at least one eligible entity present (same eligibility as `ulticleaner.clean.entities` above) | Wait up to 15 seconds without running any `/clean` command | At the 10/5/3/2/1-second countdown marks (FIVE broadcasts total, by the same count as `ulticleaner.scheduled.item-tick` above), chat broadcasts (gloss) "Entities will be cleaned in {TIME} seconds!"; at 0 seconds, the eligible entity is removed and (since the removed count is above zero) chat reads (gloss) "Cleaned N entities!" (green); the countdown then restarts at 15 | server | |
| ulticleaner.scheduled.smart-clean | `smart.enabled: true` (NOT the shipped default — set it first); `smart.item-threshold: 100` (its `@Range` minimum, NOT the shipped default of 2000 — lowered for testability); at least 101 non-whitelisted, unnamed, aged dropped items present server-wide (in a non-blacklisted world) BEFORE this row starts | Wait up to 5 seconds without running any `/clean` command | Within 5 seconds, chat broadcasts (gloss) "Detected too many entities, initiating smart cleanup..." (yellow), followed by the same item-cleanup broadcast sequence as `ulticleaner.scheduled.item-tick` above (batch removal, "Cleaned N ground items!"); the item count afterward is at or below `smart.item-threshold` | server | |
| ulticleaner.scheduled.tps-fallback | None (this repository's real-machine target is Paper 1.21.11) | Watch the server console and `/clean status`'s own TPS line for 5 seconds | No new console output attributable to `TpsAwareScheduler#updateFallbackTps` appears, and `/clean status`'s TPS reading is unaffected by whether this task ran — `ServerTypeUtil#hasTpsMethod` returns `true` on Paper, so `fallbackMonitorEnabled` is `false` and every invocation of this task returns immediately with no side effect; this task has no positive effect to observe on this repository's pinned real-machine environment, and this row's PASS criterion is that absence, not a positive log line | server | |

## Configuration

One row per `@ConfigEntity` class (D-06's config-per-file rule), not per key: `CleanerConfig`
(`config/cleaner.yml`, 34 keys), matching `FEATURES.md`'s `## Configuration` section exactly.
This row confirms every key is present at its documented default, then flips one representative
interval-style key and observes the behaviour follow — **except `messages.prefix`, which
`FEATURES.md` documents as having no observable effect (`UltiKits/UltiCleaner#18`)**, which this
row deliberately does NOT attempt to exercise for an effect.

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ulticleaner.config.cleaner-yml | Fresh `config/cleaner.yml` at its shipped default | Load the file; confirm all 34 keys listed under `FEATURES.md`'s `## Configuration` section are present at their documented defaults; then RESTART THE SERVER COMPLETELY (a full restart rather than `/ul reload UltiCleaner`, so that this row proves the value is read at boot; the reload path is proven separately by the row under `## Lifecycle Hooks` at the end of this file) and set `item.interval: 15` (default 300) before the restart, then repeat `ulticleaner.scheduled.item-tick` above, confirming the item cleanup now fires around 15 seconds later rather than 300. Do NOT vary `messages.prefix` expecting an observable effect — it has none (see its own `FEATURES.md` row and `UltiKits/UltiCleaner#18`) | All 34 keys present at their documented defaults before the change; after lowering `item.interval` to 15, the scheduled item cleanup fires (warning broadcasts, then removal) around 15 seconds later rather than 300, proving the lowered value took effect | server | |

## Lifecycle Hooks

This row exercises `UltiKits/UltiCleaner#14`'s lifecycle-hook migration: the framework's `final`
`reloadSelf()` now re-reads `config/cleaner.yml` before it calls this module's `onReload()` hook,
and that hook resets both cleanup countdowns from the reloaded values (see `FEATURES.md`'s
`## Lifecycle Hooks`). Before the migration the module's own `reloadSelf()` override skipped the
file re-read, so the countdown was reset to the OLD interval. The Expected below is chosen so that
behaviour fails the row.

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ulticleaner.lifecycle.reload | Sender is an operator (`/ul` is `requireOp = true`) and holds `ulticleaner.clean`; `item.enabled: true` and `item.interval: 300` (both shipped defaults) in `config/cleaner.yml` AS CURRENTLY LOADED — earlier rows lower `item.interval`, so set it back to 300 and then either restart or run `/ul reload UltiCleaner`; `entity.interval` at its shipped default (600); `smart.enabled: false` (shipped default) | 1. Run `/clean status` and note S1, the "Next item cleanup: S1 seconds" value. If S1 is 40 or less, wait until the countdown passes zero and restarts, then run `/clean status` once more; if S1 is still 40 or less, the loaded interval is not 300 and the precondition is not met — record `blocked`, do not keep waiting. 2. Without restarting the server, edit `config/cleaner.yml` to `item.interval: 20` and save. 3. Run `/ul reload UltiCleaner`. 4. Immediately run `/clean status` and note S2. 5. Wait 25 seconds without running any `/clean` command, then run `/clean status` and note S3. 6. Cleanup: set `item.interval` back to 300, save, and run `/ul reload UltiCleaner` again | After step 3 the sender receives (gloss) "Module UltiCleaner has been reloaded" (green), and the console shows the framework's own line (gloss) "Module 'UltiCleaner' reloaded." BEFORE this module's own (gloss) "UltiCleaner configuration has been reloaded!" line. S2 is 20 or less: the countdown was reset to the new interval, not left running down from S1 and not reset to 300. S3 is 20 or less: the countdown reached zero within about 20 seconds of the reload and restarted from the new interval of 20, not from 300. A value of S2 or S3 above 20 (in particular one near 300) is a fail. The entity countdown is also reset by the reload, to 600, so no entity cleanup can collide with the item cleanup during the wait (`UltiKits/UltiCleaner#15`) | server | |
