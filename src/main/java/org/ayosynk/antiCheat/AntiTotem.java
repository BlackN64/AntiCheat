package org.ayosynk.antiCheat;

import org.bukkit.Material;
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
    private final ViolationHandler violationHandler;

    private int minReactionTime;
    private int maxTotemUsesPerTimeFrame;
    private int totemTimeFrame;

    // Constructor
    public AntiTotem(AntiCheat plugin) {
        this.plugin = plugin;
        this.violationHandler = new ViolationHandler(plugin);
        loadConfig();
    }

    private void loadConfig() {
        minReactionTime = plugin.getConfig().getInt("autototem.minReactionTime", 500);
        maxTotemUsesPerTimeFrame = plugin.getConfig().getInt("autototem.maxTotemUsesPerTimeFrame", 3);
        totemTimeFrame = plugin.getConfig().getInt("autototem.totemTimeFrame", 5000);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        double playerHealth = player.getHealth() - event.getFinalDamage();

        if (playerHealth <= 0 && hasTotemInHand(player)) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastTotem = lastTotemUsage.getOrDefault(player, 0L);

            // Detect quick totem usage
            if (currentTime - timeSinceLastTotem < minReactionTime) {
                int violations = playerViolations.merge(player, 1, Integer::sum);
                violationHandler.handleViolation(player, violations);
            }

            lastTotemUsage.put(player, currentTime);
        }
    }

    private boolean hasTotemInHand(Player player) {
        Material mainHandItem = player.getInventory().getItemInMainHand().getType();
        Material offHandItem = player.getInventory().getItemInOffHand().getType();
        return mainHandItem == Material.TOTEM_OF_UNDYING || offHandItem == Material.TOTEM_OF_UNDYING;
    }
}
