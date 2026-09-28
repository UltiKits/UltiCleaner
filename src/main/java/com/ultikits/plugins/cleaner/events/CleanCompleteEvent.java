package com.ultikits.plugins.cleaner.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired after a cleanup operation is complete.
 * This event cannot be cancelled (cleanup already happened).
 *
 * @author wisdomme
 * @version 1.0.0
 */
public class CleanCompleteEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final CleanType cleanType;
    private final int cleanedCount;
    private final long durationMs;
    private final CleanTrigger trigger;
    
    /**
     * Type of cleanup performed. Every constant is fired by this module (UltiKits/UltiCleaner#16).
     * The former {@code CHUNKS} constant was removed: chunk unloading was removed from this module
     * (UltiKits/UltiCleaner#27), so nothing could ever fire it.
     */
    public enum CleanType {
        /** A ground-item cleanup finished: scheduled, smart, {@code /clean items}, or the item half of {@code /clean all}. */
        ITEMS,
        /** An entity cleanup finished: scheduled, smart, {@code /clean entities}, or the entity half of {@code /clean all}. */
        ENTITIES,
        /**
         * {@code /clean all} finished: fired once, after both its halves have finished (each half also
         * fires its own {@link #ITEMS} or {@link #ENTITIES} event first), with the combined count.
         */
        ALL
    }
    
    /**
     * Cleanup trigger type.
     */
    public enum CleanTrigger {
        SCHEDULED,
        SMART,
        MANUAL
    }
    
    /**
     * Create a new CleanCompleteEvent.
     * 
     * @param cleanType what was cleaned
     * @param cleanedCount number of items and/or entities cleaned
     * @param durationMs how long the cleanup took in milliseconds
     * @param trigger what triggered the cleanup
     */
    public CleanCompleteEvent(CleanType cleanType, int cleanedCount, long durationMs, CleanTrigger trigger) {
        super(true); // Async event
        this.cleanType = cleanType;
        this.cleanedCount = cleanedCount;
        this.durationMs = durationMs;
        this.trigger = trigger;
    }
    
    /**
     * Get the type of cleanup performed.
     * 
     * @return cleanup type
     */
    public CleanType getCleanType() {
        return cleanType;
    }
    
    /**
     * Get the number of items and/or entities cleaned.
     * 
     * @return cleaned count
     */
    public int getCleanedCount() {
        return cleanedCount;
    }
    
    /**
     * Get how long the cleanup took.
     * 
     * @return duration in milliseconds
     */
    public long getDurationMs() {
        return durationMs;
    }
    
    /**
     * Get what triggered the cleanup.
     * 
     * @return trigger type
     */
    public CleanTrigger getTrigger() {
        return trigger;
    }
    
    /**
     * Get formatted duration string.
     * 
     * @return duration as string (e.g., "123ms")
     */
    public String getFormattedDuration() {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else {
            return String.format("%.2fs", durationMs / 1000.0);
        }
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
