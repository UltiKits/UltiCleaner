package com.ultikits.plugins.cleaner.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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
    // Each message's Java default is the one it shipped with in every earlier version, which the
    // framework writes for a missing key; materializeText() then writes the language file's text in
    // the server's language while the value is still built-in text (maintainer decision 2026-09-25).
    @NotEmpty
    @ConfigEntry(path = "messages.warn", comment = "清理警告消息 ({TIME}为剩余秒数)")
    private String warnMessage = SHIPPED_WARN;

    @NotEmpty
    @ConfigEntry(path = "messages.entity-warn", comment = "实体清理警告消息")
    private String entityWarnMessage = SHIPPED_ENTITY_WARN;

    @NotEmpty
    @ConfigEntry(path = "messages.item-cleaned", comment = "物品清理完成消息 ({COUNT}为清理数量)")
    private String itemCleanedMessage = SHIPPED_ITEM_CLEANED;

    @NotEmpty
    @ConfigEntry(path = "messages.entity-cleaned", comment = "实体清理完成消息 ({COUNT}为清理数量)")
    private String entityCleanedMessage = SHIPPED_ENTITY_CLEANED;

    @NotEmpty
    @ConfigEntry(path = "messages.smart-triggered", comment = "智能清理触发消息")
    private String smartCleanTriggeredMessage = SHIPPED_SMART_TRIGGERED;

    @NotEmpty
    @ConfigEntry(path = "messages.clean-progress", comment = "清理进度消息")
    private String cleanProgressMessage = SHIPPED_CLEAN_PROGRESS;

    @NotEmpty
    @ConfigEntry(path = "messages.clean-cancelled", comment = "清理被取消消息")
    private String cleanCancelledMessage = SHIPPED_CLEAN_CANCELLED;

    // The default each message had in every earlier version, read from this class's history (one
    // value per key). Each is the field's Java default and one of the values materializeText()
    // recognises as built-in text in an operator's file, compared byte for byte.
    private static final String SHIPPED_WARN = "&c[清理] &f地面物品将在 &e{TIME} &f秒后清理！";
    private static final String SHIPPED_ENTITY_WARN = "&c[清理] &f实体将在 &e{TIME} &f秒后清理！";
    private static final String SHIPPED_ITEM_CLEANED = "&a[清理] &f已清理 &e{COUNT} &f个地面物品！";
    private static final String SHIPPED_ENTITY_CLEANED = "&a[清理] &f已清理 &e{COUNT} &f个实体！";
    private static final String SHIPPED_SMART_TRIGGERED = "&e[清理] &f检测到实体数量过多，正在进行智能清理...";
    private static final String SHIPPED_CLEAN_PROGRESS = "&7[清理] &f清理进度: &e{CURRENT}&f/&e{TOTAL}";
    private static final String SHIPPED_CLEAN_CANCELLED = "&c[清理] &f清理操作被其他插件取消！";

    /**
     * Writes every broadcast message in the server's language (maintainer decision 2026-09-25,
     * UltiKits/UltiCleaner#17): each message whose value is still built-in text -- the default an
     * earlier version shipped, or this jar's text for it in any language -- and differs from the
     * current text is replaced with {@code text}'s current text, when that text fits the setting's own
     * limits. Any other value is the operator's and is kept. Idempotent. Must run after the module's
     * language is loaded ({@code registerSelf()} and {@code onReload()}), never from a change listener;
     * the caller saves the file when this returns {@code true}.
     *
     * @param text the module's {@code i18n}: catalogue key to text in the server's language
     * @return whether any value was rewritten
     */
    public boolean materializeText(Function<String, String> text) {
        Map<String, Map<String, String>> jar = ConfigTextDefaults.jarCatalogues(CleanerConfig.class);
        boolean[] changed = {false};
        warnMessage = follow("warnMessage", warnMessage, text, jar, "item_warn", SHIPPED_WARN, changed);
        entityWarnMessage = follow("entityWarnMessage", entityWarnMessage, text, jar, "entity_warn", SHIPPED_ENTITY_WARN, changed);
        itemCleanedMessage = follow("itemCleanedMessage", itemCleanedMessage, text, jar, "item_cleaned", SHIPPED_ITEM_CLEANED, changed);
        entityCleanedMessage = follow("entityCleanedMessage", entityCleanedMessage, text, jar, "entity_cleaned", SHIPPED_ENTITY_CLEANED, changed);
        smartCleanTriggeredMessage = follow("smartCleanTriggeredMessage", smartCleanTriggeredMessage, text, jar, "smart_clean_triggered", SHIPPED_SMART_TRIGGERED, changed);
        cleanProgressMessage = follow("cleanProgressMessage", cleanProgressMessage, text, jar, "clean_progress", SHIPPED_CLEAN_PROGRESS, changed);
        cleanCancelledMessage = follow("cleanCancelledMessage", cleanCancelledMessage, text, jar, "clean_cancelled", SHIPPED_CLEAN_CANCELLED, changed);
        return changed[0];
    }

    /**
     * {@code value}, or {@code text}'s current text for {@code key} when {@code value} is still built-in
     * text other than that and the new text fits {@code field}'s constraints; sets {@code changed[0]}
     * when it replaces.
     */
    private static String follow(String field, String value, Function<String, String> text,
                                 Map<String, Map<String, String>> jar, String key, String shipped, boolean[] changed) {
        String result = ConfigTextDefaults.materialize(CleanerConfig.class, field, value,
                ConfigTextDefaults.currentText(text, "", key), ConfigTextDefaults.tracked(jar, "", key, shipped));
        if (!Objects.equals(result, value)) {
            changed[0] = true;
        }
        return result;
    }
    
    public CleanerConfig() {
        super("config/cleaner.yml");
    }
}
