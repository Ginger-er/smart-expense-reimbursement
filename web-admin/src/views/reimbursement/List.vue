<template>
  <div class="page-container">
    <div class="flex-between" style="margin-bottom: 24px">
      <h1 class="page-title" style="margin-bottom: 0">报销管理</h1>
      <div class="header-actions">
        <el-button size="large" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出 Excel
        </el-button>
        <el-button type="primary" size="large" @click="router.push('/reimbursement/create')">
          <el-icon><Plus /></el-icon>
          创建报销单
        </el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="card-wrapper filter-bar">
      <el-form :model="query" inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px" @change="handleQuery">
            <el-option label="草稿" :value="0" />
            <el-option label="待审批" :value="1" />
            <el-option label="审批中" :value="2" />
            <el-option label="已通过" :value="3" />
            <el-option label="已驳回" :value="4" />
            <el-option label="已打款" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="单号 / 申请人" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="card-wrapper">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="selection" width="46" />
        <el-table-column prop="orderNo" label="报销单号" width="160" />
        <el-table-column prop="applicantName" label="申请人" width="90" />
        <el-table-column prop="deptName" label="部门" width="100" />
        <el-table-column prop="amount" label="金额" width="130" sortable>
          <template #default="{ row }">
            <span class="amount-text">&yen;{{ formatYuan(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="reimbursementStatus(row.status).tag" size="small">
              {{ reimbursementStatus(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="150" sortable>
          <template #default="{ row }">{{ formatDate(row.createTime, 'MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button text type="primary" size="small" @click="router.push(`/reimbursement/detail/${row.id}`)">详情</el-button>
              <el-popconfirm v-if="row.status === 0 && (row.userId === userStore.userInfo?.id || userStore.userInfo?.role === 4)" title="确认删除？" @confirm="handleDelete(row)">
                <template #reference>
                  <el-button text type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
              <el-popconfirm v-if="row.status === 3 && (userStore.userInfo?.role === 3 || userStore.userInfo?.role === 4)" title="确认打款？" @confirm="handlePay(row)">
                <template #reference>
                  <el-button text type="success" size="small">打款</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" :image-size="80" />

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { formatYuan, formatDate } from '@/utils/format'
import { reimbursementStatus } from '@/utils/status'
import { Plus, Download } from '@element-plus/icons-vue'
import { getReimbursementList, deleteReimbursement, payReimbursement, exportReimbursement } from '@/api/reimbursement'

const router = useRouter()
const userStore = useUserStore()

interface Row {
  id: number
  userId: number
  orderNo: string
  applicantName: string
  deptName: string
  amount: number
  status: number
  createTime: string
}

const loading = ref(false)
const total = ref(0)
const tableData = ref<Row[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined as number | undefined,
  keyword: '',
  dateRange: null as [string, string] | null
})

const fetchData = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = query.dateRange || [null, null]
    const res: any = await getReimbursementList({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status,
      keyword: query.keyword || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    tableData.value = res.data || []
    total.value = res.total || 0
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

const handleQuery = () => { query.pageNum = 1; fetchData() }
const handleReset = () => { query.status = undefined; query.keyword = ''; query.dateRange = null; query.pageNum = 1; fetchData() }
const handleDelete = async (row: any) => {
  try {
    await deleteReimbursement(row.id)
    ElMessage.success('已删除')
    fetchData()
  } catch {
    /* 错误已由拦截器提示（如非草稿不可删除） */
  }
}

const handlePay = async (row: any) => {
  try {
    await payReimbursement(row.id)
    ElMessage.success('打款成功')
    fetchData()
  } catch {
    /* 错误已由拦截器提示（如非已通过不可打款） */
  }
}

const handleExport = async () => {
  try {
    const [startDate, endDate] = query.dateRange || [null, null]
    const blob = await exportReimbursement({
      status: query.status,
      keyword: query.keyword || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '报销单导出.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    /* 错误已由拦截器提示 */
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

:deep(.el-form-item) {
  margin-bottom: 0;
}

.op-btns {
  display: flex;
  align-items: center;
  gap: 2px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
