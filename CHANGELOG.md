# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Added

- On startup, and again on every `/ul reload`, this module now checks your own `config/cleaner.yml`
  for keys this version no longer reads and logs one warning per leftover, naming the module, the
  file and the key, and saying where the setting went. Deleting a key from the module stops a
  *fresh* file being written with it but does nothing to the file you already have — the framework
  only ever writes a default for a key that is missing — so without this warning an edited value
  would simply stop meaning anything, silently. The keys checked for are `messages.prefix`,
  `chunk.enabled`, `chunk.max-distance`, `chunk.batch-size` and `chunk.timeout`; delete them from
  the file to silence it.
- 本模块现在会在启动时、以及每次 `/ul reload` 时检查你自己的 `config/cleaner.yml` 中是否仍存在本版本
  不再读取的配置项，并为每个残留键各记一条警告，点明模块、文件与键名，并说明该设置改去哪里。
  从模块中删除一个键，只会让**新生成**的文件里不再有它，对你已有的文件没有任何影响——框架只为**缺失**的键
  写入默认值——因此若没有这条警告，一个被改过的值会就此悄无声息地失效。当前检查的键为 `messages.prefix`、
  `chunk.enabled`、`chunk.max-distance`、`chunk.batch-size`、`chunk.timeout`；把它们从文件中删除即可不再提示。

### Changed

