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
# Fly detection threshold in milliseconds
flyDetectionThreshold: 5000

# Maximum damage per hit for Killaura detection
maxDamagePerHit: 10.0

# Maximum movement speed (blocks per second)
maxSpeed: 10.0

# Maximum fall distance before NoFall detection
maxFallDistance: 10.0

# Maximum number of clicks per second for AutoClicker detection
maxClicksPerSecond: 15

# Time window to count clicks (milliseconds)
clickTimeThreshold: 200

# Violation thresholds for actions
violationThresholds:
  kick: 5
  ban: 10

# Notify message sent to admins
notifyMessage: "&c[AntiCheat] &e%player% &cis suspected of using %reason%."

# Kick message sent to the player
kickMessage: "&cYou have been kicked for using hacks!"
