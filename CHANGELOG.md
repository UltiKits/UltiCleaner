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

### Fixed

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
