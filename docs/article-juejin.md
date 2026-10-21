# 从 0 到 1：我用 Three.js 给零售系统做了一个赛博朋克 3D 数据大屏

> 一个应届生的全栈作品：Vue3 + Three.js + Spring Boot 的智能数码零售管理系统。
> 从内存泄漏排查到四层视觉重构，从本地开发到阿里云学生机 Docker 一键部署，全程踩坑实录。

## 🌐 先看效果

- **在线体验**：<http://47.117.130.16/> （账号 `superadmin` / 密码 `Storeiq2026!`）
- **源码仓库**：<https://github.com/San-he/StoreIQ>（Gitee 同步：[he-sansan/StoreIQ](https://gitee.com/he-sansan/StoreIQ)）

**3D 数据大屏实机演示：**

![大屏演示](https://raw.githubusercontent.com/San-he/StoreIQ/main/docs/demo.gif)

**后台管理（店铺地图选点 / 数据分析 / 店铺详情）：**

| 店铺管理 | 数据分析 | 店铺详情 |
|:---:|:---:|:---:|
| ![demo2](https://raw.githubusercontent.com/San-he/StoreIQ/main/docs/demo2.gif) | ![demo3](https://raw.githubusercontent.com/San-he/StoreIQ/main/docs/demo3.gif) | ![demo4](https://raw.githubusercontent.com/San-he/StoreIQ/main/docs/demo4.gif) |

## 📌 项目是什么

StoreIQ 是一个面向数码零售连锁的全栈管理系统：**进销存 + 数据分析 + 3D 数据大屏**。

技术栈：

| 层 | 技术 |
|:---:|:---|
| 前端 | Vue 3.5 · Vite · Element Plus · Pinia |
| 3D/图表 | Three.js（Bloom 后处理、Line2 流光、CSS2DRenderer）· ECharts · Leaflet |
| 后端 | Spring Boot 2.7 · MyBatis-Plus · Spring AOP · JWT |
| 数据库 | MySQL 8（一键初始化 + 演示数据 SQL） |
| 部署 | Docker Compose（MySQL + 后端 + Nginx 同源反代） |

## 🗺️ 大屏的技术实现（最花心思的部分）

大屏是一张 **Three.js 3D 中国地图**：每个省是挤出建模的 3D 板块，店铺按经纬度落在地图上。几个关键点：

### 1. GeoJSON → 3D 板块

从 GeoJSON 里取每个省的环坐标（外环 + 孔洞），经纬度映射到场景 XY 平面，用 `THREE.Shape` + `ExtrudeGeometry` 沿 Z 轴挤出厚度，再加 `EdgesGeometry` 描边。十段线、国界流光（Line2 + `onBeforeCompile` 向片元着色器注入流光代码）都在这个坐标系上完成。

### 2. 店铺的"四层视觉"设计

店铺多、坐标近时，31 根实心发光柱挤在一起会被 Bloom 放大成"白茫茫"。解决思路不是错位坐标（会误导地理位置），而是**用形态区分层级，所有店铺保持真实坐标**：

- **Top 3 核心营收 → 全息光柱**：细发光内芯 + 透明外壳（贴螺旋光带纹理，`offset.y` 动画实现光带环绕上升）+ 柱内粒子上升 + 底部雷达扫描环扩散；
- **4~8 名 → 水波纹**：平贴地面的 `RingGeometry` 从中心向外扩散、淡出，双圈错相位，像湖面涟漪；
- **9 名以后 → 微光点**：亮白小点 + 淡黄光晕，融入底盘；
- **停运店铺 → 幽灵片**：深灰半透明贴地圆片，零发光，hover 显示"已停运 + 停运时间"。

光带的"流动"没有用 Shader：Canvas 画一条全宽对角线（三层描边：外发光→主体→白芯），配合 `RepeatWrapping` + 每帧改 `offset.y`，就是无缝循环的螺旋光带——性能开销几乎为零。

### 3. 性能优化清单（真实踩坑）

- **mousemove 拾取节流**：射线检测从"每个像素移动一次"改为 rAF 节流 + 3px 位移阈值 + 省份拾取列表预构建；
- **GPU 资源完整释放**：`scene.clear()` 不释放显存，卸载时递归 dispose 几何体/材质/纹理，反复进出页面内存不再累积；
- **时钟组件隔离**：秒级时钟拆成独立组件，避免整屏模板每秒全量 diff；
- **resize 节流**：rAF 合并 + 尺寸未变跳过，避免拖拽窗口时反复重建 Bloom RenderTarget。

## ⚙️ 后端值得一提的

- **AOP 操作日志生成人类可读详情**：切面拦截业务变更操作，从请求参数反射/查库提取业务名称，日志长这样——`店铺「总店」出库商品「iPhone 15」× 2`，而不是 `执行 SaleController.save`；同时排除 list/export 等只读接口的噪声日志；
- **MyBatis-Plus 字段自动填充**：补上缺失的 `MetaObjectHandler`，createTime/updateTime 全局自动写入；
- 出库弹窗按店铺过滤商品、0 库存拦截、扫码录入、Excel 批量导入、预估利润。

## 🚀 部署踩坑实录（阿里云学生机 + Docker）

一台 **2 核 2G / 1M 带宽**的学生机跑三个容器，坑一个接一个：

1. **Docker Hub 拉不动**：国内网络直连超时 → 配置阿里云镜像加速器；但加速器没有 JDK8 基础镜像的缓存 → 从代理镜像源拉取后 `docker tag` 打标准标签；
2. **Vite 6 要求 Node ≥ 20**：构建镜像从 `node:18` 升级到 `node:22`；
3. **构建 OOM**：2G 内存打包 2768 个模块堆溢出 → `NODE_OPTIONS=--max-old-space-size=1280`；运行期 JVM 限 `-Xmx512m`、MySQL 关 performance_schema；
4. **1M 带宽首屏太慢** → nginx 开 gzip、删除 17MB 废弃 Cesium 静态资源、公网带宽改按量计费（流量费一个月几毛钱，带宽上限 100M）；
5. **package-lock.json 没进仓库**导致服务器 `npm ci` 失败 → 修正 .gitignore 纳入锁定文件。

完整部署文档在仓库 [docs/DEPLOY.md](https://github.com/San-he/StoreIQ/blob/main/docs/DEPLOY.md)，一条 `docker compose up -d --build` 拉起 MySQL + 后端 + 前端。

## ✨ 如果你也想做一个

三个建议：

1. **视觉是第一个 commit 就该想的事**：我的大屏迭代了四版（实心柱 → 流动光带 → 分层柱体 → 四层视觉），前两版都是"能看但不出彩"。先想清楚信息层级再动手，省一半时间；
2. **性能问题早暴露早解决**：3D 大屏最容易在 mousemove、resize、卸载清理上翻车，这三个点的优化套路可以直接抄；
3. **学生机的部署价值被低估**：0 元（学生认证）+ 一条命令部署 = 面试官可以直接点开你的作品，比任何截图都有说服力。

## 🙋 最后

我是 2026 届应届生，这是我在秋招期间独立完成的全栈项目。如果这个项目对你有帮助，欢迎 **Star** ⭐；也欢迎提 Issue 交流，祝各位面试顺利！

---

**仓库地址**：<https://github.com/San-he/StoreIQ>

**在线演示**：<http://47.117.130.16/>
