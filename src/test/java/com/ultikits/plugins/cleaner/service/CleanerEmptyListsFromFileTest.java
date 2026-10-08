package com.ultikits.plugins.cleaner.service;

import com.ultikits.plugins.cleaner.UltiCleanerTestHelper;
import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;

import org.bukkit.entity.EntityType;
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
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * An empty list in {@code config/cleaner.yml} no longer switches cleaning off silently (UltiKits/UltiCleaner#34).
 * <p>
 * The framework's {@code @NotEmpty} now covers lists (UltiKits/UltiTools-Reborn#632, in the follow-up-3
 * framework): a list setting declared {@code @NotEmpty} that holds no usable value runs on its declared default list,
 * with one framework warning naming the key, the value and the default, and the file is not touched. The module's
 * {@code entity.types} carries the annotation, so {@code entity.types: []} now cleans the declared default types; the two
 * {@code warn-times} lists carry none and keep the module's own default-with-warning.
 * <p>
 * Each case writes the file, loads it with the framework's real {@code AbstractConfigEntity#init} over a temporary
 * folder, and starts the module's own {@link CleanerService} on the loaded config, so what is asserted is what an
 * operator's file does. The first test fails on a framework that applies {@code @NotEmpty} to text only.
 * <p>
 * 空列表不再静默关闭清理：框架的 {@code @NotEmpty} 现已覆盖列表，{@code entity.types: []} 改用声明的默认类型并给出一条框架警告，文件不被改动。
 */
@DisplayName("An empty list in cleaner.yml no longer disables cleaning silently (#34)")
class CleanerEmptyListsFromFileTest {

