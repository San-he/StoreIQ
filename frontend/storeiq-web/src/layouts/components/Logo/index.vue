<script setup>
import { useLayoutMode } from "@@/composables/useLayoutMode"

defineProps({ collapse: { type: Boolean, default: true } })
const { isLeft, isTop } = useLayoutMode()
</script>

<template>
  <div class="layout-logo-container" :class="{ 'collapse': collapse, 'layout-mode-top': isTop }">
    <transition name="layout-logo-fade">
      <router-link v-if="collapse" key="collapse" to="/map" class="logo-link">
        <span class="logo-icon">SI</span>
      </router-link>
      <router-link v-else key="expand" to="/map" class="logo-link">
        <span class="logo-text">StoreIQ</span>
      </router-link>
    </transition>
  </div>
</template>

<style lang="scss" scoped>
.layout-logo-container {
  position: relative; width: 100%; height: var(--v3-header-height);
  display: flex; justify-content: center; overflow: hidden;
  .logo-link {
    display: flex; align-items: center; text-decoration: none;
    .logo-icon {
      width: 34px; height: 34px; border-radius: 8px;
      background: linear-gradient(135deg, #409eff, #67c23a);
      display: flex; align-items: center; justify-content: center;
      color: #fff; font-size: 13px; font-weight: 800; letter-spacing: -1px;
    }
    .logo-text {
      font-size: 18px; font-weight: 800; letter-spacing: 1px;
      background: linear-gradient(135deg, #409eff 30%, #67c23a);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    }
  }
}
.layout-mode-top { height: var(--v3-navigationbar-height); }
.collapse .logo-text { display: none; }
</style>
