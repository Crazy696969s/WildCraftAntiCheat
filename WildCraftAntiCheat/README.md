# WildCraft AntiCheat 3.0.0

A custom Paper plugin for WildCraft Survival that detects suspicious valuable-ore mining and gives staff a clickable investigation dashboard.

## Requirements

- Paper 1.21.11
- Java 21
- CoreProtect optional
- LiteBans optional

## Features

- Hidden diamond, emerald, and ancient-debris scoring
- Reduced scoring for exposed ores
- Score decay to reduce old false positives
- Configurable thresholds and cooldowns
- Direct Discord webhook embeds
- In-game staff alerts
- `/wcax` clickable dashboard
- Teleport, spectate, live watch, evidence review, and reset controls
- Case states: Open, Watching, Cleared, Confirmed
- Persistent `players.yml` evidence storage
- CoreProtect and LiteBans buttons
- No automatic bans

## Building on GitHub

Upload all extracted files to the root of your GitHub repository. Open **Actions**, select **Build WildCraftAntiCheat**, and run the workflow. After it succeeds, download the `WildCraftAntiCheat-3.0.0` artifact. The artifact contains the real JAR compiled against the official Paper API.

## Local build

```bash
gradle clean build
```

Output:

```text
build/libs/WildCraftAntiCheat-3.0.0.jar
```

## Installation

1. Stop the server.
2. Remove every older `WildCraftAntiXray` and `WildCraftAntiCheat` JAR.
3. Upload `WildCraftAntiCheat-3.0.0.jar` to `plugins/`.
4. Start the server once.
5. Set the webhook in `plugins/WildCraftAntiCheat/config.yml`.
6. Run `/wcax reload`.
7. Test with `/wcax test`.
8. Open the dashboard with `/wcax`.

## Permissions

```text
wildcraftanticheat.admin
wildcraftanticheat.alerts
wildcraftanticheat.bypass
```

Do not give the bypass permission to regular staff or players.

## Important

This plugin produces investigation alerts, not proof. Staff should spectate and use CoreProtect before punishing a player.
