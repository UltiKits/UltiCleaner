package com.ultikits.plugins.cleaner.service;

import com.ultikits.plugins.cleaner.UltiCleanerTestHelper;
import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.events.PreItemCleanEvent;
import com.ultikits.plugins.cleaner.i18n.CatalogueText;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A blank {@code messages.*} value in {@code config/cleaner.yml} shows the language file's text in the
 * server's language; any other value is the operator's and is shown as written (maintainer ruling
 * 2026-09-24 (d), UltiKits/UltiCleaner#17).
 * <p>
 * Each of the seven broadcast messages is driven through the path that really sends it, once per
 * language, with the configured value blank, and the text a player receives is compared with the
 * shipped catalogue. Before the change a blank value was broadcast as an empty line.
 */
@DisplayName("A blank cleanup message shows the language file's text")
class BlankMessageFallbackTest {

    private CleanerService service;
    private CleanerConfig config;
    private UltiToolsPlugin plugin;
    private Player player;

    private void build(String language) throws Exception {
        config = UltiCleanerTestHelper.createDefaultConfig();
        lenient().when(config.getWarnMessage()).thenReturn("");
        lenient().when(config.getEntityWarnMessage()).thenReturn("");
        lenient().when(config.getItemCleanedMessage()).thenReturn("");
        lenient().when(config.getEntityCleanedMessage()).thenReturn("");
        lenient().when(config.getSmartCleanTriggeredMessage()).thenReturn("");
        lenient().when(config.getCleanProgressMessage()).thenReturn("  ");
        lenient().when(config.getCleanCancelledMessage()).thenReturn("");
        lenient().when(config.getItemWhitelist()).thenReturn(Collections.<String>emptyList());
        lenient().when(config.getEntityTypes()).thenReturn(Collections.<String>emptyList());
        lenient().when(config.getWorldBlacklist()).thenReturn(Collections.<String>emptyList());
        plugin = mock(UltiToolsPlugin.class);
        when(plugin.i18n(anyString())).thenAnswer(CatalogueText.answer(language));
        service = new CleanerService();
        UltiCleanerTestHelper.setField(service, "config", config);
        UltiCleanerTestHelper.setField(service, "tpsScheduler", null);
        UltiCleanerTestHelper.setField(service, "plugin", plugin);
        service.init();
        player = mock(Player.class);
        lenient().when(player.isOp()).thenReturn(true);
        doReturn(Collections.singletonList(player)).when(UltiCleanerTestHelper.getMockServer()).getOnlinePlayers();
    }

    @BeforeEach
    void setUp() throws Exception {
        UltiCleanerTestHelper.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        UltiCleanerTestHelper.tearDown();
    }

    private static String expected(String language, String key, String... tokenValuePairs) {
        String value = CatalogueText.entries(language).get(key);
        if (value == null) {
            return "<lang/" + language + " has no " + key + ">";
        }
        for (int i = 0; i + 1 < tokenValuePairs.length; i += 2) {
            value = value.replace(tokenValuePairs[i], tokenValuePairs[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private List<String> received() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(player, atLeastOnce()).sendMessage(captor.capture());
        return captor.getAllValues();
    }

    private void invoke(String name, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] instanceof Integer ? int.class : args[i].getClass();
        }
        Method method = CleanerService.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(service, args);
    }

    @ParameterizedTest(name = "language: {0}")
    @ValueSource(strings = {"en", "zh"})
    @DisplayName("the four countdown and result broadcasts")
    void countdownAndResults(String language) throws Exception {
        build(language);

        invoke("broadcastWarn", 30);
        invoke("broadcastEntityWarn", 20);
        invoke("broadcastItemCleaned", 7);
        invoke("broadcastEntityCleaned", 8);

        assertThat(received()).containsExactly(
                expected(language, "item_warn", "{TIME}", "30"),
                expected(language, "entity_warn", "{TIME}", "20"),
                expected(language, "item_cleaned", "{COUNT}", "7"),
                expected(language, "entity_cleaned", "{COUNT}", "8"));
    }

    @ParameterizedTest(name = "language: {0}")
    @ValueSource(strings = {"en", "zh"})
    @DisplayName("the cleanup-cancelled broadcast")
    void cancelled(String language) throws Exception {
        build(language);
        PluginManager pluginManager = UltiCleanerTestHelper.getMockServer().getPluginManager();
        doAnswer(inv -> {
            ((Cancellable) inv.getArgument(0)).setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any());

        invoke("cleanItemsWithBatch", PreItemCleanEvent.CleanTrigger.MANUAL);

        assertThat(received()).containsExactly(expected(language, "clean_cancelled"));
    }

    @ParameterizedTest(name = "language: {0}")
    @ValueSource(strings = {"en", "zh"})
    @DisplayName("the smart-cleanup broadcast")
    void smartTriggered(String language) throws Exception {
        build(language);
        when(config.isSmartCleanEnabled()).thenReturn(true);
        when(config.getSmartCleanCooldown()).thenReturn(0);
        when(config.getItemMaxThreshold()).thenReturn(1);
        when(config.getMobMaxThreshold()).thenReturn(10000);
        World world = UltiCleanerTestHelper.createMockWorld("world");
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Item item = mock(Item.class);
            lenient().when(item.getUniqueId()).thenReturn(UUID.randomUUID());
            lenient().when(item.getType()).thenReturn(EntityType.ITEM);
            ItemStack stack = mock(ItemStack.class);
            lenient().when(stack.getType()).thenReturn(Material.STONE);
            lenient().when(item.getItemStack()).thenReturn(stack);
            lenient().when(item.getTicksLived()).thenReturn(1000);
            entities.add(item);
        }
        when(world.getEntities()).thenReturn(entities);
        UltiCleanerTestHelper.addMockWorld(world);

        invoke("checkSmartClean");

        assertThat(received().get(0)).isEqualTo(expected(language, "smart_clean_triggered"));
    }

    @ParameterizedTest(name = "language: {0}")
    @ValueSource(strings = {"en", "zh"})
    @DisplayName("the progress line sent to operators, for a whitespace-only value too")
    @SuppressWarnings("unchecked")
    void progress(String language) throws Exception {
        build(language);
        when(config.isShowCleanProgress()).thenReturn(true);
        ArgumentCaptor<Consumer<BukkitTask>> tick = ArgumentCaptor.forClass(Consumer.class);
        doNothing().when(UltiCleanerTestHelper.getMockScheduler()).runTaskTimer(any(), tick.capture(), anyLong(), anyLong());
        Method method = CleanerService.class.getDeclaredMethod("removeEntitiesInBatches",
                List.class, int.class, Consumer.class);
        method.setAccessible(true);
        Consumer<Integer> done = count -> { };

        method.invoke(service, Arrays.asList(UUID.randomUUID(), UUID.randomUUID()), 1, done);
        tick.getValue().accept(mock(BukkitTask.class));

        assertThat(received()).containsExactly(expected(language, "clean_progress", "{CURRENT}", "1", "{TOTAL}", "2"));
    }

    @Test
    @DisplayName("a customised message is shown as the operator wrote it")
    void customisedIsKept() throws Exception {
        build("en");
        when(config.getWarnMessage()).thenReturn("&cClearing drops in {TIME}s");

        invoke("broadcastWarn", 30);

        assertThat(received()).containsExactly(ChatColor.RED + "Clearing drops in 30s");
    }
}
