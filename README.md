# SimpleAuth for Velocity

SimpleAuth is a lightweight and effective authentication plugin for the VelocityPowered proxy server, ensuring that your players are who they say they are.

## Features

- **Player Authentication**: Ensure that only authenticated players can join your server.
- **Secure**: Uses best practices to keep player data safe.
- **Lightweight**: No unnecessary features; just the essentials for authentication.
- **Seamless Integration**: Designed specifically for Velocity, ensuring smooth operation.
- **Premium Bypass**: Allow premium players to bypass authentication.

## Requirements

- Velocity 3.x
- Java 11 or later

## Installation

1. Ensure you have Velocity set up and running.
2. Download the latest release of SimpleAuth from the [releases page](https://github.com/iSnakeBuzz/SimpleAuth/releases).
3. Place the `.jar` file into the `plugins` folder of your Velocity setup.
4. Restart your Velocity server.
5. Edit the configuration file located at `plugins/SimpleAuth/config.yml` if necessary.

## Usage

1. Players will be prompted to `/register <password> <password>` when they first join.
2. Upon subsequent joins, players must `/login <password>`.

## Developer API: Sending Captcha

If you are generating captchas on your backend auth server and want to send them to SimpleAuth, you can do so using plugin messaging channels.

1. Register the outgoing plugin channel `simple-auth:captcha` on your backend server (e.g., Spigot/Paper).
2. Send a plugin message to the proxy using this channel with the following data:
   - `UTF-8 String`: The player's username (case-insensitive, but usually lowercase).
   - `UTF-8 String`: The generated captcha string.

Example in Bukkit/Spigot:

```java
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

// ...

ByteArrayDataOutput out = ByteStreams.newDataOutput();
out.writeUTF(player.getName().toLowerCase());
out.writeUTF(captchaString);

player.sendPluginMessage(plugin, "simple-auth:captcha", out.toByteArray());
```

## Configuration

A sample configuration can be found in `plugins/SimpleAuth/config.yml`. Here are the primary options:

```yml
settings:
  registration-timeout: 60 # Time in seconds before a non-registered player is kicked
  login-timeout: 60 # Time in seconds before a non-logged in player is kicked
  premium-bypass: true # Allow premium players to bypass authentication
  premium-command: false # Allow premium users that have signed-in to bypass authentication after executing /premium (This command is disabled if premium-bypass is enabled)
```

## Building from Source

If you wish to build SimpleAuth yourself, follow these steps:

1. Clone this repository.
2. Navigate to the project directory.
3. Run the following command:

```bash
./gradlew shadowJar
```

The built `.jar` will be available in `build/libs/`.

## Support

For bug reports, features requests, and other concerns, please open an issue on our [GitHub repository](https://github.com/iSnakeBuzz/SimpleAuth/issues).
