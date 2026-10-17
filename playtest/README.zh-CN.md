# CubeCraft TD v114：Farm 场景与全塔模型重建版

本版包含完整布景的 Farm 地图、11 类塔全部 82 个有效等级/分支模型条目和通过 CI 的 Paper 26.2 / Java 25 插件。巨型谷仓、筒仓、苹果树、鸡/羊/猪、干草、庄稼、围栏、地面和观景等待平台均为实际方块。

这是按历史实拍重建的近似版本，遮挡面、尺寸、摆放和部分升级外观经过合理补全。不是原版世界文件的逐方块提取；游戏数值、路线和塔位沿用现有实现。真人完整回合、重连恢复和性能门禁仍待实际试玩。

## 安装到一个新服务器

1. 解压到空文件夹，安装 Java 25。下载 Paper 26.2，命名 `paper.jar` 放在根目录。
2. 运行 `start.bat`（Windows）或 `bash start.sh`；阅读并接受 Minecraft EULA 后，在 `eula.txt` 设置 `eula=true`，重新启动。
3. 包内 `plugins/CubeCraftTowerDefence/config.yml` 已开启 `farm-scene.enabled: true` 和完整塔模型校验。
4. 在控制台输入：

```text
ctdplaytestsetup apply ctd-playtest 0 80 0
stop
```

5. 保存退出后重启服务器，在控制台输入：

```text
ctdmapcheck
ctdmapplan
ctdpastefarm 28d24136afe8
```

6. 等待 `Farm safe paste complete`。完整场景会检查和写入较大体积，首次可能需要几分钟；期间不要重复执行。输入 `ctdpreflight` 检查配置。
7. 新服务器可将默认出生点设到观景平台：

```text
setworldspawn 62 104 -16
op 你的玩家名
```

玩家加入后可由管理员执行 `/tp 玩家名 62 104 -16` 查看完整场景。该平台是本版重建的等待/观景位置，排队本身不会强行传送或覆盖玩家真实背包。

## 两人试玩或 6v6

- 默认保留原来的六人一队排队设置，需要 12 名玩家。服务器人数上限已留出空间。
- **两个人玩：**在插件 config.yml 添加以下设置，然后重启：

```yaml
queue:
  team-size: 1
```

两名玩家各输入 `/ctdjoin`，等待时用 `/ctdvote` 投票、打开 Settings，然后走 3→2→1 开局。比赛中建塔/选择分支/升级/出售，会使用相应的方块塔模型；各级塔不再只有石头/玻璃占位。

管理员可用 `/ctdlivetest start test 红方玩家名 蓝方玩家名 wither` 启动调试对局，`/ctdlivetest stop test` 停止；普通玩家可 `/ctdleave` 离开。调试结束不等于正常胜负认证。

## 文件内容

- `plugins/CubeCraftTowerDefence.jar`：v114 插件，已内置场景和默认塔模型。
- `plugins/CubeCraftTowerDefence/maps/ImprovedFarm.schem`：保持校验值不变的社区改良赛道，供运行时解析路线和塔位。
- `plugins/CubeCraftTowerDefence/tower-models/`：完整 82 条模型目录，可独立调整，包含逐文件校验。
- `maps/Farm-Reconstructed.schem`：已组合赛道的完整场景 schematic，便于备份或在建造工具中查看；正常安装使用上面的插件粘贴指令，无需另装 WorldEdit。
- `previews/`：由实际方块生成的地图与塔模型预览，不是游戏客户端截图。
- `evidence/`：来源、实际构建、双次起服、地图和模型信息。

## 地图边界与复用

地图原点始终指赛道原点。完整场景为 **245×82×285**，相对偏移 **(-60,-3,-45)**。使用 `(0,80,0)` 时，整个盒子从 `(-60,77,-45)` 到 `(184,158,239)`，必须全部是空体积。新包的超平坦地面在该高度以下。

已有服务器请把本版部署到新的空区域，不要把新 config.yml 直接覆盖现有配置。旧版本默认 `farm-scene.enabled: false`；要使用新场景，需要明确开启并重新绑定空区域、重启后粘贴。首次粘贴拒绝覆盖任何非空气目的区域。

正常结束会恢复塔和移除怪物。若复用门禁被阻塞，先查看 `/ctdlivegate` 与提示；仅在符合条件时使用 `/ctdresetfarm 28d24136afe8`。开启完整场景时，修复使用同一份完整组合场景，不会把装饰恢复成旧的空白轨道。

正常通过一局后再连续玩第二局，检查塔、怪物、菜单残留和玩家背包/状态恢复；`/ctdperf` 用于查看真实负载。Fast Fly 仍保持 UNKNOWN，本轮未改变任何速度数值。
