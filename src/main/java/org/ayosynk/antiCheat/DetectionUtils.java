package org.ayosynk.antiCheat;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class DetectionUtils {

    public static double getAimAngle(Player attacker, Entity target) {
        Location playerLoc = attacker.getLocation();
        Location targetLoc = target.getLocation();

        double deltaX = targetLoc.getX() - playerLoc.getX();
        double deltaZ = targetLoc.getZ() - playerLoc.getZ();

        double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        double playerYaw = playerLoc.getYaw();

        double angleDifference = Math.abs(playerYaw - yaw);
        if (angleDifference > 180) {
            angleDifference = 360 - angleDifference;
        }

        return angleDifference;
    }

    public static boolean isExcessiveAttacking(int attackCount, double maxAttackFrequency) {
        return attackCount > maxAttackFrequency;
    }
}
