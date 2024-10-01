package org.ayosynk.antiCheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;

public class AntiTotem implements Listener {

    private final AntiCheat plugin; // Reference to the main plugin
    private final Map<Player, Long> lastTotemUsage = new HashMap<>();
    private final Map<Player, Integer> playerViolations = new HashMap<>();

    private int minReactionTime;
    private int maxTotemUsesPerTimeFrame;
    private int totemTimeFrame;
    private int warningThreshold;
    private int kickThreshold;
    private int banThreshold;
    private boolean notifyAdmins;
    private boolean debug;

    // Constructor takes the AntiCheat plugin instance
    public AntiTotem(AntiCheat plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        minReactionTime = config.getInt("autototem.minReactionTime", 500);
        maxTotemUsesPerTimeFrame = config.getInt("autototem.maxTotemUsesPerTimeFrame", 3);
        totemTimeFrame = config.getInt("autototem.totemTimeFrame", 5000);
        warningThreshold = config.getInt("autototem.violationThresholds.warning", 3);
        kickThreshold = config.getInt("autototem.violationThresholds.kick", 5);
        banThreshold = config.getInt("autototem.violationThresholds.ban", 10);
        notifyAdmins = config.getBoolean("autototem.notifyAdmins", true);
        debug = config.getBoolean("autototem.debug", false);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        double playerHealth = player.getHealth() - event.getFinalDamage();

        if (playerHealth <= 0 && hasTotemInHand(player)) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastTotem = currentTime - lastTotemUsage.getOrDefault(player, 0L);

            if (timeSinceLastTotem < minReactionTime) {
                handleViolation(player);
            }

            lastTotemUsage.put(player, currentTime);
        }
    }

    private boolean hasTotemInHand(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }

    private void handleViolation(Player player) {
        int violations = playerViolations.getOrDefault(player, 0) + 1;
        playerViolations.put(player, violations);

        if (violations >= banThreshold) {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("autototem.messages.ban")));
            if (notifyAdmins) notifyAdmins(player, "ban");
        } else if (violations >= kickThreshold) {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("autototem.messages.kick")));
            if (notifyAdmins) notifyAdmins(player, "kick");
        } else if (violations >= warningThreshold) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("autototem.messages.warning")));
            if (notifyAdmins) notifyAdmins(player, "warning");
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
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("antitotem.notify")) {
                admin.sendMessage(message);
            }
        }
    }
}
