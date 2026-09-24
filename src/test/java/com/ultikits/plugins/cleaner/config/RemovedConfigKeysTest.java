package com.ultikits.plugins.cleaner.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A deleted key stays in the operator's file forever, so the only thing separating "this module
 * warns about leftovers" from "this module is silent" is whether the check can actually see one.
 * Every negative assertion below is therefore paired with a positive control run through the same
 * method, on the same kind of file, in the same test class.
 */
@DisplayName("RemovedConfigKeys Tests")
class RemovedConfigKeysTest {

    /** A file holding every key this module removed, in the nesting a real cleaner.yml uses. */
    private static final String FILE_WITH_EVERY_REMOVED_KEY =
            "item:\n"
            + "  enabled: true\n"
            + "  interval: 300\n"
            + "chunk:\n"
            + "  enabled: false\n"
            + "  max-distance: 20\n"
            + "  batch-size: 5\n"
            + "  timeout: 5\n"
            + "messages:\n"
            + "  prefix: '&a[Cleaner]'\n"
            + "  warn: '&cItems will be cleaned in {TIME} seconds'\n";

    /** The same file with every removed key taken out, and nothing else changed. */
    private static final String FILE_WITH_NO_REMOVED_KEY =
            "item:\n"
            + "  enabled: true\n"
            + "  interval: 300\n"
            + "messages:\n"
            + "  warn: '&cItems will be cleaned in {TIME} seconds'\n";

    private final List<String> warnings = new ArrayList<>();

    private File write(File dir, String name, String body) throws IOException {
        File file = new File(dir, name);
        Files.write(file.toPath(), body.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Nested
    @DisplayName("warnAboutLeftovers")
    class WarnAboutLeftovers {

        @Test
        @DisplayName("POSITIVE CONTROL: every removed key still in the file is reported, naming module, file and key (UltiKits/UltiCleaner#18, #23)")
        void reportsEveryLeftoverKey(@TempDir File dir) throws IOException {
            File config = write(dir, "cleaner.yml", FILE_WITH_EVERY_REMOVED_KEY);

            RemovedConfigKeys.warnAboutLeftovers(config, warnings::add);

            assertThat(warnings).hasSize(5);
            assertThat(warnings).allSatisfy(line -> {
                assertThat(line).contains("UltiCleaner");
                assertThat(line).contains(config.getPath());
            });
            assertThat(warnings).anySatisfy(line -> assertThat(line).contains("messages.prefix"));
            assertThat(warnings).anySatisfy(line -> assertThat(line).contains("chunk.enabled"));
            assertThat(warnings).anySatisfy(line -> assertThat(line).contains("chunk.max-distance"));
            assertThat(warnings).anySatisfy(line -> assertThat(line).contains("chunk.batch-size"));
            assertThat(warnings).anySatisfy(line -> assertThat(line).contains("chunk.timeout"));
        }

        @Test
        @DisplayName("Says nothing for a file that no longer holds any removed key")
        void silentWhenTheFileIsClean(@TempDir File dir) throws IOException {
            // Paired with reportsEveryLeftoverKey above: same method, same shape of file. The
            // difference between the two results is the presence of the keys and nothing else,
            // which is what makes this zero a measured zero rather than a check that never fires.
            File config = write(dir, "cleaner.yml", FILE_WITH_NO_REMOVED_KEY);

            RemovedConfigKeys.warnAboutLeftovers(config, warnings::add);

            assertThat(warnings).isEmpty();
        }

        @Test
        @DisplayName("Reports only the keys actually present, and tells the truth about where the prefix comes from (UltiKits/UltiCleaner#18)")
        void reportsOnlyWhatIsPresent(@TempDir File dir) throws IOException {
            File config = write(dir, "cleaner.yml",
                    "messages:\n  prefix: '&a[Cleaner]'\n  warn: 'x'\n");

            RemovedConfigKeys.warnAboutLeftovers(config, warnings::add);

            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0)).contains("messages.prefix");
            assertThat(warnings.get(0)).contains("Delete the key");
            assertThat(warnings.get(0)).doesNotContain("chunk.");

            // The guidance must point at the seven sibling messages.* keys, which is where a
            // prefix actually comes from. It must NOT claim the language catalogue supplies it:
            // `grep -rn "i18n(" src/main/java` returns two console log lines and nothing else, so
            // every broadcast is read from the configuration and the catalogue's own copies of
            // these messages are read by nobody. Telling an operator otherwise inside the very
            // mechanism this wave added to stop false statements of behaviour would be the defect
            // it exists to remove.
            assertThat(warnings.get(0)).contains("messages.*");
            assertThat(warnings.get(0)).doesNotContain("language catalogue");
        }

        @Test
        @DisplayName("Names the chunk feature's removal, not a replacement setting")
        void chunkGuidanceNamesTheRemoval(@TempDir File dir) throws IOException {
            File config = write(dir, "cleaner.yml", "chunk:\n  enabled: true\n");

            RemovedConfigKeys.warnAboutLeftovers(config, warnings::add);

            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0)).contains("chunk.enabled");
            assertThat(warnings.get(0)).contains("no replacement setting");
        }

        @Test
        @DisplayName("Says nothing when the file does not exist")
        void silentWhenTheFileIsMissing(@TempDir File dir) {
            RemovedConfigKeys.warnAboutLeftovers(new File(dir, "absent.yml"), warnings::add);

            assertThat(warnings).isEmpty();
        }

        @Test
        @DisplayName("Says nothing, and does not throw, for a null file")
        void silentForNull() {
            assertThatCode(() -> RemovedConfigKeys.warnAboutLeftovers(null, warnings::add))
                    .doesNotThrowAnyException();

            assertThat(warnings).isEmpty();
        }

        @Test
        @DisplayName("Says nothing, and does not throw, for an unparseable file")
        void silentForUnparseableYaml(@TempDir File dir) throws IOException {
            File config = write(dir, "cleaner.yml", "messages:\n  prefix: '&a[Cleaner]\n\t- broken\n");

            assertThatCode(() -> RemovedConfigKeys.warnAboutLeftovers(config, warnings::add))
                    .doesNotThrowAnyException();

            assertThat(warnings).isEmpty();
        }

        @Test
        @DisplayName("Says nothing when handed a directory rather than a file")
        void silentForADirectory(@TempDir File dir) {
            RemovedConfigKeys.warnAboutLeftovers(dir, warnings::add);

            assertThat(warnings).isEmpty();
        }
    }

    @Nested
    @DisplayName("removedKeys")
    class RemovedKeys {

        @Test
        @DisplayName("Lists exactly the five keys this version removed")
        void listsTheFiveRemovedKeys() {
            assertThat(RemovedConfigKeys.removedKeys().keySet())
                    .containsExactly("messages.prefix", "chunk.enabled", "chunk.max-distance",
                            "chunk.batch-size", "chunk.timeout");
        }

        @Test
        @DisplayName("Is unmodifiable")
        void isUnmodifiable() {
            assertThatCode(() -> RemovedConfigKeys.removedKeys().put("x", "y"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

    }
}
