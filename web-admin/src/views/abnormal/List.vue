<template>
  <div class="page-container">
    <div class="flex-between" style="margin-bottom: 24px">
      <h1 class="page-title" style="margin-bottom: 0">异常预警</h1>
      <div class="header-actions">
        <el-button v-if="userStore.userInfo?.role === 4" size="large" @click="handleScan" :loading="scanning">
          <el-icon><Refresh /></el-icon>
          立即扫描昨日数据
        </el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="card-wrapper filter-bar">
      <el-form :model="query" inline>
        <el-form-item label="处理状态">
          <el-select v-model="query.handled" placeholder="全部" clearable style="width: 140px" @change="handleQuery">
            <el-option label="未处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
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
        <el-table-column prop="ruleName" label="规则" width="130">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">{{ row.ruleCode }} {{ row.ruleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="预警详情" min-width="320" show-overflow-tooltip />
        <el-table-column prop="createTime" label="扫描时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createTime, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.handled === 1 ? 'success' : 'danger'" size="small">
              {{ row.handled === 1 ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.handled === 0" text type="primary" size="small" @click="handleMark(row)">标记已处理</el-button>
            <span v-else class="handled-time">{{ row.handleTime ? formatDate(row.handleTime, 'MM-DD HH:mm') : '' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无预警记录" :image-size="80" />

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
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/format'
import { getAbnormalList, handleAbnormal, scanAbnormal } from '@/api/abnormal'

const userStore = useUserStore()

interface AbnormalRow {
  id: number
  ruleCode: string
  ruleName: string
  message: string
  handled: number
  handleTime: string | null
  createTime: string
}

const loading = ref(false)
const scanning = ref(false)
const total = ref(0)
const tableData = ref<AbnormalRow[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  handled: undefined as number | undefined
})

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getAbnormalList({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      handled: query.handled
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
const handleReset = () => { query.handled = undefined; query.pageNum = 1; fetchData() }

const handleMark = async (row: any) => {
  try {
    await handleAbnormal(row.id)
    ElMessage.success('已标记处理')
    fetchData()
  } catch {
    /* 错误已由拦截器提示 */
  }
}

const handleScan = async () => {
  scanning.value = true
  try {
    const res: any = await scanAbnormal()
    ElMessage.success(res.message || '扫描完成')
    fetchData()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    scanning.value = false
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.handled-time {
  font-size: $font-size-auxiliary;
  color: $text-secondary;
}
</style>
