package com.ultikits.plugins.cleaner;

import com.ultikits.plugins.cleaner.i18n.CatalogueText;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import com.ultikits.plugins.cleaner.service.CleanerService;
import com.ultikits.plugins.cleaner.service.TpsAwareScheduler;
import com.ultikits.ultitools.context.SimpleContainer;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UltiCleaner Main Class Tests")
class UltiCleanerTest {

    @AfterEach
    void tearDown() throws Exception {
        UltiCleanerTestHelper.tearDown();
    }

    @Test
    @DisplayName("registerSelf should return true and init services")
    void registerSelf() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);
        CleanerService mockCleanerService = mock(CleanerService.class);
        TpsAwareScheduler mockTpsScheduler = mock(TpsAwareScheduler.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("en"));
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(mockCleanerService);
        when(mockContext.getBean(TpsAwareScheduler.class)).thenReturn(mockTpsScheduler);
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(mockCleanerService).init();
        verify(mockTpsScheduler).init();
    }

    @Test
    @DisplayName("UltiCleaner declares no unload hook (UltiKits/UltiCleaner#14)")
    void noUnloadHook() {
        // unregisterSelf()/reloadSelf() need no check here: they are final in UltiToolsPlugin,
        // so the compiler already rejects any override of them.
        for (java.lang.reflect.Method method : UltiCleaner.class.getDeclaredMethods()) {
            assertThat(method.getName())
                    .as("UltiCleaner must not declare %s", method)
                    .isNotEqualTo("onUnregister");
        }
    }

    @Test
    @DisplayName("onReload is a protected override (UltiKits/UltiCleaner#14)")
    void onReloadIsProtectedOverride() throws Exception {
        java.lang.reflect.Method onReload = UltiCleaner.class.getDeclaredMethod("onReload");
        assertThat(java.lang.reflect.Modifier.isProtected(onReload.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("onReload should reload CleanerService exactly once and log the reloaded message (UltiKits/UltiCleaner#14)")
    void onReloadReloadsCleanerServiceOnce() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);
        CleanerService mockCleanerService = mock(CleanerService.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("en"));
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(mockCleanerService);
        doCallRealMethod().when(plugin).onReload();

        plugin.onReload();

        verify(mockCleanerService, times(1)).reload();
        verify(logger).info(CatalogueText.text("en", "cleaner_reloaded"));
    }

    @Test
    @DisplayName("onReload should handle a null CleanerService bean gracefully (UltiKits/UltiCleaner#14)")
    void onReloadNullService() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("en"));
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(null);
        doCallRealMethod().when(plugin).onReload();

        assertThatCode(() -> plugin.onReload()).doesNotThrowAnyException();
        verify(logger).info(CatalogueText.text("en", "cleaner_reloaded"));
    }

    @Test
    @DisplayName("registerSelf should handle null CleanerService gracefully")
    void registerSelfNullCleanerService() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("en"));
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(null);
        when(mockContext.getBean(TpsAwareScheduler.class)).thenReturn(null);
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(logger).info(CatalogueText.text("en", "cleaner_enabled"));
    }

    @Test
    @DisplayName("registerSelf should handle null TpsAwareScheduler gracefully")
    void registerSelfNullTpsScheduler() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);
        CleanerService mockCleanerService = mock(CleanerService.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("en"));
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(mockCleanerService);
        when(mockContext.getBean(TpsAwareScheduler.class)).thenReturn(null);
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(mockCleanerService).init();
    }

    /**
     * The removed-key check's PREDICATE is guarded by {@code RemovedConfigKeysTest}. These tests
     * guard its WIRING, which is a separate claim: deleting both
     * {@code warnAboutRemovedConfigKeys()} call sites left the suite at 282/282 green, so nothing
     * in the repository objected to the check never running. That is the same hazard the predicate's
     * positive control exists to close -- a check that never fires and a server with no leftover
     * keys produce the same empty console -- one level up, at the call rather than the predicate.
     * <p>
     * Both entry points are covered, not just the one that was reported: the check is registered on
     * {@code registerSelf()} and on {@code onReload()}, and a guard on one of them would leave the
     * other free to lose its call silently.
     */
    @Nested
    @DisplayName("the removed-key check is actually called (UltiKits/UltiCleaner#18, #23, #20)")
    class RemovedKeyCheckWiring {

        private static final String FILE_WITH_EVERY_REMOVED_KEY =
                "item:\n  enabled: true\n"
                + "chunk:\n  enabled: false\n  max-distance: 20\n  batch-size: 5\n  timeout: 5\n"
                + "messages:\n  prefix: '&a[Cleaner]'\n  warn: 'x'\n";

        private static final String FILE_WITH_NO_REMOVED_KEY =
                "item:\n  enabled: true\nmessages:\n  warn: 'x'\n";

        private PluginLogger logger;

        private UltiCleaner pluginReading(File dir, String body) throws IOException {
            File file = new File(dir, "cleaner.yml");
            Files.write(file.toPath(), body.getBytes(StandardCharsets.UTF_8));

            UltiCleaner plugin = mock(UltiCleaner.class);
            logger = mock(PluginLogger.class);
            SimpleContainer context = mock(SimpleContainer.class);
            when(plugin.getLogger()).thenReturn(logger);
            when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("en"));
            when(plugin.getContext()).thenReturn(context);
            when(context.getBean(CleanerService.class)).thenReturn(null);
            when(context.getBean(TpsAwareScheduler.class)).thenReturn(null);
            when(plugin.operatorConfigFile()).thenReturn(file);
            return plugin;
        }

        private List<String> warnings() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(logger, atLeast(0)).warn(captor.capture());
            return captor.getAllValues();
        }

        @Test
        @DisplayName("POSITIVE CONTROL: registerSelf warns about every leftover key")
        void registerSelfWarns(@TempDir File dir) throws IOException {
            UltiCleaner plugin = pluginReading(dir, FILE_WITH_EVERY_REMOVED_KEY);
            when(plugin.registerSelf()).thenCallRealMethod();

            plugin.registerSelf();

            assertThat(warnings()).hasSize(5);
            assertThat(warnings()).anySatisfy(l -> assertThat(l).contains("messages.prefix"));
            assertThat(warnings()).anySatisfy(l -> assertThat(l).contains("chunk.enabled"));
        }

        @Test
        @DisplayName("POSITIVE CONTROL: onReload warns about every leftover key")
        void onReloadWarns(@TempDir File dir) throws IOException {
            UltiCleaner plugin = pluginReading(dir, FILE_WITH_EVERY_REMOVED_KEY);
            doCallRealMethod().when(plugin).onReload();

            plugin.onReload();

            assertThat(warnings()).hasSize(5);
            assertThat(warnings()).anySatisfy(l -> assertThat(l).contains("chunk.timeout"));
        }

        @Test
        @DisplayName("Neither entry point warns when the file holds no removed key")
        void neitherWarnsOnACleanFile(@TempDir File dir) throws IOException {
            // Paired with the two controls above: same entry points, same shape of file, the five
            // keys taken out and nothing else changed. Without this pair, five warnings would only
            // show that something logs on start, not that the keys are what drives it.
            UltiCleaner onEnable = pluginReading(dir, FILE_WITH_NO_REMOVED_KEY);
            when(onEnable.registerSelf()).thenCallRealMethod();
            onEnable.registerSelf();
            assertThat(warnings()).isEmpty();

            UltiCleaner onReload = pluginReading(dir, FILE_WITH_NO_REMOVED_KEY);
            doCallRealMethod().when(onReload).onReload();
            onReload.onReload();
            assertThat(warnings()).isEmpty();
        }
    }

    @Test
    @DisplayName("the chunk-unload feature and its event are gone, not merely unreachable (UltiKits/UltiCleaner#23, #20)")
    void chunkUnloadFeatureIsRemoved() {
        // Positive control first: a class this module still ships must resolve, so a
        // ClassNotFoundException below means "removed", not "the loader cannot see this package".
        assertThatCode(() -> Class.forName("com.ultikits.plugins.cleaner.service.CleanerService"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> Class.forName("com.ultikits.plugins.cleaner.service.ChunkUnloadService"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.ultikits.plugins.cleaner.events.PreChunkUnloadEvent"))
                .isInstanceOf(ClassNotFoundException.class);
    }

}
