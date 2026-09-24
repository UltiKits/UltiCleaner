package com.ultikits.plugins.cleaner.config;

import java.util.Arrays;
import java.util.List;

import com.ultikits.ultitools.abstracts.AbstractConfigEntity;
import com.ultikits.ultitools.annotations.ConfigEntity;
import com.ultikits.ultitools.annotations.ConfigEntry;
import com.ultikits.ultitools.annotations.config.NotEmpty;
import com.ultikits.ultitools.annotations.config.Range;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for UltiCleaner.
 * Supports item cleanup, entity cleanup, smart cleanup,
 * and TPS-adaptive thresholds.
 *
 * @author wisdomme
 * @version 2.0.0
 */
@Getter
@Setter
@ConfigEntity("config/cleaner.yml")
public class CleanerConfig extends AbstractConfigEntity {
    
    // ============ Item Cleanup ============
    @ConfigEntry(path = "item.enabled", comment = "启用物品清理")
    private boolean itemCleanEnabled = true;

    @Range(min = 10, max = 3600)
    @ConfigEntry(path = "item.interval", comment = "清理间隔（秒）")
    private int itemCleanInterval = 300;

    @NotEmpty
    @ConfigEntry(path = "item.warn-times", comment = "清理前警告时间点（秒）")
    private List<Integer> itemWarnTimes = Arrays.asList(60, 30, 10, 5, 3, 2, 1);

    @ConfigEntry(path = "item.whitelist", comment = "物品白名单（不会被清理的物品）")
    private List<String> itemWhitelist = Arrays.asList(
        "DIAMOND",
        "EMERALD",
        "NETHER_STAR",
        "BEACON",
        "ELYTRA"
    );

    @ConfigEntry(path = "item.ignore-named", comment = "忽略有自定义名称的物品")
    private boolean itemIgnoreNamed = true;

    @Range(min = 0, max = 300)
    @ConfigEntry(path = "item.ignore-recent", comment = "忽略刚掉落的物品（秒）")
    private int itemIgnoreRecentSeconds = 30;
    
    // ============ Entity Cleanup ============
    @ConfigEntry(path = "entity.enabled", comment = "启用实体清理")
    private boolean entityCleanEnabled = true;

    @Range(min = 10, max = 7200)
    @ConfigEntry(path = "entity.interval", comment = "实体清理间隔（秒）")
    private int entityCleanInterval = 600;

    @NotEmpty
    @ConfigEntry(path = "entity.warn-times", comment = "实体清理前警告时间点（秒）")
    private List<Integer> entityWarnTimes = Arrays.asList(60, 30, 10, 5, 3, 2, 1);

    @NotEmpty
    @ConfigEntry(path = "entity.types", comment = "要清理的实体类型")
    private List<String> entityTypes = Arrays.asList(
        "ZOMBIE",
        "SKELETON",
        "CREEPER",
        "SPIDER",
        "CAVE_SPIDER",
        "ENDERMAN",
        "WITCH",
        "SLIME",
        "PHANTOM"
    );

    @ConfigEntry(path = "entity.whitelist-named", comment = "不清理有自定义名称的实体")
    private boolean entityWhitelistNamed = true;

    @ConfigEntry(path = "entity.whitelist-leashed", comment = "不清理被拴绳栓住的实体")
    private boolean entityWhitelistLeashed = true;

    @ConfigEntry(path = "entity.whitelist-tamed", comment = "不清理被驯服的实体")
    private boolean entityWhitelistTamed = true;
    
    // ============ World Settings ============
    @ConfigEntry(path = "worlds.blacklist", comment = "不进行清理的世界")
    private List<String> worldBlacklist = Arrays.asList(
        "world_creative"
    );
    
    // ============ Smart Cleanup ============
    @ConfigEntry(path = "smart.enabled", comment = "启用智能清理（基于实体数量阈值自动触发）")
    private boolean smartCleanEnabled = false;

    @Range(min = 100, max = 10000)
    @ConfigEntry(path = "smart.item-threshold", comment = "物品数量阈值（超过此数量触发智能清理）")
    private int itemMaxThreshold = 2000;

    @Range(min = 100, max = 5000)
    @ConfigEntry(path = "smart.mob-threshold", comment = "生物数量阈值（超过此数量触发智能清理）")
    private int mobMaxThreshold = 1000;

    @Range(min = 30, max = 600)
    @ConfigEntry(path = "smart.cooldown", comment = "智能清理冷却时间（秒）")
    private int smartCleanCooldown = 60;

    // ============ Batch Processing ============
    @Range(min = 10, max = 500)
    @ConfigEntry(path = "batch.size", comment = "每tick清理的实体数量（分批清理减少卡顿）")
    private int cleanBatchSize = 50;

    @ConfigEntry(path = "batch.show-progress", comment = "向OP显示清理进度")
    private boolean showCleanProgress = false;
    
    // ============ TPS Adaptive ============
    @ConfigEntry(path = "tps.adaptive-enabled", comment = "启用TPS自适应阈值调整")
    private boolean tpsAdaptiveEnabled = true;

