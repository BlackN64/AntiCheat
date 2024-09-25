package org.ayosynk.antiCheat;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class AntiCheat extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private HashMap<UUID, Integer> violationLevels = new HashMap<>();
    private HashMap<Player, Long> flyCheck = new HashMap<>();
    private HashMap<Player, Long> clickCheck = new HashMap<>();
    private HashMap<Player, Integer> clickCount = new HashMap<>();
    private HashMap<Player, Long> lastMove = new HashMap<>();

    @Override
    public void onEnable() {
        // Load configuration file
        this.saveDefaultConfig();
        config = this.getConfig();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Plugin enabled message
        getLogger().info("Advanced AntiCheat plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Advanced AntiCheat plugin disabled!");
    }

    // Commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("anticheat")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = this.getConfig();
                sender.sendMessage("AntiCheat configuration reloaded.");
                return true;
            } else {
                sender.sendMessage("Usage: /anticheat reload");
                return true;
            }
        }
        return false;
    }

    // Anti-Fly Detection
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        double distance = event.getFrom().distance(event.getTo());
        long currentTime = System.currentTimeMillis();

        // Anti-speed hack detection
        if (lastMove.containsKey(player) && (currentTime - lastMove.get(player)) < 1000) {
            double maxSpeed = config.getDouble("maxSpeed");
            if (distance > maxSpeed) {
                incrementViolation(player, "Speed Hacks Detected");
            }
        }
        lastMove.put(player, currentTime);

        // Anti-fly hack detection
        if (!player.isOnGround()) {
            if (!flyCheck.containsKey(player)) {
                flyCheck.put(player, System.currentTimeMillis());
            } else {
                long lastFlyTime = flyCheck.get(player);
                if (System.currentTimeMillis() - lastFlyTime > config.getInt("flyDetectionThreshold")) {
                    incrementViolation(player, "Fly Hacks Detected");
                }
            }
        } else {
            flyCheck.remove(player);
        }
    }

    // Anti-Killaura Detection
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            // Check for impossible hit rates
            if (event.getDamage() > config.getDouble("maxDamagePerHit")) {
                incrementViolation(attacker, "Killaura Detected");
            }
        }
    }

    // Anti-NoFall Detection
    @EventHandler
    public void onPlayerFall(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            if (player.getFallDistance() > config.getDouble("maxFallDistance")) {
                incrementViolation(player, "NoFall Detected");
            }
        }
    }

    // Anti-AutoClicker Detection
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        long currentTime = System.currentTimeMillis();

        if (!clickCheck.containsKey(player)) {
            clickCheck.put(player, currentTime);
            clickCount.put(player, 1);
        } else {
            long lastClickTime = clickCheck.get(player);
            clickCheck.put(player, currentTime);

            if ((currentTime - lastClickTime) < config.getInt("clickTimeThreshold")) {
                clickCount.put(player, clickCount.get(player) + 1);
                if (clickCount.get(player) > config.getInt("maxClicksPerSecond")) {
                    incrementViolation(player, "AutoClicker Detected");
                    clickCount.put(player, 0);
                }
            } else {
                clickCount.put(player, 1);
            }
        }
    }

    // Anti-XRay (basic mining pattern detection)
    @EventHandler
    public void onPlayerMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        if (blockType == Material.DIAMOND_ORE || blockType == Material.EMERALD_ORE) {
            // Track how many valuable blocks mined in a short time
            incrementViolation(player, "XRay Detected (Mining Too Many Ores)");
        }
    }

    // Anti-Godmode Detection
    @EventHandler
    public void onPlayerHealthChange(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getHealth() == player.getMaxHealth()) {
                incrementViolation(player, "Godmode Detected");
            }
        }
    }

    // Increment player's violation level and take actions based on thresholds
    private void incrementViolation(Player player, String reason) {
        UUID playerUUID = player.getUniqueId();
        int violations = violationLevels.getOrDefault(playerUUID, 0) + 1;
        violationLevels.put(playerUUID, violations);

        if (violations >= config.getInt("violationThresholds.kick")) {
            notifyAdmins(player, reason + " (Kick)");
            kickPlayer(player, reason);
        } else if (violations >= config.getInt("violationThresholds.ban")) {
            notifyAdmins(player, reason + " (Ban)");
            banPlayer(player);
        } else {
            notifyAdmins(player, reason + " (" + violations + " Violations)");
        }
    }

    // Notify Admins and Players
    private void notifyAdmins(Player hacker, String reason) {
        String message = config.getString("notifyMessage")
                .replace("%player%", hacker.getName())
                .replace("%reason%", reason);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("anticheat.notify")) {
                player.sendMessage(message);
            }
        }
    }

    private void kickPlayer(Player player, String reason) {
        player.kickPlayer(reason);
    }

    private void banPlayer(Player player) {
        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), "Banned for hacking", null, null);
        player.kickPlayer("You have been banned for hacking.");
    }
}