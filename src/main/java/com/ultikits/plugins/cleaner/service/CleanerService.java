package com.ultikits.plugins.cleaner.service;

import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.events.CleanCompleteEvent;
import com.ultikits.plugins.cleaner.events.PreEntityCleanEvent;
import com.ultikits.plugins.cleaner.events.PreItemCleanEvent;
import com.ultikits.plugins.cleaner.utils.Placeholders;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.Autowired;
import com.ultikits.ultitools.annotations.Scheduled;
import com.ultikits.ultitools.annotations.Service;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Service for managing entity and item cleanup.
 * Supports batch processing, smart cleanup, TPS-adaptive thresholds,
 * and custom events for extensibility.
 *
 * @author wisdomme
 * @version 2.0.0
 */
@Service
public class CleanerService {

    @Autowired
    private UltiToolsPlugin plugin;

    @Autowired
    private CleanerConfig config;

    @Autowired
    private TpsAwareScheduler tpsScheduler;

    private final Plugin bukkitPlugin = Bukkit.getPluginManager().getPlugin("UltiTools");

    private Set<String> itemWhitelistCache;
    private Set<EntityType> entityTypesCache;
    private Set<String> worldBlacklistCache;

    // Track countdown state
    private int itemCountdown;
    private int entityCountdown;

    // Smart clean tracking
    private long lastSmartCleanTime = 0;

    // Batch processing state: one flag per batch kind, so an item batch and an entity batch started
    // in the same tick both run (UltiKits/UltiCleaner#15). Each flag is set by the method that starts
    // its batch and cleared when that batch's timer task finishes.
    private boolean itemCleaningInProgress = false;
    private boolean entityCleaningInProgress = false;

    // The batch tasks currently running, and whether the module is unloading. The tasks belong to the
    // UltiTools plugin, so neither Bukkit nor the framework cancels them when this module is unloaded;
    // shutdown() does (UltiKits/UltiCleaner#26). Main thread only, like every batch tick.
    private final Set<BukkitTask> batchTasks = new HashSet<>();
    private boolean shutDown = false;
    
    /**
     * Initialize the cleaner service.
     * Note: Tasks are now automatically scheduled via @Scheduled annotations.
     */
    public void init() {
        loadCaches();
    }

    /**
     * Shutdown the cleaner service, from the module's unload hook.
     * <p>
     * The framework cancels the {@code @Scheduled} ticks that start a cleanup, but not a batch task a
     * cleanup has already started: it belongs to the UltiTools plugin, so it would go on removing
     * entities after the module is gone (UltiKits/UltiCleaner#26). Every running batch task is
     * cancelled here, and a batch scheduled but not yet run cancels itself on its first tick. A batch
     * stopped this way broadcasts nothing and fires no {@link CleanCompleteEvent}.
     */
    public void shutdown() {
        shutDown = true;
        for (BukkitTask task : new ArrayList<>(batchTasks)) {
            task.cancel();
        }
        batchTasks.clear();
        itemCleaningInProgress = false;
        entityCleaningInProgress = false;
    }

    /**
     * Reload configuration.
     */
    public void reload() {
        loadCaches();
    }
    
