<p align="center">
  <img src="img/PayDay-1280x.png" alt="PayDay Logo" width="128">
</p>

<h1 align="center">PayDay</h1>

<p align="center">玩家付费修改当前世界时间与天气的 Minecraft Bukkit 插件。</p>

<p align="center">
  <a href="#分支"><img src="https://img.shields.io/badge/Minecraft-b1.7.3_%7C_1.6.4_%7C_1.8--26.3-3fb950" alt="分支支持 Minecraft Beta 1.7.3、1.6.4 与 1.8 至 26.3"></a>
  <a href="#环境要求"><img src="https://img.shields.io/badge/Java-8%2B_bytecode-orange" alt="Java 8 字节码"></a>
  <a href="#分支"><img src="https://img.shields.io/badge/Bukkit%20%2F%20Spigot%20%2F%20Paper-supported-blue" alt="支持 Bukkit、Spigot 和 Paper"></a>
  <a href="#分支"><img src="https://img.shields.io/badge/Folia-supported-blueviolet" alt="支持 Folia"></a>
  <a href="#sqlite"><img src="https://img.shields.io/badge/SQLite-server_provided-003B57" alt="SQLite 驱动由服务端提供"></a>
  <a href="#语言"><img src="https://img.shields.io/badge/i18n-9_languages-8250df" alt="支持九种语言"></a>
</p>

<p align="center"><a href="README.md">English</a> · <b>简体中文</b></p>

## 分支

| 分支 | 服务端 | 经济 |
| --- | --- | --- |
| [`PayDay-modern`](PayDay-modern) | Bukkit/Spigot 1.8、Paper 26.3、Folia | Vault |
| [`PayDay-1.6.4`](PayDay-1.6.4) | CraftBukkit/Spigot 1.6.4 | Vault |
| [`PayDay-b1.7.3`](PayDay-b1.7.3) | Poseidon Beta 1.7.3 | iConomy |

三个分支共享相同的配置结构、购买权限、SQLite 历史、公共 API 和购买事务。Modern 额外提供 Dialog、Floodgate 表单、Folia 和 PlaceholderAPI 支持。

## 功能

- 购买白天、正午、夜晚、黎明、落日、晴天、雨天和雷暴。
- 只影响玩家当前所在世界。
- 权限检查、余额预检、扣款、世界修改、提示、广播和历史记录顺序一致。
- SQLite 保存购买历史与次数。
- 支持九种语言，自动读取服务器语言环境。

## 环境要求

- Java 8 或服务端要求的更高版本。
- Modern/1.6.4 使用 Vault 及可用经济插件。
- Beta 1.7.3 使用 iConomy。
- 服务端需提供 SQLite JDBC 驱动，插件不会打包该驱动。
- Modern 可选 PlaceholderAPI、Floodgate。
- 原生 Dialog 需要 Paper 1.21.7+。

## 安装

1. 安装对应经济插件。
2. 将所选分支的 JAR 放入 `plugins/`。
3. 重启服务器。
4. 编辑 `plugins/PayDay/config.yml` 和 `plugins/PayDay/lang/`。
5. 执行 `/paydayadmin reload` 或重启服务器。

## 命令与权限

| 命令 | 权限 | 默认 | 作用 |
| --- | --- | --- | --- |
| — | `payday.use` | 所有玩家 | 兼容总权限，通过子权限授予全部购买命令 |
| `/payday` | `payday.day` | 所有玩家 | 设置为白天 |
| `/paynoon` | `payday.noon` | 所有玩家 | 设置为正午 |
| `/paynight` | `payday.night` | 所有玩家 | 设置为夜晚 |
| `/paydawn` | `payday.dawn` | 所有玩家 | 设置为黎明 |
| `/paydusk` | `payday.dusk` | 所有玩家 | 设置为落日 |
| `/payweather` | `payday.weather` | 所有玩家 | 设置为晴天 |
| `/payrain` | `payday.weather` | 所有玩家 | 设置为雨天 |
| `/paystorm` | `payday.weather` | 所有玩家 | 设置为雷暴 |
| `/paydayadmin reload` | `payday.reload` | OP | 完整重载配置、语言和 SQLite |
| `/paydaydialog`、`/pdialog` | `payday.dialog` | 所有玩家 | Modern 的 Paper 原生购买界面 |
| `/pform`、`/paydayform` | `payday.form` | 所有玩家 | Modern 的 Floodgate 购买表单 |

界面内每次购买仍会检查对应购买权限。

## 配置

默认价格均为 `30`：

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

- `costs.time.*`：时间购买价格。
- `costs.weather.*`：天气购买价格。
- `times.*`：目标时间，单位 ticks。
- `weather_duration`：雨天/雷暴持续时间，单位 ticks。

## 语言

支持英语、简体中文、日语、韩语、繁体中文、德语、法语、西班牙语和俄语。

Modern 与 1.6.4 使用服务器全局语言：

```yaml
# auto 根据服务器 JVM 语言和地区选择，无法识别时使用英文
language: auto

# 也可指定完整文件名
# language: zh_cn.yml
```

Beta 1.7.3 的控制台和游戏内语言分开：

```yaml
language:
  # auto 或 console-zh_cn.yml 等完整文件名
  console: auto
  # 固定为 game.yml，保持英文
  game: game.yml
```

缺少翻译键时回退到英文。修改后需要重载或重启服务器。

## SQLite

历史文件：`plugins/PayDay/history.db`。

- 服务端必须提供 SQLite JDBC。
- 插件只使用 `history.db`，不会读取或迁移旧 `history.yml`。
- SQLite 驱动在所有 POM 中均为 `provided`。

## 公共 API

三个分支提供：

```text
PayDayPlugin.getInstance()
getPayDayCount(player)
getPayNoonCount(player)
getPayNightCount(player)
getPayWeatherCount(player)
getCount(player, command)
getHistory(player)
```

## 构建

```bash
cd PayDay-b1.7.3
mvn clean package

cd PayDay-1.6.4
mvn clean package

cd PayDay-modern
mvn clean package
```

产物位于各分支的 `target/`。Beta 的 `lib/iConomy.jar` 是编译依赖，不要删除。


## 赞助与支持

如果本项目对你有帮助，可以通过 Ko-fi 或 爱发电 赞助开发。赞助后可提供插件安装、配置和故障排查问答服务。

<p align="center">
  <a href='https://ko-fi.com/J5C529ZWWL' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://storage.ko-fi.com/cdn/kofi6.png?v=6' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>
  <a href='https://afdian.com/a/sss000' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://img.shields.io/badge/Afdian-Support-946CE6?style=for-the-badge&logo=afdian&logoColor=white' border='0' alt='在爱发电赞助' /></a>
</p>
