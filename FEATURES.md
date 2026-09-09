# UltiCleaner — Feature Inventory

This document catalogues every operator- or player-visible function, command, content item and
configuration key in this repository, as read directly from source. It is an internal reference
for UAT execution and issue reconciliation — the public description of these features lives on
<https://doc.ultikits.com/>. Update this file in the same pull request as any feature change.

## Conventions

- **ID grammar:** `<repo-slug>.<area>.<action>`, dot-separated, every segment lowercase ASCII
  drawn from `[a-z0-9-]`. `<repo-slug>` is the repository name lowercased with no separators —
  `ulticleaner` here. `<area>` is the feature section's slug. `<action>` is the verb.
  A `config` row is the one shape that exceeds three segments and is exempt from the
  lowercase-ASCII rule for its key-path suffix:
  `<repo-slug>.config.<file-stem>.<yml key path>`, the key path keeping its own dots and its own
  casing verbatim from the yml file — a config ID is a citation of the key, not a re-derived slug,
  so lowercasing it would make it un-greppable against its own source line. An ID changes only
  when the feature's identity changes, never on rewording. IDs are unique within a repository.
- **Kind**, exactly these eight values: `command`, `config`, `event`, `gui`, `scheduled`,
  `placeholder`, `persistence`, `gate`. Each maps one-to-one onto a reconciliation-table line.
  This module has no `event` rows (0 `@EventListener` classes and 0 `@EventHandler` methods — it
  drives everything from its own five `@Scheduled` tasks, never from a Bukkit event this module
  itself listens for), no `gui` rows (no GUI page class), no `placeholder` rows (no
  `PlaceholderExpansion`), no `persistence` rows (no `@Table` entity — every state this module
  keeps is either transient runtime state or the `config/cleaner.yml` file itself), and no `gate`
  rows (0 `@ConditionalOnConfig` sites). All five stay in the vocabulary for cross-repository
  consistency even though none appears below.
- **Tier**, exactly three: `player`, `admin`, `internal`. Judged from what the feature is for,
  not from whether it carries a permission string. `CleanCommand` is class-level
  `@CmdExecutor(permission = "ulticleaner.clean")`, so every command row below carries that one
  node, but the scheduled rows are `internal` — no player or admin directly triggers them.
