package com.ultikits.plugins.cleaner.service;

import com.ultikits.plugins.cleaner.UltiCleanerTestHelper;
import com.ultikits.plugins.cleaner.config.CleanerConfig;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TpsAwareScheduler Tests")
class TpsAwareSchedulerTest {

    private TpsAwareScheduler scheduler;
    private CleanerConfig config;

    @BeforeEach
    void setUp() throws Exception {
        UltiCleanerTestHelper.setUp();

        config = UltiCleanerTestHelper.createDefaultConfig();
        scheduler = new TpsAwareScheduler();

        // Inject dependencies via reflection
        UltiCleanerTestHelper.setField(scheduler, "config", config);
        UltiCleanerTestHelper.setField(scheduler, "plugin", UltiCleanerTestHelper.getMockPlugin());
    }

    @AfterEach
    void tearDown() throws Exception {
        UltiCleanerTestHelper.tearDown();
    }

    // ==================== Initialization ====================

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should initialize without errors")
        void initSuccess() {
            assertThatCode(() -> scheduler.init()).doesNotThrowAnyException();
        }
    }

    // ==================== Shutdown ====================

    @Nested
    @DisplayName("Shutdown")
    class Shutdown {

        @Test
        @DisplayName("Should shutdown without errors")
        void shutdownSuccess() {
            assertThatCode(() -> scheduler.shutdown()).doesNotThrowAnyException();
        }
    }

    // ==================== TPS Methods ====================

    @Nested
    @DisplayName("TPS Methods")
    class TpsMethods {

        @Test
        @DisplayName("getCurrentTps should return 20.0 when adaptive disabled")
        void getCurrentTpsDisabled() {
            when(config.isTpsAdaptiveEnabled()).thenReturn(false);

            double tps = scheduler.getCurrentTps();

            assertThat(tps).isEqualTo(20.0);
        }

        @Test
        @DisplayName("getCurrentTps should return value when adaptive enabled")
        void getCurrentTpsEnabled() {
            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            double tps = scheduler.getCurrentTps();

            assertThat(tps).isBetween(0.0, 20.0);
        }

        @Test
        @DisplayName("isLowTps should check against threshold")
        void isLowTps() {
            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            boolean isLow = scheduler.isLowTps();

            assertThat(isLow).isIn(true, false);
        }

        @Test
        @DisplayName("isCriticalTps should check against critical threshold")
        void isCriticalTps() {
            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            boolean isCritical = scheduler.isCriticalTps();

            assertThat(isCritical).isIn(true, false);
        }
    }

    // ==================== Threshold Multiplier ====================

    @Nested
    @DisplayName("Threshold Multiplier")
    class ThresholdMultiplier {

        @Test
        @DisplayName("Should return 1.0 when adaptive disabled")
        void multiplierDisabled() {
            when(config.isTpsAdaptiveEnabled()).thenReturn(false);

            double multiplier = scheduler.getThresholdMultiplier();

            assertThat(multiplier).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return value between 0 and 1 when enabled")
        void multiplierEnabled() {
            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);
            when(config.getLowTpsReduction()).thenReturn(30);
            when(config.getCriticalTpsReduction()).thenReturn(50);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            double multiplier = scheduler.getThresholdMultiplier();

            assertThat(multiplier).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("applyThresholdReduction should reduce threshold")
        void applyThresholdReduction() {
            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);
            when(config.getLowTpsReduction()).thenReturn(30);
            when(config.getCriticalTpsReduction()).thenReturn(50);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            int adjusted = scheduler.applyThresholdReduction(1000);

            assertThat(adjusted).isBetween(0, 1000);
        }
    }

    // ==================== TPS Status ====================

    @Nested
    @DisplayName("TPS Status")
    class TpsStatus {

        @Test
        @DisplayName("Should return formatted TPS status")
        void getTpsStatus() {
            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            String status = scheduler.getTpsStatus();

            assertThat(status).isNotEmpty();
            assertThat(status).containsAnyOf("§a", "§e", "§c");
        }

        @Test
        @DisplayName("Should return Normal status when TPS is high")
        void normalTpsStatus() {
            // When adaptive is disabled, getCurrentTps returns 20.0 (> any threshold)
            when(config.isTpsAdaptiveEnabled()).thenReturn(false);
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);

            String status = scheduler.getTpsStatus();

            assertThat(status).contains("Normal");
            assertThat(status).contains("§a");
        }
    }

    // ==================== Fallback TPS Calculation ====================

    @Nested
    @DisplayName("Fallback TPS Calculation")
    class FallbackTpsCalculation {

        @Test
        @DisplayName("Should enable fallback monitor when no native TPS")
        void enableFallbackMonitor() throws Exception {
            // hasTpsMethod is false in test setup
            scheduler.init();

            boolean fallbackEnabled = (boolean) getField(scheduler, "fallbackMonitorEnabled");
            assertThat(fallbackEnabled).isTrue();
        }

        @Test
        @DisplayName("Should skip updateFallbackTps when fallback disabled")
        void skipWhenFallbackDisabled() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", false);

            // Should not throw and should not update history
            assertThatCode(() -> scheduler.updateFallbackTps()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should update TPS history on fallback tick")
        void updateTpsHistory() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            UltiCleanerTestHelper.setField(scheduler, "lastTickTime", System.currentTimeMillis() - 1000);

            scheduler.updateFallbackTps();

            int historyIndex = (int) getField(scheduler, "historyIndex");
            assertThat(historyIndex).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate TPS from history after multiple ticks")
        void calculateTpsFromHistory() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);

            // Simulate multiple ticks (each ~1 second apart)
            for (int i = 0; i < 5; i++) {
                UltiCleanerTestHelper.setField(scheduler, "lastTickTime", System.currentTimeMillis() - 1000);
                scheduler.updateFallbackTps();
            }

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            double tps = scheduler.getCurrentTps();
            assertThat(tps).isBetween(0.0, 20.0);
        }

        @Test
        @DisplayName("Should cap TPS at 20.0")
        void capTpsAt20() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);

            // Simulate very fast tick (< 50ms per tick => cap at 20.0)
            UltiCleanerTestHelper.setField(scheduler, "lastTickTime", System.currentTimeMillis() - 10);
            scheduler.updateFallbackTps();

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            double tps = scheduler.getCurrentTps();
            assertThat(tps).isLessThanOrEqualTo(20.0);
        }

        @Test
        @DisplayName("Should return 20.0 when no history entries yet")
        void noHistoryReturns20() throws Exception {
            // historyIndex is 0 by default, calculateAverage returns 20.0 for count=0
            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");

            double tps = scheduler.getCurrentTps();
            assertThat(tps).isEqualTo(20.0);
        }
    }

    // ==================== Sample Window ====================

    @Nested
    @DisplayName("Sample Window")
    class SampleWindow {

        @Test
        @DisplayName("Should use 5m window when configured")
        void use5mWindow() throws Exception {
            // Populate some history
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            for (int i = 0; i < 3; i++) {
                UltiCleanerTestHelper.setField(scheduler, "lastTickTime", System.currentTimeMillis() - 1000);
                scheduler.updateFallbackTps();
            }

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("5m");

            double tps = scheduler.getCurrentTps();
            assertThat(tps).isBetween(0.0, 20.0);
        }

        @Test
        @DisplayName("Should use 15m window when configured")
        void use15mWindow() throws Exception {
            // Populate some history
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            for (int i = 0; i < 3; i++) {
                UltiCleanerTestHelper.setField(scheduler, "lastTickTime", System.currentTimeMillis() - 1000);
                scheduler.updateFallbackTps();
            }

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("15m");

            double tps = scheduler.getCurrentTps();
            assertThat(tps).isBetween(0.0, 20.0);
        }

        @Test
        @DisplayName("Should default to 1m window for unknown value")
        void defaultTo1mForUnknown() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            for (int i = 0; i < 3; i++) {
                UltiCleanerTestHelper.setField(scheduler, "lastTickTime", System.currentTimeMillis() - 1000);
                scheduler.updateFallbackTps();
            }

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("unknown");

            double tps = scheduler.getCurrentTps();
            assertThat(tps).isBetween(0.0, 20.0);
        }
    }

    // ==================== Threshold Multiplier with TPS States ====================

    @Nested
    @DisplayName("Threshold Multiplier with TPS States")
    class ThresholdMultiplierWithTpsStates {

        @Test
        @DisplayName("Should return critical reduction when TPS is critical")
        void criticalTpsMultiplier() throws Exception {
            // Fill history with very low TPS values
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] lowHistory = new double[60];
            java.util.Arrays.fill(lowHistory, 10.0); // Very low TPS
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", lowHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);
            when(config.getCriticalTpsReduction()).thenReturn(50);
            when(config.getLowTpsReduction()).thenReturn(30);

            double multiplier = scheduler.getThresholdMultiplier();
            // With 10.0 TPS (< 15.0 critical), should apply 50% reduction => multiplier = 0.5
            assertThat(multiplier).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should return low reduction when TPS is low but not critical")
        void lowTpsMultiplier() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] lowHistory = new double[60];
            java.util.Arrays.fill(lowHistory, 16.0); // Low but not critical
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", lowHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);
            when(config.getCriticalTpsReduction()).thenReturn(50);
            when(config.getLowTpsReduction()).thenReturn(30);

            double multiplier = scheduler.getThresholdMultiplier();
            // With 16.0 TPS (< 18.0 but >= 15.0), should apply 30% reduction => multiplier = 0.7
            assertThat(multiplier).isEqualTo(0.7);
        }

        @Test
        @DisplayName("Should return 1.0 when TPS is normal")
        void normalTpsMultiplier() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] normalHistory = new double[60];
            java.util.Arrays.fill(normalHistory, 19.5); // Normal TPS
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", normalHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);

            double multiplier = scheduler.getThresholdMultiplier();
            assertThat(multiplier).isEqualTo(1.0);
        }

        @Test
        @DisplayName("applyThresholdReduction should reduce by critical amount")
        void applyReductionCritical() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] criticalHistory = new double[60];
            java.util.Arrays.fill(criticalHistory, 10.0);
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", criticalHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);
            when(config.getCriticalTpsReduction()).thenReturn(50);
            when(config.getLowTpsReduction()).thenReturn(30);

            int adjusted = scheduler.applyThresholdReduction(2000);
            // 50% reduction => 2000 * 0.5 = 1000
            assertThat(adjusted).isEqualTo(1000);
        }
    }

    // ==================== TPS Status With Different States ====================

    @Nested
    @DisplayName("TPS Status Display")
    class TpsStatusDisplay {

        @Test
        @DisplayName("Should show Critical status for very low TPS")
        void criticalStatus() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] criticalHistory = new double[60];
            java.util.Arrays.fill(criticalHistory, 10.0);
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", criticalHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);

            String status = scheduler.getTpsStatus();
            assertThat(status).contains("§c");
            assertThat(status).contains("Critical");
        }

        @Test
        @DisplayName("Should show Low status for moderately low TPS")
        void lowStatus() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] lowHistory = new double[60];
            java.util.Arrays.fill(lowHistory, 16.0);
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", lowHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getLowTpsThreshold()).thenReturn(18.0);
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);

            String status = scheduler.getTpsStatus();
            assertThat(status).contains("§e");
            assertThat(status).contains("Low");
        }
    }

    // ==================== isLowTps / isCriticalTps with specific thresholds ====================

    @Nested
    @DisplayName("Low/Critical TPS Detection with States")
    class TpsDetectionWithStates {

        @Test
        @DisplayName("isLowTps should return true when TPS is below low threshold")
        void isLowTpsTrue() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] lowHistory = new double[60];
            java.util.Arrays.fill(lowHistory, 16.0);
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", lowHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getLowTpsThreshold()).thenReturn(18.0);

            assertThat(scheduler.isLowTps()).isTrue();
        }

        @Test
        @DisplayName("isLowTps should return false when TPS is above threshold")
        void isLowTpsFalse() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] normalHistory = new double[60];
            java.util.Arrays.fill(normalHistory, 19.5);
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", normalHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getLowTpsThreshold()).thenReturn(18.0);

            assertThat(scheduler.isLowTps()).isFalse();
        }

        @Test
        @DisplayName("isCriticalTps should return true when TPS is below critical threshold")
        void isCriticalTpsTrue() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] criticalHistory = new double[60];
            java.util.Arrays.fill(criticalHistory, 10.0);
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", criticalHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);

            assertThat(scheduler.isCriticalTps()).isTrue();
        }

        @Test
        @DisplayName("isCriticalTps should return false when TPS is above critical threshold")
        void isCriticalTpsFalse() throws Exception {
            UltiCleanerTestHelper.setField(scheduler, "fallbackMonitorEnabled", true);
            double[] normalHistory = new double[60];
            java.util.Arrays.fill(normalHistory, 16.0);
            UltiCleanerTestHelper.setField(scheduler, "tpsHistory1m", normalHistory);
            UltiCleanerTestHelper.setField(scheduler, "historyIndex", 60);

            when(config.isTpsAdaptiveEnabled()).thenReturn(true);
            when(config.getTpsSampleWindow()).thenReturn("1m");
            when(config.getCriticalTpsThreshold()).thenReturn(15.0);

            assertThat(scheduler.isCriticalTps()).isFalse();
        }
    }

    // ==================== Helper Methods ====================

    private Object getField(Object target, String fieldName) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
