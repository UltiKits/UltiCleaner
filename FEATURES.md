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
  `placeholder`, `persistence`, `gate`. Each maps onto a reconciliation-table line, with one
  documented exception: a framework lifecycle-hook row (`## Lifecycle Hooks`) is `event`-Kind but
  no reconciliation line counts it.
  This module has 0 `@EventListener` classes and 0 `@EventHandler` methods — it drives everything
  from its own four `@Scheduled` tasks, never from a Bukkit event this module itself listens for —
  so no `event` row is backed by a listener annotation site and the `@EventListener`
  reconciliation line below stays 0 against 0. It carries exactly two `event`-Kind rows, both in
  `## Lifecycle Hooks` below: `ulticleaner.lifecycle.reload` and
  `ulticleaner.lifecycle.removed-key-warning`. Both are driven from callbacks the framework
  invokes (`registerSelf()` and `onReload()`), not from a command this module maps or a config key
  it reads, and both are reached by overriding a framework method rather than through an
  annotation site, so no reconciliation line counts either. This module has no `gui` rows (no GUI page class), no `placeholder` rows (no
  `PlaceholderExpansion`), no `persistence` rows (no `@Table` entity — every state this module
  keeps is either transient runtime state or the `config/cleaner.yml` file itself), and no `gate`
  rows (0 `@ConditionalOnConfig` sites). All four stay in the vocabulary for cross-repository
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
  feature — for every Kind, `config` included: all 33 `config` rows below cite the reading
  member, or the config class's own field declaration when no reading member exists anywhere in
  this module's source.
- **Row order:** by section, then by ID ascending within the section.
- **No manual prose:** no troubleshooting column, no explanatory paragraphs, no draft page text.
  A hazard noticed while reading becomes a negative checklist row, not a note here. Where a
  feature's actual runtime behaviour genuinely diverges from what a config key or the shipped lang
  files describe it as doing, that fact is itself part of "what the feature does" and is stated
  here as a plain, sourced observation, with the filed issue number, never as advice on how to fix
  it.
- **Language:** every line this module shows a player or writes to the console follows the
  framework's `language` setting. `CleanCommand`'s output, the command description and the console
  lines come from `lang/en.yml` / `lang/zh.yml`. The seven scheduled-cleanup broadcasts
  (`messages.*` in `config/cleaner.yml`) are written into the file in the server's language while
  they are still built-in text, and the module broadcasts exactly what the file holds
  (`ulticleaner.lifecycle.legacy-message-defaults`); an edited value is kept. On upgrade, a value that
  is exactly the Chinese default an earlier version shipped is rewritten in the server's language at
  start-up (and on `/ul reload`). Before 6.3.0 none of
  this followed `language` (`UltiKits/UltiCleaner#17`). Rows below that quote a chat or console line
  carry a `language: en` precondition and quote the English text.

### Reconciliation command family

The canonical form for counting an annotation site across this repository's real sources:

```bash
find <repo-root> -path '*/src/main/java/*' -name '*.java' -not -path '*/target/*' \
  -not -path '*/.worktrees/*' -print0 | xargs -0 grep -nE '^[[:space:]]*@AnnotationName\b' | wc -l
```

This is a single-root Maven project with no worktree directory, so neither of the two structural
traps (multi-root sources, stray `.worktrees/`) applies here; the robust `find` form is used
regardless so the same command works unmodified across all 18 repositories.

**Positive control:** the line-start form returns `@CmdExecutor` = 1, `@CmdMapping` = 6,
`@EventListener` = 0, `@EventHandler` = 0, `@Scheduled` = 4, `@ConditionalOnConfig` = 0,
`@ConfigEntity` = 1 (class), `@ConfigEntry` = 33, `@Table` = 0 — confirmed by reading
`CleanCommand.java` directly (6 `@CmdMapping` sites: `items`, `entities`, `all`,
`check`, `status`, bare `""`) and `CleanerConfig.java` directly (33 `@ConfigEntry` fields across
item/entity/world/smart/batch/tps/messages sections). `checkSmartClean` (line 122,
`@Scheduled(period = 100, async = false)`) is this module's standing positive control for the
`@Scheduled` count — its 5-second period is easy to conflate with `tickItemClean`/
`tickEntityClean`'s 1-second period three lines below; it is checked by name, not merely by
count, below. This document's command-row count matches the `@CmdMapping` annotation-site count
exactly (6 against 6), and its scheduled-row count matches the `@Scheduled` annotation-site count
exactly (4 against 4).

