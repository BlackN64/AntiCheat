package org.ayosynk.antiCheat;

import org.bukkit.*;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.block.Action;

import java.util.*;

class ChatUtils {
    // chat colors
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}

public class AntiCheat extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private HashMap<UUID, Integer> violationLevels = new HashMap<>();
    private HashMap<Player, Long> flyCheck = new HashMap<>();
    private HashMap<Player, Long> clickCheck = new HashMap<>();
    private HashMap<Player, Integer> clickCount = new HashMap<>();
    private HashMap<Player, Long> lastMove = new HashMap<>();
    private Map<Player, Long> noFallCheck = new HashMap<>();
    private HashMap<UUID, List<MinedBlock>> playerMiningHistory = new HashMap<>();

    @Override
    public void onEnable() {
        // Load configuration file
        this.saveDefaultConfig();
        config = this.getConfig();

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Plugin enabled message
        getLogger().info("AntiCheat plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiCheat plugin disabled!");
    }

    // Commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("anticheat")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = this.getConfig();
                sender.sendMessage(ChatUtils.colorize("&aAntiCheat configuration reloaded."));
                return true;
            } else {
                sender.sendMessage(ChatUtils.colorize("&cUsage: /anticheat reload"));
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

        // Bypass check for op player, player in creative mode, or player using elytra
        if (player.isOp() || player.getGameMode() == GameMode.CREATIVE || player.isGliding()) {
            return; // skip the fly checks for these players
        }

        // Anti-speed hack detection
        if (lastMove.containsKey(player) && (currentTime - lastMove.get(player)) < 1000) {
            double maxSpeed = config.getDouble("maxSpeed", 0.5);
            if (distance > maxSpeed) {
                incrementViolation(player, "Speed Hacks Detected");
            }
        }
        lastMove.put(player, currentTime);

        // Anti-fly hack detection
        if (!player.isOnGround() && !player.isGliding()) {
            if (!flyCheck.containsKey(player)) {
                flyCheck.put(player, System.currentTimeMillis());
            } else {
                long lastFlyTime = flyCheck.get(player);
                if (System.currentTimeMillis() - lastFlyTime > config.getInt("flyDetectionThreshold", 2000)) {
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
            Player target = (Player) event.getEntity();
            double damage = event.getDamage();

            // Check for impossible hit rates
            if (damage > config.getDouble("maxDamagePerHit", 10.0)) {
                incrementViolation(attacker, "Killaura Detected (Excessive Damage)");
            }

            // Check for suspicious attack speeds
            long lastAttackTime = clickCheck.getOrDefault(attacker, 0L);
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastAttackTime) < config.getInt("killaura.attackSpeedThreshold", 200)) {
                incrementViolation(attacker, "Killaura Detected (Suspicious Attack Speed)");
            }
            clickCheck.put(attacker, currentTime);

            // Check for impossible angles of attack
            double angle = attacker.getLocation().getDirection().angle(target.getLocation().getDirection());
            if (angle > config.getDouble("killaura.maxAttackAngle", 60.0)) {
                incrementViolation(attacker, "Killaura Detected (Impossible Attack Angle)");
            }

            // Check for multi-entity hits (attacking multiple entities at the same time)
            long multiHitThreshold = config.getInt("killaura.multiHitThreshold", 100);
            if ((currentTime - lastAttackTime) < multiHitThreshold) {
                incrementViolation(attacker, "Killaura Detected (Multi-Entity Hits)");
            }

            // Check for reach
            double reachDistance = attacker.getLocation().distance(target.getLocation());
            if (reachDistance > config.getDouble("killaura.maxReach", 4.0)) {
                incrementViolation(attacker, "Killaura Detected (Excessive Reach)");
            }

            // Abnormal head movement (tracking instantly)
            float yawDifference = Math.abs(attacker.getLocation().getYaw() - target.getLocation().getYaw());
            if (yawDifference > config.getDouble("killaura.maxYawDifference", 180.0)) {
                incrementViolation(attacker, "Killaura Detected (Abnormal Head Movement)");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        noFallCheck.remove(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        noFallCheck.remove(player);
    }

    // Refined NoFall Detection Logic
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // Only handle fall damage
            if (event.getCause() == DamageCause.FALL) {
                // Detect NoFall based on actual Y-coordinate difference
                double fallDistance = player.getFallDistance();
                double expectedDamage = calculateExpectedFallDamage(fallDistance, player);

                // If the player takes less damage than expected or no damage at all
                if (event.getDamage() < expectedDamage) {
                    getLogger().info("NoFall detected for player " + player.getName() + ". Expected damage: " + expectedDamage + ", but took: " + event.getDamage());
                    incrementViolation(player, "NoFall Hack Detected (Prevented Fall Damage)");
                }
            }
        }
    }

    private double calculateExpectedFallDamage(double fallDistance, Player player) {
        double baseDamage = config.getDouble("expectedFallDamage", 2.0);

        // Adjust for Feather Falling boots
        if (player.getInventory().getBoots() != null &&
                player.getInventory().getBoots().containsEnchantment(Enchantment.FEATHER_FALLING)) {
            int featherFallLevel = player.getInventory().getBoots().getEnchantmentLevel(Enchantment.FEATHER_FALLING);
            baseDamage -= (featherFallLevel * 0.5);
        }

        // Adjust for Slow Falling effect
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return 0.0; // Slow falling negates all fall damage
        }

        return baseDamage * (fallDistance / config.getDouble("maxFallDistance", 4.0));
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

            if ((currentTime - lastClickTime) < config.getInt("clickTimeThreshold", 100)) {
                clickCount.put(player, clickCount.get(player) + 1);
                if (clickCount.get(player) > config.getInt("maxClicksPerSecond", 10)) {
                    incrementViolation(player, "AutoClicker Detected");
                    clickCount.put(player, 0);
                }
            } else {
                clickCount.put(player, 1);
            }
        }
    }

    // Advanced Anti-XRay (Mining Pattern Detection)
    @EventHandler
    public void onPlayerMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        UUID playerUUID = player.getUniqueId();

        // List of valuable ores
        List<Material> valuableOres = Arrays.asList(Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.GOLD_ORE, Material.ANCIENT_DEBRIS);

        // Detect mining valuable ores
        if (valuableOres.contains(blockType)) {
            long currentTime = System.currentTimeMillis();

            // Track the player's mining history
            if (!playerMiningHistory.containsKey(playerUUID)) {
                playerMiningHistory.put(playerUUID, new ArrayList<>());
            }

            // Add mined block location and time to the player's history
            List<MinedBlock> miningHistory = playerMiningHistory.get(playerUUID);
            miningHistory.add(new MinedBlock(event.getBlock().getLocation(), currentTime));

            // Remove old entries from mining history (older than the configured time window)
            long timeWindow = config.getLong("xray.timeWindow", 60000); // 60 seconds
            miningHistory.removeIf(minedBlock -> (currentTime - minedBlock.getTime()) > timeWindow);

            // Check mining pattern (e.g., tunneling directly to ores)
            int suspiciousCount = 0;
            for (MinedBlock minedBlock : miningHistory) {
                if (minedBlock.getLocation().distance(event.getBlock().getLocation()) < config.getDouble("xray.minDistanceBetweenOres", 5.0)) {
                    suspiciousCount++;
                }
            }

            // Trigger violation if too many ores are mined in close proximity
            if (suspiciousCount > config.getInt("xray.maxOresInProximity", 3)) {
                incrementViolation(player, "XRay Detected (Suspicious Mining Pattern)");
            }
        }
    }

    // Class to track mined blocks and their locations over time
    private static class MinedBlock {
        private final Location location;
        private final long time;

        public MinedBlock(Location location, long time) {
            this.location = location;
            this.time = time;
        }

        public Location getLocation() {
            return location;
        }

        public long getTime() {
            return time;
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

        if (violations >= config.getInt("violationThresholds.kick", 3)) {
            notifyAdmins(player, reason + " (Kick)");
            kickPlayer(player, reason);
            violationLevels.remove(playerUUID); // Reset after kick
        } else if (violations >= config.getInt("violationThresholds.ban", 5)) {
            notifyAdmins(player, reason + " (Ban)");
            banPlayer(player);
            violationLevels.remove(playerUUID); // Reset after ban
        } else {
            notifyAdmins(player, reason + " (" + violations + " Violations)");
        }
    }

    // Notify Admins and Players
    private void notifyAdmins(Player hacker, String reason) {
        String message = config.getString("notifyMessage", "&c%player% has been flagged for %reason%")
                .replace("%player%", hacker.getName())
                .replace("%reason%", reason);
        message = ChatUtils.colorize(message);

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