# CubeCraft Tower Defence：Farm 工程试玩包

此包包含通过 CI 的插件、已校验的社区改良 Farm 赛道、启动脚本与证据文件。
**它不包含完整原版 Farm 世界，也不表示已经通过真人完整游玩认证。**
普通 `/ctdjoin` 默认 6v6，等待 12 名玩家；两人试玩请在首次重启前将插件 config.yml 的 `queue.team-size` 改为 `1`。首次部署仍需完成下面的地图绑定和真人门禁。

## 新建服务器

1. 将 ZIP 解压到一个空文件夹。不要覆盖正在使用的服务器或旧的存档。
2. 安装 Java 25；用 `java -version` 确认启动脚本实际使用的版本。
3. 从 [Paper 官方下载页](https://papermc.io/downloads/paper) 下载 **Paper 26.2**，放在解压目录并命名为 `paper.jar`。
   此包随附的 `evidence/paper-smoke-*.log` 记录实际 CI 使用的 Paper build；复现 CI 时选择相同 build。
4. Windows 双击 `start.bat`；Linux/macOS 运行 `bash start.sh`。默认内存为 1–4 GB，可按机器资源调整。
5. 首次启动生成 `eula.txt`；阅读并接受 Minecraft EULA 后将 `eula=false` 改为 `eula=true`，再启动。
6. 玩家使用 Minecraft Java **26.2** 连接服务器。管理员在控制台输入 `op <管理员玩家名>`。
   同机连接 `localhost:25565`；局域网玩家连接服务器机器的局域网地址。公网连接需自行配置网络。

## 首次放置与绑定地图

1. 管理员飞到新超平坦世界一处空地，面向哪里不影响粘贴方向。
   玩家当前方块 X/Z 是赛道的最小 X/Z 角；地图原点 Y 为脚下方块 Y + 8。
   需要预留 **125 × 18 × 191** 的空体积：原点至原点 + `(124,17,190)`。
2. 输入 `/ctdplaytestsetup apply`。它生成明确标为 Engineering 的配置，并备份原配置。
3. 两人测试时，先将 `plugins/CubeCraftTowerDefence/config.yml` 的 `queue.team-size` 改为 `1`；保持 `6` 则需要 12 人。然后在控制台输入 `stop`，等待保存退出，再启动服务器。
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
- 若包中有 `evidence/deployment/`，其中是相同插件字节对应的独立部署验证：三次启动、实际赛道粘贴、重启 preflight，以及保存后的 429,750 格方块状态比对。该验证没有在线玩家，不代表完整游玩、玩家恢复或完整原版场景已认证。

其余 Stage-4 真人门禁（重启恢复、60+ 塔、移动视觉、连续复用、raytrace、GUI、重连接管、multi-arena）继续以实际服证据为准。
Fast Fly 的准确语义/速度仍为 UNKNOWN。

## v108 团队与塔模型入口

管理员可用 `/ctdlivetest teamstart test 红1,红2,红3,红4,红5,红6 蓝1,蓝2,蓝3,蓝4,蓝5,蓝6 wither`
启动最多每队六人的团队测试。列出的玩家必须全部在线，两队不能有重复玩家；结束测试用
`/ctdlivetest stop test`。v109 的普通 `/ctdjoin` 已接入团队排队，默认 6v6；FIFO/交替分队仍是 Engineering 规则，未宣称原版匹配完全一致。

现在支持外部塔体模型目录 `plugins/CubeCraftTowerDefence/tower-models/catalog.yml`，模型文件有来源标签和 SHA-256 校验。
**本包没有附带已认证原版塔体，缺失模型仍显示石头/玻璃占位。** 模型格式与安全恢复范围见源码仓库
`evidence/TOWER_MODEL_INTEGRATION.md`；校验值能确认文件字节，不能自动证明模型与原版完全一致。

## v109 普通团队排队

12 名玩家分别输入 `/ctdjoin` 后会自动保留完整六人红队/六人蓝队，走投票、Settings 和 3→2→1 倒计时。等待 HUD 显示 `6v6 (当前人数/12)`。人数未满不会开局；倒计时中有人离开/离线，会取消并将其余可用玩家按原次序返回队首。后面排队玩家的投票不会混入当前比赛。服务器只使用当前绑定的单个 Farm，其他玩家等待地图可复用。

`queue.team-size` 支持整数 1..6，需要重启生效；缺失该项的旧配置也默认六人一队。模式变更不会删除 hotbar、wins 或 recovery 文件。新默认值不会证明原版最低开局人数、组队/段位平衡或原版等待大厅已恢复。真人六人团队完整回合仍待认证。

## v110 菜单视觉与背包保护

Settings 恢复截图可见的羽毛、烈焰粉和红/绿玻璃片外观；布局编辑器恢复右上角箭头。未核实的点击语义仍为无操作，羽毛不会擅自开启 Fast Fly。Particles 功能移到明确属于 Engineering 的 slot 16。空 hotbar 格现在显示为空白，仍能点击放入已选择的 AoE。

打开插件菜单时，下面玩家背包的 Shift 转移、收集、快捷键交换和拖拽也会被拦截，避免真实物品混入投影菜单；关闭菜单后普通背包操作照常。此项通过 Paper API 事件检查，真人客户端操作及状态恢复仍需要实际试玩。


## v111 菜单文字渲染

所有菜单名称和 lore 显式关闭默认斜体；未知颜色保持未指定，仅有证据的局部文字加粗。菜单打开与动态刷新共用同一物品投影，空白格不写入物品 metadata。双 Paper 起服检查使用生产投影构造并序列化菜单物品，覆盖普通文字、空行、局部加粗和 Archer 两条路径的独立闪光。此项不替代真人客户端视觉认证；完整原版地图、塔模型和未知图标语义仍未恢复。


## v112 右键操作修复

修复 Summoner / Bazaar / Settings 右键空气可能无反应的问题；识别 Paper 的空气预测取消状态，同时保留对明确禁止物品/方块操作的拦截。菜单、AoE、塔管理/快捷升级、建塔/快捷放置只处理主手右键，避免副手事件重复触发。左键和踩踏不会再进入 AoE 武装流程。真实客户端仍需试玩确认；本轮没有新增未知速度、声音或 GUI 点击语义。


## v113 菜单退出与待放置状态

关闭建塔/路径菜单会清除待放置坐标；正常“建塔 → 选路径”切换保留坐标。切换其他菜单、打开失败或进行快捷放置时会取消旧目标，不会因关闭菜单自动放塔。点击立即拦截物品转移，实际操作延后一 tick，并再次核对原 inventory/slot，防止旧页面点击作用到新菜单。旧菜单 close 和同尺寸的其他 inventory 不会清除或刷新新菜单。本轮新增三个放置回归用例，真人客户端仍需验证。