    /**
     * Load caches from config.
     */
    private void loadCaches() {
        // Item whitelist, resolved the way the server resolves a material name, so a lower-case
        // entry protects its material; an entry that names no material protects nothing and is
        // named in a warning rather than accepted silently
        itemWhitelistCache = new HashSet<>();
        if (config.getItemWhitelist() != null) {
            for (Object entry : config.getItemWhitelist()) {
                Material material = entry == null ? null : Material.matchMaterial(String.valueOf(entry).trim());
                if (material != null) {
                    itemWhitelistCache.add(material.name());
                } else {
                    plugin.getLogger().warn(Placeholders.fill(plugin.i18n("log_unknown_whitelist_material"),
                            "{VALUE}", String.valueOf(entry)));
                }
            }
        }
        
        // Entity types to clean
        entityTypesCache = new HashSet<>();
        if (config.getEntityTypes() != null) {
            for (String type : config.getEntityTypes()) {
                try {
                    entityTypesCache.add(EntityType.valueOf(type.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warn(plugin.i18n("log_unknown_entity_type").replace("{TYPE}", type));
                }
            }
        }
        
        // World blacklist
        worldBlacklistCache = new HashSet<>();
        if (config.getWorldBlacklist() != null) {
            worldBlacklistCache.addAll(config.getWorldBlacklist());
        }
        
        // Tell the operator about a warn-times list the countdown cannot use
        warnIfNotWholeSeconds("item.warn-times", config.getItemWarnTimes());
        warnIfNotWholeSeconds("entity.warn-times", config.getEntityWarnTimes());

        // Initialize countdowns
        itemCountdown = config.getItemCleanInterval();
        entityCountdown = config.getEntityCleanInterval();
    }

    /**
     * The countdown marks of a {@code warn-times} list, as whole seconds.
     * <p>
     * The framework's config parser binds each element of a list read from the file as its string
     * form, whatever the field's declared element type, so a {@code contains(int)} lookup never
     * matched and no countdown warning was ever broadcast (UltiKits/UltiCleaner#22). Each element is
     * therefore read through its text. A list with an element that is not a whole number is not used:
     * {@link CleanerConfig#DEFAULT_WARN_TIMES} is used instead, and {@link #loadCaches()} names the
     * list once at enable and reload. The list is read from the config on every tick, as before, so a
     * change applies without waiting for a reload.
     *
     * @param configured the list as bound from the file; {@code null} means no warnings
     * @return the marks to warn at
     */
    private static Set<Integer> warnSeconds(List<?> configured) {
        Set<Integer> parsed = parseWholeSeconds(configured);
        return parsed != null ? parsed : new HashSet<>(CleanerConfig.DEFAULT_WARN_TIMES);
    }

    /**
     * @return the elements as whole numbers, an empty set for {@code null}, or {@code null} when any
     *         element is not a whole number
     */
    private static Set<Integer> parseWholeSeconds(List<?> configured) {
        Set<Integer> seconds = new HashSet<>();
        if (configured == null) {
            return seconds;
        }
        for (Object element : configured) {
            if (element == null) {
                return null;
            }
            try {
                seconds.add(Integer.parseInt(String.valueOf(element).trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return seconds;
    }

    private void warnIfNotWholeSeconds(String key, List<?> configured) {
        if (parseWholeSeconds(configured) == null) {
            // The operator's value goes in with the others in one pass, so it is printed as written
            plugin.getLogger().warn(Placeholders.fill(plugin.i18n("log_invalid_warn_times"),
                    "{KEY}", key,
                    "{VALUE}", String.valueOf(configured),
                    "{DEFAULT}", String.valueOf(CleanerConfig.DEFAULT_WARN_TIMES)));
        }
    }
    
    
    /**
     * Check if smart cleanup should be triggered.
     * Runs every 5 seconds (100 ticks).
     */
    @Scheduled(period = 100, async = false)
    public void checkSmartClean() {
        if (!config.isSmartCleanEnabled() || isCleaningInProgress()) {
            return;
        }
        
        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastSmartCleanTime < config.getSmartCleanCooldown() * 1000L) {
            return;
        }
        
        // Get thresholds (adjusted by TPS if enabled)
        int itemThreshold = tpsScheduler != null ? 
            tpsScheduler.applyThresholdReduction(config.getItemMaxThreshold()) : 
            config.getItemMaxThreshold();
        int mobThreshold = tpsScheduler != null ? 
            tpsScheduler.applyThresholdReduction(config.getMobMaxThreshold()) : 
            config.getMobMaxThreshold();
        
        // Count current entities
        int itemCount = 0;
        int mobCount = 0;
        
        for (World world : Bukkit.getWorlds()) {
            if (worldBlacklistCache.contains(world.getName())) {
                continue;
            }
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    itemCount++;
                } else if (entityTypesCache.contains(entity.getType())) {
                    mobCount++;
                }
            }
        }
        
        // Trigger smart clean if thresholds exceeded
        boolean shouldCleanItems = itemCount > itemThreshold;
        boolean shouldCleanMobs = mobCount > mobThreshold;
        
        if (shouldCleanItems || shouldCleanMobs) {
            lastSmartCleanTime = now;
            broadcastMessage(smartTriggeredText());
            
            if (shouldCleanItems) {
                cleanItemsWithBatch(PreItemCleanEvent.CleanTrigger.SMART);
            }
            if (shouldCleanMobs) {
                cleanEntitiesWithBatch(PreEntityCleanEvent.CleanTrigger.SMART);
            }
        }
    }
    
    /**
     * Item cleanup tick.
     * Runs every second (20 ticks) to countdown and trigger cleanup.
     */
    @Scheduled(period = 20, async = false)
    public void tickItemClean() {
        if (!config.isItemCleanEnabled()) {
            return;
        }
        itemCountdown--;
        
        // Check if we need to warn
        if (warnSeconds(config.getItemWarnTimes()).contains(itemCountdown)) {
            broadcastWarn(itemCountdown);
        }
        
        // Clean if countdown reached
        if (itemCountdown <= 0) {
            cleanItemsWithBatch(PreItemCleanEvent.CleanTrigger.SCHEDULED);
            itemCountdown = config.getItemCleanInterval();
        }
    }
    
    /**
     * Entity cleanup tick.
     * Runs every second (20 ticks) to countdown and trigger cleanup.
     */
    @Scheduled(period = 20, async = false)
    public void tickEntityClean() {
        if (!config.isEntityCleanEnabled()) {
            return;
        }
        entityCountdown--;
        
        // Check if we need to warn for entities
        if (warnSeconds(config.getEntityWarnTimes()).contains(entityCountdown)) {
            broadcastEntityWarn(entityCountdown);
        }
        
        if (entityCountdown <= 0) {
            cleanEntitiesWithBatch(PreEntityCleanEvent.CleanTrigger.SCHEDULED);
            entityCountdown = config.getEntityCleanInterval();
        }
    }
    
    /**
     * Clean items with batch processing and event support.
     */
    private void cleanItemsWithBatch(PreItemCleanEvent.CleanTrigger trigger) {
        cleanItemsWithBatch(trigger, count -> { });
    }

    /**
     * Clean items with batch processing and event support, reporting the outcome to {@code done}.
     *
     * @param trigger what started this cleanup
     * @param done    receives the number of items removed exactly once, on every path: 0 when an item
     *                batch is already running, when a listener cancelled the cleanup, or when there was
     *                nothing to clean; otherwise the count, after the batch's last tick
     */
    private void cleanItemsWithBatch(PreItemCleanEvent.CleanTrigger trigger, IntConsumer done) {
        if (itemCleaningInProgress) {
            done.accept(0);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        List<UUID> itemsToClean = collectItemsToClean();
        
        // Fire pre-clean event
        PreItemCleanEvent preEvent = new PreItemCleanEvent(itemsToClean, null, trigger);
        Bukkit.getPluginManager().callEvent(preEvent);
        
        if (preEvent.isCancelled()) {
            broadcastMessage(cleanCancelledText());
            done.accept(0);
            return;
        }
        
        // Use modified list from event
        List<UUID> finalItems = preEvent.getItemUuids();
        
        if (finalItems.isEmpty()) {
            broadcastItemCleaned(0);
            done.accept(0);
            return;
        }
        
        // Batch remove
        itemCleaningInProgress = true;
        removeEntitiesInBatches(finalItems, config.getCleanBatchSize(), count -> {
            itemCleaningInProgress = false;
            long duration = System.currentTimeMillis() - startTime;
            broadcastItemCleaned(count);
            // done runs after the ITEMS event has been dispatched, so an ALL event it completes follows it
            fireCompleteEvent(CleanCompleteEvent.CleanType.ITEMS, count, duration, convertTrigger(trigger),
                    () -> done.accept(count));
        });
    }
    
    /**
     * Clean entities with batch processing and event support.
     */
    private void cleanEntitiesWithBatch(PreEntityCleanEvent.CleanTrigger trigger) {
        cleanEntitiesWithBatch(trigger, count -> { });
    }

    /**
     * Clean entities with batch processing and event support, reporting the outcome to {@code done}.
     *
     * @param trigger what started this cleanup
     * @param done    receives the number of entities removed exactly once, on every path: 0 when an
     *                entity batch is already running, when a listener cancelled the cleanup, or when
     *                there was nothing to clean; otherwise the count, after the batch's last tick
     */
    private void cleanEntitiesWithBatch(PreEntityCleanEvent.CleanTrigger trigger, IntConsumer done) {
        if (entityCleaningInProgress) {
            done.accept(0);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        Map<EntityType, Integer> typeCounts = new HashMap<>();
        List<UUID> entitiesToClean = collectEntitiesToClean(typeCounts);
        
        // Fire pre-clean event
        PreEntityCleanEvent preEvent = new PreEntityCleanEvent(entitiesToClean, null, trigger, typeCounts);
        Bukkit.getPluginManager().callEvent(preEvent);
        
        if (preEvent.isCancelled()) {
            broadcastMessage(cleanCancelledText());
            done.accept(0);
            return;
        }
        
        // Use modified list from event
        List<UUID> finalEntities = preEvent.getEntityUuids();
        
        if (finalEntities.isEmpty()) {
            done.accept(0);
            return;
        }
        
        // Batch remove
        entityCleaningInProgress = true;
        removeEntitiesInBatches(finalEntities, config.getCleanBatchSize(), count -> {
            entityCleaningInProgress = false;
            long duration = System.currentTimeMillis() - startTime;
            broadcastEntityCleaned(count);
            // done runs after the ENTITIES event has been dispatched, so an ALL event it completes follows it
            fireCompleteEvent(CleanCompleteEvent.CleanType.ENTITIES, count, duration, convertTrigger(trigger),
                    () -> done.accept(count));
        });
    }

    /**
     * Fire a {@link CleanCompleteEvent} asynchronously, as every completed cleanup does, then run
     * {@code afterDispatch} on the same asynchronous task once every listener has returned.
     */
    private void fireCompleteEvent(CleanCompleteEvent.CleanType type, int count, long durationMs,
                                   CleanCompleteEvent.CleanTrigger trigger, Runnable afterDispatch) {
        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, () -> {
            Bukkit.getPluginManager().callEvent(new CleanCompleteEvent(type, count, durationMs, trigger));
            afterDispatch.run();
        });
    }

    /**
     * Fire the {@code ALL} event of {@code /clean all}. Called by whichever half finishes last: from the
     * asynchronous task that has just dispatched that half's own event, where it is dispatched
     * directly, so it follows both component events (separately submitted asynchronous tasks may run in
     * any order); or, for a half that fired no event (nothing to clean, or cancelled), from the main
     * thread, where the asynchronous event has to be handed to the scheduler.
     */
    private void fireAllEvent(int count, long durationMs) {
        Runnable dispatch = () -> Bukkit.getPluginManager().callEvent(new CleanCompleteEvent(
                CleanCompleteEvent.CleanType.ALL, count, durationMs, CleanCompleteEvent.CleanTrigger.MANUAL));
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, dispatch);
        } else {
            dispatch.run();
        }
    }
    
    /**
     * Collect items that should be cleaned.
     */
    private List<UUID> collectItemsToClean() {
        List<UUID> items = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            if (worldBlacklistCache.contains(world.getName())) {
                continue;
            }
            
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    
                    // Check if in whitelist
                    if (item.getItemStack() != null) {
                        String typeName = item.getItemStack().getType().name();
                        if (itemWhitelistCache.contains(typeName)) {
                            continue;
                        }
                        
                        // Check if named
                        if (config.isItemIgnoreNamed() && 
                            item.getItemStack().hasItemMeta() && 
                            item.getItemStack().getItemMeta().hasDisplayName()) {
                            continue;
                        }
                    }
                    
                    // Check if recently dropped
                    if (config.getItemIgnoreRecentSeconds() > 0) {
                        int ticksAlive = item.getTicksLived();
                        if (ticksAlive < config.getItemIgnoreRecentSeconds() * 20) {
                            continue;
                        }
                    }
                    
                    items.add(entity.getUniqueId());
                }
            }
        }
        
