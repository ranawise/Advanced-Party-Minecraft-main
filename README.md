# AdvancedParty

AdvancedParty is a modern, modular party plugin for Paper servers, focused on clean gameplay flow and easy server-side configuration.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Paper](https://img.shields.io/badge/Paper-1.21-blue?style=for-the-badge)
![Author](https://img.shields.io/badge/Author-ranawise-6f42c1?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active-success?style=for-the-badge)

## Project Info

- **Author:** `ranawise`
- **Main class:** `com.ranawise.AdvancedParty`
- **Minecraft API:** Paper `1.21`
- **Java:** `21`

## Highlights

- Full party lifecycle: create, invite, join, leave, kick, transfer, disband
- Party chat with `/partychat` and `/pc`
- GUI tools for party member and settings management
- Config-driven behavior from `config.yml`
- Extendable API events for integrations and custom logic

## Quick Start

```bash
mvn clean package
```

Output JAR:

```text
target/AdvancedParty-1.0.0.jar
```

Install steps:

1. Run the build command.
2. Move the generated JAR into your server `plugins` folder.
3. Start/restart the server.
4. Edit `plugins/AdvancedParty/config.yml` to match your server rules.

## Commands

- `/party` - Open party actions and help
- `/party create` - Create a new party
- `/party invite <player>` - Invite a player
- `/party join <player>` - Join a party
- `/party leave` - Leave your party
- `/party disband` - Disband your party (leader)
- `/party kick <player>` - Remove a member (leader)
- `/party transfer <player>` - Transfer leadership
- `/pc <message>` - Send party chat message

## Permissions

- `party.use` - Use party system (default: `true`)
- `party.create` - Create parties (default: `true`)
- `party.invite` - Invite players (default: `true`)
- `party.admin` - Admin controls/bypass (default: `op`)

## Configuration

Primary config path:

```text
src/main/resources/config.yml
```

Tune party limits, chat behavior, GUI settings, and gameplay options from this file.

```