- Message settings in `config/cleaner.yml` — the seven broadcast messages (`messages.warn`,
  `messages.entity-warn`, `messages.item-cleaned`, `messages.entity-cleaned`, `messages.smart-triggered`,
  `messages.clean-progress`, `messages.clean-cancelled`) — are written in the server's language when the
  module starts, and the file is what the module broadcasts (for example `messages.item-cleaned:
  '&a[Cleaner] &fCleaned &e{COUNT} &fground items!'` under `language: en`); previously they were fixed
  Chinese text, so `language: en` had no effect on any cleanup broadcast. A setting that is still built-in
  text — in any language, or a default an earlier version shipped — follows `language`: it is rewritten
  when the module starts or after `/ul reload`. A setting you edited is kept. To keep a built-in text but
  stop it following `language`, change at least one character (UltiKits/UltiCleaner#17).
- `config/cleaner.yml` 中的消息设置——七条广播消息（`messages.warn`、`messages.entity-warn`、`messages.item-cleaned`、
  `messages.entity-cleaned`、`messages.smart-triggered`、`messages.clean-progress`、`messages.clean-cancelled`）——
  在模块启动时按服务器语言写入，文件内容即模块广播的内容；此前它们是写死的中文，`language: en` 对任何清理广播都不起作用。
  仍为内置文本（任一语言的内置文本，或旧版本的出厂默认值）的设置会跟随 `language`：模块启动或执行 `/ul reload` 后改写为
  当前语言的文本。你改过的设置保持不变。若想保留内置文本又不让它跟随语言，请至少改动一个字符（UltiKits/UltiCleaner#17）。

### Fixed

- `language: en` now applies to everything `/clean` prints (the started, in-progress, `check`,
  `status` and help lines) and to the command description, which were fixed Chinese text in every
  language although the language files already held English text for most of them
  (UltiKits/UltiCleaner#17). `language: zh` now also applies to the console lines that were fixed
  English text: the detected-server and TPS-monitor start-up lines, an unknown entity type in
  `entity.types`, the TPS band shown by `/clean check` and `/clean status` (`Normal`, `Low`,
  `Critical`), and the warning about a key this version no longer reads. Their English wording is
  unchanged.
- `language: en` 现在对 `/clean` 打印的全部内容（开始清理、清理进行中、`check`、`status` 与帮助）以及命令描述生效；
  这些内容原先在任何语言下都是写死的中文，而语言文件中其实已有其中大部分的英文文本（UltiKits/UltiCleaner#17）。
  `language: zh` 现在也对原先写死为英文的控制台日志生效：检测到的服务端与 TPS 监控启动行、`entity.types` 中的未知实体类型、
  `/clean check` 与 `/clean status` 显示的 TPS 档位（`Normal`、`Low`、`Critical`），以及本版本不再读取的配置键的警告。

- `/ul reload UltiCleaner` (and a bare `/ul reload`) now re-reads `config/cleaner.yml` and
  refreshes the module's language files before the module rebuilds the cleaner service's caches and
  resets its cleanup countdowns. Previously this module replaced the framework's reload method, so
  the file was never re-read and an edited value such as `item.interval` did not take effect until a
  restart (UltiKits/UltiCleaner#14).
- After `/upm uninstall UltiCleaner`, the module's `/clean` command is now really removed.
  Previously this module replaced the framework's unload method with one that only logged a line, so
  the command stayed active until the server restarted (UltiKits/UltiCleaner#14).
- `/ul reload UltiCleaner`（以及不带参数的 `/ul reload`）现在会先重新读取 `config/cleaner.yml` 并刷新本模块的
  语言文件，再由本模块重建清理服务的缓存并重置清理倒计时。此前本模块替换了框架的重载方法，该文件从不会被
  重新读取，修改 `item.interval` 等配置要到重启后才生效（UltiKits/UltiCleaner#14）。
- 执行 `/upm uninstall UltiCleaner` 后，本模块的 `/clean` 命令现在会被真正移除。此前本模块用一个只打印
  日志的方法替换了框架的卸载方法，因此该命令会一直保持生效，直到服务器重启（UltiKits/UltiCleaner#14）。

- `/clean status` now names the TPS threshold reduction actually configured in
  `tps.low-reduction` and `tps.critical-reduction`. Previously the warning line printed a fixed
  "30%" and "50%" whatever those keys were set to, so an operator who had raised either one was
  shown a number the module was not applying (UltiKits/UltiCleaner#21).
- `/clean status` 现在会显示 `tps.low-reduction`、`tps.critical-reduction` 中实际配置的 TPS 阈值降低百分比。
  此前那行警告无论这两个键被改成什么，都固定显示「30%」和「50%」，因此调高过任一项的运维看到的是一个
  模块并未在使用的数字（UltiKits/UltiCleaner#21）。

### Removed

- Five language-file entries that no code ever displayed were removed from `lang/en.yml` and
  `lang/zh.yml`: `smart_clean_items`, `smart_clean_mobs`, `clean_complete`, `tps_low_warning` and
  `tps_critical_warning`. No message changes; a copy of them in an already-extracted language file
  is simply not read.
- 从 `lang/en.yml` 与 `lang/zh.yml` 中删除了五个从未被任何代码显示的条目：`smart_clean_items`、`smart_clean_mobs`、
  `clean_complete`、`tps_low_warning`、`tps_critical_warning`。任何消息都不受影响；已解压的语言文件中的副本只是不再被读取。

- Removed the module's own "UltiCleaner has been disabled!" console line and its
  `cleaner_disabled` language key. The unload override that printed it did no other work, so it
  was deleted rather than moved to the new unload hook.
- **Removed the chunk-unloading feature entirely.** The server engine already unloads idle chunks
  by itself, so this feature could only ever reach the chunks the server deliberately keeps
  resident. Measured with no players online, three worlds held 147 loaded chunks — exactly the
  49-chunk spawn-keep area of each — and the module's own gate rejected all 147. The deprecated
  platform check the gate was built on says the same thing in its own words: chunks "will not be
  loaded for more than 1 tick unless they are in use". Removing the feature is not a rejection of
  it; `UltiKits/UltiCleaner#27` records the measurements and the conditions for bringing it back
  (UltiKits/UltiCleaner#23, UltiKits/UltiCleaner#20). What went, item by item:
  - the command `/clean chunks`, which no longer exists and now prints the usual unknown-format
    help instead;
  - the scheduled chunk-unload task that ran every 30 seconds;
  - the configuration keys `chunk.enabled`, `chunk.max-distance`, `chunk.batch-size` and
    `chunk.timeout` in `config/cleaner.yml` — these four keys still sit in your existing file and
    are now ignored (the module logs one warning per leftover key at startup);
  - the `PreChunkUnloadEvent` extension point, which only this feature ever fired;
  - the "unloadable chunks" line of `/clean check`, and the `chunk_unloaded`, `cmd_help_chunks`
    and `stats_chunks_unloadable` language keys.
  `/clean check`'s "loaded chunks" line stays: it is a plain server statistic, not a readout of
  this feature.
- Removed the configuration key `messages.prefix` from `config/cleaner.yml`. No code ever read it, so
  no message was ever prefixed from it and nothing you see changes. The prefix in a cleanup broadcast
  is part of that broadcast's own text — the seven other `messages.*` keys in the same file, each
  still read from the configuration and each still carrying its own `[清理]` tag — so to change a
  prefix, edit the message it belongs to. Making these broadcasts follow the server's `language`
  setting is separate, still outstanding work (UltiKits/UltiCleaner#17). The removed key stays in
  your existing file and is now ignored; the module logs one warning about it at startup
  (UltiKits/UltiCleaner#18).
- 移除了本模块自身的"UltiCleaner 已禁用！"控制台日志行及其 `cleaner_disabled` 语言键；
  打印该行的卸载覆写方法没有其他工作，因此直接删除，而非迁移到新的卸载钩子。
- **完整移除区块卸载功能。** 服务端引擎本身已经会卸载闲置区块，因此这个功能唯一够得着的，恰好是服务端
  刻意保留的那些区块。实测零玩家在线时三个世界共 147 个已加载区块，正好是每个世界 49 个出生点保留区块，
  而本模块的判定门把这 147 个全部拒绝。该判定门所依赖的那个已弃用平台接口，其自身说明也是同一句话：
  「除非区块在被使用，否则不会被加载超过 1 tick」。**移除不等于否决该功能**：测量结果与「何时把它补回来」
  的条件记录在 `UltiKits/UltiCleaner#27`（UltiKits/UltiCleaner#23、UltiKits/UltiCleaner#20）。逐项如下：
  - 命令 `/clean chunks` 已不存在，现在会落到未知子命令的帮助输出；
  - 每 30 秒执行一次的定时区块卸载任务；
  - `config/cleaner.yml` 中的配置项 `chunk.enabled`、`chunk.max-distance`、`chunk.batch-size`、
    `chunk.timeout` —— 这四个键仍留在你现有的配置文件里，但已不再生效（模块启动时会为每个残留键各记一条警告）；
  - 扩展点 `PreChunkUnloadEvent`，此前只有这个功能会触发它；
  - `/clean check` 的「可卸载区块」一行，以及 `chunk_unloaded`、`cmd_help_chunks`、
    `stats_chunks_unloadable` 三个语言键。
  `/clean check` 的「已加载区块」一行保留：它是一项普通的服务器统计数字，不是该功能的读数。
- 移除了 `config/cleaner.yml` 中的配置项 `messages.prefix`。此前没有任何代码读取它，因此从来没有任何消息
  用过它作前缀，删除它不会改变你看到的任何内容。清理广播里的前缀是那条广播自身文本的一部分——即同一文件中
  另外七个 `messages.*` 键，它们仍然从配置读取，也仍然各自带着自己的 `[清理]` 标记——所以要改前缀，就去改
  它所属的那条消息。让这些广播跟随服务器的 `language` 设置是另一项尚未完成的工作
  （UltiKits/UltiCleaner#17）。被删除的键仍留在你现有的配置文件里，但已不再生效；模块启动时会为它记一条警告
  （UltiKits/UltiCleaner#18）。