        return items;
    }
    
    /**
     * Collect entities that should be cleaned.
     */
    private List<UUID> collectEntitiesToClean(Map<EntityType, Integer> typeCounts) {
        List<UUID> entities = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            if (worldBlacklistCache.contains(world.getName())) {
                continue;
            }
            
            for (Entity entity : world.getEntities()) {
                if (!entityTypesCache.contains(entity.getType())) {
                    continue;
                }
                
                // Check if named
                if (config.isEntityWhitelistNamed() && entity.getCustomName() != null) {
                    continue;
                }
                
                // Check if living entity specific conditions
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) entity;
                    
                    // Check if leashed
                    if (config.isEntityWhitelistLeashed() && living.isLeashed()) {
                        continue;
                    }
                    
                    // Check if tamed
                    if (config.isEntityWhitelistTamed() && entity instanceof Tameable) {
                        Tameable tameable = (Tameable) entity;
                        if (tameable.isTamed()) {
                            continue;
                        }
                    }
                }
                
                entities.add(entity.getUniqueId());
                typeCounts.merge(entity.getType(), 1, Integer::sum);
            }
        }
        
        return entities;
    }
    
    /**
     * Remove entities in batches to avoid lag spikes.
     * <p>
     * Holds no in-progress state of its own: the caller owns its batch kind's flag and clears it in
     * {@code onComplete}, which runs once the last batch tick has finished.
     */
    private void removeEntitiesInBatches(List<UUID> uuids, int batchSize, java.util.function.Consumer<Integer> onComplete) {
        if (uuids.isEmpty()) {
            onComplete.accept(0);
            return;
        }
        
        AtomicInteger removedCount = new AtomicInteger(0);
        AtomicInteger currentIndex = new AtomicInteger(0);
        int totalCount = uuids.size();
        
        Bukkit.getScheduler().runTaskTimer(bukkitPlugin, task -> {
            if (shutDown) {
                task.cancel();
                batchTasks.remove(task);
                return;
            }
            batchTasks.add(task);
            int processed = 0;
            
            while (processed < batchSize && currentIndex.get() < uuids.size()) {
                UUID uuid = uuids.get(currentIndex.getAndIncrement());
                Entity entity = Bukkit.getEntity(uuid);
                
                if (entity != null && entity.isValid() && !(entity instanceof Player)) {
                    entity.remove();
                    removedCount.incrementAndGet();
                }
                processed++;
            }
            
            // Show progress if enabled
            if (config.isShowCleanProgress() && currentIndex.get() < uuids.size()) {
                String progressMsg = cleanProgressText()
                    .replace("{CURRENT}", String.valueOf(currentIndex.get()))
                    .replace("{TOTAL}", String.valueOf(totalCount));
                
                Bukkit.getOnlinePlayers().stream()
                    .filter(Player::isOp)
                    .forEach(op -> op.sendMessage(ChatColor.translateAlternateColorCodes('&', progressMsg)));
            }
            
            // Check if done
            if (currentIndex.get() >= uuids.size()) {
                task.cancel();
                batchTasks.remove(task);
                onComplete.accept(removedCount.get());
            }
        }, 0L, 1L);
    }
    
    /**
     * Convert PreItemCleanEvent trigger to CleanCompleteEvent trigger.
     */
    private CleanCompleteEvent.CleanTrigger convertTrigger(PreItemCleanEvent.CleanTrigger trigger) {
        switch (trigger) {
            case SMART: return CleanCompleteEvent.CleanTrigger.SMART;
            case MANUAL: return CleanCompleteEvent.CleanTrigger.MANUAL;
            default: return CleanCompleteEvent.CleanTrigger.SCHEDULED;
        }
    }
    
    /**
     * Convert PreEntityCleanEvent trigger to CleanCompleteEvent trigger.
     */
    private CleanCompleteEvent.CleanTrigger convertTrigger(PreEntityCleanEvent.CleanTrigger trigger) {
        switch (trigger) {
            case SMART: return CleanCompleteEvent.CleanTrigger.SMART;
            case MANUAL: return CleanCompleteEvent.CleanTrigger.MANUAL;
            default: return CleanCompleteEvent.CleanTrigger.SCHEDULED;
        }
    }
    
    // ---- Broadcast text: exactly the configured message ----
    // config/cleaner.yml holds each message in the server's language (UltiCleaner#materializeText at
    // enable and reload, maintainer decision 2026-09-25), so the text sent is the file's text.

    private String warnText() {
        return config.getWarnMessage();
    }

    private String entityWarnText() {
        return config.getEntityWarnMessage();
    }

    private String itemCleanedText() {
        return config.getItemCleanedMessage();
    }

    private String entityCleanedText() {
        return config.getEntityCleanedMessage();
    }

    private String smartTriggeredText() {
        return config.getSmartCleanTriggeredMessage();
    }

    private String cleanProgressText() {
        return config.getCleanProgressMessage();
    }

    private String cleanCancelledText() {
        return config.getCleanCancelledMessage();
    }

    /**
     * Broadcast a message.
     */
    private void broadcastMessage(String message) {
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
    }
    
    /**
     * Broadcast warning message.
     */
    private void broadcastWarn(int seconds) {
        String message = warnText().replace("{TIME}", String.valueOf(seconds));
        broadcastMessage(message);
    }
    
    /**
     * Broadcast entity warning message.
     */
    private void broadcastEntityWarn(int seconds) {
        String message = entityWarnText().replace("{TIME}", String.valueOf(seconds));
        broadcastMessage(message);
    }
    
    /**
     * Broadcast item cleaned message.
     */
    private void broadcastItemCleaned(int count) {
        String message = itemCleanedText().replace("{COUNT}", String.valueOf(count));
        broadcastMessage(message);
    }
    
    /**
     * Broadcast entity cleaned message.
     */
    private void broadcastEntityCleaned(int count) {
        if (count > 0) {
            String message = entityCleanedText().replace("{COUNT}", String.valueOf(count));
            broadcastMessage(message);
        }
    }
    
    /**
     * Get remaining seconds until next item cleanup.
     */
    public int getItemCountdown() {
        return itemCountdown;
    }
    
    /**
     * Get remaining seconds until next entity cleanup.
     */
    public int getEntityCountdown() {
        return entityCountdown;
    }
    
    /**
     * Force immediate item cleanup.
     * 
     * @return number of items collected for cleaning (actual removal is async)
     */
    public int forceCleanItems() {
        List<UUID> items = collectItemsToClean();
        cleanItemsWithBatch(PreItemCleanEvent.CleanTrigger.MANUAL);
        itemCountdown = config.getItemCleanInterval();
        return items.size();
    }
    
    /**
     * Force immediate entity cleanup.
     * 
     * @return number of entities collected for cleaning (actual removal is async)
     */
    public int forceCleanEntities() {
        Map<EntityType, Integer> typeCounts = new HashMap<>();
        List<UUID> entities = collectEntitiesToClean(typeCounts);
        cleanEntitiesWithBatch(PreEntityCleanEvent.CleanTrigger.MANUAL);
        entityCountdown = config.getEntityCleanInterval();
        return entities.size();
    }
    
    /**
     * Force an immediate item cleanup and entity cleanup together ({@code /clean all}).
     * <p>
     * Each half fires its own {@code ITEMS} or {@code ENTITIES} {@link CleanCompleteEvent} exactly as
     * {@link #forceCleanItems()} and {@link #forceCleanEntities()} do. Once both halves have finished --
     * removed their last entity, found nothing to clean, or been cancelled by a pre-clean listener --
     * one further event of type {@link CleanCompleteEvent.CleanType#ALL} is fired with the combined
     * removed count, the time since this call, and trigger {@code MANUAL} (UltiKits/UltiCleaner#16).
     *
     * @return the numbers of items and of entities collected for cleaning, in that order
     *         (actual removal is batched over the following ticks)
     */
    public int[] forceCleanAll() {
        long startTime = System.currentTimeMillis();
        int itemCount = collectItemsToClean().size();
        int entityCount = collectEntitiesToClean(new HashMap<>()).size();

        AtomicInteger halvesPending = new AtomicInteger(2);
        AtomicInteger removed = new AtomicInteger(0);
        IntConsumer halfDone = count -> {
            removed.addAndGet(count);
            if (halvesPending.decrementAndGet() == 0) {
                fireAllEvent(removed.get(), System.currentTimeMillis() - startTime);
            }
        };
        cleanItemsWithBatch(PreItemCleanEvent.CleanTrigger.MANUAL, halfDone);
        cleanEntitiesWithBatch(PreEntityCleanEvent.CleanTrigger.MANUAL, halfDone);

        itemCountdown = config.getItemCleanInterval();
        entityCountdown = config.getEntityCleanInterval();
        return new int[] {itemCount, entityCount};
    }

    /**
     * Get current entity counts for status display.
     */
    public Map<String, Integer> getEntityCounts() {
        Map<String, Integer> counts = new HashMap<>();
        int itemCount = 0;
        int mobCount = 0;
        int totalEntities = 0;
        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                totalEntities++;
                if (entity instanceof Item) {
                    itemCount++;
                } else if (entityTypesCache.contains(entity.getType())) {
                    mobCount++;
                }
            }
        }
        
        counts.put("items", itemCount);
        counts.put("mobs", mobCount);
        counts.put("total", totalEntities);
        return counts;
    }
    
    /**
     * Get the total number of loaded chunks across every world.
     * <p>
     * A plain server statistic printed by {@code /clean check}, unrelated to any cleanup this
     * module performs. It lived on the chunk-unload service until that feature was removed
     * (UltiKits/UltiCleaner#27).
     *
     * @return the number of loaded chunks, summed over every world
     */
    public int getTotalLoadedChunks() {
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            total += world.getLoadedChunks().length;
        }
        return total;
    }
    
    /**
     * Check if any cleanup is currently in progress.
     *
     * @return true while an item batch or an entity batch is running
     */
    public boolean isCleaningInProgress() {
        return itemCleaningInProgress || entityCleaningInProgress;
    }

    /**
     * Check if an item cleanup batch is currently running.
     *
     * @return true from the start of an item batch until its last tick finishes
     */
    public boolean isItemCleaningInProgress() {
        return itemCleaningInProgress;
    }

    /**
     * Check if an entity cleanup batch is currently running.
     *
     * @return true from the start of an entity batch until its last tick finishes
     */
    public boolean isEntityCleaningInProgress() {
        return entityCleaningInProgress;
    }
    
    /**
     * Get TPS scheduler for status display.
     */
    public TpsAwareScheduler getTpsScheduler() {
        return tpsScheduler;
    }
}
