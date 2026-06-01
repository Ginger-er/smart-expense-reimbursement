<template>
  <div class="page-container">
    <div class="flex-between" style="margin-bottom: 16px">
      <h1 class="page-title" style="margin-bottom: 0">出差申请</h1>
      <el-button type="primary" @click="$router.push('/trip/create')">
        <el-icon><Plus /></el-icon>
        新建申请
      </el-button>
    </div>

    <div class="card-wrapper card-gap">
      <el-form :model="query" inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="已提交" :value="1" />
            <el-option label="已通过" :value="3" />
            <el-option label="已驳回" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="目的地/申请人" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card-wrapper">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="部门" width="120" />
        <el-table-column prop="destination" label="目的地" width="120" />
        <el-table-column prop="reason" label="出差事由" min-width="160" show-overflow-tooltip />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="budget" label="预算" width="130">
          <template #default="{ row }">
            <span class="amount-text">&yen;{{ formatYuan(row.budget) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tripStatus(row.status).tag" size="small">
              {{ tripStatus(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button text type="primary" size="small" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.status === 0 && (row.userId === userStore.userInfo?.id || userStore.userInfo?.role === 4)" text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="row.status === 0 && (row.userId === userStore.userInfo?.id || userStore.userInfo?.role === 4)" text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatYuan, formatDate } from '@/utils/format'
import { tripStatus } from '@/utils/status'
import { Plus } from '@element-plus/icons-vue'
import { getTripList, deleteTrip } from '@/api/trip'

const router = useRouter()
const userStore = useUserStore()

interface Row {
  id: number
  userId: number
  tripNo: string
  applicantName: string
  deptName: string
  destination: string
  reason: string
  startDate: string
  endDate: string
  budget: number
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
  keyword: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getTripList({ pageNum: query.pageNum, pageSize: query.pageSize, status: query.status, keyword: query.keyword || undefined })
    tableData.value = res.data || []
    total.value = res.total || 0
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

const handleQuery = () => { query.pageNum = 1; fetchData() }
const handleReset = () => { query.status = undefined; query.keyword = ''; query.pageNum = 1; fetchData() }

const handleView = (row: any) => {
  router.push(`/trip/detail/${row.id}`)
}

const handleEdit = (row: any) => {
  router.push(`/trip/create?id=${row.id}`)
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  try {
    await deleteTrip(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    /* 错误已由拦截器提示（如非草稿不可删除） */
  }
}

onMounted(() => fetchData())
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
.amount-text { font-weight: 600; color: $color-primary; }

.op-btns {
  display: flex;
  align-items: center;
  gap: 2px;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
</style>