- **Manual**, exactly three: `detailed`, `brief`, `none`.
- **Target**, exactly four: `player`, `console`, `both`, or `n/a`. `CleanCommand` carries no
  class-level or method-level `@CmdTarget` at all, and `BaseCommandExecutor`'s own default
  (confirmed by reading the framework's `SenderTypeValidator`) is `CONSOLE_AND_PLAYER` — every
  command row below is `both`. `n/a` is for every other Kind.
- **Permission:** the literal node string, `none`, or `n/a`. `CleanCommand` carries a single
  class-level `@CmdExecutor(permission = "ulticleaner.clean")`; none of its 7 `@CmdMapping`
  sites declares its own method-level `permission()`, so every command row below carries the
  single node `ulticleaner.clean`.
- **Source:** `ClassName#member` — the class and member that actually reads or applies the
  feature — for every Kind, `config` included: all 38 `config` rows below cite the reading
  member, or the config class's own field declaration when no reading member exists anywhere in
  this module's source.
- **Row order:** by section, then by ID ascending within the section.
- **No manual prose:** no troubleshooting column, no explanatory paragraphs, no draft page text.
  A hazard noticed while reading becomes a negative checklist row, not a note here. Where a
  feature's actual runtime behaviour genuinely diverges from what a config key or the shipped lang
  files describe it as doing, that fact is itself part of "what the feature does" and is stated
  here as a plain, sourced observation, with the filed issue number, never as advice on how to fix
  it.
- **A note on this module's actual language behaviour, read before any other row below:** this
  module ships `lang/en.yml` and `lang/zh.yml` (36 keys each, faithfully paired), but only 4 of
  those 36 keys are ever read anywhere in this module's source — confirmed by
  `grep -rn "i18n(" src/main/java`, which returns exactly 4 call sites (`UltiCleaner.java`'s
  enable/disable/reload log lines, and `ChunkUnloadService`'s one chunk-unload progress
  broadcast). Every player-facing string in `CleanCommand` (help/status/check output) is a
  hardcoded Simplified Chinese literal with NO i18n or config indirection at all. Every
  scheduled-cleanup broadcast in `CleanerService` (warnings, cleaned-count messages,
  smart-clean-triggered, progress, cancelled) is instead read from `CleanerConfig`'s own message
  fields, each defaulting to a hardcoded Chinese literal in `config/cleaner.yml`, independent of
  both the lang files and the `language` setting — these ARE operator-editable (via
  `config/cleaner.yml`), unlike `CleanCommand`'s literals, but `language: en` does not switch them
  to the shipped `lang/en.yml` wording. Filed as `UltiKits/UltiCleaner#17`. No row below carries a
  `language: en` precondition, and every quoted chat line is given as an English gloss of the
  actual (Chinese) source text, per this document's English-only rule.

### Reconciliation command family

The canonical form for counting an annotation site across this repository's real sources:

```bash
find <repo-root> -path '*/src/main/java/*' -name '*.java' -not -path '*/target/*' \
  -not -path '*/.worktrees/*' -print0 | xargs -0 grep -nE '^[[:space:]]*@AnnotationName\b' | wc -l
```

This is a single-root Maven project with no worktree directory, so neither of the two structural
traps (multi-root sources, stray `.worktrees/`) applies here; the robust `find` form is used
regardless so the same command works unmodified across all 18 repositories.

**Positive control:** the line-start form returns `@CmdExecutor` = 1, `@CmdMapping` = 7,
`@EventListener` = 0, `@EventHandler` = 0, `@Scheduled` = 5, `@ConditionalOnConfig` = 0,
`@ConfigEntity` = 1 (class), `@ConfigEntry` = 38, `@Table` = 0 — confirmed by reading
`CleanCommand.java` directly (7 `@CmdMapping` sites: `items`, `entities`, `all`, `chunks`,
`check`, `status`, bare `""`) and `CleanerConfig.java` directly (38 `@ConfigEntry` fields across
item/entity/world/smart/batch/tps/chunk/messages sections). `checkSmartClean` (line 122,
`@Scheduled(period = 100, async = false)`) is this module's standing positive control for the
`@Scheduled` count — its 5-second period is easy to conflate with `tickItemClean`/
`tickEntityClean`'s 1-second period three lines below; it is checked by name, not merely by
count, below. This document's command-row count matches the `@CmdMapping` annotation-site count
exactly (7 against 7), and its scheduled-row count matches the `@Scheduled` annotation-site count
exactly (5 against 5).

**This module fires four of its own custom Bukkit events for downstream extensibility**
(`PreItemCleanEvent`, `PreEntityCleanEvent`, `PreChunkUnloadEvent`, `CleanCompleteEvent`), but
NONE of the four is backed by an `@EventListener`/`@EventHandler` of this module's own — they are
constructed and fired (`Bukkit.getPluginManager().callEvent(...)`) directly from the
`@Scheduled`/`@CmdMapping` methods that trigger a cleanup, for OTHER plugins to listen to. Per the
Kind vocabulary's one-to-one mapping to the reconciliation table, they therefore do not get a
`event`-Kind row of their own (the `@EventListener` reconciliation line stays 0 against 0, per its
own stated reason above) — each is instead documented in prose within the `## Automatic Cleanup`
row of the method that fires it, Source-cited as `TriggeringMethod (fires EventClassName)`.

## Commands

`CleanCommand` — class-level `@CmdExecutor(alias = {"clean", "cleaner", "clear"}, permission =
"ulticleaner.clean", description = ...)` (the `description` value in source is a Chinese-only
string meaning "Clean ground items and entities"). No class-level `@CmdTarget`.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ulticleaner.clean.items | Force an immediate item cleanup, bypassing the scheduled countdown; refuses with a "cleanup in progress" message if a cleanup is already running; fires `PreItemCleanEvent` (cancellable — see below) then removes matching items in batches of `batch.size` per tick | command | `/clean items` | ulticleaner.clean | both | admin | brief | CleanCommand#cleanItems, CleanerService#forceCleanItems |
| ulticleaner.clean.entities | Force an immediate entity cleanup, bypassing the scheduled countdown; refuses with a "cleanup in progress" message if a cleanup is already running; fires `PreEntityCleanEvent` (cancellable) then removes matching entities in batches of `batch.size` per tick | command | `/clean entities` | ulticleaner.clean | both | admin | brief | CleanCommand#cleanEntities, CleanerService#forceCleanEntities |
| ulticleaner.clean.all | Force both an item and an entity cleanup in immediate succession. Known product defect: `forceCleanEntities()` is called in the SAME server tick as `forceCleanItems()`, and the two share a single `isCleaningInProgress` flag that `forceCleanItems()`'s own batch sets `true` before returning — if there is at least one ground item to clean, the entity cleanup silently never runs (its own guard clause returns immediately, with no broadcast), yet the reported entity count is still the pre-collected number, not zero. `UltiKits/UltiCleaner#15` | command | `/clean all` | ulticleaner.clean | both | admin | detailed | CleanCommand#cleanAll, CleanerService#forceCleanItems, CleanerService#forceCleanEntities |
| ulticleaner.clean.chunks | Force an immediate scan-and-unload of every chunk more than `chunk.max-distance` chunks (Chebyshev distance) from every online player in its world, skipping blacklisted worlds; refuses with a "chunk unload service not enabled" message if `chunk.enabled` is false; fires `PreChunkUnloadEvent` (cancellable) per chunk; unlike the scheduled path, this command unloads SYNCHRONOUSLY (`Chunk#unload(true)` on the main thread) even on Paper, never using the scheduled path's Paper-async-with-timeout mechanism | command | `/clean chunks` | ulticleaner.clean | both | admin | detailed | CleanCommand#cleanChunks, ChunkUnloadService#forceUnloadChunks |
| ulticleaner.clean.check | Show a live count of ground items, cleanable mobs (matching `entity.types`, regardless of whitelist exemptions), and total server-wide entities, plus (if the chunk-unload service is enabled) loaded/unloadable chunk counts, plus the current TPS reading | command | `/clean check` | ulticleaner.clean | both | admin | brief | CleanCommand#check, CleanerService#getEntityCounts, ChunkUnloadService#getTotalLoadedChunks, ChunkUnloadService#getUnloadableChunkCount, TpsAwareScheduler#getTpsStatus |
| ulticleaner.clean.status | Show seconds remaining until the next scheduled item and entity cleanup, whether a batch cleanup is currently in progress, the current TPS reading, and (when TPS is low or critical) a warning naming the percentage threshold reduction currently applied | command | `/clean status` | ulticleaner.clean | both | admin | brief | CleanCommand#status, CleanerService#getItemCountdown, CleanerService#getEntityCountdown, CleanerService#isCleaningInProgress, TpsAwareScheduler#getTpsStatus, TpsAwareScheduler#isCriticalTps, TpsAwareScheduler#isLowTps |
| ulticleaner.clean.help | Show the command list (bare `/clean` with no matching sub-format, or explicit `/clean help` since the format matcher scores the empty format as the fallback) | command | `/clean` (bare, no arguments) | ulticleaner.clean | both | player | brief | CleanCommand#help, CleanCommand#handleHelp |

## Automatic Cleanup

Five `@Scheduled` methods, none gated by `@ConditionalOnConfig` — each checks its own
`CleanerConfig` enable flag by hand at its own top and returns immediately when disabled.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ulticleaner.scheduled.item-tick | Every second, count down toward the next item cleanup; broadcast a warning at each second listed in `item.warn-times` that the countdown passes through; when the countdown reaches zero, fire `PreItemCleanEvent` (cancellable — a listener's mutations to its item-UUID list are honoured; cancelling it broadcasts `messages.clean-cancelled` and skips removal entirely) then remove matching items in batches of `batch.size` per tick, broadcast `messages.item-cleaned`, and fire `CleanCompleteEvent` (type `ITEMS`, async, non-cancellable) once the batch finishes; only active while `item.enabled` is true | scheduled | runs automatically every 20 ticks (1s, fixed) while `item.enabled` is true (shipped default); the actual cleanup fires every `item.interval` seconds (default 300) | n/a | n/a | internal | detailed | CleanerService#tickItemClean (fires PreItemCleanEvent, CleanCompleteEvent) |
| ulticleaner.scheduled.entity-tick | Identical structure to `.item-tick` for entities: countdown, warnings at `entity.warn-times`, `PreEntityCleanEvent` (cancellable) at zero, batch removal, `messages.entity-cleaned` broadcast (only if the removed count is above zero — unlike the item variant, which always broadcasts even for a zero count), `CleanCompleteEvent` (type `ENTITIES`); only active while `entity.enabled` is true | scheduled | runs automatically every 20 ticks (1s, fixed) while `entity.enabled` is true (shipped default); the actual cleanup fires every `entity.interval` seconds (default 600) | n/a | n/a | internal | detailed | CleanerService#tickEntityClean (fires PreEntityCleanEvent, CleanCompleteEvent) |
| ulticleaner.scheduled.smart-clean | Every 5 seconds, count every non-blacklisted-world item and configured-type mob server-wide; if either count exceeds its own threshold (TPS-adjusted downward when `tps.adaptive-enabled` is true and TPS is low/critical) AND `smart.cooldown` seconds have passed since the last smart trigger, broadcast `messages.smart-triggered` and fire the same `PreItemCleanEvent`/`PreEntityCleanEvent` cleanup paths as the manual/scheduled ticks above; only active while `smart.enabled` is true (NOT the shipped default — shipped `false`). Shares the same `isCleaningInProgress` defect as `.all` above when both an item AND a mob threshold are exceeded in the same check: the mob cleanup silently never runs. `UltiKits/UltiCleaner#15` | scheduled | runs automatically every 100 ticks (5s, fixed) while `smart.enabled` is true (NOT the shipped default) | n/a | n/a | internal | detailed | CleanerService#checkSmartClean (fires PreItemCleanEvent, PreEntityCleanEvent) |
| ulticleaner.scheduled.chunk-unload | Every 30 seconds, scan every loaded, non-blacklisted-world chunk more than `chunk.max-distance` chunks (Chebyshev distance) from every online player in its world (or every loaded chunk, if the world has no players at all), skip any chunk that is force-loaded, in use, missing Paper's own `isEntitiesLoaded` guarantee, or currently holding a player; fire `PreChunkUnloadEvent` (reason `DISTANCE`, cancellable) per surviving chunk, then unload it — on Paper, wrapped in a `CompletableFuture` scheduled one tick later via `Bukkit.getScheduler().runTask` with a separate `chunk.timeout`-second timeout watcher (the actual `Chunk#unload(true)` call still runs on the main thread, same as the synchronous Spigot path below; only the Future/timeout wrapper differs, not the thread the unload itself executes on), or directly and synchronously (no Future, no timeout) on plain Spigot. Never fires `CleanCompleteEvent` at all (neither this task nor the manual `.chunks` command constructs one with `CleanType.CHUNKS`, which is declared but dead). `UltiKits/UltiCleaner#16`. Only active while `chunk.enabled` is true (NOT the shipped default — shipped `false`) | scheduled | runs automatically every 600 ticks (30s, fixed) while `chunk.enabled` is true (NOT the shipped default) | n/a | n/a | internal | detailed | ChunkUnloadService#checkAndUnloadChunks (fires PreChunkUnloadEvent) |
| ulticleaner.scheduled.tps-fallback | Every second, sample a fallback TPS estimate into three rolling history windows (1/5/15 minutes) for servers whose `Bukkit.getServer()` has no `getTPS()` method. On this repository's real-machine target, Paper 1.21.11, `getTPS()` IS present, so `fallbackMonitorEnabled` is `false` at boot and every tick of this task returns immediately, doing nothing — `TpsAwareScheduler#getCurrentTps` calls the native `getTPS()` reflectively and never needs the fallback history this task would otherwise populate. This task has NO observable effect on the pinned real-machine environment; it exists only for Bukkit/Spigot builds old enough to lack `getTPS()` | scheduled | runs automatically every 20 ticks (1s, fixed); has no observable effect on Paper (native TPS is always available) | n/a | n/a | internal | brief | TpsAwareScheduler#updateFallbackTps, ServerTypeUtil#hasTpsMethod |

## Configuration

Every `@ConfigEntry`-annotated field on this module's one `@ConfigEntity` class, `CleanerConfig`
(`config/cleaner.yml`, 38 keys total — matching the reconciliation table's own `@ConfigEntry`
count of 38 exactly).

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ulticleaner.config.cleaner.batch.show-progress | Whether every online OP is sent a progress message (`messages.clean-progress`) partway through a multi-tick item/entity removal batch (or a chunk-unload batch, via a separate hardcoded lang key) | config | `config/cleaner.yml: batch.show-progress (default: false)` | n/a | n/a | admin | brief | CleanerService#removeEntitiesInBatches, ChunkUnloadService#unloadChunksInBatches |
| ulticleaner.config.cleaner.batch.size | Number of items or entities removed per server tick during a batch cleanup, and the same value reused as the per-tick removal cap for both the scheduled and manual paths | config | `config/cleaner.yml: batch.size (default: 50)` | n/a | n/a | admin | brief | CleanerService#removeEntitiesInBatches |
| ulticleaner.config.cleaner.chunk.batch-size | Number of chunks processed per server tick during a chunk-unload batch | config | `config/cleaner.yml: chunk.batch-size (default: 5)` | n/a | n/a | admin | brief | ChunkUnloadService#unloadChunksInBatches |
| ulticleaner.config.cleaner.chunk.enabled | Master switch for the chunk-unload feature — both the scheduled task and `/clean chunks` refuse to do anything while this is false | config | `config/cleaner.yml: chunk.enabled (default: false)` | n/a | n/a | admin | brief | ChunkUnloadService#checkAndUnloadChunks, CleanCommand#cleanChunks |
| ulticleaner.config.cleaner.chunk.max-distance | Chunk (not block) Chebyshev distance from the nearest player beyond which a chunk becomes an unload candidate | config | `config/cleaner.yml: chunk.max-distance (default: 20)` | n/a | n/a | admin | brief | ChunkUnloadService#isChunkFarFromAllPlayers |
| ulticleaner.config.cleaner.chunk.timeout | Seconds Paper's async chunk unload is given before this module logs a timeout warning and treats the unload as failed (the chunk itself may still complete unloading later — this only affects whether `unloadedCount` counts it) | config | `config/cleaner.yml: chunk.timeout (default: 5)` | n/a | n/a | admin | brief | ChunkUnloadService#unloadChunkAsync |
| ulticleaner.config.cleaner.entity.enabled | Master switch for scheduled entity cleanup (`.entity-tick` above); does NOT gate `/clean entities` or `/clean all`, which force a cleanup regardless of this flag | config | `config/cleaner.yml: entity.enabled (default: true)` | n/a | n/a | admin | brief | CleanerService#tickEntityClean |
| ulticleaner.config.cleaner.entity.interval | Seconds between scheduled entity cleanups | config | `config/cleaner.yml: entity.interval (default: 600)` | n/a | n/a | admin | brief | CleanerService#tickEntityClean |
| ulticleaner.config.cleaner.entity.types | Entity type names eligible for cleanup; an unrecognized name is logged as a warning at load time and silently excluded from the cache, never crashing startup | config | `config/cleaner.yml: entity.types (default: ZOMBIE, SKELETON, CREEPER, SPIDER, CAVE_SPIDER, ENDERMAN, WITCH, SLIME, PHANTOM)` | n/a | n/a | admin | brief | CleanerService#loadCaches |
| ulticleaner.config.cleaner.entity.warn-times | Seconds-remaining values, counted down from `entity.interval`, at which `entity-warn` is broadcast | config | `config/cleaner.yml: entity.warn-times (default: 60, 30, 10, 5, 3, 2, 1)` | n/a | n/a | admin | brief | CleanerService#tickEntityClean |
| ulticleaner.config.cleaner.entity.whitelist-leashed | Skip an otherwise-eligible living entity if it is currently leashed | config | `config/cleaner.yml: entity.whitelist-leashed (default: true)` | n/a | n/a | admin | brief | CleanerService#collectEntitiesToClean |
| ulticleaner.config.cleaner.entity.whitelist-named | Skip an otherwise-eligible entity if it has a custom name set | config | `config/cleaner.yml: entity.whitelist-named (default: true)` | n/a | n/a | admin | brief | CleanerService#collectEntitiesToClean |
| ulticleaner.config.cleaner.entity.whitelist-tamed | Skip an otherwise-eligible `Tameable` entity if it is currently tamed | config | `config/cleaner.yml: entity.whitelist-tamed (default: true)` | n/a | n/a | admin | brief | CleanerService#collectEntitiesToClean |
| ulticleaner.config.cleaner.item.enabled | Master switch for scheduled item cleanup (`.item-tick` above); does NOT gate `/clean items` or `/clean all`, which force a cleanup regardless of this flag | config | `config/cleaner.yml: item.enabled (default: true)` | n/a | n/a | admin | brief | CleanerService#tickItemClean |
| ulticleaner.config.cleaner.item.ignore-named | Skip an otherwise-eligible dropped item stack if its `ItemMeta` has a custom display name | config | `config/cleaner.yml: item.ignore-named (default: true)` | n/a | n/a | admin | brief | CleanerService#collectItemsToClean |
| ulticleaner.config.cleaner.item.ignore-recent | Skip a dropped item stack whose `ticksLived` is below this many seconds converted to ticks (`0` disables the check, cleaning items of any age) | config | `config/cleaner.yml: item.ignore-recent (default: 30)` | n/a | n/a | admin | brief | CleanerService#collectItemsToClean |
| ulticleaner.config.cleaner.item.interval | Seconds between scheduled item cleanups | config | `config/cleaner.yml: item.interval (default: 300)` | n/a | n/a | admin | brief | CleanerService#tickItemClean |
| ulticleaner.config.cleaner.item.warn-times | Seconds-remaining values, counted down from `item.interval`, at which `item-warn` is broadcast | config | `config/cleaner.yml: item.warn-times (default: 60, 30, 10, 5, 3, 2, 1)` | n/a | n/a | admin | brief | CleanerService#tickItemClean |
| ulticleaner.config.cleaner.item.whitelist | Item material names exempt from cleanup entirely, regardless of age or name | config | `config/cleaner.yml: item.whitelist (default: DIAMOND, EMERALD, NETHER_STAR, BEACON, ELYTRA)` | n/a | n/a | admin | brief | CleanerService#collectItemsToClean |
| ulticleaner.config.cleaner.messages.clean-cancelled | Message broadcast when a `PreItemCleanEvent`/`PreEntityCleanEvent` listener cancels a cleanup | config | `config/cleaner.yml: messages.clean-cancelled (default: a Chinese-language message meaning "Cleanup operation was cancelled by another plugin!")` | n/a | n/a | admin | brief | CleanerService#cleanItemsWithBatch, CleanerService#cleanEntitiesWithBatch |
| ulticleaner.config.cleaner.messages.clean-progress | Progress message sent to OPs partway through a multi-tick batch, with `{CURRENT}`/`{TOTAL}` placeholders, when `batch.show-progress` is true | config | `config/cleaner.yml: messages.clean-progress (default: a Chinese-language template meaning "Cleaning progress: {CURRENT}/{TOTAL}")` | n/a | n/a | admin | brief | CleanerService#removeEntitiesInBatches |
| ulticleaner.config.cleaner.messages.entity-cleaned | Message broadcast (to all players) when a scheduled/manual/smart entity cleanup removes at least one entity, with a `{COUNT}` placeholder | config | `config/cleaner.yml: messages.entity-cleaned (default: a Chinese-language template meaning "Cleaned {COUNT} entities!")` | n/a | n/a | admin | brief | CleanerService#broadcastEntityCleaned |
| ulticleaner.config.cleaner.messages.entity-warn | Warning message broadcast at each `entity.warn-times` countdown mark, with a `{TIME}` placeholder | config | `config/cleaner.yml: messages.entity-warn (default: a Chinese-language template meaning "Entities will be cleaned in {TIME} seconds!")` | n/a | n/a | admin | brief | CleanerService#broadcastEntityWarn |
| ulticleaner.config.cleaner.messages.item-cleaned | Message broadcast (to all players) when a scheduled/manual/smart item cleanup completes, with a `{COUNT}` placeholder — unlike the entity variant, this one broadcasts even when `{COUNT}` is `0` | config | `config/cleaner.yml: messages.item-cleaned (default: a Chinese-language template meaning "Cleaned {COUNT} ground items!")` | n/a | n/a | admin | brief | CleanerService#broadcastItemCleaned |
| ulticleaner.config.cleaner.messages.prefix | Declared as the message prefix ("cleaner name") shown before every broadcast; no code anywhere in this module reads `getMessagePrefix()` — every broadcast message already carries its own hardcoded prefix text inline (a green- or red-colored Chinese bracketed tag meaning "[Cleaner]"), so this key has no effect. Known product defect, `UltiKits/UltiCleaner#18` | config | `config/cleaner.yml: messages.prefix (default: a Chinese-language bracketed tag meaning "[Cleaner]", has no effect, see UltiKits/UltiCleaner#18)` | n/a | n/a | admin | brief | CleanerConfig#messagePrefix (declared, never read outside this class) |
| ulticleaner.config.cleaner.messages.smart-triggered | Message broadcast the instant smart cleanup triggers (before the actual item/entity removal begins) | config | `config/cleaner.yml: messages.smart-triggered (default: a Chinese-language message meaning "Detected too many entities, initiating smart cleanup...")` | n/a | n/a | admin | brief | CleanerService#checkSmartClean |
| ulticleaner.config.cleaner.messages.warn | Warning message broadcast at each `item.warn-times` countdown mark, with a `{TIME}` placeholder | config | `config/cleaner.yml: messages.warn (default: a Chinese-language template meaning "Ground items will be cleaned in {TIME} seconds!")` | n/a | n/a | admin | brief | CleanerService#broadcastWarn |
| ulticleaner.config.cleaner.smart.cooldown | Minimum seconds between two smart-cleanup triggers, counted from the previous trigger's own start time | config | `config/cleaner.yml: smart.cooldown (default: 60)` | n/a | n/a | admin | brief | CleanerService#checkSmartClean |
| ulticleaner.config.cleaner.smart.enabled | Master switch for smart (threshold-triggered) cleanup, independent of the scheduled interval-based cleanup above | config | `config/cleaner.yml: smart.enabled (default: false)` | n/a | n/a | admin | brief | CleanerService#checkSmartClean |
| ulticleaner.config.cleaner.smart.item-threshold | Server-wide ground-item count (across all non-blacklisted worlds) above which smart cleanup triggers for items, before any TPS-based reduction | config | `config/cleaner.yml: smart.item-threshold (default: 2000)` | n/a | n/a | admin | brief | CleanerService#checkSmartClean |
| ulticleaner.config.cleaner.smart.mob-threshold | Server-wide configured-type mob count above which smart cleanup triggers for entities, before any TPS-based reduction | config | `config/cleaner.yml: smart.mob-threshold (default: 1000)` | n/a | n/a | admin | brief | CleanerService#checkSmartClean |
| ulticleaner.config.cleaner.tps.adaptive-enabled | Master switch for TPS-based threshold reduction; when false, `getCurrentTps()` always returns `20.0` and every threshold is applied unmodified | config | `config/cleaner.yml: tps.adaptive-enabled (default: true)` | n/a | n/a | admin | brief | TpsAwareScheduler#getCurrentTps, TpsAwareScheduler#getThresholdMultiplier |
| ulticleaner.config.cleaner.tps.critical-reduction | Percentage by which a smart-cleanup threshold is reduced while TPS is BELOW (strictly less than, per `TpsAwareScheduler#isCriticalTps`'s `<` comparison, not `<=`) `tps.critical-threshold` | config | `config/cleaner.yml: tps.critical-reduction (default: 50)` | n/a | n/a | admin | brief | TpsAwareScheduler#getThresholdMultiplier |
| ulticleaner.config.cleaner.tps.critical-threshold | TPS value STRICTLY BELOW which the server is considered critically low (at exactly this value, the low — not critical — reduction applies, since `isCriticalTps` uses `<` not `<=`), applying `tps.critical-reduction` instead of `tps.low-reduction` | config | `config/cleaner.yml: tps.critical-threshold (default: 15.0)` | n/a | n/a | admin | brief | TpsAwareScheduler#isCriticalTps |
| ulticleaner.config.cleaner.tps.low-reduction | Percentage by which a smart-cleanup threshold is reduced while TPS is BELOW (strictly less than) `tps.low-threshold` (and at or above `tps.critical-threshold`) | config | `config/cleaner.yml: tps.low-reduction (default: 30)` | n/a | n/a | admin | brief | TpsAwareScheduler#getThresholdMultiplier |
| ulticleaner.config.cleaner.tps.low-threshold | TPS value STRICTLY BELOW which the server is considered low (at exactly this value, no reduction applies, since `isLowTps` uses `<` not `<=`), applying `tps.low-reduction` | config | `config/cleaner.yml: tps.low-threshold (default: 18.0)` | n/a | n/a | admin | brief | TpsAwareScheduler#isLowTps |
| ulticleaner.config.cleaner.tps.sample-window | Which rolling TPS average (`1m`/`5m`/`15m`) `getCurrentTps()` reads, from either the native `getTPS()` array or (when unavailable) the fallback history arrays | config | `config/cleaner.yml: tps.sample-window (default: 1m)` | n/a | n/a | admin | brief | TpsAwareScheduler#getTpsBySampleWindow |
| ulticleaner.config.cleaner.worlds.blacklist | World names excluded from every cleanup (item, entity) and chunk-unload scan | config | `config/cleaner.yml: worlds.blacklist (default: world_creative)` | n/a | n/a | admin | brief | CleanerService#collectItemsToClean, CleanerService#collectEntitiesToClean, ChunkUnloadService#collectChunksToUnload |
