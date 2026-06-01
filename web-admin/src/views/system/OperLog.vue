<template>
  <div class="page-container">
    <div class="flex-between" style="margin-bottom: 16px">
      <h1 class="page-title" style="margin-bottom: 0">操作日志</h1>
      <span class="page-tip">记录系统关键操作，含操作人、IP、耗时与结果</span>
    </div>

    <div class="card-wrapper">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="title" label="操作" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.title }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="方式" width="70" align="center" />
        <el-table-column prop="requestUrl" label="接口" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ip" label="来源IP" width="130" />
        <el-table-column prop="costMs" label="耗时" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.costMs > 1000 ? 'cost-slow' : ''">{{ row.costMs }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="170">
          <template #default="{ row }">{{ row.createTime ? formatDate(row.createTime, 'YYYY-MM-DD HH:mm') : '—' }}</template>
        </el-table-column>
        <el-table-column label="参数" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="params-text">{{ row.requestParams || '—' }}</span>
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
import { getOperLogList } from '@/api/operlog'
import { formatDate } from '@/utils/format'

interface OperLogRow {
  id: number
  username: string
  title: string
  requestMethod: string
  requestUrl: string
  requestParams: string
  ip: string
  status: number
  errorMsg: string
  costMs: number
  createTime: string
}

const loading = ref(false)
const total = ref(0)
const tableData = ref<OperLogRow[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10
})

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getOperLogList({
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    tableData.value = res.data || []
    total.value = res.total || 0
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchData())
</script>

<style lang="scss" scoped>
.page-tip {
  font-size: 13px;
  color: #8a8a94;
}

.params-text {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12px;
  color: #6b7280;
}

.cost-slow {
  color: #e6a23c;
  font-weight: 600;
}
</style>
