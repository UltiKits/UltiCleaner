package com.ultikits.plugins.cleaner.commands;

import com.ultikits.plugins.cleaner.UltiCleanerTestHelper;
import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.service.CleanerService;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the real command, service and captured batch ticks, without booting the framework. */
class CleanCommandCompletionTest {
    private static final String ITEMS_DONE = ChatColor.GREEN + "[Cleaner] Cleaned 2 ground items!";
    private static final String ENTITIES_DONE = ChatColor.GREEN + "[Cleaner] Cleaned 2 entities!";

    private CleanCommand command;
    private CleanerService service;
    private CleanerConfig config;
    private ConsoleCommandSender console;
    private Player player;
    private Player observer;
    private final List<Consumer<BukkitTask>> ticks = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        UltiCleanerTestHelper.setUp();
        config = UltiCleanerTestHelper.createDefaultConfig();
        when(config.getEntityTypes()).thenReturn(Collections.singletonList("ZOMBIE"));
        when(config.getCleanBatchSize()).thenReturn(2);
        when(config.getItemCleanedMessage()).thenReturn("&a[Cleaner] Cleaned {COUNT} ground items!");
        when(config.getEntityCleanedMessage()).thenReturn("&a[Cleaner] Cleaned {COUNT} entities!");
        console = mock(ConsoleCommandSender.class);
        player = mock(Player.class);
        observer = mock(Player.class);
        when(UltiCleanerTestHelper.getMockServer().getConsoleSender()).thenReturn(console);

