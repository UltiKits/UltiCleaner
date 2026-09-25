package com.ultikits.plugins.cleaner;

import com.ultikits.plugins.cleaner.i18n.CatalogueText;
import com.ultikits.ultitools.context.SimpleContainer;
import com.ultikits.ultitools.interfaces.impl.logger.PluginLogger;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The warning about a key this module removed but an operator's file still holds follows the
 * framework's {@code language} setting, and still names the file and the key.
 * <p>
 * Driven through {@code registerSelf}, the real entry point, with the module's {@code i18n} answering
 * from the shipped Chinese catalogue. Before the language sweep this warning was fixed English text.
 * In this package because {@link UltiCleaner#operatorConfigFile()} is package-private.
 */
@DisplayName("The removed-key warning follows the language setting")
class RemovedKeyWarningLanguageTest {

    @Test
    @DisplayName("a leftover messages.prefix is reported in Chinese, naming the file and the key")
    void removedKeyIsChinese(@TempDir File dir) throws IOException {
        File file = new File(dir, "cleaner.yml");
        Files.write(file.toPath(), "messages:\n  prefix: '&a[Cleaner]'\n".getBytes(StandardCharsets.UTF_8));
        UltiCleaner plugin = mock(UltiCleaner.class);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer("zh"));
        PluginLogger logger = mock(PluginLogger.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getContext()).thenReturn(mock(SimpleContainer.class));
        when(plugin.operatorConfigFile()).thenReturn(file);
        when(plugin.registerSelf()).thenCallRealMethod();

        plugin.registerSelf();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger, atLeast(0)).warn(captor.capture());
        String template = CatalogueText.entries("zh").get("removed_key_warning");
        String reason = CatalogueText.entries("zh").get("removed_key_reason_prefix");
        String expected = template == null || reason == null
                ? "<lang/zh lacks removed_key_warning or removed_key_reason_prefix>"
                : ChatColor.translateAlternateColorCodes('&', template.replace("{FILE}", file.getPath())
                        .replace("{KEY}", "messages.prefix").replace("{REASON}", reason));
        assertThat(captor.getAllValues()).containsExactly(expected);
    }
}
