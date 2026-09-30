# CubeCraft Tower Defence：Farm 工程试玩包

此包包含通过 CI 的插件、已校验的社区改良 Farm 赛道、启动脚本与证据文件。
**它不包含完整原版 Farm 世界，也不表示已经通过真人完整游玩认证。**
游戏流程代码可用于两人 1v1 试玩；首次部署仍需完成下面的地图绑定和真人门禁。

## 新建服务器

1. 将 ZIP 解压到一个空文件夹。不要覆盖正在使用的服务器或旧的存档。
2. 安装 Java 25；用 `java -version` 确认启动脚本实际使用的版本。
3. 从 [Paper 官方下载页](https://papermc.io/downloads/paper) 下载 **Paper 26.2**，放在解压目录并命名为 `paper.jar`。
   此包随附的 `evidence/paper-smoke-*.log` 记录实际 CI 使用的 Paper build；复现 CI 时选择相同 build。
4. Windows 双击 `start.bat`；Linux/macOS 运行 `bash start.sh`。默认内存为 1–4 GB，可按机器资源调整。
5. 首次启动生成 `eula.txt`；阅读并接受 Minecraft EULA 后将 `eula=false` 改为 `eula=true`，再启动。
6. 两位玩家使用 Minecraft Java **26.2** 连接服务器。管理员在控制台输入 `op <管理员玩家名>`。
   同机连接 `localhost:25565`；局域网玩家连接服务器机器的局域网地址。公网连接需自行配置网络。

## 首次放置与绑定地图

1. 管理员飞到新超平坦世界一处空地，面向哪里不影响粘贴方向。
   玩家当前方块 X/Z 是赛道的最小 X/Z 角；地图原点 Y 为脚下方块 Y + 8。
   需要预留 **125 × 18 × 191** 的空体积：原点至原点 + `(124,17,190)`。
2. 输入 `/ctdplaytestsetup apply`。它生成明确标为 Engineering 的配置，并备份原配置。
3. 在控制台输入 `stop`，等待保存退出，再启动服务器。
4. 管理员在线且不在比赛中，输入 `/ctdsnapshotcheck`；检查玩家状态保存/恢复结果。
5. 输入 `/ctdmapcheck`，然后 `/ctdpastefarm 28d24136afe8`，等待粘贴完成。
   若提示目标区域非空气，换一处空区域重新绑定；不要直接用 WorldEdit 绕过检查。
6. 输入 `/ctdpreflight`。只有阻塞项已清除才进入比赛测试。

也可以直接在控制台输入 `ctdplaytestsetup apply ctd-playtest 0 80 0`，明确绑定已加载的
`ctd-playtest` 世界，地图原点为 `(0,80,0)`。这个形式**不再额外加 8 格高度**，适合自动部署。
之后仍必须重启并完成同样的地图检查/粘贴/preflight；它不会自动通过玩家状态或真人门禁。

## 两人完整回合测试

1. 两名玩家各输入 `/ctdjoin`；等候时用 `/ctdvote` 打开投票和 Settings，检查 3→2→1 后开始。
2. 两方分别建塔、选择升级分支、升级/出售、提升召唤等级、发送怪物、使用 AoE 和 Settings。
   将比赛进行到一方城堡被摧毁，检查胜负展示和玩家原背包/状态恢复。
3. 管理员输入 `/ctdlivegate`、`/ctdperf`，保存实际服务器输出。
4. 再次排队完成第二局，核对上局怪物、塔和 GUI 没有残留，地图可继续复用。
5. 需要中止时玩家用 `/ctdleave`。管理员手动测试可用
   `/ctdlivetest start test <红方玩家名> <蓝方玩家名> wither`，并用 `/ctdlivetest stop test` 停止该管理员测试。

`/ctdjoin` 启动的比赛与管理员 `test` 测试不是同一个操作入口；正常胜负结束才会记录持久 wins。
若 Farm reuse gate 被阻塞，先查看提示；仅在满足命令条件后使用 `/ctdresetfarm 28d24136afe8` 修复。
不要删除 recovery journal、progress 或 reuse gate 文件来伪造通过结果。

## 地图范围与复刻证据

- 地图文件是 Shotgun 在 [Farm Improvements（2022）](https://www.cubecraft.net/threads/309871/) 分享的改良赛道/塔位。
- SHA-256：`28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e`。
- 它修改过塔位，不是原始 Farm 几何的完全一致复制；完整建筑、地形、外围背景和原版 lobby 尚未恢复认证。
- `evidence/farm-audit.json` 是从实际 NBT 方块数据计算的范围和材料数量。
- `evidence/farm-top-view.svg` 是赛道方块俯视图，X 向右、Z 向下。颜色只是材料分类，空白区域不是已复刻地形。
- `evidence/build-manifest.json` 记录插件来源 commit、Actions run 和文件校验值；双次 CI 起服不能替代两名真人实际打一整局。

其余 Stage-4 真人门禁（重启恢复、60+ 塔、移动视觉、连续复用、raytrace、GUI、重连接管、multi-arena）继续以实际服证据为准。
Fast Fly 的准确语义/速度仍为 UNKNOWN。
