package com.ultikits.plugins.cleaner;

import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.i18n.CatalogueText;
import com.ultikits.ultitools.annotations.config.NotEmpty;
import com.ultikits.ultitools.context.SimpleContainer;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An operator file still holding a message default an earlier version shipped (all seven were Chinese)
 * is rewritten to blank and saved, at start-up and on reload, so the language file's text takes over in
 * the server's language; any other value is the operator's and is kept (maintainer ruling 2026-09-24 (d),
 * UltiKits/UltiCleaner#17).
 * <p>
 * The shipped values below are copied from this module's history ({@code git log -p} of
 * {@code CleanerConfig}): each message had exactly one default, from the first commit until this change.
 */
@DisplayName("Shipped Chinese message defaults give way to the language file")
class LegacyMessageDefaultsTest {

    private static final String WARN = "&c[清理] &f地面物品将在 &e{TIME} &f秒后清理！";
    private static final String ENTITY_WARN = "&c[清理] &f实体将在 &e{TIME} &f秒后清理！";
    private static final String ITEM_CLEANED = "&a[清理] &f已清理 &e{COUNT} &f个地面物品！";
    private static final String ENTITY_CLEANED = "&a[清理] &f已清理 &e{COUNT} &f个实体！";
    private static final String SMART = "&e[清理] &f检测到实体数量过多，正在进行智能清理...";
    private static final String PROGRESS = "&7[清理] &f清理进度: &e{CURRENT}&f/&e{TOTAL}";
    private static final String CANCELLED = "&c[清理] &f清理操作被其他插件取消！";

    private static final List<String> MESSAGE_FIELDS = Arrays.asList("warnMessage", "entityWarnMessage",
            "itemCleanedMessage", "entityCleanedMessage", "smartCleanTriggeredMessage", "cleanProgressMessage",
            "cleanCancelledMessage");

    private CleanerConfig config;

    private UltiCleaner pluginWith(CleanerConfig cleanerConfig) {
        UltiCleaner plugin = mock(UltiCleaner.class);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("en"));
        when(plugin.getLogger()).thenReturn(mock(PluginLogger.class));
        SimpleContainer context = mock(SimpleContainer.class);
        when(plugin.getContext()).thenReturn(context);
        when(context.getBean(CleanerConfig.class)).thenReturn(cleanerConfig);
        return plugin;
    }

    private CleanerConfig shippedDefaults() throws Exception {
        CleanerConfig real = spy(new CleanerConfig());
        doNothing().when(real).save();
        real.setWarnMessage(WARN);
        real.setEntityWarnMessage(ENTITY_WARN);
        real.setItemCleanedMessage(ITEM_CLEANED);
        real.setEntityCleanedMessage(ENTITY_CLEANED);
        real.setSmartCleanTriggeredMessage(SMART);
        real.setCleanProgressMessage(PROGRESS);
        real.setCleanCancelledMessage(CANCELLED);
        return real;
    }

    private void assertAllBlank(CleanerConfig c) {
        assertThat(Arrays.asList(c.getWarnMessage(), c.getEntityWarnMessage(), c.getItemCleanedMessage(),
                c.getEntityCleanedMessage(), c.getSmartCleanTriggeredMessage(), c.getCleanProgressMessage(),
                c.getCleanCancelledMessage())).containsOnly("");
    }

    @Test
    @DisplayName("start-up blanks every shipped default and saves the file")
    void startUpBlanksAndSaves() throws Exception {
        config = shippedDefaults();
        UltiCleaner plugin = pluginWith(config);
        when(plugin.registerSelf()).thenCallRealMethod();

        plugin.registerSelf();

        assertAllBlank(config);
        verify(config).save();
    }

    @Test
    @DisplayName("/ul reload blanks every shipped default and saves the file")
    void reloadBlanksAndSaves() throws Exception {
        config = shippedDefaults();
        UltiCleaner plugin = pluginWith(config);
        doCallRealMethod().when(plugin).onReload();

        plugin.onReload();

        assertAllBlank(config);
        verify(config).save();
    }

    @Test
    @DisplayName("a customised message is kept while the shipped ones beside it are blanked")
    void customisedIsKept() throws Exception {
        config = shippedDefaults();
        config.setWarnMessage("&cClearing drops in {TIME}s");
        config.setCleanProgressMessage(PROGRESS + " ");
        UltiCleaner plugin = pluginWith(config);
        when(plugin.registerSelf()).thenCallRealMethod();

        plugin.registerSelf();

        assertThat(config.getWarnMessage()).isEqualTo("&cClearing drops in {TIME}s");
        assertThat(config.getCleanProgressMessage()).isEqualTo(PROGRESS + " ");
        assertThat(config.getEntityWarnMessage()).isEmpty();
        assertThat(config.getCleanCancelledMessage()).isEmpty();
        verify(config).save();
    }

    @Test
    @DisplayName("a file already blank is not rewritten on the next start")
    void blankIsNotRewritten() throws Exception {
        config = shippedDefaults();
        for (String field : MESSAGE_FIELDS) {
            Field f = CleanerConfig.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(config, "");
        }
        UltiCleaner plugin = pluginWith(config);
        when(plugin.registerSelf()).thenCallRealMethod();

        plugin.registerSelf();

        assertAllBlank(config);
        verify(config, never()).save();
    }

    @Test
    @DisplayName("a fresh file gets blank messages, which the configuration accepts")
    void freshDefaultsAreBlankAndAccepted() throws Exception {
        CleanerConfig fresh = new CleanerConfig();

        assertAllBlank(fresh);
        for (String field : MESSAGE_FIELDS) {
            assertThat(CleanerConfig.class.getDeclaredField(field).isAnnotationPresent(NotEmpty.class))
                    .as("%s must accept a blank value", field).isFalse();
        }
    }
}
