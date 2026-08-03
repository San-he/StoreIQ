<script lang="ts" setup>
import { useGreyAndColorWeakness } from "@@/composables/useGreyAndColorWeakness"
import { useTheme } from "@@/composables/useTheme"
import AiChat from "@@/components/AiChat/index.vue"
import zhCn from "element-plus/es/locale/lang/zh-cn"

const { initTheme } = useTheme()
const { initGreyAndColorWeakness } = useGreyAndColorWeakness()

initTheme()
initGreyAndColorWeakness()
</script>

<template>
  <el-config-provider :locale="zhCn">
    <router-view />
    <AiChat />
  </el-config-provider>
</template>
