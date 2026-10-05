package com.ultikits.plugins.cleaner.config;

import com.ultikits.plugins.cleaner.UltiCleaner;
import com.ultikits.plugins.cleaner.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The comments above the keys of {@code config/cleaner.yml} come from the module's language files, so
 * a server set to English writes English comments (UltiKits/UltiCleaner#33; framework
 * UltiTools-Reborn#542). Every case runs the framework's real {@code AbstractConfigEntity#init} on a
 * temporary folder and answers {@code i18n} from the module's real catalogues.
 * <p>
 * 注释从语言文件取：英文服务器写入英文注释，中文服务器写入中文注释；升级时只改注释，不改值。
 */
@DisplayName("cleaner.yml writes its comments in the server's language (#33)")
class CleanerConfigCommentsTest {

    /** The 33 settings' paths, in declaration order; the comment's catalogue key is derived from the path. */
    private static final List<String> PATHS = Arrays.asList(
            "item.enabled",
            "item.interval",
            "item.warn-times",
            "item.whitelist",
            "item.ignore-named",
            "item.ignore-recent",
            "entity.enabled",
            "entity.interval",
            "entity.warn-times",
            "entity.types",
            "entity.whitelist-named",
            "entity.whitelist-leashed",
            "entity.whitelist-tamed",
            "worlds.blacklist",
            "smart.enabled",
            "smart.item-threshold",
            "smart.mob-threshold",
            "smart.cooldown",
            "batch.size",
            "batch.show-progress",
            "tps.adaptive-enabled",
            "tps.sample-window",
            "tps.low-threshold",
            "tps.critical-threshold",
            "tps.low-reduction",
            "tps.critical-reduction",
            "messages.warn",
            "messages.entity-warn",
            "messages.item-cleaned",
            "messages.entity-cleaned",
            "messages.smart-triggered",
            "messages.clean-progress",
            "messages.clean-cancelled");

    @TempDir
    Path tempDir;

    /** {@code item.warn-times} to {@code cleaner_config_comment_item_warn_times}. */
    private static String commentKey(String path) {
        return "cleaner_config_comment_" + path.replace('.', '_').replace('-', '_');
    }

    private File file() {
        return new File(tempDir.toFile(), "config/cleaner.yml");
    }

    private String read() throws IOException {
        return new String(Files.readAllBytes(file().toPath()), StandardCharsets.UTF_8);
    }

    private CleanerConfig load(String language) throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        // The module's own class, so the framework's real shippedCatalogueTexts reads this module's catalogues from
        // its code source and recognises a comment it wrote in either language as its own (framework #604, PR #611).
        UltiToolsPlugin plugin = Mockito.mock(UltiCleaner.class, invocation -> {
            String name = invocation.getMethod().getName();
            if ("shippedCatalogueTexts".equals(name)) {
                return invocation.callRealMethod();
            }
            if ("getConfigFolder".equals(name)) {
                return tempDir.toString();
            }
            if ("getConfigFile".equals(name)) {
                return new File(tempDir.toFile(), invocation.<String>getArgument(0));
            }
            if ("i18n".equals(name)) {
                return CatalogueText.answer(language).answer(invocation);
            }
            if ("getPluginName".equals(name)) {
                return "UltiCleaner";
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        CleanerConfig config = new CleanerConfig();
        config.init(plugin);
        return config;
    }

    /**
     * The comment line directly above the line of {@code path} (a dotted path: its section line, then the
     * leaf line below it), without its indentation and leading {@code # }.
     */
    private static String commentAbove(String text, String path) {
        String[] lines = text.split("\\R");
        String[] parts = path.split("\\.");
        int from = 0;
        for (int p = 0; p < parts.length; p++) {
            boolean found = false;
            for (int i = from; i < lines.length; i++) {
                String trimmed = lines[i].trim();
                boolean topLevel = lines[i].length() == trimmed.length();
                if (trimmed.startsWith(parts[p] + ":") && (p == 0) == topLevel) {
                    from = i + 1;
                    found = true;
                    if (p == parts.length - 1) {
                        String above = lines[i - 1].trim();
                        return above.startsWith("# ") ? above.substring(2) : above;
                    }
                    break;
                }
            }
            if (!found) {
                throw new AssertionError("no line for " + path + " in:\n" + text);
            }
        }
        throw new AssertionError("unreachable");
    }

    @Test
    @DisplayName("a fresh install under language: en writes the English comment above every one of the 33 keys")
    void freshInstallWritesEnglishComments() throws IOException {
        load("en");

        String text = read();
        for (String path : PATHS) {
            assertThat(commentAbove(text, path)).as("comment above " + path)
                    .isEqualTo(CatalogueText.text("en", commentKey(path)));
        }
        assertThat(text).as("no Chinese comment is written").doesNotContainPattern("#.*[\\u4e00-\\u9fff]");
    }

    @Test
    @DisplayName("a fresh install under language: zh writes the Chinese comment above every one of the 33 keys")
    void freshInstallWritesChineseComments() throws IOException {
        load("zh");

        String text = read();
        for (String path : PATHS) {
            assertThat(commentAbove(text, path)).as("comment above " + path)
                    .isEqualTo(CatalogueText.text("zh", commentKey(path)));
        }
    }

    @Test
    @DisplayName("an upgrade: a file written with the Chinese comments gets the English ones, its values stay, and a second start leaves it byte for byte")
    void upgradeSwitchesTheCommentsAndKeepsTheValues() throws IOException {
        load("zh");
        String zhFile = read();
        String edited = zhFile.replaceFirst("(?m)^(\\s*)interval: 300$", "$1interval: 450")
                .replaceFirst("(?m)^(\\s*)size: 50$", "$1size: 25");
        assertThat(edited).as("the edit applied to the written file").isNotEqualTo(zhFile);
        Files.write(file().toPath(), edited.getBytes(StandardCharsets.UTF_8));

        CleanerConfig first = load("en");

        String afterFirst = read();
        for (String path : PATHS) {
            assertThat(commentAbove(afterFirst, path)).as("comment above " + path)
                    .isEqualTo(CatalogueText.text("en", commentKey(path)));
        }
        assertThat(first.getItemCleanInterval()).as("the operator's interval").isEqualTo(450);
        assertThat(first.getCleanBatchSize()).as("the operator's batch size").isEqualTo(25);

        load("en");

        assertThat(read()).as("the second start").isEqualTo(afterFirst);
    }
}