**This module fires three of its own custom Bukkit events for downstream extensibility**
(`PreItemCleanEvent`, `PreEntityCleanEvent`, `CleanCompleteEvent`), but
NONE of the three is backed by an `@EventListener`/`@EventHandler` of this module's own — they are
constructed and fired (`Bukkit.getPluginManager().callEvent(...)`) directly from the
`@Scheduled`/`@CmdMapping` methods that trigger a cleanup, for OTHER plugins to listen to. Per the
Kind vocabulary's mapping to the reconciliation table (whose only exception is the framework-invoked
lifecycle-hook override under `## Lifecycle Hooks`, which is not a Bukkit event), they therefore do not get a
`event`-Kind row of their own (the `@EventListener` reconciliation line stays 0 against 0, per its
own stated reason above) — each is instead documented in prose within the `## Automatic Cleanup`
row of the method that fires it, Source-cited as `TriggeringMethod (fires EventClassName)`.

**Two of `CleanCompleteEvent.CleanType`'s four constants are never constructed**, so a listener's
`switch` arm for either can never execute. `CleanType.ALL` is never constructed because `/clean all`
runs an item cleanup and an entity cleanup in succession, each firing its own event with its own
type. `CleanType.CHUNKS` was never constructed either, and is now **permanently unconstructible**:
the chunk-unload feature that was its only conceivable producer was removed from this module
(`UltiKits/UltiCleaner#27`). Both are tracked by `UltiKits/UltiCleaner#16`, which owns the decision
about removing them — the constants are left in place here because removing one from a published
enum is that issue's call, not this change's. This paragraph replaces the same statement that used
to live in the deleted `ulticleaner.scheduled.chunk-unload` row.

## Commands

