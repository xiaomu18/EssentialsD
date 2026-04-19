# Deer's Essentials Reloaded

适用于 `Minecraft 1.21` API 的基础功能插件，面向 `Folia` 设计，并兼容常见高版本服务端环境。EssentialsD 提供了传送、家、传送点、隐身、禁言、背包检查、聊天增强、创造物品限制等一组偏实用与管理向的能力。

## 功能概览

### 主要命令

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/home [name]` | 传送到家 | `essd.home` |
| `/homes [page]` | 查看家列表 | `essd.home` |
| `/sethome [name]` | 设置家 | `essd.home` |
| `/delhome <name>` | 删除家 | `essd.home` |
| `/home-editor <player> view/tp/remove (home)` | 查看和编辑他人的家 | `essd.home.use-editor` |
| `/warp <name>` | 传送到传送点 | `essd.warp` |
| `/warps [page]` | 查看传送点列表 | `essd.warp` |
| `/setwarp <name>` | 设置传送点 | `essd.setwarp` |
| `/delwarp <name>` | 删除传送点 | `essd.delwarp` |
| `/tpa <player>` | 发起传送请求 | `essd.tp.tpa` |
| `/tpahere <player>` | 请求对方传送到你这里 | `essd.tp.tpahere` |
| `/tpacancel` | 取消自己发起的传送请求 | `essd.tp.tpacancel` |
| `/rtp` | 随机传送 | `essd.tp.rtp` |
| `/back` | 回到上一次传送前的位置 | `essd.tp.back` |
| `/fly [player]` | 切换飞行状态 | `essd.fly` |
| `/flyspeed <reset\|speed> [player]` | 设置飞行速度 | `essd.flyspeed` |
| `/god [player]` | 切换无敌模式 | `essd.god` |
| `/heal [player]` | 治疗自己或目标玩家 | `essd.heal` |
| `/gamemode <mode> [player]` | 设置游戏模式 | `essd.gamemode` |
| `/vanish <list\|on\|off> [player]` | 切换隐身状态 | `essd.vanish` |
| `/inspect <player> [--ender]` | 查看玩家背包或末影箱 | `essd.inspect` |
| `/mute ...` | 禁言玩家或 IP | `essd.mute` |
| `/unmute ...` | 解除禁言 | `essd.unmute` |
| `/kickall <reason>` | 踢出全部玩家 | `essd.kickall` |
| `/enderchest` | 打开自己的末影箱 | `essd.enderchest` |
| `/showitem` | 展示手中物品 | `essd.showitem` |
| `/hat` | 把手中物品戴到头上 | `essd.hat` |
| `/skull` | 使用任意头颅交换自己的头颅 | `essd.skull` |
| `/more [amount]` | 填满手中物品堆叠 | `essd.more` |
| `/sit` | 原地坐下 | `essd.sit` |
| `/suicide` | 自杀 | `essd.suicide` |
| `/save` | 保存服务器 | `essd.save` |
| `/essd <reload\|version>` | 重载配置、查看版本信息 | `essd.control` |

### 非命令功能

- 可配置传送延迟、冷却、随机传送半径与传送黑名单世界
- 可限制 home 数量与禁止在特定世界设置 home
- 支持隐身增强模式，配合 `ProtocolLib` 在数据包层面隐藏玩家
- 支持拦截向隐身玩家发送的常见私聊命令
- 支持隐身时关闭碰撞、禁止攻击玩家、静默打开容器
- 支持聊天格式自定义、敏感词拦截、敏感词替换、聊天冷却
- 支持禁言系统，含临时禁言、永久禁言、IP 禁言、额外禁用命令
- 支持命令冷却与全局禁用指定一级命令
- 支持创造模式拿取特定物品或带 NBT 物品的限制
- 支持附魔瓶经验倍率、经验球合并、经验吸收无冷却
- 支持椅子、撬棍、隐形展示框、光源方块、附魔书堆叠等扩展玩法

## 安装与依赖

### 运行环境

- Java `21`
- Minecraft 服务端 API `1.21`
- `Folia` 优先，插件已声明 `folia-supported: true`

### 可选依赖

- `PlaceholderAPI`
  用于聊天格式中的占位符解析
- `AuthMe`
  启用后，未登录玩家无法发送聊天消息
- `ProtocolLib`
  用于 `vanish.enhanced-mode` 数据包级隐身
- `LuckPerms`
  非硬依赖，推荐用于权限管理

## 权限说明

### 权限总览

插件提供聚合权限 `essd.*`，默认 `op` 拥有。下面列出主要节点及用途。

### 基础命令权限

| 权限 | 默认值 | 说明 |
| --- | --- | --- |
| `essd.suicide` | `true` | 使用 `/suicide` |
| `essd.hat` | `true` | 使用 `/hat` |
| `essd.showitem` | `true` | 使用 `/showitem` |
| `essd.skull` | `true` | 使用 `/skull` |
| `essd.sit` | `true` | 使用 `/sit` |
| `essd.enderchest` | `true` | 使用 `/enderchest` |
| `essd.home` | `true` | 使用 `/home`、`/homes`、`/sethome`、`/delhome` |
| `essd.warp` | `true` | 使用 `/warp`、`/warps` |
| `essd.tp.tpa` | `true` | 使用 `/tpa` |
| `essd.tp.tpahere` | `true` | 使用 `/tpahere` |
| `essd.tp.tpacancel` | `true` | 使用 `/tpacancel` |
| `essd.tp.rtp` | `true` | 使用 `/rtp` |
| `essd.tp.back` | `true` | 使用 `/back` |

### 管理类权限

| 权限 | 默认值 | 说明 |
| --- | --- | --- |
| `essd.god` | `op` | 使用 `/god` |
| `essd.fly` | `op` | 使用 `/fly` |
| `essd.flyspeed` | `op` | 使用 `/flyspeed` |
| `essd.save` | `op` | 使用 `/save` |
| `essd.more` | `op` | 使用 `/more` |
| `essd.inspect` | `op` | 使用 `/inspect` 查看在线玩家背包 |
| `essd.inspect.write` | `op` | 允许在检查界面中修改物品 |
| `essd.inspect.offline` | `op` | 允许检查离线玩家数据 |
| `essd.home.use-editor` | `op` | 使用 `/home-editor` |
| `essd.setwarp` | `op` | 使用 `/setwarp` |
| `essd.delwarp` | `op` | 使用 `/delwarp` |
| `essd.mute` | `op` | 使用 `/mute` |
| `essd.unmute` | `op` | 使用 `/unmute` |
| `essd.kickall` | `op` | 使用 `/kickall` |
| `essd.vanish` | `op` | 使用 `/vanish` |
| `essd.control` | `op` | 使用 `/essd reload`、`/essd version` |
| `essd.heal` | `op` | 使用 `/heal` |
| `essd.gamemode` | `op` | 使用 `/gamemode` |

### 扩展权限

| 权限 | 默认值 | 说明 |
| --- | --- | --- |
| `essd.fly.other` | `false` | 允许对其他玩家使用 `/fly` |
| `essd.flyspeed.other` | `false` | 允许对其他玩家使用 `/flyspeed` |
| `essd.vanish.other` | `false` | 允许切换其他玩家的隐身 |
| `essd.vanish.see` | `op` | 允许看到隐身玩家 |
| `essd.vanish.chat` | `false` | 允许隐身时在公屏发言 |
| `essd.vanish.tpahere` | `false` | 允许隐身时使用 `/tpahere` |
| `essd.gamemode.other` | `false` | 允许修改其他玩家的游戏模式 |
| `essd.gamemode.survival` | `false` | 允许切换到生存模式 |
| `essd.gamemode.creative` | `false` | 允许切换到创造模式 |
| `essd.gamemode.adventure` | `false` | 允许切换到冒险模式 |
| `essd.gamemode.spectator` | `false` | 允许切换到旁观模式 |
| `essd.bypass.CommandCD` | 未在 `plugin.yml` 中声明 | 绕过命令冷却 |
| `essd.chat.allow-use-minimessage` | 未在 `plugin.yml` 中声明 | 允许聊天消息直接解析 MiniMessage |


### vanish

`vanish` 相关配置支持以下能力：

- `force-in-different-gamemode`
  当玩家游戏模式不同于服务器默认游戏模式时自动隐身
- `disable-collidable`
  隐身时关闭碰撞
- `allow-vanisher-attack-player`
  控制隐身玩家是否可以攻击其他玩家
- `cancel-container-animation`
  隐身玩家打开带开合动画的容器时改为静默打开
- `block-private-message-to-vanished`
  拦截向隐身玩家发送 `/msg`、`/tell`、`/w` 等常见私聊
- `enhanced-mode`
  配合 `ProtocolLib` 启用数据包级隐身

### chat

聊天系统支持：

- 基于 `PlaceholderAPI` 的聊天格式
- MiniMessage 与原版 `§` 颜色代码混用
- 敏感词拦截与替换
- 聊天冷却
- 掩耳盗铃模式
- 与禁言系统联动

### command-manager

命令管理支持：

- 命令冷却
- 全局禁用指定一级命令
- `essd.bypass.CommandCD` 绕过权限

## 构建

项目使用 Maven 构建，要求使用 JDK 21+ 进行编译：

```bash
mvn clean package
```

构建产物位于 `target/` 目录。可在 Github Actions 页面直接下载预构建产物。

## 开发者

- `LunaDeer` 旧版本作者
- `xiaomu18` 重置版作者

### 开发不易，感谢支持。求个 Star 🙏🙏🙏
你的支持，就是我们开发的动力。
