package com.ultikits.plugins.cleaner.commands;

import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.service.CleanerService;
import com.ultikits.plugins.cleaner.service.TpsAwareScheduler;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.abstracts.command.BaseCommandExecutor;
import com.ultikits.ultitools.annotations.command.*;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Command for manual cleanup operations.
 * Supports item cleanup, entity cleanup, status checking,
 * and server statistics.
 *
 * @author wisdomme
 * @version 2.0.0
 */
@CmdExecutor(
    alias = {"clean", "cleaner", "clear"},
    permission = "ulticleaner.clean",
    description = "command_description"
)
public class CleanCommand extends BaseCommandExecutor {
    
    private final CleanerService cleanerService;
    private final CleanerConfig config;
    private final UltiToolsPlugin plugin;
    
    public CleanCommand(CleanerService cleanerService, CleanerConfig config, UltiToolsPlugin plugin) {
        this.cleanerService = cleanerService;
        this.config = config;
        this.plugin = plugin;
    }

    /** Colour codes in catalogue text use {@code &}, as the module's configured messages do. */
    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    @CmdMapping(format = "items")
    public void cleanItems(@CmdSender CommandSender sender) {
        if (cleanerService.isCleaningInProgress()) {
            sender.sendMessage(color(plugin.i18n("clean_in_progress")));
            return;
        }
        int count = cleanerService.forceCleanItems();
        sender.sendMessage(color(plugin.i18n("clean_started_items").replace("{COUNT}", String.valueOf(count))));
    }
    
    @CmdMapping(format = "entities")
    public void cleanEntities(@CmdSender CommandSender sender) {
        if (cleanerService.isCleaningInProgress()) {
            sender.sendMessage(color(plugin.i18n("clean_in_progress")));
            return;
        }
        int count = cleanerService.forceCleanEntities();
        sender.sendMessage(color(plugin.i18n("clean_started_entities").replace("{COUNT}", String.valueOf(count))));
    }
    
    @CmdMapping(format = "all")
    public void cleanAll(@CmdSender CommandSender sender) {
        if (cleanerService.isCleaningInProgress()) {
            sender.sendMessage(color(plugin.i18n("clean_in_progress")));
            return;
        }
        int itemCount = cleanerService.forceCleanItems();
        int entityCount = cleanerService.forceCleanEntities();
        sender.sendMessage(color(plugin.i18n("clean_started_all")
                .replace("{ITEMS}", String.valueOf(itemCount))
                .replace("{ENTITIES}", String.valueOf(entityCount))));
    }
    
    @CmdMapping(format = "check")
    public void check(@CmdSender CommandSender sender) {
        Map<String, Integer> counts = cleanerService.getEntityCounts();
        
        sender.sendMessage(color(plugin.i18n("stats_title")));
        sender.sendMessage(color(plugin.i18n("stats_items").replace("{COUNT}", String.valueOf(counts.get("items")))));
        sender.sendMessage(color(plugin.i18n("stats_mobs").replace("{COUNT}", String.valueOf(counts.get("mobs")))));
        sender.sendMessage(color(plugin.i18n("stats_total").replace("{COUNT}", String.valueOf(counts.get("total")))));
        
        sender.sendMessage(color(plugin.i18n("stats_chunks_loaded")
                .replace("{COUNT}", String.valueOf(cleanerService.getTotalLoadedChunks()))));
        
        TpsAwareScheduler tpsScheduler = cleanerService.getTpsScheduler();
        if (tpsScheduler != null) {
            sender.sendMessage(color(plugin.i18n("stats_tps").replace("{TPS}", tpsScheduler.getTpsStatus())));
        }
    }
    
    @CmdMapping(format = "status")
    public void status(@CmdSender CommandSender sender) {
        sender.sendMessage(color(plugin.i18n("status_title")));
        sender.sendMessage(color(plugin.i18n("status_next_item_clean")
                .replace("{TIME}", String.valueOf(cleanerService.getItemCountdown()))));
        sender.sendMessage(color(plugin.i18n("status_next_entity_clean")
                .replace("{TIME}", String.valueOf(cleanerService.getEntityCountdown()))));
        
        if (cleanerService.isCleaningInProgress()) {
            sender.sendMessage(color(plugin.i18n("status_cleaning")));
        } else {
            sender.sendMessage(color(plugin.i18n("status_idle")));
        }
        
        TpsAwareScheduler tpsScheduler = cleanerService.getTpsScheduler();
        if (tpsScheduler != null) {
            sender.sendMessage(color(plugin.i18n("status_tps").replace("{TPS}", tpsScheduler.getTpsStatus())));
            if (tpsScheduler.isCriticalTps()) {
                sender.sendMessage(color(plugin.i18n("status_tps_critical")
                        .replace("{PERCENT}", String.valueOf(config.getCriticalTpsReduction()))));
            } else if (tpsScheduler.isLowTps()) {
                sender.sendMessage(color(plugin.i18n("status_tps_low")
                        .replace("{PERCENT}", String.valueOf(config.getLowTpsReduction()))));
            }
        }
    }
    
    @CmdMapping(format = "")
    public void help(@CmdSender CommandSender sender) {
        sender.sendMessage(color(plugin.i18n("cmd_help_title")));
        sender.sendMessage(color(plugin.i18n("cmd_help_items")));
        sender.sendMessage(color(plugin.i18n("cmd_help_entities")));
        sender.sendMessage(color(plugin.i18n("cmd_help_all")));
        sender.sendMessage(color(plugin.i18n("cmd_help_check")));
        sender.sendMessage(color(plugin.i18n("cmd_help_status")));
    }
    
    @Override
    protected void handleHelp(CommandSender sender) {
        help(sender);
    }
}