        World world = UltiCleanerTestHelper.createMockWorld("world");
        UltiCleanerTestHelper.addMockWorld(world);
        // Three collected of each kind, but only two remain valid by their batch tick.
        for (EntityType type : Arrays.asList(EntityType.ITEM, EntityType.ZOMBIE)) {
            for (int index = 0; index < 3; index++) {
                Entity entity = type == EntityType.ITEM ? mock(Item.class) : mock(Entity.class);
                UUID uuid = UUID.randomUUID();
                when(entity.getType()).thenReturn(type);
                when(entity.getUniqueId()).thenReturn(uuid);
                when(entity.isValid()).thenReturn(index < 2);
                when(UltiCleanerTestHelper.getMockServer().getEntity(uuid)).thenReturn(entity);
                entities.add(entity);
            }
        }
        when(world.getEntities()).thenReturn(entities);
        doAnswer(invocation -> {
            ticks.add(invocation.getArgument(1));
            return null;
        }).when(UltiCleanerTestHelper.getMockScheduler())
                .runTaskTimer(any(), anyConsumer(), eq(0L), eq(1L));
        when(UltiCleanerTestHelper.getMockScheduler().runTaskAsynchronously(any(), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.<Runnable>getArgument(1).run();
                    return mock(BukkitTask.class);
                });
        service = new CleanerService();
        UltiCleanerTestHelper.setField(service, "config", config);
        UltiCleanerTestHelper.setField(service, "plugin", UltiCleanerTestHelper.getMockPlugin());
        service.init();
        command = new CleanCommand(service, config, UltiCleanerTestHelper.getMockPlugin());
    }

    private static Consumer<BukkitTask> anyConsumer() {
        return any();
    }

    @AfterEach
    void tearDown() throws Exception {
        UltiCleanerTestHelper.tearDown();
    }

    @ParameterizedTest
    @ValueSource(strings = {"entities", "items", "all"})
    void consoleReceivesCompletionAfterTheLastBatchWithoutOnlinePlayers(String operation) {
        execute(operation, console);
        verify(console).sendMessage(contains("3"));
        verifyNoCompletion(console);

        finishBatches();

        verifyCompletion(operation, console);
        assertThat(service.isCleaningInProgress()).isFalse();
        verifyRemovals(operation);
    }

    @ParameterizedTest
    @ValueSource(strings = {"entities", "items", "all"})
    void playerReceivesExactlyOneCompletionAndExistingBroadcastIsPreserved(String operation) {
        doReturn(Arrays.asList(player, observer)).when(UltiCleanerTestHelper.getMockServer()).getOnlinePlayers();
        execute(operation, player);
        verify(player).sendMessage(contains("3"));
        verifyNoCompletion(player);
        verifyNoCompletion(observer);

        finishBatches();

        verifyCompletion(operation, player);
        verifyCompletion(operation, observer);
        verifyNoInteractions(console);
        verifyRemovals(operation);
    }

    @ParameterizedTest
    @ValueSource(strings = {"entities", "items", "all"})
    void consoleReplyDoesNotReplaceThePlayerBroadcast(String operation) {
        doReturn(Collections.singletonList(observer)).when(UltiCleanerTestHelper.getMockServer()).getOnlinePlayers();
        execute(operation, console);

        finishBatches();

        verifyCompletion(operation, console);
        verifyCompletion(operation, observer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"entities", "items", "all"})
    void originalNoArgumentServiceMethodsDoNotAddConsoleOutput(String operation) {
        switch (operation) {
            case "items": assertThat(service.forceCleanItems()).isEqualTo(3); break;
            case "entities": assertThat(service.forceCleanEntities()).isEqualTo(3); break;
            default: assertThat(service.forceCleanAll()).containsExactly(3, 3);
        }

        finishBatches();

        verifyNoInteractions(console);
        verifyRemovals(operation);
    }

    @ParameterizedTest
    @ValueSource(strings = {"scheduled", "smart"})
    void automaticCleanupDoesNotAddConsoleOutput(String trigger) throws Exception {
        if (trigger.equals("scheduled")) {
            UltiCleanerTestHelper.setField(service, "itemCountdown", 1);
            UltiCleanerTestHelper.setField(service, "entityCountdown", 1);
            service.tickItemClean();
            service.tickEntityClean();
        } else {
            when(config.isSmartCleanEnabled()).thenReturn(true);
            service.checkSmartClean();
        }
        assertThat(ticks).hasSize(2);

        finishBatches();

        verifyNoInteractions(console);
        verifyRemovals("all");
    }

    private void execute(String operation, CommandSender sender) {
        switch (operation) {
            case "items": command.cleanItems(sender); break;
            case "entities": command.cleanEntities(sender); break;
            default: command.cleanAll(sender);
        }
        assertThat(ticks).hasSize(operation.equals("all") ? 2 : 1);
        assertThat(service.isCleaningInProgress()).isTrue();
    }

    private void finishBatches() {
        List<BukkitTask> tasks = new ArrayList<>();
        for (Consumer<BukkitTask> tick : ticks) {
            BukkitTask task = mock(BukkitTask.class);
            tasks.add(task);
            tick.accept(task);
            verify(task, never()).cancel();
        }
        verifyNoCompletion(console);
        verifyNoCompletion(player);
        verifyNoCompletion(observer);
        for (int index = 0; index < ticks.size(); index++) {
            ticks.get(index).accept(tasks.get(index));
            verify(tasks.get(index)).cancel();
        }
    }

    private void verifyCompletion(String operation, CommandSender recipient) {
        verify(recipient, times(operation.equals("entities") ? 0 : 1)).sendMessage(ITEMS_DONE);
        verify(recipient, times(operation.equals("items") ? 0 : 1)).sendMessage(ENTITIES_DONE);
    }

    private void verifyNoCompletion(CommandSender recipient) {
        verify(recipient, never()).sendMessage(ITEMS_DONE);
        verify(recipient, never()).sendMessage(ENTITIES_DONE);
    }

    private void verifyRemovals(String operation) {
        for (int index = 0; index < entities.size(); index++) {
            boolean selected = operation.equals("all")
                    || (operation.equals("items") ? index < 3 : index >= 3);
            verify(entities.get(index), times(selected && index % 3 < 2 ? 1 : 0)).remove();
        }
    }
}