`CleanCommand` — class-level `@CmdExecutor(alias = {"clean", "cleaner", "clear"}, permission =
"ulticleaner.clean", description = "command_description")`, a language key the framework translates
("Clean up ground items and entities" under `language: en`). No class-level `@CmdTarget`.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ulticleaner.clean.items | Force an immediate item cleanup, bypassing the scheduled countdown; refuses with a "cleanup in progress" message if a cleanup is already running; fires `PreItemCleanEvent` (cancellable — see below) then removes matching items in batches of `batch.size` per tick | command | `/clean items` | ulticleaner.clean | both | admin | brief | CleanCommand#cleanItems, CleanerService#forceCleanItems |
| ulticleaner.clean.entities | Force an immediate entity cleanup, bypassing the scheduled countdown; refuses with a "cleanup in progress" message if a cleanup is already running; fires `PreEntityCleanEvent` (cancellable) then removes matching entities in batches of `batch.size` per tick | command | `/clean entities` | ulticleaner.clean | both | admin | brief | CleanCommand#cleanEntities, CleanerService#forceCleanEntities |
| ulticleaner.clean.all | Force both an item and an entity cleanup in immediate succession. Known product defect: `forceCleanEntities()` is called in the SAME server tick as `forceCleanItems()`, and the two share a single `isCleaningInProgress` flag that `forceCleanItems()`'s own batch sets `true` before returning — if there is at least one ground item to clean, the entity cleanup silently never runs (its own guard clause returns immediately, with no broadcast), yet the reported entity count is still the pre-collected number, not zero. `UltiKits/UltiCleaner#15` | command | `/clean all` | ulticleaner.clean | both | admin | detailed | CleanCommand#cleanAll, CleanerService#forceCleanItems, CleanerService#forceCleanEntities |
| ulticleaner.clean.check | Show a live count of ground items, cleanable mobs (matching `entity.types`, regardless of whitelist exemptions), total server-wide entities, the number of loaded chunks across every world, and the current TPS reading. The loaded-chunk figure is a plain server statistic, unconditional and unrelated to any cleanup this module performs; the former "unloadable chunks" line beside it was a readout of the chunk-unload feature and went with it (`UltiKits/UltiCleaner#27`) | command | `/clean check` | ulticleaner.clean | both | admin | brief | CleanCommand#check, CleanerService#getEntityCounts, CleanerService#getTotalLoadedChunks, TpsAwareScheduler#getTpsStatus |
| ulticleaner.clean.status | Show seconds remaining until the next scheduled item and entity cleanup, whether a batch cleanup is currently in progress, the current TPS reading, and (when TPS is low or critical) a warning line. The warning names the actually-configured percentage, read from `tps.critical-reduction` or `tps.low-reduction` — the same value `TpsAwareScheduler#getThresholdMultiplier` applies — so changing either key changes this line (fixed, `UltiKits/UltiCleaner#21`) | command | `/clean status` | ulticleaner.clean | both | admin | detailed | CleanCommand#status, CleanerService#getItemCountdown, CleanerService#getEntityCountdown, CleanerService#isCleaningInProgress, TpsAwareScheduler#getTpsStatus, TpsAwareScheduler#isCriticalTps, TpsAwareScheduler#isLowTps, CleanerConfig#getLowTpsReduction, CleanerConfig#getCriticalTpsReduction |
| ulticleaner.clean.help | Show the command list — five sub-commands (`items`, `entities`, `all`, `check`, `status`) since `chunks` was removed with the chunk-unload feature (`UltiKits/UltiCleaner#27`) — printed for a bare `/clean` with no matching sub-format, or for an explicit `/clean help` since the format matcher scores the empty format as the fallback | command | `/clean` (bare, no arguments) | ulticleaner.clean | both | player | brief | CleanCommand#help, CleanCommand#handleHelp |

## Automatic Cleanup

