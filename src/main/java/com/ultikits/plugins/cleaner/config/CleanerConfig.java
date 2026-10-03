package com.ultikits.plugins.cleaner.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    /**
     * Default of both {@code item.warn-times} and {@code entity.warn-times}. The framework binds each
     * list element as an {@code Integer} and skips one that is not a whole number with a located
     * warning, so a file's list is used as far as it binds (UltiKits/UltiCleaner#22); a list that binds
     * to nothing is replaced by this default, with a warning from the service (UltiKits/UltiCleaner#34).
     * These two fields carry no {@code @NotEmpty}: the framework applies it to text only.
     */
    public static final List<Integer> DEFAULT_WARN_TIMES =
            Collections.unmodifiableList(Arrays.asList(60, 30, 10, 5, 3, 2, 1));
    
    // ============ Item Cleanup ============
    @ConfigEntry(path = "item.enabled", comment = "{cleaner_config_comment_item_enabled}")
    private boolean itemCleanEnabled = true;

    @Range(min = 10, max = 3600)
    @ConfigEntry(path = "item.interval", comment = "{cleaner_config_comment_item_interval}")
    private int itemCleanInterval = 300;

    @ConfigEntry(path = "item.warn-times", comment = "{cleaner_config_comment_item_warn_times}")
    private List<Integer> itemWarnTimes = new ArrayList<>(DEFAULT_WARN_TIMES);

    @ConfigEntry(path = "item.whitelist", comment = "{cleaner_config_comment_item_whitelist}")
    private List<String> itemWhitelist = Arrays.asList(
        "DIAMOND",
        "EMERALD",
        "NETHER_STAR",
        "BEACON",
        "ELYTRA"
    );

    @ConfigEntry(path = "item.ignore-named", comment = "{cleaner_config_comment_item_ignore_named}")
    private boolean itemIgnoreNamed = true;

    @Range(min = 0, max = 300)
    @ConfigEntry(path = "item.ignore-recent", comment = "{cleaner_config_comment_item_ignore_recent}")
    private int itemIgnoreRecentSeconds = 30;
    
    // ============ Entity Cleanup ============
    @ConfigEntry(path = "entity.enabled", comment = "{cleaner_config_comment_entity_enabled}")
    private boolean entityCleanEnabled = true;

    @Range(min = 10, max = 7200)
    @ConfigEntry(path = "entity.interval", comment = "{cleaner_config_comment_entity_interval}")
    private int entityCleanInterval = 600;

    @ConfigEntry(path = "entity.warn-times", comment = "{cleaner_config_comment_entity_warn_times}")
    private List<Integer> entityWarnTimes = new ArrayList<>(DEFAULT_WARN_TIMES);

    @NotEmpty
    @ConfigEntry(path = "entity.types", comment = "{cleaner_config_comment_entity_types}")
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

    @ConfigEntry(path = "entity.whitelist-named", comment = "{cleaner_config_comment_entity_whitelist_named}")
    private boolean entityWhitelistNamed = true;

    @ConfigEntry(path = "entity.whitelist-leashed", comment = "{cleaner_config_comment_entity_whitelist_leashed}")
    private boolean entityWhitelistLeashed = true;

    @ConfigEntry(path = "entity.whitelist-tamed", comment = "{cleaner_config_comment_entity_whitelist_tamed}")
    private boolean entityWhitelistTamed = true;
    
    // ============ World Settings ============
    @ConfigEntry(path = "worlds.blacklist", comment = "{cleaner_config_comment_worlds_blacklist}")
    private List<String> worldBlacklist = Arrays.asList(
        "world_creative"
    );
    
    // ============ Smart Cleanup ============
    @ConfigEntry(path = "smart.enabled", comment = "{cleaner_config_comment_smart_enabled}")
    private boolean smartCleanEnabled = false;

    @Range(min = 100, max = 10000)
    @ConfigEntry(path = "smart.item-threshold", comment = "{cleaner_config_comment_smart_item_threshold}")
    private int itemMaxThreshold = 2000;

    @Range(min = 100, max = 5000)
    @ConfigEntry(path = "smart.mob-threshold", comment = "{cleaner_config_comment_smart_mob_threshold}")
    private int mobMaxThreshold = 1000;

    @Range(min = 30, max = 600)
    @ConfigEntry(path = "smart.cooldown", comment = "{cleaner_config_comment_smart_cooldown}")
    private int smartCleanCooldown = 60;

    // ============ Batch Processing ============
    @Range(min = 10, max = 500)
    @ConfigEntry(path = "batch.size", comment = "{cleaner_config_comment_batch_size}")
    private int cleanBatchSize = 50;

    @ConfigEntry(path = "batch.show-progress", comment = "{cleaner_config_comment_batch_show_progress}")
    private boolean showCleanProgress = false;
    
    // ============ TPS Adaptive ============
    @ConfigEntry(path = "tps.adaptive-enabled", comment = "{cleaner_config_comment_tps_adaptive_enabled}")
    private boolean tpsAdaptiveEnabled = true;

    @NotEmpty
    @ConfigEntry(path = "tps.sample-window", comment = "{cleaner_config_comment_tps_sample_window}")
    private String tpsSampleWindow = "1m";

    @Range(min = 10, max = 20)
    @ConfigEntry(path = "tps.low-threshold", comment = "{cleaner_config_comment_tps_low_threshold}")
    private double lowTpsThreshold = 18.0;

    @Range(min = 5, max = 18)
    @ConfigEntry(path = "tps.critical-threshold", comment = "{cleaner_config_comment_tps_critical_threshold}")
    private double criticalTpsThreshold = 15.0;

    @Range(min = 0, max = 80)
    @ConfigEntry(path = "tps.low-reduction", comment = "{cleaner_config_comment_tps_low_reduction}")
    private int lowTpsReduction = 30;

    @Range(min = 0, max = 90)
    @ConfigEntry(path = "tps.critical-reduction", comment = "{cleaner_config_comment_tps_critical_reduction}")
    private int criticalTpsReduction = 50;
    
    // ============ Messages ============
    // Each message's Java default is the one it shipped with in every earlier version, which the
    // framework writes for a missing key; materializeText() then writes the language file's text in
    // the server's language while the value is still built-in text (maintainer decision 2026-09-25).
    @NotEmpty
    @ConfigEntry(path = "messages.warn", comment = "{cleaner_config_comment_messages_warn}")
    private String warnMessage = SHIPPED_WARN;

    @NotEmpty
    @ConfigEntry(path = "messages.entity-warn", comment = "{cleaner_config_comment_messages_entity_warn}")
    private String entityWarnMessage = SHIPPED_ENTITY_WARN;

    @NotEmpty
    @ConfigEntry(path = "messages.item-cleaned", comment = "{cleaner_config_comment_messages_item_cleaned}")
    private String itemCleanedMessage = SHIPPED_ITEM_CLEANED;

    @NotEmpty
    @ConfigEntry(path = "messages.entity-cleaned", comment = "{cleaner_config_comment_messages_entity_cleaned}")
    private String entityCleanedMessage = SHIPPED_ENTITY_CLEANED;

    @NotEmpty
    @ConfigEntry(path = "messages.smart-triggered", comment = "{cleaner_config_comment_messages_smart_triggered}")
    private String smartCleanTriggeredMessage = SHIPPED_SMART_TRIGGERED;

    @NotEmpty
    @ConfigEntry(path = "messages.clean-progress", comment = "{cleaner_config_comment_messages_clean_progress}")
    private String cleanProgressMessage = SHIPPED_CLEAN_PROGRESS;

    @NotEmpty
    @ConfigEntry(path = "messages.clean-cancelled", comment = "{cleaner_config_comment_messages_clean_cancelled}")
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
     * @param text catalogue key to text in the server's language, from this jar's own catalogue
     *             ({@code ConfigTextDefaults#jarLanguage}), so every value written is in the tracked set
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
