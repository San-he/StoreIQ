# StoreIQ 前端项目学习笔记

> 项目：数码零售管理系统（StoreIQ Web）
> 技术栈：Vue 3.5 + TypeScript + Vite 7 + Element Plus + Pinia 3 + Vue Router 5
> 包管理器：pnpm

---

## 1. 项目目录结构

```
frontend/storeiq-web/
├── .husky/                    # Git hooks（commit 前自动 lint）
├── .vscode/                   # 团队 VS Code 配置 + 推荐插件 + 代码片段
├── public/                    # 静态资源（favicon、Cesium 3D 地图库）
├── src/
│   ├── common/                # 通用资源
│   │   ├── apis/              #   API 接口（storeiq/、tables/、users/）
│   │   ├── assets/            #   静态资源（icons/、images/、styles/）
│   │   ├── components/        #   通用组件（AiChat、Notify、SearchMenu 等）
│   │   ├── composables/       #   组合式函数（useDevice、useTheme 等）
│   │   ├── constants/         #   常量（app-key、cache-key）
│   │   └── utils/             #   工具函数（css、datetime、permission 等）
│   ├── http/                  # 网络请求封装（Axios）
│   ├── layouts/               # 布局系统
│   │   ├── components/        #   布局子组件（Sidebar、TagsView 等）
│   │   ├── composables/       #   布局组合式函数
│   │   └── modes/             #   三种布局模式（Left / Top / LeftTop）
│   ├── pages/                 # 业务页面（模块化内聚）
│   ├── pinia/stores/          # 状态管理
│   ├── plugins/               # 插件（全局组件、自定义指令）
│   ├── router/                # 路由
│   ├── styles/                # 项目全局样式
│   ├── App.vue                # 根组件
│   └── main.ts                # 入口文件
├── types/                     # TypeScript 类型声明
│   └── auto/                  #   自动生成的类型（禁止手动修改）
├── .editorconfig              # 编辑器统一格式
├── .env / .env.development / .env.production  # 环境变量
├── eslint.config.js           # ESLint 扁平化配置
├── tsconfig.json              # TypeScript 配置
├── uno.config.ts              # UnoCSS 原子化 CSS 配置
├── vite.config.ts             # Vite 构建配置
├── package.json
└── pnpm-lock.yaml
```



vite.config.ts    —— 构建系统核心
 ├── 代理转发（12 条 API → localhost:8002）
 ├── 预热（启动时预转译 layouts/pinia/router）
 ├── 分块策略（vue 系 / element 系 / vxe 分三包）
 ├── esbuild 构建清理（删 console.log、debugger、注释）
 ├── 依赖预构建（强制预构建 Element Plus CSS）
 └── 插件体系（vue / cesium / svg / UnoCSS / AutoImport / Components）

eslint.config.js  —— 代码规范（@antfu/eslint-config 全家桶 + 项目级覆盖）

环境变量
 ├── .env               —— 公共（标题、路由模式）
 ├── .env.development   —— 开发（后端地址、静态路径）
 └── .env.production    —— 生产（已修正为项目实际值）

其他工程化设施
 ├── pnpm vs npm        —— 包管理器差异
 ├── .husky/             —— Git commit 前自动检查
 ├── .vscode/            —— 团队编辑器配置 + 推荐插件
 └── UnoCSS              —— 原子化 CSS（非预处理器，和 SCSS 互补）

### 路径别名

| 别名 | 指向 | 示例 |
|------|------|------|
| `@` | `src/` | `import { router } from "@/router"` |
| `@@` | `src/common/` | `import { getToken } from "@@/utils/local-storage"` |

### 目录规范

- **业务代码内聚**：同一个模块的 API、组件、组合式函数放在 `pages/<模块>/` 下
- **跨模块复用**：放在 `common/` 下

---

## 2. 构建配置 — vite.config.ts

### 2.1 开发服务器

```ts
server: {
  host: true,          // 监听所有地址（局域网可访问）
  port: 3333,          // 开发端口
  strictPort: false,   // 端口被占用时不退出
  open: true,          // 启动时自动打开浏览器
}
```

### 2.2 反向代理

开发时前端 `localhost:3333` → 代理转发 → 后端 `localhost:8002`。

12 条 API 路径被代理：`/auth`、`/stores`、`/products`、`/categories`、`/inventory`、`/purchases`、`/sales`、`/suppliers`、`/stats`、`/ai`、`/notifications`、`/logs`。

```ts
proxy: {
  "/auth": {
    target: "http://localhost:8002",
    ws: false,          // 不代理 WebSocket
    changeOrigin: true  // 修改请求头的 Host 为 target 的主机名
  }
}
```

### 2.3 预热（Warmup）

开发服务器启动时预转译并缓存这些核心文件，首屏加载更快（不用等浏览器第一次请求时才转译）。

```ts
warmup: {
  clientFiles: [
    "./src/layouts/**/*.*",
    "./src/pinia/**/*.*",
    "./src/router/**/*.*"
  ]
}
```

### 2.4 分块策略

将不常变动的依赖拆成独立 JS 文件，利用浏览器缓存。改了业务代码后用户只需重新下载业务 chunk。

| Chunk 名 | 包含的包 |
|----------|---------|
| `vue` | vue + vue-router + pinia |
| `element` | element-plus + @element-plus/icons-vue |
| `vxe` | vxe-table |

### 2.5 构建清理（esbuild）

生产构建时自动执行（开发模式不执行）：

