package com.ultikits.plugins.cleaner;

import com.ultikits.plugins.cleaner.config.ConfigTextDefaults;
import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.config.RemovedConfigKeys;
import com.ultikits.plugins.cleaner.service.CleanerService;
import com.ultikits.plugins.cleaner.service.TpsAwareScheduler;
import com.ultikits.plugins.cleaner.utils.ServerTypeUtil;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.UltiToolsModule;

import java.io.File;
import java.io.IOException;

/**
 * UltiCleaner - Advanced automatic entity and item cleanup for Minecraft servers.
 * <p>
 * Features:
 * - Automatic cleanup of dropped items and entities
 * - Smart cleanup based on entity count thresholds
 * - TPS-adaptive threshold adjustment
 * - Batch processing to minimize lag spikes
 * - Custom events for extensibility
 * </p>
 *
 * @author wisdomme
 * @version 2.0.0
 */
@UltiToolsModule(scanBasePackages = {"com.ultikits.plugins.cleaner"})
public class UltiCleaner extends UltiToolsPlugin {

    /** The one configuration file this module owns, relative to its own config folder. */
    private static final String CONFIG_FILE = "config/cleaner.yml";

    @Override
    public boolean registerSelf() {
        // Log server type
        getLogger().info(i18n("log_server_detected").replace("{SERVER}", ServerTypeUtil.getServerSoftware()));

        // Tell the operator about keys this version no longer reads but which are still in
        // their own file -- deleting a key from CleanerConfig does nothing to files on disk.
        warnAboutRemovedConfigKeys();
        writeConfigTextInServerLanguage();

        // Load configuration caches
        CleanerService cleanerService = getContext().getBean(CleanerService.class);
        if (cleanerService != null) {
            cleanerService.init();
        }

        // Initialize TPS scheduler
        TpsAwareScheduler tpsScheduler = getContext().getBean(TpsAwareScheduler.class);
        if (tpsScheduler != null) {
            tpsScheduler.init();
        }

        getLogger().info(i18n("cleaner_enabled"));
        return true;
    }

    @Override
    protected void onReload() {
        warnAboutRemovedConfigKeys();
        writeConfigTextInServerLanguage();
        CleanerService cleanerService = getContext().getBean(CleanerService.class);
        if (cleanerService != null) {
            cleanerService.reload();
        }
        getLogger().info(i18n("cleaner_reloaded"));
    }

    /**
     * Writes every broadcast message in {@code config/cleaner.yml} that is still built-in text in the
     * server's language and saves the file once, so the file holds what the module broadcasts; any other
     * value is the operator's and is kept (maintainer decision 2026-09-25, UltiKits/UltiCleaner#17).
     * Runs from {@link #registerSelf()} and from {@link #onReload()}, both after the module's language is
     * loaded -- never from a configuration change listener, which the framework fires before it reloads
     * the language. A value already in the current language matches nothing to replace, so a second
     * start writes nothing.
     * The text comes from this jar's own catalogue for the server's language, not from {@code i18n} (which
     * reads the operator's extracted language file first), so every value written is one the next pass
     * recognises (orchestrator ruling O3, 2026-09-25).
     */
    private void writeConfigTextInServerLanguage() {
        CleanerConfig config = getContext().getBean(CleanerConfig.class);
        if (config == null || !config.materializeText(ConfigTextDefaults.jarLanguage(CleanerConfig.class, getLanguageCode())::getLocalizedText)) {
            return;
        }
        try {
            config.save();
        } catch (IOException e) {
            getLogger().warn(i18n("log_config_default_save_failed")
                    .replace("{FILE}", CONFIG_FILE)
                    .replace("{ERROR}", String.valueOf(e.getMessage())));
        }
    }

    private void warnAboutRemovedConfigKeys() {
        RemovedConfigKeys.warnAboutLeftovers(operatorConfigFile(), getLogger()::warn, this);
    }

    /**
     * The operator's own copy of this module's configuration file.
     * <p>
     * A seam, package-private on purpose. {@code UltiToolsPlugin#getConfigFile} is {@code protected}
     * and {@code final}, so a test in this package can neither call it nor stub it, and a mocked
     * plugin returns {@code null} from it -- which means that without this method the removed-key
     * check's wiring cannot be asserted at all, only its predicate. Overriding this one method lets
     * a test point the check at a real file and prove the call actually happens.
     *
     * @return the file {@code config/cleaner.yml} resolves to for this installation
     */
    File operatorConfigFile() {
        return getConfigFile(CONFIG_FILE);
    }
}
