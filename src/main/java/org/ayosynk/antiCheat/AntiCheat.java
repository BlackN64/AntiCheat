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
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.block.Action;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private final Map<UUID, Long> totemEquipTime = new HashMap<>();
    private final Map<Player, Long> lastClickTime = new HashMap<>();
    private final HashMap<UUID, Long> lastTotemUsage = new HashMap<>();

    private static AntiCheat instance;
    private AntiTotem antiTotem;
    private AntiKillaura antiKillaura;

    public static AntiCheat getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        this.getCommand("anticheat").setExecutor(this);
        saveDefaultConfig(); // Saves the default config if it does not exist
        config = getConfig();
        instance = this;

        antiTotem = new AntiTotem(this);  // 'this' refers to the AntiCheat plugin instance
        // Register events from AntiTotem
        Bukkit.getPluginManager().registerEvents(antiTotem, this);
        getServer().getPluginManager().registerEvents(new AntiKillaura(this), this);
        if (antiKillaura != null) {
            getServer().getPluginManager().registerEvents(antiKillaura, this);
            getLogger().info("AntiKillaura events registered.");
        } else {
            getLogger().info("Failed to initialize AntiKillaura.");
        }

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
                sender.sendMessage(ChatUtils.colorize("&f&l[VoidAntiCheat] &aconfiguration reloaded."));
                sender.sendMessage(ChatUtils.colorize("&f&l[VoidAntiCheat] &eServer Restart is Recommended."));
                return true;
            } else {
                sender.sendMessage(ChatUtils.colorize("&cUsage: /anticheat reload"));
                return true;
            }
        }
        return false;
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
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        double healthBefore = player.getHealth();
        double damage = event.getDamage();
        double healthAfter = healthBefore - damage;

        // Check if the AutoTotem detection is enabled
        if (config.getBoolean("general.enableAutoTotemDetection") && healthAfter <= 0) {
            long currentTime = System.currentTimeMillis();
            if (lastTotemUsage.containsKey(player.getUniqueId())) {
                long lastUsed = lastTotemUsage.get(player.getUniqueId());
                long rapidUsageThreshold = config.getLong("AutoTotem.rapidUsageThreshold");

                // Check for rapid totem use
                if (currentTime - lastUsed < rapidUsageThreshold) {
                    // Trigger violation
                    triggerViolation(player, config.getString("AutoTotem.violationMessage"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerUseTotem(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.TOTEM_OF_UNDYING) {
            lastTotemUsage.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    private void triggerViolation(Player player, String violationMessage) {
        // Display violation message to the player
        player.sendMessage(violationMessage);

        // Optionally log the violation
        if (config.getBoolean("general.logViolations")) {
            // Log to console
            System.out.println("Player " + player.getName() + " triggered a violation: " + violationMessage);
        }

        // Handle actions based on the config
        for (var action : config.getConfigurationSection("violationHandling.actions").getKeys(false)) {
            String actionType = config.getString("violationHandling.actions." + action + ".action");
            switch (actionType.toLowerCase()) {
                case "kick":
                    String kickMessage = config.getString("violationHandling.actions." + action + ".message");
                    player.kickPlayer(kickMessage);
                    break;
                case "ban":
                    long banDuration = config.getLong("violationHandling.actions." + action + ".duration");
                    String banReason = config.getString("violationHandling.actions." + action + ".reason");
                    // Implement the ban logic here (you may need to use an external library or method)
                    break;
                default:
                    // Handle other actions or log an unsupported action
                    System.out.println("Unsupported action: " + actionType);
                    break;
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

        // Track legitimate left-clicks by the player
        if (config.getBoolean("antiKillaura.enabled")) {
            if (event.getAction().toString().contains("LEFT_CLICK")) {
                lastClickTime.put(event.getPlayer(), System.currentTimeMillis());
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