| 配置 | 效果 |
|------|------|
| `pure: ["console.log"]` | 删除所有 `console.log()` |
| `drop: ["debugger"]` | 删除所有 `debugger` 语句 |
| `legalComments: "none"` | 删除第三方库的许可证注释 |

### 2.6 依赖预构建

强制 Vite 在启动时预构建 Element Plus 的 CSS 文件（避坑：这些 CSS 不在 node_modules 主入口中，esbuild 扫描不到）。

```ts
optimizeDeps: {
  include: ["element-plus/es/components/*/style/css"]
}
```

### 2.7 CSS 预处理器

```ts
css: {
  preprocessorMaxWorkers: true  // SCSS → CSS 多线程并行编译，提速
}
```

### 2.8 插件

| 插件 | 作用 |
|------|------|
| `vue()` | 让 Vite 能解析 .vue 文件 |
| `cesium()` | CesiumJS 3D 地图支持 |
| `svgLoader()` | SVG 文件可作为 Vue 组件引用 |
| `SvgComponent()` | 自动生成 SvgIcon 全局组件和 SVG 雪碧图 |
| `UnoCSS()` | 原子化 CSS 引擎 |
| `AutoImport()` | 自动导入 Vue/Pinia/Router/Element Plus 的 JS API |
| `Components()` | 自动导入在 template 中使用的 Element Plus 组件 |

---

## 3. 代码规范 — eslint.config.js

使用 `@antfu/eslint-config`（Anthony Fu 的 ESLint 全家桶预设）。

### 项目级覆盖规则

```js
rules: {
  "vue/block-order": "off",                  // 不限制 .vue 文件块顺序
  "style/comma-dangle": ["error", "never"],  // 禁止尾随逗号
  "style/brace-style": ["error", "1tbs"],    // 大括号和条件在同一行
  "no-console": "off",                       // 允许 console
  "no-debugger": "off",                      // 允许 debugger
}
```

---

## 4. 环境变量

| 文件 | 用途 |
|------|------|
| `.env` | 所有环境共享：`VITE_APP_TITLE`、`VITE_ROUTER_HISTORY` |
| `.env.development` | 开发环境：`VITE_BASE_URL`、`VITE_PUBLIC_PATH` |
| `.env.production` | 生产环境：实际部署的后端地址和静态资源路径 |

---

## 5. 工程化设施

### .husky/

Git hooks：`pre-commit` 钩子在 commit 前自动执行 `npx vue-tsc`（TS 类型检查）和 `npx lint-staged`（代码格式化），不通过则拒绝 commit。

### .vscode/

| 文件 | 作用 |
|------|------|
| `extensions.json` | 推荐插件列表（Volar、ESLint、UnoCSS 等） |
| `settings.json` | 项目级编辑器设置（保存时格式化等） |
| `*.code-snippets` | 常用代码片段 |

### .npmrc

```ini
registry = https://registry.npmmirror.com   # 淘宝镜像源
save-exact = true                            # 锁定精确版本号
```

### pnpm vs npm

| | npm | pnpm |
|--|-----|------|
| 存储方式 | 每个项目独立 node_modules | 全局 store + 硬链接 |
| 磁盘占用 | 大 | 小 |
| 安全性 | 允许幽灵依赖 | 严格模式 |
| 锁文件 | package-lock.json | pnpm-lock.yaml |

---

## 6. 样式体系

### 项目中的四种样式方案

| 方案 | 类型 | 用途 |
|------|------|------|
| **SCSS** | CSS 预处理器（语言扩展） | 全局样式、主题变量、组件私有样式、Element Plus 样式覆盖 |
| **UnoCSS** | 原子化 CSS 引擎 | 在 HTML 中直接写工具类（`class="flex items-center p-4"`） |
| **Element Plus CSS** | UI 组件库样式 | 全量引入 + 暗黑模式 CSS 变量 |
| **vxe-table CSS** | 表格组件库样式 | 专业表格样式 |

### UnoCSS 快捷类

```css
wh-full       → width: 100%; height: 100%
flex-center   → display: flex; justify-content: center; align-items: center
flex-x-center → display: flex; justify-content: center
flex-y-center → display: flex; align-items: center
```

### main.ts CSS 引入清单

```
normalize.css           → CSS Reset（统一浏览器默认样式）
nprogress.css           → 页面加载进度条
element-plus CSS 全套   → 组件库样式 + 暗黑模式变量
vxe-table CSS           → 专业表格样式
@@/assets/styles/       → 项目主题系统入口（mixins、变量、过渡等）
virtual:uno.css         → UnoCSS 运行时生成的原子化 CSS
@/styles/global.scss    → 业务全局样式（表格/弹窗/表单统一风格）
```

---

## 7. 路由系统

*待补充...*

---

## 8. 状态管理

*待补充...*

---

## 9. 网络请求

*待补充...*

---

## 10. 布局系统

*待补充...*

---

## 11. 业务页面

*待补充...*

---

## 12. 待深入

- [ ] `src/router/` — 路由守卫 + 权限模型
- [ ] `src/pinia/stores/` — user、permission、settings、tags-view
- [ ] `src/http/axios.ts` — 请求封装（拦截器、Token、错误处理）
- [ ] `src/layouts/` — 三种布局模式切换原理
- [ ] `src/common/composables/` — 组合式函数详解
- [ ] `src/common/utils/` — 工具函数详解
- [ ] `src/plugins/` — 插件系统
- [ ] 后端 `backend/storeiq-api/` — API 接口结构
- [ ] 自动导入的优缺点
- [ ] 路由 hash 模式 vs history 模式的选择
