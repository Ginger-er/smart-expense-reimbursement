<template>
  <div class="page-container">
    <div class="flex-between" style="margin-bottom: 16px">
      <h1 class="page-title" style="margin-bottom: 0">工作台</h1>
      <span class="role-hint">{{ roleLabel(role) }}</span>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="s in statCards" :key="s.label" @click="router.push(s.link)">
        <div class="stat-icon-wrap" :style="{ background: s.grad, color: s.color }">
          <el-icon :size="20"><component :is="s.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content-grid">
      <!-- 左：最近报销单 -->
      <div class="card-wrapper">
        <div class="flex-between" style="margin-bottom: 16px">
          <h3 class="section-title" style="margin-bottom: 0">最近报销单</h3>
          <el-button text type="primary" size="small" @click="router.push('/reimbursement')">
            查看全部 <el-icon :size="14" style="margin-left: 2px"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <el-table :data="recentList" stripe style="width: 100%">
          <el-table-column prop="orderNo" label="报销单号" width="150" />
          <el-table-column prop="applicantName" label="申请人" width="90" />
          <el-table-column prop="remark" label="报销说明" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">
              <span class="amount-text">&yen;{{ formatYuan(row.amount || 0) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="reimbursementStatus(row.status).tag" size="small" effect="plain">
                {{ reimbursementStatus(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="recentList.length === 0" description="暂无报销记录" :image-size="70" />
      </div>

      <!-- 右列 -->
      <div class="right-column">
        <!-- 快捷操作 -->
        <div class="card-wrapper">
          <h3 class="section-title">快捷操作</h3>
          <div class="quick-actions">
            <div class="quick-item" v-for="a in quickActions" :key="a.label" @click="router.push(a.link)">
              <div class="quick-icon" :style="{ background: a.bg, color: a.color }">
                <el-icon :size="18"><component :is="a.icon" /></el-icon>
              </div>
              <span>{{ a.label }}</span>
            </div>
          </div>
        </div>

        <!-- 待办提醒 -->
        <div class="card-wrapper">
          <h3 class="section-title">待办提醒</h3>
          <div class="todo-list">
            <div class="todo-item" v-for="t in todos" :key="t.id">
              <span class="todo-dot" :class="t.urgent ? 'urgent' : ''" />
              <span class="todo-text">{{ t.text }}</span>
            </div>
            <el-empty v-if="todos.length === 0" description="暂无待办" :image-size="60" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Clock, Money, Tickets, WarningFilled, User, OfficeBuilding,
  Plus, Upload, Promotion, Checked, ArrowRight
} from '@element-plus/icons-vue'
import { formatYuan } from '@/utils/format'
import { reimbursementStatus } from '@/utils/status'
import { getDashboardStats } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role ?? 1)

const roleLabel = (r: number) => ({ 1: '员工', 2: '领导', 3: '财务', 4: '管理员' } as Record<number, string>)[r] || '员工'

const stats = ref<any>({
  pendingApproval: 0,
  monthTotalAmount: 0,
  reimbursementCount: 0,
  invoiceCount: 0,
  myDraftCount: 0,
  myRejectedCount: 0,
  userCount: 0,
  deptCount: 0,
  recentReimbursements: []
})

const money = (v: any) => '¥' + formatYuan(Number(v || 0))

const statCards = computed(() => {
  const s = stats.value
  if (role.value === 1) {
    return [
      { label: '我的报销单', value: s.reimbursementCount, icon: Money, color: '#3b82f6', grad: 'linear-gradient(135deg, #eff6ff, #dbeafe)', link: '/reimbursement' },
      { label: '我的草稿', value: s.myDraftCount, icon: Clock, color: '#f59e0b', grad: 'linear-gradient(135deg, #fefce8, #fef3c7)', link: '/reimbursement' },
      { label: '被驳回', value: s.myRejectedCount, icon: WarningFilled, color: '#ef4444', grad: 'linear-gradient(135deg, #fef2f2, #fecaca)', link: '/reimbursement' },
      { label: '我的发票', value: s.invoiceCount, icon: Tickets, color: '#10b981', grad: 'linear-gradient(135deg, #ecfdf5, #d1fae5)', link: '/invoice' }
    ]
  }
  if (role.value === 2) {
    return [
      { label: '待我审批', value: s.pendingApproval, icon: Clock, color: '#3b82f6', grad: 'linear-gradient(135deg, #eff6ff, #dbeafe)', link: '/approval' },
      { label: '本月报销总额', value: money(s.monthTotalAmount), icon: Money, color: '#10b981', grad: 'linear-gradient(135deg, #ecfdf5, #d1fae5)', link: '/reimbursement' },
      { label: '部门报销单', value: s.reimbursementCount, icon: Tickets, color: '#f59e0b', grad: 'linear-gradient(135deg, #fefce8, #fef3c7)', link: '/reimbursement' },
      { label: '部门发票', value: s.invoiceCount, icon: Tickets, color: '#ef4444', grad: 'linear-gradient(135deg, #fef2f2, #fecaca)', link: '/invoice' }
    ]
  }
  if (role.value === 3) {
    return [
      { label: '待审批', value: s.pendingApproval, icon: Clock, color: '#3b82f6', grad: 'linear-gradient(135deg, #eff6ff, #dbeafe)', link: '/approval' },
      { label: '本月报销总额', value: money(s.monthTotalAmount), icon: Money, color: '#10b981', grad: 'linear-gradient(135deg, #ecfdf5, #d1fae5)', link: '/reimbursement' },
      { label: '报销单总数', value: s.reimbursementCount, icon: Tickets, color: '#f59e0b', grad: 'linear-gradient(135deg, #fefce8, #fef3c7)', link: '/reimbursement' },
      { label: '发票总数', value: s.invoiceCount, icon: Tickets, color: '#ef4444', grad: 'linear-gradient(135deg, #fef2f2, #fecaca)', link: '/invoice' }
    ]
  }
  return [
    { label: '待审批', value: s.pendingApproval, icon: Clock, color: '#3b82f6', grad: 'linear-gradient(135deg, #eff6ff, #dbeafe)', link: '/approval' },
    { label: '本月报销总额', value: money(s.monthTotalAmount), icon: Money, color: '#10b981', grad: 'linear-gradient(135deg, #ecfdf5, #d1fae5)', link: '/reimbursement' },
    { label: '用户数', value: s.userCount, icon: User, color: '#8b5cf6', grad: 'linear-gradient(135deg, #f5f3ff, #ede9fe)', link: '/system/user' },
    { label: '部门数', value: s.deptCount, icon: OfficeBuilding, color: '#16a34a', grad: 'linear-gradient(135deg, #f0fdf4, #dcfce7)', link: '/system/dept' }
  ]
})

const quickActions = computed(() => {
  const list = [
    { label: '新建报销', icon: Plus, bg: '#eff6ff', color: '#3b82f6', link: '/reimbursement/create' },
    { label: '上传发票', icon: Upload, bg: '#ecfdf5', color: '#10b981', link: '/invoice/upload' },
    { label: '出差申请', icon: Promotion, bg: '#fefce8', color: '#f59e0b', link: '/trip/create' }
  ]
  if (role.value >= 2) {
    list.push({ label: '审批中心', icon: Checked, bg: '#fef2f2', color: '#ef4444', link: '/approval' })
  }
  if (role.value === 4) {
    list.push({ label: '用户管理', icon: User, bg: '#f5f3ff', color: '#8b5cf6', link: '/system/user' })
    list.push({ label: '部门管理', icon: OfficeBuilding, bg: '#f0fdf4', color: '#16a34a', link: '/system/dept' })
  }
  return list
})

const todos = computed(() => {
  const list: { id: number; text: string; urgent: boolean }[] = []
  if (role.value >= 2 && stats.value.pendingApproval > 0) {
    list.push({ id: 1, text: `有 ${stats.value.pendingApproval} 条报销/出差待审批`, urgent: true })
  }
  if (role.value === 1 && stats.value.myDraftCount > 0) {
    list.push({ id: 2, text: `有 ${stats.value.myDraftCount} 条草稿待提交`, urgent: false })
  }
  if (role.value === 1 && stats.value.myRejectedCount > 0) {
    list.push({ id: 3, text: `有 ${stats.value.myRejectedCount} 条报销被驳回，请修改后重新提交`, urgent: true })
  }
  return list
})

const recentList = computed(() => stats.value.recentReimbursements || [])

const fetchData = async () => {
  try {
    const res: any = await getDashboardStats()
    stats.value = res.data || stats.value
  } catch {
    /* 错误已由拦截器提示 */
  }
}

onMounted(() => fetchData())
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.role-hint {
  font-size: 13px;
  color: $text-secondary;
  background: $bg-page;
  padding: 4px 12px;
  border-radius: 999px;
}

// ===== 统计卡片 =====
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
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
  cursor: pointer;
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

// ===== 主内容网格 =====
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.right-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.card-wrapper {
  min-width: 0;
}

.amount-text {
  font-weight: 600;
  color: $color-primary;
}

// ===== 快捷操作 =====
.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 10px;
  background: $bg-page;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: $text-regular;
  transition: background 0.2s ease;

  &:hover {
    background: #e9edf3;
  }
}

.quick-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

// ===== 待办列表 =====
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid $border-light;

  &:last-child {
    border-bottom: none;
  }
}

.todo-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;

  &.urgent {
    background: $color-danger;
    box-shadow: 0 0 0 3px rgba($color-danger, 0.15);
  }
}

.todo-text {
  flex: 1;
  font-size: 13px;
  color: $text-regular;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// ===== 响应式 =====
@media (max-width: 1366px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
}
</style>