    @NotEmpty
    @ConfigEntry(path = "tps.sample-window", comment = "TPS采样窗口（1m/5m/15m）")
    private String tpsSampleWindow = "1m";

    @Range(min = 10, max = 20)
    @ConfigEntry(path = "tps.low-threshold", comment = "低TPS阈值")
    private double lowTpsThreshold = 18.0;

    @Range(min = 5, max = 18)
    @ConfigEntry(path = "tps.critical-threshold", comment = "严重低TPS阈值")
    private double criticalTpsThreshold = 15.0;

    @Range(min = 0, max = 80)
    @ConfigEntry(path = "tps.low-reduction", comment = "低TPS时阈值降低百分比")
    private int lowTpsReduction = 30;

    @Range(min = 0, max = 90)
    @ConfigEntry(path = "tps.critical-reduction", comment = "严重低TPS时阈值降低百分比")
    private int criticalTpsReduction = 50;
    
    // ============ Messages ============
    // Blank by default: a blank message is broadcast with the language file's text in the server's
    // language, resolved when it is sent (maintainer ruling 2026-09-24 (d)). Any other value is the
    // operator's and is broadcast as written.
    @ConfigEntry(path = "messages.warn", comment = "清理警告消息 ({TIME}为剩余秒数；留空则使用语言文件中的文本)")
    private String warnMessage = "";

    @ConfigEntry(path = "messages.entity-warn", comment = "实体清理警告消息 ({TIME}为剩余秒数；留空则使用语言文件中的文本)")
    private String entityWarnMessage = "";

    @ConfigEntry(path = "messages.item-cleaned", comment = "物品清理完成消息 ({COUNT}为清理数量；留空则使用语言文件中的文本)")
    private String itemCleanedMessage = "";

    @ConfigEntry(path = "messages.entity-cleaned", comment = "实体清理完成消息 ({COUNT}为清理数量；留空则使用语言文件中的文本)")
    private String entityCleanedMessage = "";

    @ConfigEntry(path = "messages.smart-triggered", comment = "智能清理触发消息 (留空则使用语言文件中的文本)")
    private String smartCleanTriggeredMessage = "";

    @ConfigEntry(path = "messages.clean-progress", comment = "清理进度消息 ({CURRENT}/{TOTAL}；留空则使用语言文件中的文本)")
    private String cleanProgressMessage = "";

    @ConfigEntry(path = "messages.clean-cancelled", comment = "清理被取消消息 (留空则使用语言文件中的文本)")
    private String cleanCancelledMessage = "";

    // The default each message had in every earlier version, read from this class's history (one
    // value per key, from the first release until the language file took over). They are here only to
    // be recognised in an upgraded operator's file and blanked; they are never shown.
    private static final String SHIPPED_WARN = "&c[清理] &f地面物品将在 &e{TIME} &f秒后清理！";
    private static final String SHIPPED_ENTITY_WARN = "&c[清理] &f实体将在 &e{TIME} &f秒后清理！";
    private static final String SHIPPED_ITEM_CLEANED = "&a[清理] &f已清理 &e{COUNT} &f个地面物品！";
    private static final String SHIPPED_ENTITY_CLEANED = "&a[清理] &f已清理 &e{COUNT} &f个实体！";
    private static final String SHIPPED_SMART_TRIGGERED = "&e[清理] &f检测到实体数量过多，正在进行智能清理...";
    private static final String SHIPPED_CLEAN_PROGRESS = "&7[清理] &f清理进度: &e{CURRENT}&f/&e{TOTAL}";
    private static final String SHIPPED_CLEAN_CANCELLED = "&c[清理] &f清理操作被其他插件取消！";

    /**
     * Blanks every message that is exactly the default an earlier version shipped, so the language
     * file's text takes over in the server's language; any other value is the operator's and is kept.
     * Idempotent: a blank value matches no shipped default. The caller saves the file when this
     * returns true (maintainer ruling 2026-09-24 (d)).
     *
     * @return whether any value was rewritten
     */
    public boolean migrateLegacyDefaults() {
        boolean changed = false;
        if (SHIPPED_WARN.equals(warnMessage)) {
            warnMessage = "";
            changed = true;
        }
        if (SHIPPED_ENTITY_WARN.equals(entityWarnMessage)) {
            entityWarnMessage = "";
            changed = true;
        }
        if (SHIPPED_ITEM_CLEANED.equals(itemCleanedMessage)) {
            itemCleanedMessage = "";
            changed = true;
        }
        if (SHIPPED_ENTITY_CLEANED.equals(entityCleanedMessage)) {
            entityCleanedMessage = "";
            changed = true;
        }
        if (SHIPPED_SMART_TRIGGERED.equals(smartCleanTriggeredMessage)) {
            smartCleanTriggeredMessage = "";
            changed = true;
        }
        if (SHIPPED_CLEAN_PROGRESS.equals(cleanProgressMessage)) {
            cleanProgressMessage = "";
            changed = true;
        }
        if (SHIPPED_CLEAN_CANCELLED.equals(cleanCancelledMessage)) {
            cleanCancelledMessage = "";
            changed = true;
        }
        return changed;
    }
    
    public CleanerConfig() {
        super("config/cleaner.yml");
    }
}
