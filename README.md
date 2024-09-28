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
maxSpeed: 0.5 # Maximum speed a player can move (in blocks per second)

# Anti fly hack config
flyDetectionThreshold: 2000 # Threshold for fly detection (in milliseconds)

#Anti auto-clicker hack config
clickTimeThreshold: 100  # Time threshold for click detection (in milliseconds)
maxClicksPerSecond: 10 # Maximum clicks allowed per second

# Anti NoFall hack config
# Maximum fall distance before triggering no-fall detection
maxFallDistance: 4.0  # Height above which fall damage should occur
# Minimum damage expected from falls to avoid false positives
expectedFallDamage: 2.0  # Adjust this based on testing fall damage
# Time (in milliseconds) to reset no-fall detection (for repeated checks)
noFallResetTime: 5000  # 5 seconds

# XRay Detection config
xray:
  timeWindow: 60000  # Time window (in milliseconds) to analyze mining patterns (default: 60 seconds)
  minDistanceBetweenOres: 5.0  # Minimum distance (in blocks) between mined valuable ores to trigger suspicion
  maxOresInProximity: 3  # Maximum number of ores mined in close proximity before flagging as suspicious
  valuableOres:  # List of ores considered valuable for XRay detection
    - DIAMOND_ORE
    - EMERALD_ORE
    - GOLD_ORE
    - ANCIENT_DEBRIS
   # you can add more

# Anti Killaura hack config
killaura:
  attackSpeedThreshold: 200     # Minimum time between attacks (milliseconds)
  maxAttackAngle: 60.0          # Maximum allowed attack angle
  multiHitThreshold: 100        # Time threshold for multiple entity hits (milliseconds)
  maxReach: 4.0                 # Maximum allowed reach distance (blocks)
  maxYawDifference: 180.0       # Maximum allowed yaw difference for head movement

# Violation thresholds for actions
violationThresholds:
  kick: 3  # Number of violations before kicking the player
  ban: 5   # Number of violations before banning the player

# Notification message format for admins
notifyMessage: "&c%player% has been flagged for %reason%"
