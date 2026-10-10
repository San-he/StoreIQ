<script setup>
/**
 * 数据清理详情弹窗 — 展示清理汇总 + 保留规则编辑
 */
import { getCleanupRulesApi, saveCleanupRulesApi } from '@@/apis/storeiq/notifications'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  notification: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const rules = ref([])

// 解析通知内容
const summary = computed(() => {
  if (!props.notification?.content) return null
  try { return JSON.parse(props.notification.content) } catch { return null }
})

const tableLabelMap = { operation_log: '操作日志', purchase_record: '进货记录', sale_record: '销售记录' }

watch(visible, async (val) => {
  if (val) {
    loading.value = true
    try {
      const { data } = await getCleanupRulesApi()
      rules.value = (data || []).map(r => ({
        ...r,
        storeIdsInput: r.storeIds || '',
        categoryIdsInput: r.categoryIds || ''
      }))
    } catch { rules.value = [] }
    finally { loading.value = false }
  }
})

function getTableSummary(tableType) {
  if (!summary.value?.tables) return null
  return summary.value.tables.find(t => t.tableType === tableType)
}

async function handleSave() {
  loading.value = true
  try {
    const payload = rules.value.map(r => ({
      id: r.id,
      tableType: r.tableType,
      retentionDays: r.retentionDays,
      storeIds: r.storeIdsInput || null,
      categoryIds: r.categoryIdsInput || null
    }))
    await saveCleanupRulesApi(payload)
    ElMessage.success('规则已保存，下次清理将按新规则执行')
  } catch { /* */ }
  finally { loading.value = false }
}

function getMonthlyTotal(monthly) {
  if (!monthly) return 0
  return monthly.reduce((sum, m) => sum + (m.count || 0), 0)
}

// 为 v-model 提供可写的引用（避免模板中使用 find + 可选链）
function getRuleForm(tableType) {
  let rule = rules.value.find(r => r.tableType === tableType)
  if (!rule) {
    rule = { tableType, retentionDays: 365, storeIdsInput: '', categoryIdsInput: '' }
    rules.value.push(rule)
  }
  if (rule.storeIdsInput === undefined) rule.storeIdsInput = rule.storeIds || ''
  if (rule.categoryIdsInput === undefined) rule.categoryIdsInput = rule.categoryIds || ''
  return rule
}
</script>

<template>
  <el-dialog v-model="visible" title="数据清理详情" width="700px" :close-on-click-modal="false" destroy-on-close>
    <div v-loading="loading">
      <!-- 汇总信息 -->
      <el-alert v-if="summary" :title="`共扫描到 ${summary.totalRecords} 条过期记录`" type="warning" :closable="false" show-icon style="margin-bottom:16px" />

      <div v-if="!summary" style="text-align:center;padding:40px;color:var(--el-text-color-secondary)">
        暂无待清理数据
      </div>

      <!-- 按表分组 -->
      <el-collapse v-if="summary?.tables?.length">
        <el-collapse-item v-for="t in summary.tables" :key="t.tableType">
          <template #title>
            <div style="display:flex;align-items:center;gap:10px">
              <span style="font-weight:600">{{ tableLabelMap[t.tableType] || t.tableType }}</span>
              <el-tag size="small" type="warning">{{ t.totalRecords }} 条待清理</el-tag>
            </div>
          </template>

          <!-- 统计信息 -->
          <el-descriptions :column="2" border size="small" style="margin-bottom:12px">
            <el-descriptions-item label="保留天数">{{ t.retentionDays }} 天</el-descriptions-item>
            <el-descriptions-item label="最早记录">{{ t.oldestDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="待清理数">{{ t.totalRecords }} 条</el-descriptions-item>
            <el-descriptions-item label="涉及月份">{{ t.monthlyBreakdown?.length || 0 }} 个月</el-descriptions-item>
          </el-descriptions>

          <!-- 按月分布 -->
          <div v-if="t.monthlyBreakdown?.length" style="margin-bottom:12px">
            <span style="font-size:12px;color:var(--el-text-color-secondary)">按月分布：</span>
            <div style="display:flex;flex-wrap:wrap;gap:6px;margin-top:4px">
              <el-tag v-for="m in t.monthlyBreakdown" :key="m.month" size="small" type="info">
                {{ m.month }}: {{ m.count }} 条
              </el-tag>
            </div>
          </div>

          <!-- 保留规则编辑 -->
          <el-divider content-position="left" style="margin:8px 0">保留规则</el-divider>
          <el-form label-width="100px" size="small">
            <el-form-item label="保留天数">
              <el-input-number
                v-model="getRuleForm(t.tableType).retentionDays"
                :min="30" :max="1095" :step="30"
                style="width:160px"
              />
              <span style="margin-left:8px;color:var(--el-text-color-secondary);font-size:12px">超过此天数的数据将被清理</span>
            </el-form-item>
            <el-form-item v-if="t.tableType !== 'operation_log'" label="排除店铺">
              <el-input
                v-model="getRuleForm(t.tableType).storeIdsInput"
                placeholder="店铺ID，逗号分隔，留空=不排除"
              />
            </el-form-item>
            <el-form-item v-if="t.tableType !== 'operation_log'" label="排除分类">
              <el-input
                v-model="getRuleForm(t.tableType).categoryIdsInput"
                placeholder="分类ID，逗号分隔，留空=不排除"
              />
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="loading" @click="handleSave" v-if="summary?.tables?.length">保存规则</el-button>
    </template>
  </el-dialog>
</template>
