<template>
  <div class="page-container">
    <div class="flex-between" style="margin-bottom: 16px">
      <h1 class="page-title" style="margin-bottom: 0">发票管理</h1>
      <el-button type="primary" @click="$router.push('/invoice/upload')">
        <el-icon><Plus /></el-icon>
        上传发票
      </el-button>
    </div>

    <!-- 筛选区 -->
    <div class="card-wrapper card-gap">
      <el-form :model="query" inline>
        <el-form-item label="识别状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="handleQuery">
            <el-option label="待识别" :value="0" />
            <el-option label="识别成功" :value="1" />
            <el-option label="识别失败" :value="2" />
            <el-option label="人工修正" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="发票类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px" @change="handleQuery">
            <el-option label="交通" :value="1" />
            <el-option label="住宿" :value="2" />
            <el-option label="餐饮" :value="3" />
            <el-option label="其他" :value="4" />
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
      <el-table :data="pageData" v-loading="loading" style="width: 100%">
        <el-table-column prop="invoiceNo" label="发票号码" width="150">
          <template #default="{ row }">{{ row.invoiceNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="invoiceCode" label="发票代码" width="140">
          <template #default="{ row }">{{ row.invoiceCode || '—' }}</template>
        </el-table-column>
        <el-table-column prop="type" label="发票类型" width="90">
          <template #default="{ row }">
            <el-tag :type="invoiceType(row.type).tag" size="small" effect="plain">{{ invoiceType(row.type).label || '其他' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sellerName" label="销售方" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sellerName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span class="amount-text">{{ row.amount != null ? '&yen;' + formatYuan(row.amount) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceDate" label="开票日期" width="110">
          <template #default="{ row }">{{ row.invoiceDate || '—' }}</template>
        </el-table-column>
        <el-table-column prop="ocrStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="invoiceOcrStatus(row.ocrStatus).tag" size="small">{{ invoiceOcrStatus(row.ocrStatus).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button text type="primary" size="small" @click="handleView(row)">查看</el-button>
              <el-button v-if="row.userId === userStore.userInfo?.id || userStore.userInfo?.role === 4" text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && pageData.length === 0" description="暂无发票" :image-size="80" />

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
      />
    </div>

    <!-- 发票详情对话框 -->
    <el-dialog v-model="detailVisible" title="发票详情" width="520px">
      <div v-if="currentInvoice" class="invoice-detail">
        <div class="detail-row"><span class="detail-label">发票号码</span><span class="detail-value">{{ currentInvoice.invoiceNo || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">发票代码</span><span class="detail-value">{{ currentInvoice.invoiceCode || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">发票类型</span><span class="detail-value">{{ invoiceType(currentInvoice.type).label || '其他' }}</span></div>
        <div class="detail-row"><span class="detail-label">销售方</span><span class="detail-value">{{ currentInvoice.sellerName || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">购买方</span><span class="detail-value">{{ currentInvoice.buyerName || '—' }}</span></div>
        <div class="detail-row"><span class="detail-label">金额</span><span class="detail-value">&yen;{{ formatYuan(currentInvoice.amount || 0) }}</span></div>
        <div class="detail-row"><span class="detail-label">税额</span><span class="detail-value">&yen;{{ formatYuan(currentInvoice.taxAmount || 0) }}</span></div>
        <div class="detail-row"><span class="detail-label">开票日期</span><span class="detail-value">{{ currentInvoice.invoiceDate || '—' }}</span></div>
        <div class="detail-row">
          <span class="detail-label">识别状态</span>
          <el-tag :type="invoiceOcrStatus(currentInvoice.ocrStatus).tag" size="small" effect="plain">{{ invoiceOcrStatus(currentInvoice.ocrStatus).label }}</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatYuan } from '@/utils/format'
import { invoiceType, invoiceOcrStatus } from '@/utils/status'
import { useUserStore } from '@/stores/user'
import { Plus } from '@element-plus/icons-vue'
import { getInvoiceList, deleteInvoice } from '@/api/invoice'

interface InvoiceRow {
  id: number
  userId: number
  invoiceNo: string
  invoiceCode: string
  amount: number
  taxAmount: number
  invoiceDate: string
  type: number
  sellerName: string
  buyerName: string
  ocrStatus: number
}

const userStore = useUserStore()

const loading = ref(false)
const allItems = ref<InvoiceRow[]>([])

const query = reactive({
  page: 1,
  pageSize: 10,
  status: undefined as number | undefined,
  type: undefined as number | undefined
})

const filtered = computed(() => {
  let list = allItems.value
  if (query.status != null) list = list.filter((i) => i.ocrStatus === query.status)
  if (query.type != null) list = list.filter((i) => i.type === query.type)
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
    const res: any = await getInvoiceList({})
    allItems.value = res.data || []
    // 详情弹窗展示的是列表对象的引用，列表刷新后按 id 同步最新数据，
    // 否则弹窗里的"识别中"状态永远不变
    if (currentInvoice.value) {
      const fresh = allItems.value.find((i) => i.id === currentInvoice.value!.id)
      if (fresh) {
        currentInvoice.value = fresh
      }
    }
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  query.page = 1
}
const handleReset = () => {
  query.status = undefined
  query.type = undefined
  query.page = 1
}

const detailVisible = ref(false)
const currentInvoice = ref<InvoiceRow | null>(null)
const handleView = (row: any) => {
  currentInvoice.value = row
  detailVisible.value = true
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该发票？', '提示', { type: 'warning' })
  try {
    await deleteInvoice(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    /* 错误已由拦截器提示 */
  }
}

// 有识别中的发票时自动刷新，识别全部完成后停止
let refreshTimer: number | undefined

const stopAutoRefresh = () => {
  if (refreshTimer != null) {
    clearInterval(refreshTimer)
    refreshTimer = undefined
  }
}

const startAutoRefresh = () => {
  if (refreshTimer != null) return
  refreshTimer = window.setInterval(() => {
    if (allItems.value.some((i) => i.ocrStatus === 0)) {
      fetchData()
    } else {
      stopAutoRefresh()
    }
  }, 3000)
}

onMounted(async () => {
  await fetchData()
  startAutoRefresh()
})

onBeforeUnmount(() => stopAutoRefresh())
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.amount-text {
  font-weight: 600;
  color: $color-primary;
}

.op-btns {
  display: flex;
  align-items: center;
  gap: 2px;
}

.invoice-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid $border-light;
}

.detail-label {
  font-size: 13px;
  color: $text-secondary;
}

.detail-value {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  text-align: right;
}
</style>
