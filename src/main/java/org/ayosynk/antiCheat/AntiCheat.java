package org.ayosynk.antiCheat;

import com.comphenix.protocol.PacketType;
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
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
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
    private ProtocolManager protocolManager;
    private final Map<Player, Location> lastPlayerLocation = new HashMap<>();
    private final Map<Player, Long> playerLastMoveTime = new HashMap<>();


    // Configuration variables
    private double maxDamagePerHit;
    private int attackSpeedThreshold;
    private int maxHitsInTimeFrame;
    private long hitsTimeFrame;
    private double maxAttackAngle;
    private long multiHitThreshold;
    private double maxReach;
    private double maxYawDifference;


    @Override
    public void onEnable() {
        saveDefaultConfig(); // Saves the default config if it does not exist
        config = getConfig();
        loadConfig(); // Load configuration values

        Bukkit.getPluginManager().registerEvents(this, this);
        protocolManager = ProtocolLibrary.getProtocolManager();

        // Add a packet listener to monitor player interactions
        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                // Handle entity usage packets for further validation if necessary
                double x = event.getPacket().getDoubles().read(0);
                double y = event.getPacket().getDoubles().read(1);
                double z = event.getPacket().getDoubles().read(2);

                handleMovementCheck(player, x, y, z);

            }
        });

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Plugin enabled message
        getLogger().info("AntiCheat plugin enabled!");
    }

    private void loadConfig() {
        maxDamagePerHit = getConfig().getDouble("maxDamagePerHit", 10.0);
        attackSpeedThreshold = getConfig().getInt("killaura.attackSpeedThreshold", 200);
        maxHitsInTimeFrame = getConfig().getInt("maxHitsInTimeFrame", 3);
        hitsTimeFrame = getConfig().getInt("hitsTimeFrame", 1000);
        maxAttackAngle = getConfig().getDouble("killaura.maxAttackAngle", 60.0);
        multiHitThreshold = getConfig().getInt("killaura.multiHitThreshold", 100);
        maxReach = getConfig().getDouble("killaura.maxReach", 4.0);
        maxYawDifference = getConfig().getDouble("killaura.maxYawDifference", 180.0);
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
        Location oldLocation = event.getFrom();
        Location newLocation = event.getTo();

        if (newLocation == null || oldLocation == null) {
            return;
        }

        // Get values from the config file
        double maxAllowedDistance = config.getDouble("movement.maxAllowedDistance", 5.0);
        long minTimeBetweenMoves = config.getLong("movement.minTimeBetweenMoves", 50);

        // Perform movement checks
        double distance = oldLocation.distance(newLocation);
        long lastMoveTime = playerLastMoveTime.getOrDefault(player, 0L);
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMove = currentTime - lastMoveTime;

        // Check if the player is moving too fast
        if (distance > maxAllowedDistance) {
            incrementViolation(player, "Suspicious Movement: Exceeding Max Distance");
        }

        // Check if the player is moving too frequently
        if (timeSinceLastMove < minTimeBetweenMoves) {
            incrementViolation(player, "Suspicious Movement: Moving Too Fast Between Moves");
        }

        // Update last move time and player location
        lastPlayerLocation.put(player, newLocation);
        playerLastMoveTime.put(player, currentTime);
    }

    // Anti-Killaura Detection
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            double damage = event.getDamage();

            // loasd config
            double maxDamagePerHit = config.getDouble("Killaura.maxDamagePerHit", 10.0);
            double maxAttackAngle = config.getDouble("killaura.maxAttackAngle", 60.0);
            long attackSpeedThreshold = config.getLong("killaura.attackSpeedthreshold", 200);
            double maxReach = config.getDouble("killaura.maxReach", 4.0);
            double maxYawDifference = config.getDouble("killaura.maxYawDifference", 180.0);
            long multiHitThreshold = config.getInt("killaura.multiHitThreshold", 100);
            // Check for impossible hit rates considering sharpness
            if (damage > maxDamagePerHit) {
                incrementViolation(attacker, "Killaura Detected (Excessive Damage)");
            }

            // Check for suspicious attack speeds
            long lastAttackTime = clickCheck.getOrDefault(attacker, 0L);
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastAttackTime) < attackSpeedThreshold) {
                incrementViolation(attacker, "Killaura Detected (Suspicious Attack Speed)");
            }
            clickCheck.put(attacker, currentTime);

            // Count hits to check for spamming
            hitCount.putIfAbsent(attacker, 0);
            int hits = hitCount.get(attacker);
            if ((currentTime - lastAttackTime) <= hitsTimeFrame) {
                hits++;
            } else {
                hits = 1; // Reset the hit count
            }
            hitCount.put(attacker, hits);

            // Check if hits exceed the maximum allowed in the timeframe
            if (hits > maxHitsInTimeFrame) {
                incrementViolation(attacker, "Killaura Detected (Excessive Hits in Timeframe)");
            }

            // Check for impossible angles of attack
            double angle = attacker.getLocation().getDirection().angle(target.getLocation().getDirection());
            if (angle > maxAttackAngle) {
                incrementViolation(attacker, "Killaura Detected (Impossible Attack Angle)");
            }

            // Check for multi-entity hits (attacking multiple entities at the same time)
            if ((currentTime - lastAttackTime) < multiHitThreshold) {
                incrementViolation(attacker, "Killaura Detected (Multi-Entity Hits)");
            }

            // Check for reach
            double reachDistance = attacker.getLocation().distance(target.getLocation());
            if (reachDistance > maxReach) {
                incrementViolation(attacker, "Killaura Detected (Excessive Reach)");
            }

            // Abnormal head movement (tracking instantly)
            float yawDifference = Math.abs(attacker.getLocation().getYaw() - target.getLocation().getYaw());
            if (yawDifference > maxYawDifference) {
                incrementViolation(attacker, "Killaura Detected (Abnormal Head Movement)");
            }

            // Monitor packet data for additional checks
            WrappedDataWatcher watcher = new WrappedDataWatcher(attacker);
            // Example: Check if the player is not moving or moving in an unrealistic way
            // Add your logic here as needed
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