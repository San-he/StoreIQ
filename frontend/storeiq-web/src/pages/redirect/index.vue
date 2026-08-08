<script lang="ts" setup>
const route = useRoute()

const router = useRouter()
// 用 replace 是为了不污染浏览历史，避免用户后退时看到中间页
router.replace({ path: `/${route.params.path}`, query: route.query })
</script>

<template>
  <div />
</template>