    private static final List<String> DEFAULT_TYPES = Arrays.asList(
            "ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "CAVE_SPIDER", "ENDERMAN", "WITCH", "SLIME", "PHANTOM");

    private static final String DEFAULT_WARN_LIST = "[60, 30, 10, 5, 3, 2, 1]";

    @TempDir
    Path tempDir;

    private final List<String> frameworkWarnings = new ArrayList<>();
    private Handler capture;
    private Logger frameworkLogger;

    @BeforeEach
    void setUp() throws Exception {
        UltiCleanerTestHelper.setUp();
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

    private File file() {
        return new File(tempDir.toFile(), "config/cleaner.yml");
    }

    /** Writes {@code yaml} as {@code config/cleaner.yml} and loads it with the framework's own init. */
    private CleanerConfig load(String yaml) throws IOException {
        Files.createDirectories(file().getParentFile().toPath());
        Files.write(file().toPath(), yaml.getBytes(StandardCharsets.UTF_8));
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
        config.setItemWhitelist(Collections.<String>emptyList());
        config.setWorldBlacklist(Collections.<String>emptyList());
        return config;
    }

    private CleanerService start(CleanerConfig config) throws Exception {
        CleanerService service = new CleanerService();
        UltiCleanerTestHelper.setField(service, "config", config);
        UltiCleanerTestHelper.setField(service, "tpsScheduler", mock(TpsAwareScheduler.class));
        UltiCleanerTestHelper.setField(service, "plugin", UltiCleanerTestHelper.getMockPlugin());
        service.init();
        return service;
    }

    /** The entity types the service's next entity cleanup removes, as it resolved them from the loaded list. */
    @SuppressWarnings({"unchecked", "PMD.AvoidAccessibilityAlteration"}) // reads the resolved set the cleanup consults
    private static Set<EntityType> eligibleTypes(CleanerService service) throws Exception {
        Field field = CleanerService.class.getDeclaredField("entityTypesCache");
        field.setAccessible(true);
        return new HashSet<>((Set<EntityType>) field.get(service));
    }

    private static Set<EntityType> types(List<String> names) {
        Set<EntityType> result = new HashSet<>();
        for (String name : names) {
            result.add(EntityType.valueOf(name));
        }
        return result;
    }

    private List<String> moduleWarnings() {
        ArgumentCaptor<String> warned = ArgumentCaptor.forClass(String.class);
        verify(UltiCleanerTestHelper.getMockLogger(), atLeast(0)).warn(warned.capture());
        return warned.getAllValues();
    }

    @Test
    @DisplayName("entity.types: [] runs on the declared default types, the framework warns once naming key, value and default, and the file is unchanged")
    void anEmptyEntityTypesListUsesTheDefaultTypes() throws Exception {
        String yaml = "entity:\n  types: []\n";
        CleanerConfig config = load(yaml);

        assertThat(config.getEntityTypes()).as("the declared default list, not the empty one").isEqualTo(DEFAULT_TYPES);
        assertThat(frameworkWarnings).as("one framework warning names the key, the value and the default")
                .filteredOn(w -> w.contains("entity.types"))
                .containsExactly("File config/cleaner.yml, key 'entity.types': the list is empty (found []) but the setting is "
                        + "declared @NotEmpty; using the declared default [ZOMBIE, SKELETON, CREEPER, SPIDER, CAVE_SPIDER, "
                        + "ENDERMAN, WITCH, SLIME, PHANTOM] in memory (the file is not changed)");
        assertThat(new String(Files.readAllBytes(file().toPath()), StandardCharsets.UTF_8))
                .as("the operator's file is not rewritten").contains("types: []");

        CleanerService service = start(config);

        assertThat(eligibleTypes(service)).as("the next entity cleanup removes the default types")
                .isEqualTo(types(DEFAULT_TYPES));
        assertThat(moduleWarnings()).as("the module has nothing to add for entity.types")
                .noneMatch(w -> w.contains("entity.types"));
    }

    @Test
    @DisplayName("control: a non-empty entity.types is used as written, with no framework warning about it")
    void aListedTypeIsUsedAsWritten() throws Exception {
        CleanerConfig config = load("entity:\n  types: [ZOMBIE]\n");

        assertThat(config.getEntityTypes()).containsExactly("ZOMBIE");
        assertThat(frameworkWarnings).noneMatch(w -> w.contains("entity.types"));

        CleanerService service = start(config);

        assertThat(eligibleTypes(service)).containsExactly(EntityType.ZOMBIE);
    }

    @Test
    @DisplayName("a list holding only names that are no entity type is not empty: the module warns once per name and cleans no entity (documented limitation)")
    void aListOfOnlyUnknownTypesCleansNothingAndNamesEachEntry() throws Exception {
        CleanerConfig config = load("entity:\n  types: [FOO, BAR]\n");

        assertThat(config.getEntityTypes()).as("the framework cannot tell a type name from any other text").containsExactly("FOO", "BAR");
        assertThat(frameworkWarnings).noneMatch(w -> w.contains("entity.types"));

        CleanerService service = start(config);

        assertThat(eligibleTypes(service)).isEmpty();
        assertThat(moduleWarnings()).filteredOn(w -> w.contains("FOO") || w.contains("BAR")).hasSize(2);
    }

    @Test
    @DisplayName("item.warn-times: [] and entity.warn-times: [abc] each give exactly one module warning, no @NotEmpty warning from the framework, and the default countdown list")
    void emptyWarnTimesKeepTheModulesOwnDefaultWithWarning() throws Exception {
        CleanerConfig config = load("item:\n  warn-times: []\nentity:\n  warn-times: [abc]\n");

        assertThat(config.getItemWarnTimes()).as("the framework binds the empty list as it is").isEmpty();
        assertThat(config.getEntityWarnTimes()).as("an unbindable element is skipped, leaving nothing").isEmpty();
        assertThat(frameworkWarnings).as("nothing about item.warn-times: the field carries no @NotEmpty")
                .noneMatch(w -> w.contains("item.warn-times"));
        assertThat(frameworkWarnings).as("the framework names the skipped element and nothing else for entity.warn-times")
                .filteredOn(w -> w.contains("entity.warn-times"))
                .singleElement().satisfies(w -> assertThat(w).contains("entity.warn-times[0]").contains("abc"));

        start(config);

        assertThat(moduleWarnings()).filteredOn(w -> w.contains("item.warn-times")).singleElement()
                .satisfies(w -> assertThat(w).contains(DEFAULT_WARN_LIST));
        assertThat(moduleWarnings()).filteredOn(w -> w.contains("entity.warn-times")).singleElement()
                .satisfies(w -> assertThat(w).contains(DEFAULT_WARN_LIST));
    }

    @Test
    @DisplayName("neither warn-times field declares @NotEmpty, and entity.types still does")
    void theDeclarationsAreWhatTheTextsSay() throws Exception {
        assertThat(CleanerConfig.class.getDeclaredField("entityTypes")
                .isAnnotationPresent(com.ultikits.ultitools.annotations.config.NotEmpty.class)).isTrue();
        for (String field : new String[] {"itemWarnTimes", "entityWarnTimes"}) {
            assertThat(CleanerConfig.class.getDeclaredField(field)
                    .isAnnotationPresent(com.ultikits.ultitools.annotations.config.NotEmpty.class)).as(field).isFalse();
        }
    }
}
