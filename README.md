# DailyStreak

Daily login rewards for Paper servers. Players open a calendar with `/daily`, claim one reward per day and build a streak. Miss a day and the streak starts over.

[Русская версия](README.ru.md)

## Features

- Calendar of up to 28 days, each with its own icon and any mix of items, money and console commands
- The day changes at the same moment for everyone: time zone and reset time are set in the config
- Optional grace days, so one missed day doesn't have to break the streak
- After the last day the calendar starts over or keeps giving the last day's reward
- Items that don't fit in the inventory are dropped at the player's feet
- SQLite out of the box, MySQL and MariaDB for networks
- PlaceholderAPI placeholders, Vault for money rewards; both optional
- English and Russian messages, all editable, MiniMessage formatting

## Installation

1. Put `DailyStreak.jar` into `plugins/`.
2. Start the server once. `config.yml`, `rewards.yml` and `lang/` appear in `plugins/DailyStreak/`.
3. Set your rewards and time zone, then run `/daily reload`.

Libraries for the database (HikariCP, SQLite, MariaDB drivers) are downloaded by Paper on the first start, nothing else to install.

## Commands and permissions

| Command | Permission | Default | What it does |
|---|---|---|---|
| `/daily` | `dailystreak.command.daily` | everyone | Open the calendar |
| `/daily reload` | `dailystreak.command.reload` | op | Reload config, rewards and messages |
| `/daily reset <player>` | `dailystreak.command.reset` | op | Reset a player's streak, online or offline |

`dailystreak.admin` gives both admin permissions.

## Rewards

`rewards.yml` holds the calendar, one entry per day:

```yaml
days:
  - icon: diamond
    rewards:
      - type: item
        material: diamond
        amount: 3
      - type: money
        amount: 500
      - type: command
        command: "lp user <player> permission settemp kits.vip true 1d"
        description: "<gold>VIP kit for a day"
```

`description` is the line players see in the menu for a command reward. Without it the command still runs but isn't listed.

## Placeholders

With PlaceholderAPI installed:

| Placeholder | Value |
|---|---|
| `%dailystreak_streak%` | Days in a row, counting today's claim |
| `%dailystreak_day%` | Calendar day that is claimed today, or will be |
| `%dailystreak_total%` | Rewards claimed in total |
| `%dailystreak_available%` | `true` if today's reward is still waiting |
| `%dailystreak_next%` | Time until the next day, e.g. `5h 12m` |

## Requirements

- Paper 26.2
- Java 25
- Vault and an economy plugin, only for money rewards
- PlaceholderAPI, only for placeholders

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`. `./gradlew runServer` starts a test server with the plugin.

## License

[MIT](LICENSE)
