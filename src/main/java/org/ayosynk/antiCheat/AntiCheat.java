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
import org.bukkit.Location;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.HashMap;
import java.util.Map;

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
    private HashMap<Player, Integer> clickCount = new HashMap<>();
    private HashMap<Player, Long> lastMove = new HashMap<>();
    private Map<Player, Long> noFallCheck = new HashMap<>();
    private HashMap<UUID, List<MinedBlock>> playerMiningHistory = new HashMap<>();
    private final Map<Player, Long> clickCheck = new HashMap<>();
    private final Map<Player, Integer> hitCount = new HashMap<>();
    private final Map<Player, Location> lastPlayerLocation = new HashMap<>();
    private final Map<Player, Long> playerLastMoveTime = new HashMap<>();
    private Map<Player, Long> lastAttackTime = new HashMap<>();
    private final HashMap<UUID, Long> lastTotemUse = new HashMap<>();
    private final HashMap<UUID, Integer> violationCount = new HashMap<>();


    @Override
    public void onEnable() {
        saveDefaultConfig(); // Saves the default config if it does not exist
        config = getConfig();


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

    // Anti-Killaura Detection
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            double damage = event.getDamage();
            long currentTime = System.currentTimeMillis();

            // Check for excessive damage
            if (damage > config.getDouble("maxDamagePerHit", 10.0)) {
                incrementViolation(attacker, "Killaura Detected (Excessive Damage)");
            }

            // Check for suspicious attack speeds
            long lastAttack = lastAttackTime.getOrDefault(attacker, 0L);
            if ((currentTime - lastAttack) < config.getInt("killaura.attackSpeedThreshold", 200)) {
                incrementViolation(attacker, "Killaura Detected (Suspicious Attack Speed)");
            }
            lastAttackTime.put(attacker, currentTime);

            // Track hits
            int hits = hitCount.getOrDefault(attacker, 0);
            hits++;
            hitCount.put(attacker, hits);

            // Check for impossible angles of attack
            double angle = attacker.getLocation().getDirection().angle(target.getLocation().getDirection());
            if (angle > config.getDouble("killaura.maxAttackAngle", 60.0)) {
                incrementViolation(attacker, "Killaura Detected (Impossible Attack Angle)");
            }

            // Check for multi-entity hits
            if (hits > config.getInt("killaura.multiHitThreshold", 2)) {
                incrementViolation(attacker, "Killaura Detected (Multi-Entity Hits)");
            }

            // Check for reach
            double reachDistance = attacker.getLocation().distance(target.getLocation());
            if (reachDistance > config.getDouble("killaura.maxReach", 4.0)) {
                incrementViolation(attacker, "Killaura Detected (Excessive Reach)");
            }

            // Abnormal head movement
            float yawDifference = Math.abs(attacker.getLocation().getYaw() - target.getLocation().getYaw());
            if (yawDifference > config.getDouble("killaura.maxYawDifference", 180.0)) {
                incrementViolation(attacker, "Killaura Detected (Abnormal Head Movement)");
            }

            // Reset hit count after a certain time frame
            if (currentTime - lastAttack > 1000) {
                hitCount.put(attacker, 0);
            }
        }
    }
  //movement
    private void handleMovementCheck(Player player, double x, double y, double z) {
        Location lastlocation = lastPlayerLocation.get(player);
        Location newLocation = new Location(player.getWorld(), x, y, z);

        if (lastlocation != null) {
            double distance = lastlocation.distance(newLocation);
            long currentTime = System.currentTimeMillis();
            long timeElapsed = System.currentTimeMillis() - playerLastMoveTime.getOrDefault(player, System.currentTimeMillis());

            if (distance > config.getDouble("maxAllowedDistance", 5.0) && timeElapsed < 50) {
                incrementViolation(player, "Suspicious Movement (Speed/Fly Hack Detected)");
            }
        }

        lastPlayerLocation.put(player, newLocation);
        playerLastMoveTime.put(player, System.currentTimeMillis());
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

    // Anti-AutoTotem
// Event to detect when a player uses a totem of undying (resurrection event)
    @EventHandler
    public void onPlayerResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Check if the totem was used to resurrect the player
        if (event.isCancelled() || player.getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Get config values
        long totemUsageThreshold = config.getLong("totem_usage_threshold");
        int maxViolations = config.getInt("max_violations");
        String violationMessage = config.getString("violation_message").replace("{player}", player.getName());
        String actionOnViolation = config.getString("action_on_violation");

        // Check if the player used a totem recently
        if (lastTotemUse.containsKey(playerUUID)) {
            long lastUseTime = lastTotemUse.get(playerUUID);
            long timeDiff = currentTime - lastUseTime;

            if (timeDiff < totemUsageThreshold) {
                // Player flagged for AutoTotem usage
                Bukkit.getLogger().info(player.getName() + " might be using AutoTotem! Time between uses: " + timeDiff + "ms");

                // Increment the player's violation count
                int currentViolations = violationCount.getOrDefault(playerUUID, 0) + 1;
                violationCount.put(playerUUID, currentViolations);

                // Send message to the player
                player.sendMessage(violationMessage);

                // Check if the player exceeds max violations
                if (currentViolations >= maxViolations) {
                    handleViolationAction(player, actionOnViolation);
                }

                // Notify admins
                if (actionOnViolation.equalsIgnoreCase("NOTIFY_ADMIN")) {
                    String adminNotification = config.getString("admin_notification").replace("{player}", player.getName());
                    notifyAdmins(adminNotification);
                }

                // Log the event if enabled
                if (config.getBoolean("log_to_file")) {
                    logToFile(player, timeDiff);
                }

                // Optionally broadcast to all players
                if (config.getBoolean("broadcast_to_all_players")) {
                    String broadcastMessage = config.getString("broadcast_message").replace("{player}", player.getName());
                    Bukkit.broadcastMessage(broadcastMessage);
                }
            }
        }

        // Update the last totem use time for this player
        lastTotemUse.put(playerUUID, currentTime);
    }

    // Event to track when players switch items (monitor for totem switches)
    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem != null && newItem.getType() == Material.TOTEM_OF_UNDYING) {
            lastTotemUse.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    // Event to detect inventory click and item movement (for manual totem placement)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Check if the player moves a totem into their off-hand slot
        if (currentItem != null && currentItem.getType() == Material.TOTEM_OF_UNDYING ||
                cursorItem != null && cursorItem.getType() == Material.TOTEM_OF_UNDYING) {

            lastTotemUse.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    // Handle violation actions based on config settings
    private void handleViolationAction(Player player, String action) {
        switch (action.toUpperCase()) {
            case "KICK":
                player.kickPlayer("You have been kicked for suspected AutoTotem usage.");
                break;
            case "BAN":
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(player.getName(), "Banned for AutoTotem usage", null, null);
                player.kickPlayer("You have been banned for AutoTotem usage.");
                break;
            case "NOTIFY_ADMIN":
                // Already handled in the resurrect event
                break;
            default:
                // No action
                break;
        }
    }

    // Notify all online admins
    private void notifyAdmins(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("anticheat.notify")) {
                player.sendMessage(message);
            }
        }
    }

    // Log suspicious totem usage to a file
    private void logToFile(Player player, long timeDiff) {
        // Logging logic (write to a custom file, e.g., logs/anticheat-autototem.log)
        Bukkit.getLogger().info("Logging AutoTotem event for " + player.getName() + ". Time between uses: " + timeDiff + "ms");
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