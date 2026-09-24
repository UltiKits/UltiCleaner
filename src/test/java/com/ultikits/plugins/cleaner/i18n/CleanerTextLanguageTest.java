package com.ultikits.plugins.cleaner.i18n;

import com.ultikits.plugins.cleaner.UltiCleaner;
import com.ultikits.plugins.cleaner.UltiCleanerTestHelper;
import com.ultikits.plugins.cleaner.commands.CleanCommand;
import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.service.CleanerService;
import com.ultikits.plugins.cleaner.service.TpsAwareScheduler;
import com.ultikits.plugins.cleaner.utils.ServerTypeUtil;
import com.ultikits.ultitools.annotations.command.CmdExecutor;
import com.ultikits.ultitools.context.SimpleContainer;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Text this module shows to players and writes to the console follows the framework's
 * {@code language} setting (UltiKits/UltiCleaner#17).
 * <p>
 * The module's {@code i18n} answers from the catalogue this module really ships ({@link CatalogueText}).
 * Before the language sweep every {@code /clean} line was fixed Chinese text and the console lines
 * below were fixed English text, while the catalogue already held English and Chinese text for most
 * of them that no code read.
 */
@DisplayName("UltiCleaner text follows the language setting (UltiKits/UltiCleaner#17)")
class CleanerTextLanguageTest {

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    /** The catalogue text for {@code key} with its colour codes applied, or a marker naming the missing key. */
    private static String line(String code, String key, String... tokenValuePairs) {
        String value = CatalogueText.entries(code).get(key);
        if (value == null) {
            return "<lang/" + code + " has no " + key + ">";
        }
        for (int i = 0; i + 1 < tokenValuePairs.length; i += 2) {
            value = value.replace(tokenValuePairs[i], tokenValuePairs[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private static UltiCleaner pluginSpeaking(String code) {
        UltiCleaner plugin = mock(UltiCleaner.class);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer(code));
        return plugin;
    }

    @BeforeEach
    void setUp() throws Exception {
        UltiCleanerTestHelper.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        UltiCleanerTestHelper.tearDown();
    }

    @Nested
    @DisplayName("/clean under language: en")
    class CommandInEnglish {

        private CleanerService service;
        private CleanerConfig config;
        private TpsAwareScheduler tps;
        private CleanCommand command;
        private CommandSender sender;

        @BeforeEach
        void build() {
            service = mock(CleanerService.class);
            config = UltiCleanerTestHelper.createDefaultConfig();
            tps = mock(TpsAwareScheduler.class);
            when(service.getTpsScheduler()).thenReturn(tps);
            command = CleanerSeams.command(service, config, pluginSpeaking("en"));
            sender = mock(CommandSender.class);
        }

        private List<String> sent() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeastOnce()).sendMessage(captor.capture());
            assertThat(captor.getAllValues()).noneMatch(l -> CJK.matcher(l).find());
            return captor.getAllValues();
        }

        @Test
        @DisplayName("the command description the framework translates has English text")
        void description() {
            String key = CleanCommand.class.getAnnotation(CmdExecutor.class).description();

            assertThat(CatalogueText.entries("en").get(key)).isEqualTo("Clean up ground items and entities");
        }

        @Test
        @DisplayName("items, entities and all report what they started in English")
        void startedLines() {
            when(service.forceCleanItems()).thenReturn(12);
            when(service.forceCleanEntities()).thenReturn(34);

            command.cleanItems(sender);
            command.cleanEntities(sender);
            command.cleanAll(sender);

            assertThat(sent()).containsExactly(
                    line("en", "clean_started_items", "{COUNT}", "12"),
                    line("en", "clean_started_entities", "{COUNT}", "34"),
                    line("en", "clean_started_all", "{ITEMS}", "12", "{ENTITIES}", "34"));
        }

        @Test
        @DisplayName("a clean already running is reported in English")
        void inProgress() {
            when(service.isCleaningInProgress()).thenReturn(true);

            command.cleanItems(sender);

            assertThat(sent()).containsExactly(line("en", "clean_in_progress"));
        }

        @Test
        @DisplayName("check prints the statistics in English")
        void check() {
            Map<String, Integer> counts = new HashMap<>();
            counts.put("items", 5);
            counts.put("mobs", 6);
            counts.put("total", 7);
            when(service.getEntityCounts()).thenReturn(counts);
            when(service.getTotalLoadedChunks()).thenReturn(8);
            when(tps.getTpsStatus()).thenReturn("20.00");

            command.check(sender);

            assertThat(sent()).containsExactly(
                    line("en", "stats_title"),
                    line("en", "stats_items", "{COUNT}", "5"),
                    line("en", "stats_mobs", "{COUNT}", "6"),
                    line("en", "stats_total", "{COUNT}", "7"),
                    line("en", "stats_chunks_loaded", "{COUNT}", "8"),
                    line("en", "stats_tps", "{TPS}", "20.00"));
        }

        @Test
        @DisplayName("status prints the countdowns, the state and a critical TPS warning in English")
        void statusCritical() {
            when(service.getItemCountdown()).thenReturn(30);
            when(service.getEntityCountdown()).thenReturn(40);
            when(service.isCleaningInProgress()).thenReturn(true);
            when(tps.getTpsStatus()).thenReturn("12.00");
            when(tps.isCriticalTps()).thenReturn(true);
            when(config.getCriticalTpsReduction()).thenReturn(50);

            command.status(sender);

            assertThat(sent()).containsExactly(
                    line("en", "status_title"),
                    line("en", "status_next_item_clean", "{TIME}", "30"),
                    line("en", "status_next_entity_clean", "{TIME}", "40"),
                    line("en", "status_cleaning"),
                    line("en", "status_tps", "{TPS}", "12.00"),
                    line("en", "status_tps_critical", "{PERCENT}", "50"));
        }

        @Test
        @DisplayName("status prints the idle state and a low TPS warning in English")
        void statusLow() {
            when(tps.getTpsStatus()).thenReturn("17.00");
            when(tps.isLowTps()).thenReturn(true);
            when(config.getLowTpsReduction()).thenReturn(30);

            command.status(sender);

            List<String> lines = sent();
            assertThat(lines).contains(line("en", "status_idle"), line("en", "status_tps_low", "{PERCENT}", "30"));
        }

        @Test
        @DisplayName("the help lines are English")
        void help() {
            command.help(sender);

            assertThat(sent()).containsExactly(
                    line("en", "cmd_help_title"),
                    line("en", "cmd_help_items"),
                    line("en", "cmd_help_entities"),
                    line("en", "cmd_help_all"),
                    line("en", "cmd_help_check"),
                    line("en", "cmd_help_status"));
        }
    }

    @Nested
    @DisplayName("Console lines under language: zh")
    class ConsoleInChinese {

        private List<String> logged(PluginLogger logger, String level) {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            if ("warn".equals(level)) {
                verify(logger, atLeast(0)).warn(captor.capture());
            } else {
                verify(logger, atLeast(0)).info(captor.capture());
            }
            return captor.getAllValues();
        }

        @Test
        @DisplayName("start-up names the detected server in Chinese")
        void serverDetected() {
            UltiCleaner plugin = pluginSpeaking("zh");
            PluginLogger logger = mock(PluginLogger.class);
            SimpleContainer context = mock(SimpleContainer.class);
            when(plugin.getLogger()).thenReturn(logger);
            when(plugin.getContext()).thenReturn(context);
            when(plugin.registerSelf()).thenCallRealMethod();

            plugin.registerSelf();

            assertThat(logged(logger, "info")).contains(
                    line("zh", "log_server_detected", "{SERVER}", ServerTypeUtil.getServerSoftware()));
        }

        @Test
        @DisplayName("the TPS monitor start and the TPS status are Chinese")
        void tpsMonitor() throws Exception {
            TpsAwareScheduler scheduler = new TpsAwareScheduler();
            UltiCleaner plugin = pluginSpeaking("zh");
            PluginLogger logger = mock(PluginLogger.class);
            when(plugin.getLogger()).thenReturn(logger);
            UltiCleanerTestHelper.setField(scheduler, "plugin", plugin);
            UltiCleanerTestHelper.setField(scheduler, "config", UltiCleanerTestHelper.createDefaultConfig());

            scheduler.init();

            assertThat(logged(logger, "info")).containsExactly(
                    line("zh", "log_tps_monitor_initialized", "{SERVER}", ServerTypeUtil.getServerSoftware()));
            assertThat(scheduler.getTpsStatus()).isEqualTo(
                    line("zh", "tps_status_normal", "{TPS}", String.format("%.2f", scheduler.getCurrentTps())));
        }

        @Test
        @DisplayName("an unknown entity type in the configuration is reported in Chinese")
        void unknownEntityType() throws Exception {
            CleanerService service = new CleanerService();
            UltiCleaner plugin = pluginSpeaking("zh");
            PluginLogger logger = mock(PluginLogger.class);
            when(plugin.getLogger()).thenReturn(logger);
            CleanerConfig config = UltiCleanerTestHelper.createDefaultConfig();
            when(config.getEntityTypes()).thenReturn(Arrays.asList("NOT_A_TYPE"));
            UltiCleanerTestHelper.setField(service, "plugin", plugin);
            UltiCleanerTestHelper.setField(service, "config", config);

            service.init();

            assertThat(logged(logger, "warn")).containsExactly(
                    line("zh", "log_unknown_entity_type", "{TYPE}", "NOT_A_TYPE"));
        }
    }
}
