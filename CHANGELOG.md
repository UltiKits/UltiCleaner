# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Fixed

- `/ul reload UltiCleaner` (and a bare `/ul reload`) now re-reads `config/cleaner.yml` and
  refreshes the module's language files before the module rebuilds the cleaner service's caches and
  resets its cleanup countdowns. Previously this module replaced the framework's reload method, so
  the file was never re-read and an edited value such as `item.interval` did not take effect until a
  restart (UltiKits/UltiCleaner#14).
- Unloading this module (`/upm uninstall UltiCleaner`, or server shutdown) now unregisters its
  `/clean` command. Previously this module replaced the framework's unload method with one that only
  logged a line, so command unregistration was skipped on both paths; listener unregistration was
  also skipped on `/upm uninstall`, which has no effect here because the module registers no
  listeners (UltiKits/UltiCleaner#14).
- `/ul reload UltiCleaner`（以及不带参数的 `/ul reload`）现在会先重新读取 `config/cleaner.yml` 并刷新本模块的
  语言文件，再由本模块重建清理服务的缓存并重置清理倒计时。此前本模块替换了框架的重载方法，该文件从不会被
  重新读取，修改 `item.interval` 等配置要到重启后才生效（UltiKits/UltiCleaner#14）。
- 卸载本模块（`/upm uninstall UltiCleaner` 或关闭服务器）现在会注销其 `/clean` 命令。此前本模块用一个只打印
  日志的方法替换了框架的卸载方法，因此两条路径都跳过了命令注销；`/upm uninstall` 还跳过了监听器注销，但本
  模块没有注册任何监听器，因此对本模块没有影响（UltiKits/UltiCleaner#14）。

### Removed

- Removed the module's own "UltiCleaner has been disabled!" console line and its
  `cleaner_disabled` language key. The unload override that printed it did no other work, so it
  was deleted rather than moved to the new unload hook.
- 移除了本模块自身的"UltiCleaner 已禁用！"控制台日志行及其 `cleaner_disabled` 语言键；
  打印该行的卸载覆写方法没有其他工作，因此直接删除，而非迁移到新的卸载钩子。