Four `@Scheduled` methods, none gated by `@ConditionalOnConfig` — each checks its own
`CleanerConfig` enable flag by hand at its own top and returns immediately when disabled.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ulticleaner.scheduled.item-tick | Every second, count down toward the next item cleanup; broadcast a warning at each second listed in `item.warn-times` that the countdown passes through; when the countdown reaches zero, fire `PreItemCleanEvent` (cancellable — a listener's mutations to its item-UUID list are honoured; cancelling it broadcasts `messages.clean-cancelled` and skips removal entirely) then remove matching items in batches of `batch.size` per tick, broadcast `messages.item-cleaned`, and fire `CleanCompleteEvent` (type `ITEMS`, async, non-cancellable) once the batch finishes; only active while `item.enabled` is true | scheduled | runs automatically every 20 ticks (1s, fixed) while `item.enabled` is true (shipped default); the actual cleanup fires every `item.interval` seconds (default 300) | n/a | n/a | internal | detailed | CleanerService#tickItemClean (fires PreItemCleanEvent, CleanCompleteEvent) |
| ulticleaner.scheduled.entity-tick | Identical structure to `.item-tick` for entities: countdown, warnings at `entity.warn-times`, `PreEntityCleanEvent` (cancellable) at zero, batch removal, `messages.entity-cleaned` broadcast (only if the removed count is above zero — unlike the item variant, which always broadcasts even for a zero count), `CleanCompleteEvent` (type `ENTITIES`); only active while `entity.enabled` is true | scheduled | runs automatically every 20 ticks (1s, fixed) while `entity.enabled` is true (shipped default); the actual cleanup fires every `entity.interval` seconds (default 600) | n/a | n/a | internal | detailed | CleanerService#tickEntityClean (fires PreEntityCleanEvent, CleanCompleteEvent) |
| ulticleaner.scheduled.smart-clean | Every 5 seconds, count every non-blacklisted-world item and configured-type mob server-wide; if either count exceeds its own threshold (TPS-adjusted downward when `tps.adaptive-enabled` is true and TPS is low/critical) AND `smart.cooldown` seconds have passed since the last smart trigger, broadcast `messages.smart-triggered` and fire the same `PreItemCleanEvent`/`PreEntityCleanEvent` cleanup paths as the manual/scheduled ticks above; only active while `smart.enabled` is true (NOT the shipped default — shipped `false`). Shares the same `isCleaningInProgress` defect as `.all` above when both an item AND a mob threshold are exceeded in the same check: the mob cleanup silently never runs. `UltiKits/UltiCleaner#15` | scheduled | runs automatically every 100 ticks (5s, fixed) while `smart.enabled` is true (NOT the shipped default) | n/a | n/a | internal | detailed | CleanerService#checkSmartClean (fires PreItemCleanEvent, PreEntityCleanEvent) |
| ulticleaner.scheduled.tps-fallback | Every second, sample a fallback TPS estimate into three rolling history windows (1/5/15 minutes) for servers whose `Bukkit.getServer()` has no `getTPS()` method. On this repository's real-machine target, Paper 1.21.11, `getTPS()` IS present, so `fallbackMonitorEnabled` is `false` at boot and every tick of this task returns immediately, doing nothing — `TpsAwareScheduler#getCurrentTps` calls the native `getTPS()` reflectively and never needs the fallback history this task would otherwise populate. This task has NO observable effect on the pinned real-machine environment; it exists only for Bukkit/Spigot builds old enough to lack `getTPS()` | scheduled | runs automatically every 20 ticks (1s, fixed); has no observable effect on Paper (native TPS is always available) | n/a | n/a | internal | brief | TpsAwareScheduler#updateFallbackTps, ServerTypeUtil#hasTpsMethod |

## Configuration

Every `@ConfigEntry`-annotated field on this module's one `@ConfigEntity` class, `CleanerConfig`
(`config/cleaner.yml`, 33 keys total — matching the reconciliation table's own `@ConfigEntry`
count of 33 exactly).

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ulticleaner.config.cleaner.batch.show-progress | Whether every online OP is sent a progress message (`messages.clean-progress`) partway through a multi-tick item/entity removal batch | config | `config/cleaner.yml: batch.show-progress (default: false)` | n/a | n/a | admin | brief | CleanerService#removeEntitiesInBatches |
| ulticleaner.config.cleaner.batch.size | Number of items or entities removed per server tick during a batch cleanup, and the same value reused as the per-tick removal cap for both the scheduled and manual paths | config | `config/cleaner.yml: batch.size (default: 50)` | n/a | n/a | admin | brief | CleanerService#removeEntitiesInBatches |
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
| ulticleaner.config.cleaner.messages.clean-cancelled | Message broadcast when a `PreItemCleanEvent`/`PreEntityCleanEvent` listener cancels a cleanup | config | `config/cleaner.yml: messages.clean-cancelled (default: written in the server's language, the language file's clean_cancelled; under language: en '&c[Cleaner] &fCleanup operation was cancelled by another plugin!'; the text broadcast is the file's text)` | n/a | n/a | admin | brief | CleanerService#cleanItemsWithBatch, CleanerService#cleanEntitiesWithBatch |
| ulticleaner.config.cleaner.messages.clean-progress | Progress message sent to OPs partway through a multi-tick batch, with `{CURRENT}`/`{TOTAL}` placeholders, when `batch.show-progress` is true | config | `config/cleaner.yml: messages.clean-progress (default: written in the server's language, the language file's clean_progress; under language: en '&7[Cleaner] &fCleaning progress: &e{CURRENT}&f/&e{TOTAL}'; the text broadcast is the file's text)` | n/a | n/a | admin | brief | CleanerService#removeEntitiesInBatches |
| ulticleaner.config.cleaner.messages.entity-cleaned | Message broadcast (to all players) when a scheduled/manual/smart entity cleanup removes at least one entity, with a `{COUNT}` placeholder | config | `config/cleaner.yml: messages.entity-cleaned (default: written in the server's language, the language file's entity_cleaned; under language: en '&a[Cleaner] &fCleaned &e{COUNT} &fentities!'; the text broadcast is the file's text)` | n/a | n/a | admin | brief | CleanerService#broadcastEntityCleaned |
| ulticleaner.config.cleaner.messages.entity-warn | Warning message broadcast at each `entity.warn-times` countdown mark, with a `{TIME}` placeholder | config | `config/cleaner.yml: messages.entity-warn (default: written in the server's language, the language file's entity_warn; under language: en '&c[Cleaner] &fEntities will be cleaned in &e{TIME} &fseconds!'; the text broadcast is the file's text)` | n/a | n/a | admin | brief | CleanerService#broadcastEntityWarn |
| ulticleaner.config.cleaner.messages.item-cleaned | Message broadcast (to all players) when a scheduled/manual/smart item cleanup completes, with a `{COUNT}` placeholder — unlike the entity variant, this one broadcasts even when `{COUNT}` is `0` | config | `config/cleaner.yml: messages.item-cleaned (default: written in the server's language, the language file's item_cleaned; under language: en '&a[Cleaner] &fCleaned &e{COUNT} &fground items!'; the text broadcast is the file's text)` | n/a | n/a | admin | brief | CleanerService#broadcastItemCleaned |
| ulticleaner.config.cleaner.messages.smart-triggered | Message broadcast the instant smart cleanup triggers (before the actual item/entity removal begins) | config | `config/cleaner.yml: messages.smart-triggered (default: written in the server's language, the language file's smart_clean_triggered; under language: en '&e[Cleaner] &fDetected too many entities, initiating smart cleanup...'; the text broadcast is the file's text)` | n/a | n/a | admin | brief | CleanerService#checkSmartClean |
| ulticleaner.config.cleaner.messages.warn | Warning message broadcast at each `item.warn-times` countdown mark, with a `{TIME}` placeholder | config | `config/cleaner.yml: messages.warn (default: written in the server's language, the language file's item_warn; under language: en '&c[Cleaner] &fGround items will be cleaned in &e{TIME} &fseconds!'; the text broadcast is the file's text)` | n/a | n/a | admin | brief | CleanerService#broadcastWarn |
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
| ulticleaner.config.cleaner.worlds.blacklist | World names excluded from every cleanup (item, entity) | config | `config/cleaner.yml: worlds.blacklist (default: world_creative)` | n/a | n/a | admin | brief | CleanerService#collectItemsToClean, CleanerService#collectEntitiesToClean |

## Lifecycle Hooks

`UltiCleaner#onReload()` is the extension-point hook that the framework's `final`
`UltiToolsPlugin#reloadSelf()` invokes. Since `UltiKits/UltiCleaner#14`'s lifecycle-hook
migration, `/ul reload UltiCleaner` (the framework's own command, not a `@CmdMapping` site in this
repository, so no `command`-Kind row is added for it) runs, in order:
`ConfigManager#reloadConfigs` (which re-reads `config/cleaner.yml` from disk into the same
`CleanerConfig` bean `CleanerService` holds), the module's `language` object refresh, the
`@ConditionalOnConfig` drift report (a no-op here, since this module has 0 such sites), the
framework's own per-module reload INFO line, and finally `onReload()`. Before the migration this
module overrode `reloadSelf()` itself, so neither the config re-read nor the language refresh ran:
`config/cleaner.yml` was never re-read and `CleanerService#reload()` rebuilt its caches from the stale values. This
module declares no `onUnregister()` override: its former unload override only logged a
"disabled" line, and it was deleted along with the `cleaner_disabled` language key.

`UltiCleaner#registerSelf()` is the other framework-invoked callback with a row below. Besides
initialising the services, it runs the removed-key check: this version deleted five configuration
keys, and those keys are still in every existing operator's `config/cleaner.yml`, because the
framework writes a declared default only for a key that is *missing* and never removes one.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ulticleaner.lifecycle.removed-key-warning | On module enable and again on every `/ul reload`, read the operator's own `config/cleaner.yml` and log one WARNING per key this version no longer reads but which is still present in that file — `messages.prefix`, `chunk.enabled`, `chunk.max-distance`, `chunk.batch-size`, `chunk.timeout`. Each warning names the module, the file's path and the key, says the key no longer has any effect, says where the setting went (for `messages.prefix`, nowhere new: nothing ever read it, and a broadcast's prefix is part of that broadcast's own `messages.*` text; for the four chunk keys, nowhere, since that feature was removed), and tells the operator to delete the key to silence it; the line comes from the language file, so it follows the `language` setting. Nothing is logged when the file holds none of them, when the file is absent, or when it cannot be parsed — the framework's own config loading already reports an unparseable file, and a second message would only add noise. Deleting a key from `CleanerConfig` stops the framework WRITING it into a fresh file but does nothing to files already on disk: the framework writes a declared default only for a key that is missing, so an existing install keeps the key, keeps its value, and would otherwise get no indication the value stopped meaning anything | event | automatic, at module enable and at `/ul reload UltiCleaner` | n/a | n/a | admin | brief | UltiCleaner#registerSelf, UltiCleaner#onReload, RemovedConfigKeys#warnAboutLeftovers |
| ulticleaner.lifecycle.legacy-message-defaults | On module enable and again on every reload of it (`UltiCleaner#registerSelf`, `#onReload`, both after the framework has read `config/cleaner.yml` and loaded the language — never from a configuration change listener, which the framework fires before it reloads the language): each of the seven `messages.*` values that is still built-in text — the Chinese default an earlier version shipped, or this jar's English or Chinese text for it (read from the module jar, never from the language files on disk) — and differs from the current text is replaced with the language file's text in the server's language, and the file is saved once. An untouched value therefore follows a `language` switch in both directions; a value that differs in any way, even by one character, is the operator's and is kept byte for byte; a text that would break the setting's `@NotEmpty` is never written; a second start with the same language writes nothing. A single-module `/ul reload UltiCleaner` does not re-read the framework's `language`, so a changed `language` is picked up on a bare `/ul reload` or a restart. A failed save is logged as one warning from the language file (`log_config_default_save_failed`) | event | automatic, at module enable and at `/ul reload` | n/a | n/a | admin | brief | UltiCleaner#registerSelf, UltiCleaner#onReload, CleanerConfig#materializeText, ConfigTextDefaults |
| ulticleaner.lifecycle.reload | Rebuild `CleanerService`'s item-whitelist, entity-type and world-blacklist caches from the just-reloaded `config/cleaner.yml`, reset both the item and the entity cleanup countdowns to the reloaded `item.interval` and `entity.interval` values, and log the module's own `cleaner_reloaded` line, after the framework has already re-read the config file | event | `/ul reload UltiCleaner` (framework calls `reloadSelf()`, which runs its own steps first, then invokes this hook) | n/a | n/a | admin | brief | UltiCleaner#onReload, CleanerService#reload |

## Language

Every chat line, the command description and every console line this module writes goes through
the framework's language catalogue (`lang/en.yml`, `lang/zh.yml`), so it follows the framework-wide
`language` setting (`plugins/UltiTools/config.yml`); the seven broadcast messages are written into
`config/cleaner.yml` in that language while they are still built-in text (see the Conventions note). Two JUnit guards
(`UltiCleanerLanguageCatalogueTest`, `UltiCleanerCjkLiteralScopeTest`) fail the build when a key is
missing from either catalogue, a catalogue key is read by nothing, or Chinese text appears outside
one.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ulticleaner.i18n.language | All of this module's chat, command-description and console text in the server's language: `lang/en.yml` under `language: en`, `lang/zh.yml` under `language: zh` | config | framework `config.yml: language` | n/a | both | admin | none | `lang/en.yml`, `lang/zh.yml`, every `i18n(...)` call |
