# AntiCheat Plugin for Minecraft

## Overview
The AntiCheat plugin for Minecraft is designed to enhance server security by detecting and preventing common hacks, cheats, and exploits. This plugin is built for Paper/Spigot and offers a variety of detection mechanisms, including anti-killaura, anti-fly, anti-speed, anti-autoclicker, and more. It helps maintain a fair gaming environment by notifying admins of suspicious activity and taking action against offenders.
## Bugs and Issues
I know there is some bugs and issues. im trying to fix those issues as soon as possible. you can check the source codee in my [GitHub](https://Github.com/synkfr/ANtiCheat) page.

## Features
- **Anti-Fly Detection**: Prevents players from using fly hacks.
- **Anti-Killaura Detection**: Detects players dealing excessive damage.
- **Anti-Speed Detection**: Identifies players moving faster than allowed.
- **Anti-AutoClicker Detection**: Monitors for excessively rapid clicking.
- **Anti-NoFall Detection**: Detects players who do not take fall damage.
- **Anti-XRay Detection**: Monitors abnormal mining patterns.
- **Anti-AutoTotem Detection**: Monitors abnormal mining patterns.
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

# ============================
#         AUTO TOTEM
# ============================
# AntiCheat AutoTotem Detection Configuration

# Time in milliseconds between totem uses to be considered suspicious.
# If a player uses two totems in a time lower than this, they will be flagged.
totem_usage_threshold: 500

# Violation Settings
# The number of violations required before taking action.
max_violations: 3

# Action to take when a player exceeds max violations:
# Options: NONE, KICK, BAN, NOTIFY_ADMIN
action_on_violation: "NOTIFY_ADMIN"

# Violation Message
# This message will be sent to the player if they are flagged for AutoTotem usage.
# Use {player} to insert the player's name dynamically.
violation_message: "&e[AntiCheat] &cSuspicious behavior detected: AutoTotem usage! Please refrain from using unfair advantages."

# Admin Notification
# If 'NOTIFY_ADMIN' is set as the action, this message will be sent to all admins.
# Use {player} to insert the player's name dynamically.
admin_notification: "&6[AntiCheat] &e{player} &cmight be using AutoTotem! Flagged for suspicious totem usage."

# Log settings
# Enable logging of suspicious totem uses to a file (logs/anticheat-autototem.log)
log_to_file: true

# Broadcast to all players if a player is flagged (optional)
broadcast_to_all_players: false

# Broadcast message format (only if 'broadcast_to_all_players' is true)
# Use {player} to insert the player's name dynamically.
broadcast_message: "&e{player} &chas been flagged for possible AutoTotem usage!"

# ============================
#        Anti-Killaura
# ============================
# Anti Killaura hack config

# Maximum damage a player can inflict with a single hit
maxDamagePerHit: 10.0

killaura:

  # Threshold for suspicious attack speed (in milliseconds)
  attackSpeedThreshold: 200

  # Allowable hits in a timeframe (milliseconds)
  maxHitsInTimeFrame: 3

  # Timeframe in milliseconds
  hitsTimeFrame: 1000

  # Maximum angle allowed for an attack (in degrees)
  maxAttackAngle: 60.0

  # Threshold for multi-entity hits (in milliseconds)
  multiHitThreshold: 100

  # Maximum reach distance for player attacks (in blocks)
  maxReach: 4.0

  # Maximum allowed difference in yaw between attacker and target
  maxYawDifference: 180.0


# ============================
#        Auto-Clicker
# ============================
#Anti auto-clicker hack config

#time old for click detection (in milliseconds)
clickTimeThreshold: 150

#maximum clicks allowed per second
maxClicksPerSecond: 15


# ============================
#            X-Ray
# ============================
# XRay Detection config

xray:

  # Time window (in milliseconds) to analyze mining patterns (default: 60 seconds)
  timeWindow: 180000

  # Minimum distance (in blocks) between mined valuable ores to trigger suspicion
  minDistanceBetweenOres: 3.5

  # Maximum number of ores mined in close proximity before flagging as suspicious
  maxOresInProximity: 5

  # List of ores considered valuable for XRay detection
  valuableOres:
    - DIAMOND_ORE
    - EMERALD_ORE
    - GOLD_ORE
    - ANCIENT_DEBRIS
    #  add more if necessary


# ============================
#           No-Fall
# ============================
# NoFall AntiCheat Configuration

# The maximum fall distance a player can fall before triggering NoFall detection.
maxFallDistance: 4.0

# The time after a fall that NoFall detection resets, in milliseconds.
noFallResetTime: 5000

# The expected fall damage when no hacks are being used.
expectedFallDamage: 2.0


# ============================
#           No-Fall
# ============================
# Anti fly hack config

# Threshold for fly detection (in milliseconds)
flyDetectionThreshold: 5000

# fly is still on progress


# ============================
#          Violations
# ============================
# Violation thresholds for actions
violationThresholds:
  # Number of violations before kicking the player
  kick: 3

  # Number of violations before banning the player
  ban: 5

# Notification message format for admins
notifyMessage: "&c%player% has been flagged for %reason%"


# More improvement well come soon and Thanks fo Downloading This plugin
