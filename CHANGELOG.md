# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Fixed

- Unloading or reloading this module (`/ul reload UltiCleaner`, or disabling the module) now
  actually runs the framework's own reload/unload steps (config reload, language refresh,
  `@ConditionalOnConfig` drift reporting, command/listener cleanup) before this module's own
  reload work (rebuilding the cleaner service's caches and resetting the cleanup countdowns from
  the reloaded `config/cleaner.yml`) — previously these framework steps were silently skipped, so
  an edit to `config/cleaner.yml` did not take effect until a restart (UltiKits/UltiCleaner#14).
- 卸载或重载本模块（`/ul reload UltiCleaner`，或禁用本模块）现在会先真正执行框架自身的重载/卸载步骤
  （配置重载、语言刷新、`@ConditionalOnConfig` 漂移报告、命令/监听器清理），再执行本模块自身的重载
  工作（清理服务依据重新读取的 `config/cleaner.yml` 重建缓存并重置清理倒计时）——此前这些框架步骤会被
  静默跳过，因此修改 `config/cleaner.yml` 要到重启后才生效（UltiKits/UltiCleaner#14）。

### Removed

- Removed the module's own "UltiCleaner has been disabled!" console line and its
  `cleaner_disabled` language key. The unload override that printed it did no other work, so it
  was deleted rather than moved to the new unload hook.
- 移除了本模块自身的"UltiCleaner 已禁用！"控制台日志行及其 `cleaner_disabled` 语言键；
  打印该行的卸载覆写方法没有其他工作，因此直接删除，而非迁移到新的卸载钩子。
