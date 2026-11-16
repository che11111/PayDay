<p align="center">
  <img src="https://raw.githubusercontent.com/che11111/PayDay/main/img/PayDay-1280x.png" alt="PayDay Logo" width="128">
</p>

<h1 align="center">PayDay</h1>

<p align="center">Let players pay to change time and weather in the current world.</p>

<p align="center">
  <a href="#branches"><img src="https://img.shields.io/badge/Minecraft-b1.7.3_%7C_1.6.4_%7C_1.8--26.3-3fb950" alt="Branches support Minecraft Beta 1.7.3, 1.6.4, and 1.8 through 26.3"></a>
  <a href="#requirements"><img src="https://img.shields.io/badge/Java-8%2B_bytecode-orange" alt="Java 8 bytecode"></a>
  <a href="#branches"><img src="https://img.shields.io/badge/Bukkit%20%2F%20Spigot%20%2F%20Paper-supported-blue" alt="Bukkit, Spigot, and Paper supported"></a>
  <a href="#branches"><img src="https://img.shields.io/badge/Folia-supported-blueviolet" alt="Folia supported"></a>
  <a href="#sqlite"><img src="https://img.shields.io/badge/SQLite-server_provided-003B57" alt="SQLite provided by the server"></a>
  <a href="#language"><img src="https://img.shields.io/badge/i18n-9_languages-8250df" alt="Nine languages"></a>
</p>

<p align="center"><b>English</b> · <a href="README-zh.md">简体中文</a></p>

PayDay is a Bukkit plugin for paid time and weather changes. Three branches share the same configuration layout, purchase permissions, SQLite history, public API, and purchase flow.

## Branches

| Branch | Server | Economy |
| --- | --- | --- |
| **PayDay-modern** | Bukkit/Spigot 1.8, Paper 26.3, Folia | Vault |
| **PayDay-1.6.4** | CraftBukkit/Spigot 1.6.4 | Vault |
| **PayDay-b1.7.3** | Poseidon Beta 1.7.3 | iConomy |

Modern also provides Paper Dialog, Floodgate forms, Folia scheduling, and PlaceholderAPI support.

## Features

- Buy day, noon, night, dawn, dusk, clear weather, rain, and thunderstorms.
- Only the purchasing player's current world is changed.
- Consistent flow: permission check, balance check, withdrawal, world change, messages, broadcast, and history.
- SQLite storage for purchase history and counters.
- Nine built-in languages with server-wide automatic language selection.
- Anonymous bStats metrics using service ID `35365`.

## Requirements

- Java 8 or the newer runtime required by the server.
- Modern/1.6.4: Vault and a working Vault economy provider.
- Beta 1.7.3: iConomy.
- SQLite JDBC must be provided by the server; it is not bundled.
- Modern: optional PlaceholderAPI and Floodgate.
- Paper Dialog requires Paper 1.21.7 or newer.

## Installation

1. Install the economy plugin required by the selected branch.
2. Place the branch JAR in `plugins/`.
3. Restart the server.
4. Edit `plugins/PayDay/config.yml` and the files under `plugins/PayDay/lang/`.
5. Run `/paydayadmin reload` or restart the server.

## Commands and Permissions

| Command | Permission | Default | Description |
| --- | --- | --- | --- |
| — | `payday.use` | All players | Compatibility parent for all purchase commands |
| `/payday` | `payday.day` | All players | Set time to daytime |
| `/paynoon` | `payday.noon` | All players | Set time to noon |
| `/paynight` | `payday.night` | All players | Set time to night |
| `/paydawn` | `payday.dawn` | All players | Set time to dawn |
| `/paydusk` | `payday.dusk` | All players | Set time to dusk |
| `/payweather` | `payday.weather` | All players | Set clear weather |
| `/payrain` | `payday.weather` | All players | Set rain |
| `/paystorm` | `payday.weather` | All players | Set a thunderstorm |
| `/paydayadmin reload` | `payday.reload` | OP | Fully reload config, language, and SQLite |
| `/paydaydialog`, `/pdialog` | `payday.dialog` | All players | Modern Paper Dialog page |
| `/pform`, `/paydayform` | `payday.form` | All players | Modern Floodgate Bedrock form |

Purchases triggered inside Dialog or a Bedrock form still check the corresponding purchase permission.

## Configuration

All public prices default to `30`:

```yaml
costs:
  time:
    day: 30
    noon: 30
    night: 30
    dawn: 30
    dusk: 30
  weather:
    clear: 30
    rain: 30
    storm: 30

times:
  day: 1000
  noon: 6000
  night: 18000
  dawn: 0
  dusk: 12000

weather_duration: 12000
```

- `costs.time.*`: time purchase prices.
- `costs.weather.*`: weather purchase prices.
- `times.*`: target game times in ticks.
- `weather_duration`: rain and thunder duration in ticks.

## Language

Built-in languages: English, Simplified Chinese, Japanese, Korean, Traditional Chinese, German, French, Spanish, and Russian.

Modern and 1.6.4 use one global server language:

```yaml
language: auto
# language: zh_cn.yml
```

Beta 1.7.3 separates console and in-game languages:

```yaml
language:
  console: auto
  game: game.yml
```

`game.yml` remains English because Beta 1.7.3 clients only render English. Unsupported locales and missing keys fall back to English.

## SQLite

History is stored in `plugins/PayDay/history.db`.

- The server must provide SQLite JDBC.
- Legacy `history.yml` files are not read or migrated.
- SQLite JDBC is marked `provided` in every POM and is not bundled.

## bStats

- [PayDay on bStats](https://bstats.org/plugin/bukkit/PayDay/35365)
- Modern and 1.6.4 use the official `bstats-bukkit` adapter.
- Beta 1.7.3 uses a Poseidon-compatible `MetricsBase` adapter.

## Public API

All branches expose:

```text
PayDayPlugin.getInstance()
getPayDayCount(player)
getPayNoonCount(player)
getPayNightCount(player)
getPayWeatherCount(player)
getCount(player, command)
getHistory(player)
```

## Build

```bash
cd PayDay-b1.7.3
mvn clean package

cd PayDay-1.6.4
mvn clean package

cd PayDay-modern
mvn clean package
```

JARs are written to each branch's `target/` directory. `PayDay-b1.7.3/lib/iConomy.jar` is required for compilation.

## Notes

- Economy systems and Modern's UI/platform integrations are intentional differences.
- Test economy withdrawals, multi-world behavior, permissions, reload, SQLite, and bStats before production use.

Author: `che1s`  
Main class: `dev.che1s.payday.PayDayPlugin`

## Sponsorship

If this project helps you, you can support development through Ko-fi or Afdian.

<p align="center">
  <a href="https://ko-fi.com/J5C529ZWWL" target="_blank"><img height="36" src="https://storage.ko-fi.com/cdn/kofi6.png?v=6" alt="Support on Ko-fi"></a>
  <a href="https://afdian.com/a/sss000" target="_blank"><img height="36" src="https://img.shields.io/badge/Afdian-Support-946CE6?style=for-the-badge&logo=afdian&logoColor=white" alt="Support on Afdian"></a>
</p>
