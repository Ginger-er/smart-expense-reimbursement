<template>
  <div class="page-container">
    <h1 class="page-title">审批中心</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" style="margin-bottom: 16px">
      <el-tab-pane label="待审批" name="pending">
        <template #label>
          <span>
            待审批
            <el-badge :value="pendingCount" :hidden="pendingCount === 0" class="tab-badge" />
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="已审批" name="processed" />
    </el-tabs>

    <!-- 筛选 -->
    <div class="card-wrapper filter-bar">
      <el-form :model="query" inline>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px" @change="handleQuery">
            <el-option label="报销审批" value="REIMBURSEMENT" />
            <el-option label="出差审批" value="TRIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="单号 / 申请人" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="card-wrapper">
      <el-table :data="pageData" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="编号" width="180" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'REIMBURSEMENT' ? 'primary' : 'success'" size="small" effect="plain">
              {{ row.type === 'REIMBURSEMENT' ? '报销' : '出差' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="90" />
        <el-table-column prop="deptName" label="部门" width="100" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="130">
          <template #default="{ row }">
            <span class="amount-text">&yen;{{ formatYuan(row.amount ?? 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'processed'" prop="status" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 3 ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.status === 3 ? '通过' : '驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="130">
          <template #default="{ row }">{{ formatDate(row.submitTime, 'MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="activeTab === 'pending' && canApprove(row)" type="primary" size="small" @click="handleApprove(row)">
              审批
            </el-button>
            <el-button v-else-if="activeTab === 'pending'" text type="primary" size="small" @click="handleView(row)">
              查看
            </el-button>
            <template v-else>
              <el-button text type="primary" size="small" @click="handleView(row)">查看</el-button>
              <el-popconfirm
                v-if="row.type === 'REIMBURSEMENT' && row.status === 3 && (userStore.userInfo?.role === 3 || userStore.userInfo?.role === 4)"
                title="确认打款？"
                @confirm="handlePayApproval(row)"
              >
                <template #reference>
                  <el-button text type="success" size="small">打款</el-button>
                </template>
              </el-popconfirm>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && pageData.length === 0" description="暂无数据" :image-size="80" />

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
      />
    </div>

    <!-- 出差审批弹窗 -->
    <el-dialog v-model="tripApprovalVisible" title="出差审批" width="480px">
      <div v-if="currentTrip" class="trip-approval-box">
        <div class="ta-row"><span class="ta-label">单号</span><span class="ta-value">{{ currentTrip.orderNo }}</span></div>
        <div class="ta-row"><span class="ta-label">申请人</span><span class="ta-value">{{ currentTrip.applicantName }} / {{ currentTrip.deptName }}</span></div>
        <div class="ta-row"><span class="ta-label">出差事由</span><span class="ta-value">{{ currentTrip.title }}</span></div>
        <div class="ta-row"><span class="ta-label">预算金额</span><span class="ta-value ta-amount">&yen;{{ formatYuan(currentTrip.amount ?? 0) }}</span></div>
        <div class="ta-comment">
          <el-input v-model="tripApprovalComment" type="textarea" :rows="3" placeholder="请输入审批意见（驳回时必填）" />
        </div>
      </div>
      <template #footer>
        <el-button @click="tripApprovalVisible = false">取消</el-button>
        <el-button type="success" :loading="tripApproving" @click="approveTripAction(1)">通过</el-button>
        <el-button type="danger" :loading="tripRejecting" @click="approveTripAction(2)">驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { formatYuan, formatDate } from '@/utils/format'
import { getPendingApprovals, getProcessedApprovals, type ApprovalItem } from '@/api/approval'
import { approveTrip } from '@/api/trip'
import { payReimbursement } from '@/api/reimbursement'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'pending' | 'processed'>('pending')
const loading = ref(false)
const pendingCount = ref(0)
const allItems = ref<ApprovalItem[]>([])

const query = reactive({ page: 1, pageSize: 10, type: '', keyword: '' })

const filtered = computed(() => {
  let list = allItems.value
  if (query.type) list = list.filter((i) => i.type === query.type)
  return list
})

const total = computed(() => filtered.value.length)

const pageData = computed(() => {
  const start = (query.page - 1) * query.pageSize
  return filtered.value.slice(start, start + query.pageSize)
})

const fetchData = async () => {
  loading.value = true
  try {
    const fn = activeTab.value === 'pending' ? getPendingApprovals : getProcessedApprovals
    allItems.value = await fn({ keyword: query.keyword })
    if (activeTab.value === 'pending') pendingCount.value = allItems.value.length
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  query.page = 1
  fetchData()
}

const handleQuery = () => {
  query.page = 1
  fetchData()
}

// 当前用户是否可审批该行：不能审自己的单子；二级审批(状态2)只能财务/管理员操作
const canApprove = (row: any) => {
  const role = userStore.userInfo?.role ?? 1
  if (row.userId === userStore.userInfo?.id) {
    return false
  }
  if (row.type === 'REIMBURSEMENT' && row.status === 2 && role === 2) {
    return false
  }
  return true
}

const handleApprove = (row: any) => {
  if (row.type === 'REIMBURSEMENT') {
    router.push(`/reimbursement/detail/${row.id}`)
  } else {
    currentTrip.value = row
    tripApprovalComment.value = ''
    tripApprovalVisible.value = true
  }
}

const handleView = (row: any) => {
  if (row.type === 'REIMBURSEMENT') {
    router.push(`/reimbursement/detail/${row.id}`)
  } else {
    router.push(`/trip/detail/${row.id}`)
  }
}

const handlePayApproval = async (row: any) => {
  try {
    await payReimbursement(row.id)
    ElMessage.success('打款成功')
    fetchData()
  } catch {
    /* 错误已由拦截器提示 */
  }
}

// 出差审批
const tripApprovalVisible = ref(false)
const currentTrip = ref<ApprovalItem | null>(null)
const tripApprovalComment = ref('')
const tripApproving = ref(false)
const tripRejecting = ref(false)

const approveTripAction = async (action: number) => {
  if (!currentTrip.value) return
  if (action === 2 && !tripApprovalComment.value.trim()) {
    ElMessage.warning('驳回时请输入审批意见')
    return
  }
  const flag = action === 1 ? tripApproving : tripRejecting
  flag.value = true
  try {
    await approveTrip(currentTrip.value.id, action, tripApprovalComment.value.trim())
    ElMessage.success(action === 1 ? '已通过' : '已驳回')
    tripApprovalVisible.value = false
    fetchData()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    flag.value = false
  }
}

onMounted(() => fetchData())
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.filter-bar {
  margin-bottom: $spacing-card-gap;
  padding: 16px 24px;
}

.tab-badge {
  margin-left: 6px;
  :deep(.el-badge__content) {
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    padding: 0 5px;
  }
}

.amount-text {
  font-weight: 600;
  color: $color-primary;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

.trip-approval-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: $font-size-body;
}

.ta-label {
  width: 70px;
  flex-shrink: 0;
  color: $text-secondary;
  font-size: 13px;
}

.ta-value {
  color: $text-primary;
  word-break: break-all;
}

.ta-amount {
  font-weight: 600;
  color: $color-primary;
}

.ta-comment {
  margin-top: 4px;
}
</style>
