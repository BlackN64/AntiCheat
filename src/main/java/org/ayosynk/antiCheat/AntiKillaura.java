package org.ayosynk.antiCheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;

public class AntiKillaura implements Listener {

    private final AntiCheat plugin;
    private final Map<Player, AttackData> attackDataMap = new HashMap<>();
    private final double maxAttackFrequency;
    private final double maxAimAngle;
    private final int minAttackInterval;
    private final int maxWarnings;

    public AntiKillaura(AntiCheat plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.maxAttackFrequency = config.getDouble("anti-killaura.max-attack-frequency", 4.0);
        this.maxAimAngle = config.getDouble("anti-killaura.max-aim-angle", 60.0);
        this.minAttackInterval = config.getInt("anti-killaura.minimum-attack-interval", 200);
        this.maxWarnings = config.getInt("anti-killaura.max-warnings", 3); // Number of warnings before kick
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Entity target = event.getEntity();
            long currentTime = System.currentTimeMillis();

            AttackData data = attackDataMap.getOrDefault(attacker, new AttackData(0, 0, 0));
            long lastAttackTime = data.getLastAttackTime();
            int attackCount = data.getAttackCount();
            int warningCount = data.getWarningCount();

            // Check attack frequency
            if (currentTime - lastAttackTime < minAttackInterval) {
                attackCount++;
            } else {
                attackCount = 1; // Reset if too much time has passed
            }

            data.setLastAttackTime(currentTime);
            data.setAttackCount(attackCount);
            attackDataMap.put(attacker, data);

            // Check aim angle
            double aimAngle = DetectionUtils.getAimAngle(attacker, target);
            if (aimAngle > maxAimAngle) {
                issueWarningOrKick(attacker, data, "Suspicious aim angle: " + aimAngle);
                return;
            }

            // Check if the attack frequency exceeds the limit
            if (DetectionUtils.isExcessiveAttacking(attackCount, maxAttackFrequency)) {
                issueWarningOrKick(attacker, data, "Excessive attack speed detected.");
            }
        }
    }

    private void issueWarningOrKick(Player player, AttackData data, String reason) {
        int warningCount = data.getWarningCount();

        if (warningCount >= maxWarnings) {
            kickPlayerForKillaura(player, reason);
        } else {
            warningCount++;
            data.setWarningCount(warningCount);
            attackDataMap.put(player, data);
            warnPlayer(player, reason, warningCount);
        }
    }

    private void warnPlayer(Player player, String reason, int warningCount) {
        String warningMessage = plugin.getConfig().getString("anti-killaura.warning-message")
                .replace("{warnings_left}", String.valueOf(maxWarnings - warningCount));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', warningMessage));
        Bukkit.getLogger().info("Warning issued to player: " + player.getName() + ". Reason: " + reason);
    }

    private void kickPlayerForKillaura(Player player, String reason) {
        Bukkit.getLogger().info("Killaura detection triggered for player: " + player.getName() + ". Reason: " + reason);
        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("anti-killaura.kick-message")));

        if (plugin.getConfig().getBoolean("anti-killaura.logging.notify-admins")) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "&f&l[VoidAntiCheat] &cAdmin Alert: Killaura detected from player " + player.getName()));
        }
    }
}
