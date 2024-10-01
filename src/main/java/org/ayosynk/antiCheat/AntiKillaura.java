package org.ayosynk.antiCheat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class AntiKillaura implements Listener {

    private final Map<Player, Long> lastAttackTimes = new HashMap<>();
    private final Map<Player, Location> lastLocations = new HashMap<>();
    private final FileConfiguration config;

    private final double maxMovementSpeed;
    private final double maxAttackAngle;
    private final int minAttackInterval;


    public AntiKillaura(FileConfiguration config) {
        this.config = config;
        this.maxMovementSpeed = config.getDouble("anti-killaura.max-movement-speed");
        this.maxAttackAngle = config.getDouble("anti-killaura.max-attack-angle");
        this.minAttackInterval = config.getInt("anti-killaura.minimum-attack-interval");
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            long currentTime = System.currentTimeMillis();

            // Check if the attack is suspicious based on the config values
            if (lastAttackTimes.containsKey(attacker)) {
                long lastAttackTime = lastAttackTimes.get(attacker);
                if (currentTime - lastAttackTime < AntiCheat.getInstance().getConfig().getInt("anti-killaura.minimum-attack-interval")) {
                    // Player is attacking too quickly, could be killaura
                    handleKillauraDetection(attacker);
                }
            }

            // Update last attack time
            lastAttackTimes.put(attacker, currentTime);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location newLocation = event.getTo();

        if (lastLocations.containsKey(player)) {
            Location lastLocation = lastLocations.get(player);
            updateMovement(lastLocation, newLocation, player);
        }

        // Update the player's last known location
        lastLocations.put(player, newLocation);
    }

    public void updateMovement(Location lastLocation, Location newLocation, Player player) {
        // Calculate the movement vector
        Vector movement = newLocation.toVector().subtract(lastLocation.toVector());
        double distance = movement.length();

        // Check if the player moved too quickly
        if (distance > AntiCheat.getInstance().getConfig().getDouble("anti-killaura.max-movement-speed")) {
            handleKillauraDetection(player);
            return; // No need to check further
        }

        // Calculate the angle of movement
        double angle = getAttackAngle(lastLocation, newLocation);

        // Check for sudden direction changes (example threshold can be 45 degrees)
        if (angle > AntiCheat.getInstance().getConfig().getDouble("anti-killaura.max-attack-angle")) {
            handleKillauraDetection(player);
            return; // No need to check further
        }

        // Track the player's last movement direction
        Vector lastDirection = lastLocation.getDirection();
        Vector newDirection = movement.normalize(); // Normalize to get the direction vector

        // Check for erratic movement
        if (isErraticMovement(lastDirection, newDirection)) {
            handleKillauraDetection(player);
        }

        // Update the last movement direction for the next check
        lastLocations.put(player, newLocation);
    }

    // Helper method to determine if the player's movement is erratic
    private boolean isErraticMovement(Vector lastDirection, Vector newDirection) {
        // Define a threshold for direction change (e.g., 0.5)
        double threshold = 0.5;

        // Calculate the dot product to see how aligned the vectors are
        double dotProduct = lastDirection.dot(newDirection);

        // If the dot product is below the threshold, it indicates erratic movement
        return dotProduct < threshold;
    }


    private double getAttackAngle(Location lastLocation, Location newLocation) {
        Vector lastVector = lastLocation.getDirection();
        Vector newVector = newLocation.getDirection();

        // Calculate the angle between the two direction vectors
        double angle = Math.toDegrees(Math.acos(lastVector.dot(newVector) / (lastVector.length() * newVector.length())));
        return angle;
    }

    private void handleKillauraDetection(Player player) {
        // Handle killaura detection (e.g., kick player, log detection, notify admins)
        player.kickPlayer(AntiCheat.getInstance().getConfig().getString("anti-killaura.kick-message"));
        Bukkit.getLogger().info("Killaura detected from player: " + player.getName());
        if (AntiCheat.getInstance().getConfig().getBoolean("anti-killaura.logging.notify-admins")) {
            // Notify admins or do something else based on your logic
            Bukkit.broadcastMessage(ChatUtils.colorize("&f&l[VoidAntiCheat] &cAdmin Alert: Killaura detected from player " + player.getName()));
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
