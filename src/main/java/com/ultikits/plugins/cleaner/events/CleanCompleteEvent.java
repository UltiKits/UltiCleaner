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
     * Type of cleanup performed.
     * <p>
     * Two of these four constants are never constructed by any code path in this module, so a
     * listener's {@code switch} arm for either one can never execute:
     * <ul>
     * <li>{@link #CHUNKS} -- was already never constructed, and is now permanently
     * unconstructible: chunk unloading was removed from this module entirely because the server
     * engine already unloads idle chunks itself (UltiKits/UltiCleaner#27), so nothing is left that
     * could ever complete a chunk cleanup.</li>
     * <li>{@link #ALL} -- {@code /clean all} runs an item cleanup and an entity cleanup in
     * succession, each firing its own event with its own type, and never constructs one with this
     * value.</li>
     * </ul>
     * Both are tracked by UltiKits/UltiCleaner#16, which owns the decision about removing them;
     * removing a constant from a published enum is not this change's call to make.
     */
    public enum CleanType {
        ITEMS,
        ENTITIES,
        /** Never constructed; permanently unconstructible since chunk unloading was removed. See the enum javadoc and UltiKits/UltiCleaner#16. */
        CHUNKS,
        /** Never constructed; {@code /clean all} fires ITEMS and ENTITIES separately. See the enum javadoc and UltiKits/UltiCleaner#16. */
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
     * @param cleanedCount number of items/entities/chunks cleaned
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
     * Get the number of items/entities/chunks cleaned.
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
