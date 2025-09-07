# Deer's Essentials Redesigned
### 适用于 Folia 服务端的 Minecraft 基础插件

本插件为 MC 服务器提供原版没有的实用基础命令和扩展功能，适配新一代多线程服务端 [Folia](https://papermc.io/downloads/folia)
与 Essentials/EssentialsX 等老牌基础插件功能类似

此插件是 [EssentialsD](https://ssl.lunadeer.cn:14446/zhangyuheng/EssentialsD) 的重置版 (原作者似乎已不再更新且储存库已经无法访问)

## 🍂 功能概述

EssentialsD 拥有以下命令:

* /home -> 传送到家
* /sethome -> 设置家
* /delhome -> 删除家
* /homes -> 查看家列表
* /suicide -> 紫砂
* /hat -> 将你手中的任意物品带到头上
* /showitem -> 展示你手中的物品
* /skull -> 使用任意头颅交换获得一个自己的头颅
* /god -> 启用上帝模式 (免疫一切伤害 + 不吸引怪物仇恨)
* /fly -> 开启飞行模式 (即使处于生存模式)
* /more -> 获取更多手持物品
* /enderchest -> 打开自己的末影箱
* /tpa -> 向其他玩家发起传送请求
* /tpahere -> 让其他玩家传送到你的位置
* /rtp -> 在地图中随机传送
* /back -> 回到上一次传送的地方
* /inspect -> 查看或修改玩家背包
* /inspect-ender -> 查看或修改玩家末影箱 **(新功能)**
* /vanish on/off -> 可使管理员完全隐身, 取消加入提示, 且 TAB 和游戏内均不可见 **(新功能)**
* /warp -> 设置传送点
* /setwarp
* /delwarp -> 删除传送点
* /mute -> 禁言玩家 **(新功能)**
* /unmute -> 取消禁言玩家 **(新功能)**
* /heal -> 一键治愈自己/玩家 **(新功能)**
* /home-editor -> 查看和编辑玩家的家

以上命令对应的权限均为 `essd.<命令名>`, 部分带有 `essd.<命令名>.other` 用于设置他人权限, 如 /fly


非命令功能：

* 修改附魔瓶经验值倍率
* 自动合并经验球
* 开启经验吸收无冷却
* 强加载指定区块
* 实用的合成表扩展
* 把楼梯当作椅子使用 (点击楼梯坐上去, 类似于 sit 插件)
* 自定义聊天格式，支持使用 MiniMessage 样式化格式 (可代替简单的聊天插件)
* 可允许玩家在聊天中使用 minimessage 样式化聊天消息
* 一键屏蔽或替换敏感词 **(新功能)**
* 限制发言速度 **(新功能)**
* 禁止创造拿取特定物品或带有nbt的物品 (在创造服中很有用)
* 限制玩家使用命令的间隔
* 强制禁用指定命令

以上功能均可在 `config.yml` 中启用和配置

## 🌳 下载地址
在这里 [下载最新的 EssentialsD 构建](https://github.com/xiaomu18/EssentialsD/releases)

### 开发不易，感谢支持。求个 Star 🙏🙏🙏
你们的支持，就是我开发的动力。