package org.ayosynk.antiCheat;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ViolationHandler {
    private final AntiCheat plugin;
    private final boolean notifyAdmins;
    private final boolean debug;
    private final int warningThreshold;
    private final int kickThreshold;
    private final int banThreshold;

    public ViolationHandler(AntiCheat plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.notifyAdmins = config.getBoolean("autototem.notifyAdmins", true);
        this.debug = config.getBoolean("autototem.debug", false);
        this.warningThreshold = config.getInt("autototem.violationThresholds.warning", 3);
        this.kickThreshold = config.getInt("autototem.violationThresholds.kick", 5);
        this.banThreshold = config.getInt("autototem.violationThresholds.ban", 10);
    }

    public void handleViolation(Player player, int violations) {
        if (violations >= banThreshold) {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("autototem.messages.ban")));
            notifyAdmins(player, "ban");
        } else if (violations >= kickThreshold) {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("autototem.messages.kick")));
            notifyAdmins(player, "kick");
        } else if (violations >= warningThreshold) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("autototem.messages.warning")));
            notifyAdmins(player, "warning");
        }

        if (debug) {
            plugin.getLogger().info("Player " + player.getName() + " has " + violations + " violations.");
        }
    }

    private void notifyAdmins(Player player, String action) {
        String message = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("autototem.adminNotificationMessage")
                        .replace("%player%", player.getName())
                        .replace("%action%", action));
        plugin.getServer().getOnlinePlayers().stream()
                .filter(admin -> admin.hasPermission("antitotem.notify"))
                .forEach(admin -> admin.sendMessage(message));
    }
}
