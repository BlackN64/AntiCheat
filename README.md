# AntiCheat Plugin for Minecraft

## Overview
The AntiCheat plugin for Minecraft is designed to enhance server security by detecting and preventing common hacks, cheats, and exploits. This plugin is built for Paper/Spigot and offers a variety of detection mechanisms, including anti-killaura, anti-fly, anti-speed, anti-autoclicker, and more. It helps maintain a fair gaming environment by notifying admins of suspicious activity and taking action against offenders.

## Features
- **Anti-Fly Detection**: Prevents players from using fly hacks.
- **Anti-Killaura Detection**: Detects players dealing excessive damage.
- **Anti-Speed Detection**: Identifies players moving faster than allowed.
- **Anti-AutoClicker Detection**: Monitors for excessively rapid clicking.
- **Anti-NoFall Detection**: Detects players who do not take fall damage.
- **Anti-XRay Detection**: Monitors abnormal mining patterns.
- **Configurable Violation System**: Players accumulate violations before action is taken.
- **Admin Notifications**: Alerts admins of suspected cheating activities.
  
## Installation
1. Download the latest release of the plugin from the 
2. Place the `.jar` file into your server's `plugins` directory.
3. Restart the server to enable the plugin.
4. Configure the plugin settings in `plugins/AntiCheat/config.yml`.

## Configuration
The configuration file allows customization of thresholds and messages. Here’s a brief overview of the configurable options:

```yaml
# AntiCheat Configuration File

# Anti Speed hack config
movement:
  # The maximum allowed distance a player can move between checks (default is 7.0 blocks)
  maxAllowedDistance: 7.0
  # The minimum time (in milliseconds) allowed between player moves.
  # This prevents players from moving too quickly in rapid succession (default is 80ms).
  minTimeBetweenMoves: 80


# Anti fly hack config
flyDetectionThreshold: 5000 # Threshold for fly detection (in milliseconds)

#Anti auto-clicker hack config
clickTimeThreshold: 150 #time old for click detection (in milliseconds)
maxClicksPerSecond: 15 #maximum clicks allowed per second

# Anti NoFall hack config
# NoFall AntiCheat Configuration
maxFallDistance: 4.0 # The maximum fall distance a player can fall before triggering NoFall detection.
noFallResetTime: 5000 # The time after a fall that NoFall detection resets, in milliseconds.
expectedFallDamage: 2.0 # The expected fall damage when no hacks are being used.

# XRay Detection config
xray:
  timeWindow: 180000  # Time window (in milliseconds) to analyze mining patterns (default: 60 seconds)
  minDistanceBetweenOres: 3.5  # Minimum distance (in blocks) between mined valuable ores to trigger suspicion
  maxOresInProximity: 5  # Maximum number of ores mined in close proximity before flagging as suspicious
  valuableOres:  # List of ores considered valuable for XRay detection
    - DIAMOND_ORE
    - EMERALD_ORE
    - GOLD_ORE
    - ANCIENT_DEBRIS
   #  add more if necessary

# Anti Killaura hack config
maxDamagePerHit: 10.0 # Maximum damage a player can inflict with a single hit
killaura:
  attackSpeedThreshold: 200 # Threshold for suspicious attack speed (in milliseconds)
  maxHitsInTimeFrame: 3 # Allowable hits in a timeframe (milliseconds)
  hitsTimeFrame: 1000 # Timeframe in milliseconds
  maxAttackAngle: 60.0 # Maximum angle allowed for an attack (in degrees)
  multiHitThreshold: 100 # Threshold for multi-entity hits (in milliseconds)
  maxReach: 4.0 # Maximum reach distance for player attacks (in blocks)
  maxYawDifference: 180.0 # Maximum allowed difference in yaw between attacker and target


# Violation thresholds for actions
violationThresholds:
  kick: 3  # Number of violations before kicking the player
  ban: 5   # Number of violations before banning the player

# Notification message format for admins
notifyMessage: "&c%player% has been flagged for %reason%"
