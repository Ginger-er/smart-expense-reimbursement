<template>
  <div class="page-container">
    <h1 class="page-title">数据报表</h1>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="s in statCards" :key="s.label">
        <div class="stat-icon-wrap" :style="{ background: s.grad, color: s.color }">
          <el-icon :size="20"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- 报表筛选 -->
    <div class="card-wrapper card-gap">
      <el-form :model="query" inline>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button :loading="exporting" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出报表
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <!-- 部门报销排行 -->
      <el-col :span="12">
        <div class="card-wrapper">
          <h3 class="section-title">部门报销排行</h3>
          <div class="dept-ranking">
            <div v-for="(item, idx) in deptRanking" :key="item.name" class="rank-item">
              <span class="rank-num" :class="idx < 3 ? `top-${idx + 1}` : ''">{{ idx + 1 }}</span>
              <span class="rank-name">{{ item.name }}</span>
              <div class="rank-bar">
                <div class="rank-fill" :style="{ width: (item.amount / maxDeptAmount * 100) + '%' }" />
              </div>
              <span class="rank-amount">&yen;{{ formatYuan(item.amount) }}</span>
            </div>
            <el-empty v-if="!deptRanking.length" description="暂无数据" :image-size="60" />
          </div>
        </div>
      </el-col>

      <!-- 费用类型分布 -->
      <el-col :span="12">
        <div class="card-wrapper">
          <h3 class="section-title">费用类型分布</h3>
          <div class="type-distribution">
            <div v-for="t in computedExpenseTypes" :key="t.name" class="type-item">
              <span class="type-name">{{ t.name }}</span>
              <div class="type-bar">
                <div class="type-fill" :style="{ width: t.pct + '%', background: t.color }" />
              </div>
              <span class="type-amount">&yen;{{ formatYuan(t.amount) }}</span>
              <span class="type-pct">{{ t.pct }}%</span>
            </div>
            <el-empty v-if="!computedExpenseTypes.length" description="暂无数据" :image-size="60" />
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 报销明细表 -->
    <div class="card-wrapper">
      <h3 class="section-title">报销明细</h3>
      <el-table :data="detailData" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderNo" label="报销单号" width="160" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="部门" width="120" />
        <el-table-column prop="amount" label="金额" width="130" sortable>
          <template #default="{ row }">
            <span class="amount-text">&yen;{{ formatYuan(row.amount ?? 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="报销说明" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="reimbursementStatus(row.status).tag" size="small">
              {{ reimbursementStatus(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170">
          <template #default="{ row }">{{ row.createTime ? formatDate(row.createTime, 'YYYY-MM-DD HH:mm') : '—' }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && detailData.length === 0" description="暂无数据" :image-size="80" />

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { formatYuan, formatDate } from '@/utils/format'
import { reimbursementStatus } from '@/utils/status'
import { Download, Money, Tickets, Checked, DataAnalysis } from '@element-plus/icons-vue'
import { getReportStats, exportReport } from '@/api/report'
import { getReimbursementList } from '@/api/reimbursement'

interface RankItem {
  name: string
  amount: number
}

interface ExpenseType {
  name: string
  amount: number
}

const loading = ref(false)
const exporting = ref(false)

const query = reactive({
  page: 1,
  pageSize: 10,
  dateRange: null as [string, string] | null
})

const stats = ref({
  totalAmount: 0,
  totalCount: 0,
  approvalRate: 0,
  avgAmount: 0
})

const deptRanking = ref<RankItem[]>([])
const expenseTypes = ref<ExpenseType[]>([])
const allDetail = ref<any[]>([])

const typeColors = ['#5ac8fa', '#34c759', '#ff9500', '#4f46e5', '#aeaeb2']

const statCards = computed(() => [
  { label: '报销总额', value: '¥' + formatYuan(stats.value.totalAmount), icon: Money, color: '#10b981', grad: 'linear-gradient(135deg, #ecfdf5, #d1fae5)' },
  { label: '报销单数', value: String(stats.value.totalCount), icon: Tickets, color: '#3b82f6', grad: 'linear-gradient(135deg, #eff6ff, #dbeafe)' },
  { label: '审批通过率', value: stats.value.approvalRate + '%', icon: Checked, color: '#f59e0b', grad: 'linear-gradient(135deg, #fefce8, #fef3c7)' },
  { label: '平均单笔金额', value: '¥' + formatYuan(stats.value.avgAmount), icon: DataAnalysis, color: '#4f46e5', grad: 'linear-gradient(135deg, #eef2ff, #e0e7ff)' }
])

const maxDeptAmount = computed(() => Math.max(...deptRanking.value.map(d => d.amount), 1))

const computedExpenseTypes = computed(() => {
  const total = expenseTypes.value.reduce((s, t) => s + Number(t.amount || 0), 0) || 1
  return expenseTypes.value.map((t, i) => ({
    ...t,
    pct: Math.round(Number(t.amount || 0) / total * 100),
    color: typeColors[i % typeColors.length]
  }))
})

const loadStats = async () => {
  try {
    const [startDate, endDate] = query.dateRange || [null, null]
    const res: any = await getReportStats({ startDate: startDate || undefined, endDate: endDate || undefined })
    const d = res.data || {}
    stats.value.totalAmount = Number(d.totalAmount || 0)
    stats.value.totalCount = Number(d.totalCount || 0)
    stats.value.approvalRate = Number(d.approvalRate || 0)
    stats.value.avgAmount = Number(d.avgAmount || 0)
    deptRanking.value = (d.deptRanking || []).map((x: any) => ({ name: x.name, amount: Number(x.amount || 0) }))
    expenseTypes.value = (d.expenseTypes || []).map((x: any) => ({ name: x.name, amount: Number(x.amount || 0) }))
  } catch {
    /* 错误已由拦截器提示 */
  }
}

const detailData = computed(() => {
  const start = (query.page - 1) * query.pageSize
  return allDetail.value.slice(start, start + query.pageSize)
})

const total = computed(() => allDetail.value.length)

const fetchData = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = query.dateRange || [null, null]
    const res: any = await getReimbursementList({
      pageNum: 1,
      pageSize: 100000,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    // 明细与统计卡片口径一致：排除草稿
    allDetail.value = (res.data || []).filter((r: any) => r.status !== 0)
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  query.page = 1
  loadStats()
  fetchData()
}

const handleExport = async () => {
  exporting.value = true
  try {
    const [startDate, endDate] = query.dateRange || [null, null]
    const blob = await exportReport({ startDate: startDate || undefined, endDate: endDate || undefined })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const range = query.dateRange ? query.dateRange.join('至') : '全部'
    a.download = `报销明细报表_${range}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('报表已导出')
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadStats()
  fetchData()
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

// ===== 统计卡片 =====
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: $bg-card;
  border-radius: $radius-card;
  padding: 20px;
  border: 1px solid $border-color;
  box-shadow: $shadow-xs;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-3px);
    border-color: #cbd5e1;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  }
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}

.chart-row { margin-bottom: 20px; }

.section-title {
  font-size: $font-size-card-title;
  font-weight: $font-weight-card-title;
  color: $text-primary;
  margin-bottom: 16px;
}

// ---- 部门排行 ----
.dept-ranking {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rank-num {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: $text-secondary;
  flex-shrink: 0;

  &.top-1 { background: #ff9500; color: #fff; }
  &.top-2 { background: $color-accent; color: #fff; }
  &.top-3 { background: #5ac8fa; color: #fff; }
}

.rank-name {
  width: 60px;
  font-size: $font-size-body;
  color: $text-primary;
  flex-shrink: 0;
}

.rank-bar {
  flex: 1;
  height: 8px;
  background: #f5f5f7;
  border-radius: 4px;
  overflow: hidden;
}

.rank-fill {
  height: 100%;
  background: linear-gradient(90deg, $color-accent, #818cf8);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.rank-amount {
  width: 100px;
  text-align: right;
  font-size: $font-size-body;
  font-weight: 600;
  color: $text-primary;
  flex-shrink: 0;
}

// ---- 费用类型分布 ----
.type-distribution {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-name {
  width: 60px;
  flex-shrink: 0;
  font-size: $font-size-body;
  color: $text-primary;
}

.type-bar {
  flex: 1;
  height: 8px;
  background: #f5f5f7;
  border-radius: 4px;
  overflow: hidden;
}

.type-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.type-amount {
  width: 100px;
  text-align: right;
  flex-shrink: 0;
  font-size: $font-size-body;
  font-weight: 600;
  color: $text-primary;
}

.type-pct {
  width: 42px;
  text-align: right;
  flex-shrink: 0;
  font-size: $font-size-auxiliary;
  color: $text-secondary;
}

.amount-text {
  font-weight: 600;
  color: $color-accent;
}
</style>
