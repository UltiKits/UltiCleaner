package com.ultikits.plugins.cleaner;

import com.ultikits.plugins.cleaner.service.ChunkUnloadService;
import com.ultikits.plugins.cleaner.service.CleanerService;
import com.ultikits.plugins.cleaner.service.TpsAwareScheduler;
import com.ultikits.ultitools.context.SimpleContainer;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.junit.jupiter.api.*;

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
        ChunkUnloadService mockChunkService = mock(ChunkUnloadService.class);
        TpsAwareScheduler mockTpsScheduler = mock(TpsAwareScheduler.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenReturn("cleaner_enabled");
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(mockCleanerService);
        when(mockContext.getBean(ChunkUnloadService.class)).thenReturn(mockChunkService);
        when(mockContext.getBean(TpsAwareScheduler.class)).thenReturn(mockTpsScheduler);
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(mockCleanerService).init();
        verify(mockTpsScheduler).init();
        verify(mockChunkService).init();
    }

    @Test
    @DisplayName("unregisterSelf should log disabled message")
    void unregisterSelf() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenReturn("cleaner_disabled");
        when(plugin.getContext()).thenReturn(mockContext);
        doCallRealMethod().when(plugin).unregisterSelf();

        plugin.unregisterSelf();

        verify(logger).info("cleaner_disabled");
    }

    @Test
    @DisplayName("reloadSelf should reload CleanerService and log message")
    void reloadSelf() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);
        CleanerService mockCleanerService = mock(CleanerService.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenReturn("cleaner_reloaded");
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(mockCleanerService);
        doCallRealMethod().when(plugin).reloadSelf();

        plugin.reloadSelf();

        verify(mockCleanerService).reload();
        verify(logger).info("cleaner_reloaded");
    }

    @Test
    @DisplayName("reloadSelf should handle null CleanerService gracefully")
    void reloadSelfNullService() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenReturn("cleaner_reloaded");
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(null);
        doCallRealMethod().when(plugin).reloadSelf();

        assertThatCode(() -> plugin.reloadSelf()).doesNotThrowAnyException();
        verify(logger).info("cleaner_reloaded");
    }

    @Test
    @DisplayName("registerSelf should handle null CleanerService gracefully")
    void registerSelfNullCleanerService() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenReturn("cleaner_enabled");
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(null);
        when(mockContext.getBean(ChunkUnloadService.class)).thenReturn(null);
        when(mockContext.getBean(TpsAwareScheduler.class)).thenReturn(null);
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(logger).info("cleaner_enabled");
    }

    @Test
    @DisplayName("registerSelf should handle null TpsAwareScheduler gracefully")
    void registerSelfNullTpsScheduler() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);
        CleanerService mockCleanerService = mock(CleanerService.class);
        ChunkUnloadService mockChunkService = mock(ChunkUnloadService.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenReturn("cleaner_enabled");
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(mockCleanerService);
        when(mockContext.getBean(ChunkUnloadService.class)).thenReturn(mockChunkService);
        when(mockContext.getBean(TpsAwareScheduler.class)).thenReturn(null);
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(mockCleanerService).init();
        verify(mockChunkService).init();
    }

    @Test
    @DisplayName("registerSelf should handle null ChunkUnloadService gracefully")
    void registerSelfNullChunkService() throws Exception {
        UltiCleaner plugin = mock(UltiCleaner.class);
        PluginLogger logger = mock(PluginLogger.class);
        SimpleContainer mockContext = mock(SimpleContainer.class);
        CleanerService mockCleanerService = mock(CleanerService.class);
        TpsAwareScheduler mockTpsScheduler = mock(TpsAwareScheduler.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.i18n(anyString())).thenReturn("cleaner_enabled");
        when(plugin.getContext()).thenReturn(mockContext);
        when(mockContext.getBean(CleanerService.class)).thenReturn(mockCleanerService);
        when(mockContext.getBean(ChunkUnloadService.class)).thenReturn(null);
        when(mockContext.getBean(TpsAwareScheduler.class)).thenReturn(mockTpsScheduler);
        when(plugin.registerSelf()).thenCallRealMethod();

        boolean result = plugin.registerSelf();

        assertThat(result).isTrue();
        verify(mockCleanerService).init();
        verify(mockTpsScheduler).init();
    }
}
