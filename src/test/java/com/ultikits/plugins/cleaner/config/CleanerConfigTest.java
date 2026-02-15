package com.ultikits.plugins.cleaner.config;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CleanerConfig Tests")
class CleanerConfigTest {

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have item cleanup enabled by default")
        void itemCleanEnabled() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isItemCleanEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have 300 second default item interval")
        void itemCleanInterval() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getItemCleanInterval()).isEqualTo(300);
        }

        @Test
        @DisplayName("Should have entity cleanup enabled by default")
        void entityCleanEnabled() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isEntityCleanEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have 600 second default entity interval")
        void entityCleanInterval() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getEntityCleanInterval()).isEqualTo(600);
        }

        @Test
        @DisplayName("Should have smart cleanup disabled by default")
        void smartCleanEnabled() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isSmartCleanEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should have item threshold of 2000")
        void itemMaxThreshold() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getItemMaxThreshold()).isEqualTo(2000);
        }

        @Test
        @DisplayName("Should have mob threshold of 1000")
        void mobMaxThreshold() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getMobMaxThreshold()).isEqualTo(1000);
        }

        @Test
        @DisplayName("Should have batch size of 50")
        void cleanBatchSize() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getCleanBatchSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should have TPS adaptive enabled by default")
        void tpsAdaptiveEnabled() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isTpsAdaptiveEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have low TPS threshold of 18.0")
        void lowTpsThreshold() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getLowTpsThreshold()).isEqualTo(18.0);
        }

        @Test
        @DisplayName("Should have critical TPS threshold of 15.0")
        void criticalTpsThreshold() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getCriticalTpsThreshold()).isEqualTo(15.0);
        }

        @Test
        @DisplayName("Should have chunk unload disabled by default")
        void chunkUnloadEnabled() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isChunkUnloadEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should have max chunk distance of 20")
        void maxChunkDistance() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getMaxChunkDistance()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should have item whitelist with default values")
        void itemWhitelist() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getItemWhitelist()).isNotEmpty();
            assertThat(config.getItemWhitelist()).contains("DIAMOND", "EMERALD");
        }

        @Test
        @DisplayName("Should have entity types list with default values")
        void entityTypes() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getEntityTypes()).isNotEmpty();
            assertThat(config.getEntityTypes()).contains("ZOMBIE", "SKELETON");
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("Should update item clean enabled")
        void setItemCleanEnabled() {
            CleanerConfig config = createRealConfig();
            config.setItemCleanEnabled(false);
            assertThat(config.isItemCleanEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should update item clean interval")
        void setItemCleanInterval() {
            CleanerConfig config = createRealConfig();
            config.setItemCleanInterval(600);
            assertThat(config.getItemCleanInterval()).isEqualTo(600);
        }

        @Test
        @DisplayName("Should update entity clean enabled")
        void setEntityCleanEnabled() {
            CleanerConfig config = createRealConfig();
            config.setEntityCleanEnabled(false);
            assertThat(config.isEntityCleanEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should update smart clean enabled")
        void setSmartCleanEnabled() {
            CleanerConfig config = createRealConfig();
            config.setSmartCleanEnabled(true);
            assertThat(config.isSmartCleanEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should update item max threshold")
        void setItemMaxThreshold() {
            CleanerConfig config = createRealConfig();
            config.setItemMaxThreshold(3000);
            assertThat(config.getItemMaxThreshold()).isEqualTo(3000);
        }

        @Test
        @DisplayName("Should update TPS adaptive enabled")
        void setTpsAdaptiveEnabled() {
            CleanerConfig config = createRealConfig();
            config.setTpsAdaptiveEnabled(false);
            assertThat(config.isTpsAdaptiveEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should update chunk unload enabled")
        void setChunkUnloadEnabled() {
            CleanerConfig config = createRealConfig();
            config.setChunkUnloadEnabled(true);
            assertThat(config.isChunkUnloadEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Additional Default Values")
    class AdditionalDefaultValues {

        @Test
        @DisplayName("Should have item ignore named enabled by default")
        void itemIgnoreNamed() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isItemIgnoreNamed()).isTrue();
        }

        @Test
        @DisplayName("Should have item ignore recent seconds of 30")
        void itemIgnoreRecentSeconds() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getItemIgnoreRecentSeconds()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should have entity whitelist named enabled by default")
        void entityWhitelistNamed() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isEntityWhitelistNamed()).isTrue();
        }

        @Test
        @DisplayName("Should have entity whitelist leashed enabled by default")
        void entityWhitelistLeashed() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isEntityWhitelistLeashed()).isTrue();
        }

        @Test
        @DisplayName("Should have entity whitelist tamed enabled by default")
        void entityWhitelistTamed() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isEntityWhitelistTamed()).isTrue();
        }

        @Test
        @DisplayName("Should have world blacklist with default values")
        void worldBlacklist() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getWorldBlacklist()).isNotEmpty();
            assertThat(config.getWorldBlacklist()).contains("world_creative");
        }

        @Test
        @DisplayName("Should have smart clean cooldown of 60")
        void smartCleanCooldown() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getSmartCleanCooldown()).isEqualTo(60);
        }

        @Test
        @DisplayName("Should have show clean progress disabled by default")
        void showCleanProgress() {
            CleanerConfig config = createRealConfig();
            assertThat(config.isShowCleanProgress()).isFalse();
        }

        @Test
        @DisplayName("Should have TPS sample window of 1m")
        void tpsSampleWindow() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getTpsSampleWindow()).isEqualTo("1m");
        }

        @Test
        @DisplayName("Should have low TPS reduction of 30")
        void lowTpsReduction() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getLowTpsReduction()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should have critical TPS reduction of 50")
        void criticalTpsReduction() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getCriticalTpsReduction()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should have chunk unload batch size of 5")
        void chunkUnloadBatchSize() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getChunkUnloadBatchSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should have chunk unload timeout of 5")
        void chunkUnloadTimeout() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getChunkUnloadTimeout()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should have item warn times with default values")
        void itemWarnTimes() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getItemWarnTimes()).isNotEmpty();
            assertThat(config.getItemWarnTimes()).contains(60, 30, 10, 5, 3, 2, 1);
        }

        @Test
        @DisplayName("Should have entity warn times with default values")
        void entityWarnTimes() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getEntityWarnTimes()).isNotEmpty();
            assertThat(config.getEntityWarnTimes()).contains(60, 30, 10, 5, 3, 2, 1);
        }

        @Test
        @DisplayName("Should have entity clean interval of 600")
        void entityCleanInterval() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getEntityCleanInterval()).isEqualTo(600);
        }

        @Test
        @DisplayName("Should have message prefix")
        void messagePrefix() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getMessagePrefix()).isNotEmpty();
        }

        @Test
        @DisplayName("Should have warn message with {TIME} placeholder")
        void warnMessage() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getWarnMessage()).contains("{TIME}");
        }

        @Test
        @DisplayName("Should have entity warn message with {TIME} placeholder")
        void entityWarnMessage() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getEntityWarnMessage()).contains("{TIME}");
        }

        @Test
        @DisplayName("Should have item cleaned message with {COUNT} placeholder")
        void itemCleanedMessage() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getItemCleanedMessage()).contains("{COUNT}");
        }

        @Test
        @DisplayName("Should have entity cleaned message with {COUNT} placeholder")
        void entityCleanedMessage() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getEntityCleanedMessage()).contains("{COUNT}");
        }

        @Test
        @DisplayName("Should have smart clean triggered message")
        void smartCleanTriggeredMessage() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getSmartCleanTriggeredMessage()).isNotEmpty();
        }

        @Test
        @DisplayName("Should have clean progress message with placeholders")
        void cleanProgressMessage() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getCleanProgressMessage()).contains("{CURRENT}");
            assertThat(config.getCleanProgressMessage()).contains("{TOTAL}");
        }

        @Test
        @DisplayName("Should have clean cancelled message")
        void cleanCancelledMessage() {
            CleanerConfig config = createRealConfig();
            assertThat(config.getCleanCancelledMessage()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Additional Setters")
    class AdditionalSetters {

        @Test
        @DisplayName("Should update entity clean interval")
        void setEntityCleanInterval() {
            CleanerConfig config = createRealConfig();
            config.setEntityCleanInterval(1200);
            assertThat(config.getEntityCleanInterval()).isEqualTo(1200);
        }

        @Test
        @DisplayName("Should update item ignore named")
        void setItemIgnoreNamed() {
            CleanerConfig config = createRealConfig();
            config.setItemIgnoreNamed(false);
            assertThat(config.isItemIgnoreNamed()).isFalse();
        }

        @Test
        @DisplayName("Should update item ignore recent seconds")
        void setItemIgnoreRecentSeconds() {
            CleanerConfig config = createRealConfig();
            config.setItemIgnoreRecentSeconds(60);
            assertThat(config.getItemIgnoreRecentSeconds()).isEqualTo(60);
        }

        @Test
        @DisplayName("Should update entity whitelist named")
        void setEntityWhitelistNamed() {
            CleanerConfig config = createRealConfig();
            config.setEntityWhitelistNamed(false);
            assertThat(config.isEntityWhitelistNamed()).isFalse();
        }

        @Test
        @DisplayName("Should update entity whitelist leashed")
        void setEntityWhitelistLeashed() {
            CleanerConfig config = createRealConfig();
            config.setEntityWhitelistLeashed(false);
            assertThat(config.isEntityWhitelistLeashed()).isFalse();
        }

        @Test
        @DisplayName("Should update entity whitelist tamed")
        void setEntityWhitelistTamed() {
            CleanerConfig config = createRealConfig();
            config.setEntityWhitelistTamed(false);
            assertThat(config.isEntityWhitelistTamed()).isFalse();
        }

        @Test
        @DisplayName("Should update mob max threshold")
        void setMobMaxThreshold() {
            CleanerConfig config = createRealConfig();
            config.setMobMaxThreshold(2000);
            assertThat(config.getMobMaxThreshold()).isEqualTo(2000);
        }

        @Test
        @DisplayName("Should update smart clean cooldown")
        void setSmartCleanCooldown() {
            CleanerConfig config = createRealConfig();
            config.setSmartCleanCooldown(120);
            assertThat(config.getSmartCleanCooldown()).isEqualTo(120);
        }

        @Test
        @DisplayName("Should update clean batch size")
        void setCleanBatchSize() {
            CleanerConfig config = createRealConfig();
            config.setCleanBatchSize(100);
            assertThat(config.getCleanBatchSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should update show clean progress")
        void setShowCleanProgress() {
            CleanerConfig config = createRealConfig();
            config.setShowCleanProgress(true);
            assertThat(config.isShowCleanProgress()).isTrue();
        }

        @Test
        @DisplayName("Should update TPS sample window")
        void setTpsSampleWindow() {
            CleanerConfig config = createRealConfig();
            config.setTpsSampleWindow("5m");
            assertThat(config.getTpsSampleWindow()).isEqualTo("5m");
        }

        @Test
        @DisplayName("Should update low TPS threshold")
        void setLowTpsThreshold() {
            CleanerConfig config = createRealConfig();
            config.setLowTpsThreshold(16.0);
            assertThat(config.getLowTpsThreshold()).isEqualTo(16.0);
        }

        @Test
        @DisplayName("Should update critical TPS threshold")
        void setCriticalTpsThreshold() {
            CleanerConfig config = createRealConfig();
            config.setCriticalTpsThreshold(12.0);
            assertThat(config.getCriticalTpsThreshold()).isEqualTo(12.0);
        }

        @Test
        @DisplayName("Should update low TPS reduction")
        void setLowTpsReduction() {
            CleanerConfig config = createRealConfig();
            config.setLowTpsReduction(40);
            assertThat(config.getLowTpsReduction()).isEqualTo(40);
        }

        @Test
        @DisplayName("Should update critical TPS reduction")
        void setCriticalTpsReduction() {
            CleanerConfig config = createRealConfig();
            config.setCriticalTpsReduction(60);
            assertThat(config.getCriticalTpsReduction()).isEqualTo(60);
        }

        @Test
        @DisplayName("Should update max chunk distance")
        void setMaxChunkDistance() {
            CleanerConfig config = createRealConfig();
            config.setMaxChunkDistance(30);
            assertThat(config.getMaxChunkDistance()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should update chunk unload batch size")
        void setChunkUnloadBatchSize() {
            CleanerConfig config = createRealConfig();
            config.setChunkUnloadBatchSize(10);
            assertThat(config.getChunkUnloadBatchSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should update chunk unload timeout")
        void setChunkUnloadTimeout() {
            CleanerConfig config = createRealConfig();
            config.setChunkUnloadTimeout(10);
            assertThat(config.getChunkUnloadTimeout()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should update message prefix")
        void setMessagePrefix() {
            CleanerConfig config = createRealConfig();
            config.setMessagePrefix("&b[Custom]");
            assertThat(config.getMessagePrefix()).isEqualTo("&b[Custom]");
        }

        @Test
        @DisplayName("Should update warn message")
        void setWarnMessage() {
            CleanerConfig config = createRealConfig();
            config.setWarnMessage("Custom warn {TIME}");
            assertThat(config.getWarnMessage()).isEqualTo("Custom warn {TIME}");
        }

        @Test
        @DisplayName("Should update entity warn message")
        void setEntityWarnMessage() {
            CleanerConfig config = createRealConfig();
            config.setEntityWarnMessage("Custom entity warn {TIME}");
            assertThat(config.getEntityWarnMessage()).isEqualTo("Custom entity warn {TIME}");
        }

        @Test
        @DisplayName("Should update item cleaned message")
        void setItemCleanedMessage() {
            CleanerConfig config = createRealConfig();
            config.setItemCleanedMessage("Cleaned {COUNT} items");
            assertThat(config.getItemCleanedMessage()).isEqualTo("Cleaned {COUNT} items");
        }

        @Test
        @DisplayName("Should update entity cleaned message")
        void setEntityCleanedMessage() {
            CleanerConfig config = createRealConfig();
            config.setEntityCleanedMessage("Cleaned {COUNT} entities");
            assertThat(config.getEntityCleanedMessage()).isEqualTo("Cleaned {COUNT} entities");
        }

        @Test
        @DisplayName("Should update smart clean triggered message")
        void setSmartCleanTriggeredMessage() {
            CleanerConfig config = createRealConfig();
            config.setSmartCleanTriggeredMessage("Smart clean!");
            assertThat(config.getSmartCleanTriggeredMessage()).isEqualTo("Smart clean!");
        }

        @Test
        @DisplayName("Should update clean progress message")
        void setCleanProgressMessage() {
            CleanerConfig config = createRealConfig();
            config.setCleanProgressMessage("{CURRENT} of {TOTAL}");
            assertThat(config.getCleanProgressMessage()).isEqualTo("{CURRENT} of {TOTAL}");
        }

        @Test
        @DisplayName("Should update clean cancelled message")
        void setCleanCancelledMessage() {
            CleanerConfig config = createRealConfig();
            config.setCleanCancelledMessage("Cancelled!");
            assertThat(config.getCleanCancelledMessage()).isEqualTo("Cancelled!");
        }

        @Test
        @DisplayName("Should update item whitelist")
        void setItemWhitelist() {
            CleanerConfig config = createRealConfig();
            config.setItemWhitelist(java.util.Arrays.asList("GOLD_INGOT", "IRON_INGOT"));
            assertThat(config.getItemWhitelist()).containsExactly("GOLD_INGOT", "IRON_INGOT");
        }

        @Test
        @DisplayName("Should update entity types")
        void setEntityTypes() {
            CleanerConfig config = createRealConfig();
            config.setEntityTypes(java.util.Arrays.asList("ZOMBIE", "DROWNED"));
            assertThat(config.getEntityTypes()).containsExactly("ZOMBIE", "DROWNED");
        }

        @Test
        @DisplayName("Should update world blacklist")
        void setWorldBlacklist() {
            CleanerConfig config = createRealConfig();
            config.setWorldBlacklist(java.util.Arrays.asList("world_test"));
            assertThat(config.getWorldBlacklist()).containsExactly("world_test");
        }

        @Test
        @DisplayName("Should update item warn times")
        void setItemWarnTimes() {
            CleanerConfig config = createRealConfig();
            config.setItemWarnTimes(java.util.Arrays.asList(30, 10, 5));
            assertThat(config.getItemWarnTimes()).containsExactly(30, 10, 5);
        }

        @Test
        @DisplayName("Should update entity warn times")
        void setEntityWarnTimes() {
            CleanerConfig config = createRealConfig();
            config.setEntityWarnTimes(java.util.Arrays.asList(30, 10, 5));
            assertThat(config.getEntityWarnTimes()).containsExactly(30, 10, 5);
        }
    }

    /**
     * Create a real CleanerConfig instance.
     * The constructor sets default field values via field initializers.
     */
    private CleanerConfig createRealConfig() {
        return new CleanerConfig();
    }
}
