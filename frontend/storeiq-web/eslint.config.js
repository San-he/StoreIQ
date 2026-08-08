import antfu from "@antfu/eslint-config"

export default antfu(
  {
    // 使用外部格式化程序格式化 css、html、markdown 等文件
    formatters: true,
    // 启用样式规则
    stylistic: {
      // 缩进级别
      indent: 2,
      // 引号风格 'single' | 'double'
      quotes: "double",
      // 是否启用分号
      semi: false
    },
    // 忽略文件
    ignores: ["*/skills"]
  },
  {
    // 对所有文件都生效的规则
    rules: {
      // vue
      // "vue/block-order": ["error", { order: ["template", "script", "style"] }],
      "vue/block-order": "off",
      "vue/attributes-order": "off",
      // ts
      "ts/no-use-before-define": "off",
      // node
      "node/prefer-global/process": "off",
      // style
      "style/comma-dangle": ["error", "never"],
      "style/brace-style": ["error", "1tbs"],
      // regexp
      "regexp/no-unused-capturing-group": "off",
      // other
      "no-console": "off",
      "no-debugger": "off",
      "symbol-description": "off",
      "antfu/if-newline": "off",
      "unicorn/no-instanceof-builtins": "off"
    }
  }
)
