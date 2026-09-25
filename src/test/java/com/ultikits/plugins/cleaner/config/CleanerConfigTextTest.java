package com.ultikits.plugins.cleaner.config;

import com.ultikits.plugins.cleaner.UltiCleaner;
import com.ultikits.plugins.cleaner.i18n.CatalogueText;
import com.ultikits.plugins.cleaner.service.CleanerService;
import com.ultikits.ultitools.annotations.ConfigEntry;
import com.ultikits.ultitools.annotations.config.NotEmpty;
import com.ultikits.ultitools.interfaces.ConfigChangeListener;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code config/cleaner.yml} holds every broadcast message in the server's language, and the module
 * broadcasts exactly what the file holds (maintainer decision 2026-09-25, 17-CONTEXT D-16; UltiKits/UltiCleaner#17).
 * A value that is still built-in text — any language's text from this jar, or the default an earlier
 * version shipped — follows {@code language} at enable and on reload, in both
 * directions; anything else is the operator's and is kept byte for byte. Every case runs the framework's
 * real {@code AbstractConfigEntity#init} on a temporary folder, the module's real {@code registerSelf()}
 * and {@code onReload()}, and answers {@code i18n} from the module's real catalogues.
 */
@DisplayName("cleaner.yml holds the broadcast messages in the server's language (UltiKits/UltiCleaner#17)")
class CleanerConfigTextTest {

    /** One text setting: its field, its path in cleaner.yml, its catalogue key, its colour, its shipped default. */
    private static final class Setting {
        final String field;
        final String path;
        final String key;
        final String prefix;
        final String shipped;

        Setting(String field, String path, String key, String prefix, String shipped) {
            this.field = field;
            this.path = path;
            this.key = key;
            this.prefix = prefix;
            this.shipped = shipped;
        }

        String text(String code) {
            return prefix + CatalogueText.text(code, key);
        }

        String getter() {
            return "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
        }
    }

    /** The 7 settings, with the one default each shipped in every earlier version (census §4). */
    private static final List<Setting> SETTINGS = Arrays.asList(
            new Setting("warnMessage", "messages.warn", "item_warn", "", "&c[清理] &f地面物品将在 &e{TIME} &f秒后清理！"),
            new Setting("entityWarnMessage", "messages.entity-warn", "entity_warn", "", "&c[清理] &f实体将在 &e{TIME} &f秒后清理！"),
            new Setting("itemCleanedMessage", "messages.item-cleaned", "item_cleaned", "", "&a[清理] &f已清理 &e{COUNT} &f个地面物品！"),
            new Setting("entityCleanedMessage", "messages.entity-cleaned", "entity_cleaned", "", "&a[清理] &f已清理 &e{COUNT} &f个实体！"),
            new Setting("smartCleanTriggeredMessage", "messages.smart-triggered", "smart_clean_triggered", "", "&e[清理] &f检测到实体数量过多，正在进行智能清理..."),
            new Setting("cleanProgressMessage", "messages.clean-progress", "clean_progress", "", "&7[清理] &f清理进度: &e{CURRENT}&f/&e{TOTAL}"),
            new Setting("cleanCancelledMessage", "messages.clean-cancelled", "clean_cancelled", "", "&c[清理] &f清理操作被其他插件取消！"));

    /** The fields that carried {@code @NotEmpty} at origin/master: the 7 above plus four that never changed. */
    private static final Set<String> NOT_EMPTY_AT_MASTER = new TreeSet<>();

    static {
        for (Setting s : SETTINGS) {
            NOT_EMPTY_AT_MASTER.add(s.field);
        }
        NOT_EMPTY_AT_MASTER.addAll(Arrays.asList("itemWarnTimes", "entityWarnTimes", "entityTypes", "tpsSampleWindow"));
    }

    private static final String[] LANGUAGES = {"en", "zh"};

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    @TempDir
    Path tempDir;

    private final String[] language = {"en"};

    private final PluginLogger logger = mock(PluginLogger.class);

    /** The configuration the module double returns from {@code getConfig(CleanerConfig.class)}. */
    private CleanerConfig current;

    private UltiCleaner plugin;

    @BeforeEach
    void setUp() {
        plugin = moduleDouble();
    }

    @AfterEach
    void tearDown() {
        current = null;
    }

    @Test
    @DisplayName("the catalogues give each setting English text under en and exactly its shipped default under zh")
    void catalogueTexts() {
        for (Setting s : SETTINGS) {
            assertThat(s.text("zh")).as(s.field).isEqualTo(s.shipped);
            assertThat(s.text("en")).as(s.field).startsWith(s.prefix).doesNotMatch(".*" + CJK.pattern() + ".*");
        }
    }

    @Test
    @DisplayName("fresh start under en: cleaner.yml holds every setting's English text, and each getter returns the file's value")
    void freshStartEnglish() throws Exception {
        language[0] = "en";
        CleanerConfig config = spy(load());

        start(config);

        YamlConfiguration disk = onDisk();
        for (Setting s : SETTINGS) {
            assertThat(disk.getString(s.path)).as(s.path).isEqualTo(s.text("en"));
            assertThat(get(config, s)).as(s.field).isEqualTo(disk.getString(s.path));
        }
        verify(config, times(1)).save();
    }

    @Test
    @DisplayName("fresh start under zh: cleaner.yml holds every setting's Chinese text, which is its shipped default, and the module writes nothing")
    void freshStartChinese() throws Exception {
        language[0] = "zh";
        CleanerConfig config = spy(load());
        byte[] afterFramework = bytes();

        start(config);

        YamlConfiguration disk = onDisk();
        for (Setting s : SETTINGS) {
            assertThat(disk.getString(s.path)).as(s.path).isEqualTo(s.shipped);
            assertThat(get(config, s)).as(s.field).isEqualTo(s.shipped);
        }
        verify(config, never()).save();
        assertThat(bytes()).isEqualTo(afterFramework);
    }

    @Test
    @DisplayName("every built-in text in the file (shipped default, jar en text, jar zh text) is replaced with the current language's text and saved, under en and zh")
    void everyTrackedValueFollowsTheLanguage() throws Exception {
        for (String code : LANGUAGES) {
            for (String member : new String[] {"shipped", "en", "zh"}) {
                language[0] = code;
                Map<String, String> values = new LinkedHashMap<>();
                for (Setting s : SETTINGS) {
                    values.put(s.path, "shipped".equals(member) ? s.shipped : s.text(member));
                }
                write(values);
                CleanerConfig config = spy(load());

                start(config);

                YamlConfiguration disk = onDisk();
                for (Setting s : SETTINGS) {
                    String what = "language " + code + ", file held the " + member + " text of " + s.path;
                    assertThat(disk.getString(s.path)).as(what).isEqualTo(s.text(code));
                    assertThat(get(config, s)).as(what).isEqualTo(s.text(code));
                }
                boolean alreadyCurrent = member.equals(code) || ("shipped".equals(member) && "zh".equals(code));
                verify(config, times(alreadyCurrent ? 0 : 1)).save();
            }
        }
    }

    @Test
    @DisplayName("an upgraded file holding the shipped defaults reads exactly the English text under en (pinned, not read from the catalogue)")
    void upgradedFileReadsExactEnglish() throws Exception {
        language[0] = "en";
        Map<String, String> values = new LinkedHashMap<>();
        for (Setting s : SETTINGS) {
            values.put(s.path, s.shipped);
        }
        write(values);
        CleanerConfig config = spy(load());

        start(config);

        YamlConfiguration disk = onDisk();
        assertThat(disk.getString("messages.warn")).isEqualTo("&c[Cleaner] &fGround items will be cleaned in &e{TIME} &fseconds!");
        assertThat(disk.getString("messages.item-cleaned")).isEqualTo("&a[Cleaner] &fCleaned &e{COUNT} &fground items!");
        assertThat(disk.getString("messages.clean-progress")).isEqualTo("&7[Cleaner] &fCleaning progress: &e{CURRENT}&f/&e{TOTAL}");
        assertThat(config.getWarnMessage()).isEqualTo("&c[Cleaner] &fGround items will be cleaned in &e{TIME} &fseconds!");
        verify(config, times(1)).save();
    }

    @Test
    @DisplayName("a customised value, or built-in text changed by one character, is kept byte for byte under both languages and the file is not rewritten")
    void customisedValuesAreKept() throws Exception {
        for (String code : LANGUAGES) {
            for (String variant : new String[] {"shipped!", "en!", "own"}) {
                language[0] = code;
                Map<String, String> values = new LinkedHashMap<>();
                for (Setting s : SETTINGS) {
                    String v;
                    if ("shipped!".equals(variant)) {
                        v = s.shipped + "!";
                    } else if ("en!".equals(variant)) {
                        v = s.text("en") + " ";
                    } else {
                        v = "&dOperator text for " + s.field;
                    }
                    values.put(s.path, v);
                }
                write(values);
                CleanerConfig config = spy(load());
                byte[] before = bytes();

                start(config);

                assertThat(bytes()).as(code + " " + variant).isEqualTo(before);
                for (Setting s : SETTINGS) {
                    assertThat(get(config, s)).as(code + " " + variant + " " + s.field).isEqualTo(values.get(s.path));
                }
                verify(config, never()).save();
            }
        }
    }

    @Test
    @DisplayName("a second enable with the same language writes nothing")
    void secondEnableWritesNothing() throws Exception {
        for (String code : LANGUAGES) {
            language[0] = code;
            Map<String, String> values = new LinkedHashMap<>();
            for (Setting s : SETTINGS) {
                values.put(s.path, s.shipped);
            }
            write(values);
            start(load());
            byte[] afterFirst = bytes();

            CleanerConfig second = spy(load());
            start(second);

            assertThat(bytes()).as(code).isEqualTo(afterFirst);
            verify(second, never()).save();
        }
    }

    @Test
    @DisplayName("onReload() after a language switch rewrites every setting in the new language, in both directions")
    void reloadFollowsALanguageSwitchBothWays() throws Exception {
        for (String[] direction : new String[][] {{"en", "zh"}, {"zh", "en"}}) {
            language[0] = direction[0];
            Files.deleteIfExists(file().toPath());
            CleanerConfig config = load();
            start(config);

            language[0] = direction[1];
            config.init(plugin);
            reload();

            YamlConfiguration disk = onDisk();
            for (Setting s : SETTINGS) {
                String what = direction[0] + " -> " + direction[1] + ": " + s.path;
                assertThat(disk.getString(s.path)).as(what).isEqualTo(s.text(direction[1]));
                assertThat(get(config, s)).as(what).isEqualTo(s.text(direction[1]));
            }
        }
    }

    @Test
    @DisplayName("no configuration change listener rewrites the text (the framework fires them before it reloads the language)")
    void changeListenersDoNotMaterialize() throws Exception {
        language[0] = "en";
        CleanerConfig config = load();
        start(config);
        byte[] before = bytes();

        language[0] = "zh";
        for (ConfigChangeListener listener : new ArrayList<>(config.getChangeListeners())) {
            listener.onConfigReload(config);
        }

        assertThat(bytes()).isEqualTo(before);
        assertThat(config.getWarnMessage()).isEqualTo(SETTINGS.get(0).text("en"));
    }

    @Test
    @DisplayName("an operator-edited language file on disk does not widen what counts as built-in text")
    void diskCatalogueDoesNotWidenTheTrackedSet() throws Exception {
        Path lang = Files.createDirectories(tempDir.resolve("lang"));
        StringBuilder yml = new StringBuilder();
        for (Setting s : SETTINGS) {
            yml.append(s.key).append(": \"Edited ").append(s.key).append("\"\n");
        }
        for (String code : LANGUAGES) {
            Files.write(lang.resolve(code + ".yml"), yml.toString().getBytes(StandardCharsets.UTF_8));
        }
        for (String code : LANGUAGES) {
            language[0] = code;
            Map<String, String> values = new LinkedHashMap<>();
            for (Setting s : SETTINGS) {
                values.put(s.path, s.prefix + "Edited " + s.key);
            }
            write(values);
            CleanerConfig config = spy(load());

            start(config);

            for (Setting s : SETTINGS) {
                assertThat(onDisk().getString(s.path)).as(code + " " + s.path).isEqualTo(values.get(s.path));
            }
            verify(config, never()).save();
        }
    }

    @Test
    @DisplayName("a file that cannot be saved is reported in the server's language, and the module still uses the new text")
    void saveFailureIsReported() throws Exception {
        language[0] = "en";
        CleanerConfig config = spy(load());
        doThrow(new IOException("read-only")).when(config).save();

        assertThat(start(config)).isTrue();

        String expected = CatalogueText.text("en", "log_config_default_save_failed").replace("{FILE}", "config/cleaner.yml")
                .replace("{ERROR}", "read-only");
        verify(logger).warn(expected);
        assertThat(config.getWarnMessage()).isEqualTo(SETTINGS.get(0).text("en"));
    }

    @Test
    @DisplayName("CleanerService's warning broadcast is rendered from the file's text")
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    void warningBroadcastUsesTheFile() throws Exception {
        for (String code : LANGUAGES) {
            language[0] = code;
            Files.deleteIfExists(file().toPath());
            CleanerConfig config = load();
            start(config);
            YamlConfiguration disk = onDisk();
            Player player = mock(Player.class);
            java.lang.reflect.Method warn = CleanerService.class.getDeclaredMethod("broadcastWarn", int.class);
            warn.setAccessible(true);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getPluginManager).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
                bukkit.when(Bukkit::getOnlinePlayers).thenReturn(java.util.Collections.singletonList(player));
                CleanerService service = new CleanerService();
                set(service, "plugin", plugin);
                set(service, "config", config);
                warn.invoke(service, 30);
            }

            verify(player).sendMessage(ChatColor.translateAlternateColorCodes('&',
                    disk.getString("messages.warn").replace("{TIME}", "30")));
        }
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    @DisplayName("@NotEmpty is on exactly the fields that carried it at origin/master, and each text field's Java default is its shipped default")
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    void validationAndJavaDefaults() throws Exception {
        Set<String> notEmpty = new TreeSet<>();
        for (Field f : CleanerConfig.class.getDeclaredFields()) {
            if (f.isAnnotationPresent(ConfigEntry.class) && f.isAnnotationPresent(NotEmpty.class)) {
                notEmpty.add(f.getName());
            }
        }
        assertThat(notEmpty).containsExactlyElementsOf(NOT_EMPTY_AT_MASTER);

        CleanerConfig fresh = new CleanerConfig();
        for (Setting s : SETTINGS) {
            Field f = CleanerConfig.class.getDeclaredField(s.field);
            f.setAccessible(true);
            assertThat(f.get(fresh)).as(s.field).isEqualTo(s.shipped);
            assertThat(f.getAnnotation(ConfigEntry.class).path()).as(s.field).isEqualTo(s.path);
        }
    }

    // ---- harness ----

    private static String get(CleanerConfig config, Setting s) throws Exception {
        return (String) CleanerConfig.class.getMethod(s.getter()).invoke(config);
    }

    private File file() {
        return new File(tempDir.toFile(), "config/cleaner.yml");
    }

    private byte[] bytes() throws IOException {
        return Files.readAllBytes(file().toPath());
    }

    private YamlConfiguration onDisk() {
        return YamlConfiguration.loadConfiguration(file());
    }

    /** Writes a cleaner.yml holding {@code values} (path to value), as an earlier version or an operator left it. */
    private void write(Map<String, String> values) throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        YamlConfiguration persisted = new YamlConfiguration();
        for (Map.Entry<String, String> e : values.entrySet()) {
            persisted.set(e.getKey(), e.getValue());
        }
        persisted.save(file());
    }

    /** The framework's own load: {@code init} fills missing keys with the Java defaults, saves, validates. */
    private CleanerConfig load() throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        CleanerConfig config = new CleanerConfig();
        config.init(plugin);
        return config;
    }

    /** The module's enable path: {@code UltiCleaner#registerSelf()} with {@code config} as the module's configuration. */
    private boolean start(CleanerConfig config) {
        current = config;
        return plugin.registerSelf();
    }

    /** The module's {@code onReload()} (protected), as the framework calls it after rebuilding the language. */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private void reload() throws Exception {
        java.lang.reflect.Method onReload = UltiCleaner.class.getDeclaredMethod("onReload");
        onReload.setAccessible(true);
        onReload.invoke(plugin);
    }

    /**
     * A module double whose {@code registerSelf()} and {@code onReload()} are the real ones, whose
     * configuration folder is the temporary directory, whose {@code i18n} answers from the module's real
     * catalogue for the language in {@link #language} (read at call time), and whose
     * {@code getConfig(CleanerConfig.class)} is {@link #current}.
     */
    private UltiCleaner moduleDouble() {
        return Mockito.mock(UltiCleaner.class, this::moduleAnswer);
    }

    private Object moduleAnswer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
        final Map<String, org.mockito.stubbing.Answer<String>> answers = new LinkedHashMap<>();
        for (String code : LANGUAGES) {
            answers.put(code, CatalogueText.answer(code));
        }
        {
            String name = invocation.getMethod().getName();
            switch (name) {
                case "registerSelf":
                case "onReload":
                    return invocation.callRealMethod();
                case "getConfigFolder":
                    return tempDir.toString();
                case "getConfigFile":
                    return new File(tempDir.toFile(), invocation.<String>getArgument(0));
                case "operatorConfigFile":
                    return file();
                case "i18n":
                    return answers.get(language[0]).answer(invocation);
                case "getLogger":
                    return logger;
                case "getContext":
                    return Mockito.mock(invocation.getMethod().getReturnType(), inv ->
                            "getBean".equals(inv.getMethod().getName()) && inv.getArgument(0) == CleanerConfig.class
                                    ? current : Answers.RETURNS_DEFAULTS.answer(inv));
                default:
                    return Answers.RETURNS_DEFAULTS.answer(invocation);
            }
        }
    }
}
