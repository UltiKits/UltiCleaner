package com.ultikits.plugins.cleaner.service;

import com.ultikits.plugins.cleaner.UltiCleanerTestHelper;
import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The countdown warnings fire for {@code item.warn-times} and {@code entity.warn-times} read from a real
 * {@code config/cleaner.yml} by the framework's real config layer (UltiKits/UltiCleaner#22, and the
 * framework's typed list binding of 6.3.0).
 * <p>
 * Each case writes the file, loads it with the framework's {@code AbstractConfigEntity#init} and runs the
 * module's own countdown tick against the loaded list, so what is asserted is what an operator's file
 * does, not what a mocked list does. The framework binds each element as the declared {@code Integer},
 * including numbers a 6.2 file stored as text, and skips an element it cannot bind with a located warning.
 * <p>
 * 倒计时警告按框架真实读取的 warn-times 触发：文本写法的数字按整数绑定，无法绑定的元素被框架跳过并警告，其余照常使用。
 */
@DisplayName("Countdown warnings fire for warn-times read from a real file by the framework (#22)")
class CleanerWarnTimesFromFileTest {

    @TempDir
    Path tempDir;

    private Player online;
    private final List<String> frameworkWarnings = new ArrayList<>();
    private Handler capture;
    private Logger frameworkLogger;

    @BeforeEach
    void setUp() throws Exception {
        UltiCleanerTestHelper.setUp();
        online = mock(Player.class);
        doReturn(Collections.singletonList(online)).when(UltiCleanerTestHelper.getMockServer()).getOnlinePlayers();
        // the framework's config layer loads a file only on the server thread, and logs through the server's logger
        doReturn(true).when(UltiCleanerTestHelper.getMockServer()).isPrimaryThread();
        doReturn(Logger.getLogger("UltiCleanerTest")).when(UltiCleanerTestHelper.getMockServer()).getLogger();
        frameworkLogger = Logger.getLogger(com.ultikits.ultitools.abstracts.AbstractConfigEntity.class.getName());
        capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                frameworkWarnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        frameworkLogger.addHandler(capture);
    }

    @AfterEach
    void tearDown() throws Exception {
        frameworkLogger.removeHandler(capture);
        UltiCleanerTestHelper.tearDown();
    }

    /** Writes {@code yaml} as {@code config/cleaner.yml} and loads it with the framework's own init. */
    private CleanerConfig load(String yaml) throws IOException {
        File file = new File(tempDir.toFile(), "config/cleaner.yml");
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), yaml.getBytes(StandardCharsets.UTF_8));
        UltiToolsPlugin plugin = Mockito.mock(UltiToolsPlugin.class, invocation -> {
            String name = invocation.getMethod().getName();
            if ("getConfigFolder".equals(name)) {
                return tempDir.toString();
            }
            if ("getConfigFile".equals(name)) {
                return new File(tempDir.toFile(), invocation.<String>getArgument(0));
            }
            if ("i18n".equals(name)) {
                return CatalogueText.answer("en").answer(invocation);
            }
            if ("getPluginName".equals(name)) {
                return "UltiCleaner";
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        CleanerConfig config = new CleanerConfig();
        config.init(plugin);
        config.setWarnMessage("item {TIME}");
        config.setEntityWarnMessage("entity {TIME}");
        config.setItemWhitelist(Collections.<String>emptyList());
        config.setEntityTypes(Collections.<String>emptyList());
        config.setWorldBlacklist(Collections.<String>emptyList());
        return config;
    }

    private CleanerService serviceFor(CleanerConfig config) throws Exception {
        CleanerService service = new CleanerService();
        UltiCleanerTestHelper.setField(service, "config", config);
        UltiCleanerTestHelper.setField(service, "tpsScheduler", mock(TpsAwareScheduler.class));
        UltiCleanerTestHelper.setField(service, "plugin", UltiCleanerTestHelper.getMockPlugin());
        service.init();
        return service;
    }

    /** What the online player is sent while the countdown runs from {@code from} seconds down to 1. */
    private List<String> broadcastsCountingDownFrom(CleanerService service, String countdownField, String tickMethod,
                                                    int from) throws Exception {
        UltiCleanerTestHelper.setField(service, countdownField, from + 1);
        Method tick = CleanerService.class.getDeclaredMethod(tickMethod);
        tick.setAccessible(true);
        for (int second = from; second >= 1; second--) {
            tick.invoke(service);
        }
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(online, atLeast(0)).sendMessage(sent.capture());
        return sent.getAllValues();
    }

    @Test
    @DisplayName("item.warn-times written as quoted numbers (as 6.2 wrote them) warns at exactly those seconds")
    void itemWarningsFromQuotedNumbers() throws Exception {
        CleanerConfig config = load("item:\n  interval: 15\n  warn-times: ['12', '5', '3']\n");

        assertThat(config.getItemWarnTimes()).as("bound as the declared Integer").containsExactly(12, 5, 3);
        CleanerService service = serviceFor(config);

        assertThat(broadcastsCountingDownFrom(service, "itemCountdown", "tickItemClean", 12))
                .containsExactly("item 12", "item 5", "item 3");
    }

    @Test
    @DisplayName("entity.warn-times written as quoted numbers warns at exactly those seconds")
    void entityWarningsFromQuotedNumbers() throws Exception {
        CleanerConfig config = load("entity:\n  interval: 15\n  warn-times: ['12', '5', '3']\n");

        assertThat(config.getEntityWarnTimes()).containsExactly(12, 5, 3);
        CleanerService service = serviceFor(config);

        assertThat(broadcastsCountingDownFrom(service, "entityCountdown", "tickEntityClean", 12))
                .containsExactly("entity 12", "entity 5", "entity 3");
    }

    @Test
    @DisplayName("item.warn-times written as plain numbers warns at the configured seconds only")
    void itemWarningsFromPlainNumbers() throws Exception {
        CleanerConfig config = load("item:\n  interval: 15\n  warn-times: [5, 1]\n");

        CleanerService service = serviceFor(config);

        assertThat(broadcastsCountingDownFrom(service, "itemCountdown", "tickItemClean", 12))
                .containsExactly("item 5", "item 1");
    }

    @Test
    @DisplayName("an element that is not a whole number is skipped by the framework with a warning naming it, and the rest still warn")
    void aBadElementIsSkippedAndTheRestStillWarn() throws Exception {
        CleanerConfig config = load("item:\n  interval: 15\n  warn-times: [10, abc, 2.5, 1]\n");

        assertThat(config.getItemWarnTimes()).as("the elements the framework could bind").containsExactly(10, 1);
        assertThat(frameworkWarnings).as("the framework names each skipped element")
                .anySatisfy(w -> assertThat(w).contains("item.warn-times[1]").contains("abc"))
                .anySatisfy(w -> assertThat(w).contains("item.warn-times[2]").contains("2.5"));
        CleanerService service = serviceFor(config);

        assertThat(broadcastsCountingDownFrom(service, "itemCountdown", "tickItemClean", 12))
                .containsExactly("item 10", "item 1");
    }

    @Test
    @DisplayName("control: the capture sees the framework's warning for a bad element and sees none for a clean file")
    void aCleanFileProducesNoFrameworkWarning() throws Exception {
        load("item:\n  interval: 15\n  warn-times: [10, 1]\n");

        assertThat(frameworkWarnings).noneMatch(w -> w.contains("warn-times"));
    }
}
