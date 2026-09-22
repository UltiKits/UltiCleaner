package com.ultikits.plugins.cleaner;

import com.ultikits.plugins.cleaner.config.RemovedConfigKeys;
import com.ultikits.plugins.cleaner.service.CleanerService;
import com.ultikits.plugins.cleaner.service.TpsAwareScheduler;
import com.ultikits.plugins.cleaner.utils.ServerTypeUtil;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.UltiToolsModule;

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
        getLogger().info("Detected server: " + ServerTypeUtil.getServerSoftware());

        // Tell the operator about keys this version no longer reads but which are still in
        // their own file -- deleting a key from CleanerConfig does nothing to files on disk.
        warnAboutRemovedConfigKeys();

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
        CleanerService cleanerService = getContext().getBean(CleanerService.class);
        if (cleanerService != null) {
            cleanerService.reload();
        }
        getLogger().info(i18n("cleaner_reloaded"));
    }

    private void warnAboutRemovedConfigKeys() {
        RemovedConfigKeys.warnAboutLeftovers(getConfigFile(CONFIG_FILE), getLogger()::warn);
    }
}
