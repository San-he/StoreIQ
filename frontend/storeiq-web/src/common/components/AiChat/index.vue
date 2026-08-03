<script setup>
/**
 * 全局 AI 浮动聊天助手 — 右下角悬浮按钮 + 展开面板
 */
import { aiChatApi } from '@@/apis/storeiq/ai'
import { ChatDotRound, Close, Promotion } from '@element-plus/icons-vue'

const visible = ref(false)
const question = ref('')
const messages = ref([{ role: 'ai', text: '你好！我是 StoreIQ AI 分析助手。\n\n你可以问我：\n• 哪个店铺利润最高？\n• 本月销售趋势怎么样？\n• 需要补货哪些商品？' }])
const loading = ref(false)
const chatBody = ref(null)

function toggle() { visible.value = !visible.value }

async function send() {
  const q = question.value.trim()
  if (!q || loading.value) return
  messages.value.push({ role: 'user', text: q })
  question.value = ''
  loading.value = true
  try {
    const { data } = await aiChatApi({ question: q })
    messages.value.push({ role: 'ai', text: data.answer || '抱歉，分析出错了' })
  } catch {
    messages.value.push({ role: 'ai', text: '抱歉，AI 服务暂时不可用，请稍后再试。' })
  } finally {
    loading.value = false
    nextTick(() => { if (chatBody.value) chatBody.value.scrollTop = chatBody.value.scrollHeight })
  }
}

function handleKey(e) { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }
</script>

<template>
  <div class="ai-floater" :class="{ open: visible }">
    <!-- 浮窗面板 -->
    <transition name="slide-up">
      <div v-if="visible" class="ai-panel">
        <div class="ai-panel-header">
          <div class="ai-panel-title"><el-icon><ChatDotRound /></el-icon> StoreIQ AI 助手</div>
          <el-button text :icon="Close" @click="toggle" style="color:#fff" />
        </div>
        <div ref="chatBody" class="ai-chat-body">
          <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
            <div class="msg-bubble">{{ msg.text }}</div>
          </div>
          <div v-if="loading" class="msg ai"><div class="msg-bubble typing">分析中...</div></div>
        </div>
        <div class="ai-input-row">
          <el-input v-model="question" placeholder="输入问题..." size="small" @keydown="handleKey" />
          <el-button type="primary" size="small" :icon="Promotion" :loading="loading" @click="send" />
        </div>
      </div>
    </transition>
    <!-- 悬浮按钮 -->
    <div class="ai-btn" @click="toggle">
      <el-icon :size="22" v-if="!visible"><ChatDotRound /></el-icon>
      <el-icon :size="22" v-else><Close /></el-icon>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.ai-floater { position: fixed; right: 24px; bottom: 24px; z-index: 9999; }
.ai-btn {
  width: 50px; height: 50px; border-radius: 50%; cursor: pointer;
  background: linear-gradient(135deg, #409eff, #67c23a); color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 20px rgba(64,158,255,.4); transition: transform .2s;
  &:hover { transform: scale(1.08); }
}
.ai-panel {
  position: absolute; right: 0; bottom: 64px; width: 380px; height: 520px;
  background: var(--el-bg-color); border-radius: 14px; box-shadow: 0 8px 40px rgba(0,0,0,.18);
  display: flex; flex-direction: column; overflow: hidden;
}
.ai-panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; background: linear-gradient(135deg, #409eff, #337ecc);
  .ai-panel-title { color:#fff; font-size:15px; font-weight:600; display:flex; align-items:center; gap:6px }
}
.ai-chat-body { flex:1; overflow-y:auto; padding:12px; display:flex; flex-direction:column; gap:8px;
  .msg { display:flex; &.user { justify-content:flex-end } }
  .msg-bubble { max-width:85%; padding:8px 12px; border-radius:12px; font-size:13px; line-height:1.6; white-space:pre-wrap;
    .user & { background: var(--el-color-primary); color:#fff; border-radius:12px 12px 4px 12px }
    .ai & { background: var(--el-fill-color-light); color:var(--el-text-color-primary); border-radius:12px 12px 12px 4px }
    &.typing { color: var(--el-text-color-secondary); font-style:italic }
  }
}
.ai-input-row { display:flex; gap:6px; padding:10px 12px; border-top:1px solid var(--el-border-color-light) }
.slide-up-enter-active, .slide-up-leave-active { transition: all .25s ease }
.slide-up-enter-from, .slide-up-leave-to { opacity:0; transform:translateY(20px) }
</style>
