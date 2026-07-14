<template>
  <div class="page-container">
    <h1 class="page-title">上传发票</h1>

    <div class="upload-card">
      <!-- 关联报销单 -->
      <div class="card-wrapper card-gap">
        <el-form inline>
          <el-form-item label="关联报销单（可选）">
            <el-select v-model="reimbursementId" placeholder="选择草稿报销单，可稍后在发票列表关联" clearable filterable style="width: 320px">
              <el-option v-for="r in reimburseOptions" :key="r.id" :label="r.label" :value="r.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- 拖拽上传区域 -->
      <el-upload
        class="upload-area"
        drag
        multiple
        :auto-upload="false"
        :show-file-list="false"
        :accept="'.jpg,.jpeg,.png,.pdf'"
        :on-change="handleFileChange"
      >
        <div class="upload-placeholder">
          <el-icon :size="48" color="#aeaeb2"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p class="upload-title">将发票文件拖到此处，或<em>点击上传</em></p>
            <p class="upload-tip">支持 JPG、PNG、PDF 格式，单张不超过 10MB，上传后自动 OCR 识别</p>
          </div>
        </div>
      </el-upload>

      <!-- 已上传列表 -->
      <div class="uploaded-list" v-if="uploadedItems.length > 0">
        <h3 class="section-title">已上传发票（{{ uploadedItems.length }}）</h3>
        <div
          v-for="item in uploadedItems"
          :key="item.key"
          class="invoice-item"
        >
          <div class="item-thumbnail">
            <img :src="item.thumbUrl" alt="发票缩略图" />
          </div>
          <div class="item-info">
            <div class="item-type">{{ item.ocrStatus === 0 ? '识别中…' : (invoiceType(item.type).label || '其他') }}</div>
            <div class="item-amount" v-if="item.amount != null">&yen;{{ formatYuan(item.amount) }}</div>
            <div class="item-code" v-if="item.invoiceNo">{{ item.invoiceNo }}</div>
          </div>
          <div class="item-status">
            <span v-if="item.ocrStatus === 0" class="status-recognizing">
              <el-icon :size="16" class="is-loading"><Loading /></el-icon>
              识别中
            </span>
            <span v-else-if="item.ocrStatus === 1" class="status-success">
              <el-icon :size="16"><CircleCheckFilled /></el-icon>
              识别成功
            </span>
            <span v-else-if="item.ocrStatus === 2" class="status-failed">
              <el-icon :size="16"><CircleCloseFilled /></el-icon>
              识别失败
            </span>
            <span v-else-if="item.ocrStatus === 3" class="status-abnormal">
              <el-icon :size="16"><CircleCheckFilled /></el-icon>
              人工修正
            </span>
            <div v-if="item.ocrStatus === 0" class="item-poll-debug">
              发票id={{ item.id ?? '?' }} · 已轮询{{ item.pollCount ?? 0 }}次 · {{ item.lastPollResult || '等待第一次轮询…' }}
            </div>
          </div>
          <div class="item-actions">
            <el-button v-if="item.ocrStatus === 0" size="small" @click="refreshItem(item)">立即刷新</el-button>
            <el-button v-if="item.ocrStatus === 2" size="small" @click="manualFill(item)">手动填写</el-button>
            <el-button size="small" text type="danger" @click="removeItem(item)">删除</el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="upload-empty" v-if="uploadedItems.length === 0">
        <p class="empty-text">尚未上传发票，请拖拽文件或点击上方区域上传</p>
      </div>

      <!-- 底部按钮 -->
      <div class="upload-footer">
        <el-button @click="$router.back()">返回</el-button>
        <el-button type="primary" @click="handleDone">完成</el-button>
      </div>
    </div>

    <!-- 手动填写弹窗 -->
    <el-dialog v-model="manualVisible" title="手动填写发票信息" width="480px">
      <el-form v-if="manualItem" label-width="90px">
        <el-form-item label="发票号码">
          <el-input v-model="manualForm.invoiceNo" placeholder="请输入发票号码" />
        </el-form-item>
        <el-form-item label="发票类型">
          <el-select v-model="manualForm.type" style="width: 100%">
            <el-option label="交通" :value="1" />
            <el-option label="住宿" :value="2" />
            <el-option label="餐饮" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额（元）">
          <el-input-number v-model="manualForm.amount" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="销售方">
          <el-input v-model="manualForm.sellerName" placeholder="请输入销售方名称" />
        </el-form-item>
        <el-form-item label="开票日期">
          <el-date-picker v-model="manualForm.invoiceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualLoading" @click="confirmManual">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { formatYuan } from '@/utils/format'
import { invoiceType } from '@/utils/status'
import { UploadFilled, Loading, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { uploadInvoice, confirmInvoice, getInvoiceDetail } from '@/api/invoice'
import { getReimbursementList } from '@/api/reimbursement'

interface UploadedItem {
  key: number
  id?: number
  fileName: string
  thumbUrl: string
  invoiceNo?: string
  type?: number
  amount?: number
  ocrStatus: number
  pollCount?: number
  lastPollResult?: string
}

const uploading = ref(false)
const uploadedItems = ref<UploadedItem[]>([])
const reimbursementId = ref<number>()
const reimburseOptions = ref<{ id: number; label: string }[]>([])
let nextKey = 1

const loadReimburseOptions = async () => {
  try {
    // 后端分页上限 100 条/页，循环翻页取全部草稿，避免下拉漏单
    const all: any[] = []
    let pageNum = 1
    while (true) {
      const res: any = await getReimbursementList({ pageNum, pageSize: 100, status: 0 })
      const list = (res.data || []) as any[]
      all.push(...list)
      if (list.length < 100) {
        break
      }
      pageNum++
    }
    reimburseOptions.value = all.map((r: any) => ({ id: r.id, label: `${r.applicantName} · ${r.remark || '报销单'} · ¥${formatYuan(r.amount || 0)} · ${r.orderNo}` }))
  } catch {
    /* 忽略 */
  }
}

const handleFileChange = async (file: UploadFile) => {
  if (file.size && file.size > 10 * 1024 * 1024) {
    ElMessage.warning(`文件 ${file.name} 超过 10MB 限制`)
    return
  }
  const raw = file.raw
  if (!raw) return

  const item: UploadedItem = {
    key: nextKey++,
    fileName: file.name,
    thumbUrl: URL.createObjectURL(raw),
    ocrStatus: 0
  }
  uploadedItems.value.push(item)
  uploading.value = true

  try {
    const res: any = await uploadInvoice(raw, { reimbursementId: reimbursementId.value })
    const inv = res.data
    item.id = inv.id
    item.ocrStatus = inv.ocrStatus ?? 0
    // 上传即返回，OCR 在后台异步识别，这里轮询刷新结果
    pollOcrStatus(item)
  } catch {
    item.ocrStatus = 2
  } finally {
    uploading.value = false
  }
}

const removeItem = (item: UploadedItem) => {
  const idx = uploadedItems.value.findIndex((i) => i.key === item.key)
  if (idx > -1) {
    URL.revokeObjectURL(item.thumbUrl)
    uploadedItems.value.splice(idx, 1)
  }
  stopPoll(item)
}

// 轮询 OCR 识别结果：最多 15 次（约 30 秒），期间瞬时错误不中断轮询；
// 超时后标记为失败并展示「手动填写」入口，避免永远卡在"识别中"
const MAX_POLL_ATTEMPTS = 15
const pollTimers = new Map<number, number>()
const pollAttempts = new Map<number, number>()

const stopPoll = (item: UploadedItem) => {
  const timer = pollTimers.get(item.key)
  if (timer != null) {
    clearInterval(timer)
    pollTimers.delete(item.key)
  }
  pollAttempts.delete(item.key)
}

/** 拉取一次发票详情并更新条目（轮询与「立即刷新」按钮共用） */
const refreshItemOnce = async (item: UploadedItem) => {
  try {
    const res: any = await getInvoiceDetail(item.id!)
    const inv = res.data
    item.invoiceNo = inv.invoiceNo
    item.type = inv.type
    item.amount = inv.amount
    item.ocrStatus = inv.ocrStatus
    item.lastPollResult = `上次请求 ${new Date().toLocaleTimeString()} → 服务端状态=${inv.ocrStatus}`
    if (inv.ocrStatus !== 0) stopPoll(item)
    return inv
  } catch (e: any) {
    item.lastPollResult = `上次请求 ${new Date().toLocaleTimeString()} → 失败:${e?.message || '未知错误'}`
    return null
  }
}

const pollOcrStatus = (item: UploadedItem) => {
  pollAttempts.set(item.key, 0)
  const timer = window.setInterval(async () => {
    const attempts = pollAttempts.get(item.key) ?? 0
    if (attempts >= MAX_POLL_ATTEMPTS) {
      // 超时兜底：标记为识别失败，用户可以手动填写
      item.ocrStatus = 2
      item.lastPollResult = `轮询 ${MAX_POLL_ATTEMPTS} 次仍未完成，已标记失败`
      stopPoll(item)
      return
    }
    pollAttempts.set(item.key, attempts + 1)
    item.pollCount = attempts + 1
    await refreshItemOnce(item)
  }, 2000)
  pollTimers.set(item.key, timer)
}

/** 「立即刷新」按钮：手动拉取一次最新状态 */
const refreshItem = async (item: UploadedItem) => {
  await refreshItemOnce(item)
}

// 手动填写
const manualVisible = ref(false)
const manualLoading = ref(false)
const manualItem = ref<UploadedItem | null>(null)
const manualForm = ref({
  invoiceNo: '',
  type: 4,
  amount: 0,
  sellerName: '',
  invoiceDate: ''
})

const manualFill = (item: UploadedItem) => {
  manualItem.value = item
  manualForm.value = {
    invoiceNo: item.invoiceNo || '',
    type: item.type || 4,
    amount: item.amount || 0,
    sellerName: '',
    invoiceDate: ''
  }
  manualVisible.value = true
}

const confirmManual = async () => {
  if (!manualItem.value?.id) return
  manualLoading.value = true
  try {
    const res: any = await confirmInvoice({
      id: manualItem.value.id,
      invoiceNo: manualForm.value.invoiceNo,
      type: manualForm.value.type,
      amount: manualForm.value.amount,
      sellerName: manualForm.value.sellerName,
      invoiceDate: manualForm.value.invoiceDate || null
    })
    const inv = res.data
    const item = uploadedItems.value.find((i) => i.key === manualItem.value!.key)
    if (item) {
      item.invoiceNo = inv.invoiceNo
      item.type = inv.type
      item.amount = inv.amount
      item.ocrStatus = inv.ocrStatus
    }
    ElMessage.success('已保存')
    manualVisible.value = false
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    manualLoading.value = false
  }
}

const router = useRouter()

// 点击完成：若有发票还在识别中，先提醒用户——跳转后可在「发票管理」页继续查看结果
const handleDone = () => {
  const recognizing = uploadedItems.value.filter((i) => i.ocrStatus === 0).length
  if (recognizing > 0) {
    ElMessageBox.confirm(
      `还有 ${recognizing} 张发票正在识别中。跳转后可在「发票管理」页继续查看识别结果，该页会自动刷新识别状态。`,
      '还有发票在识别中',
      { type: 'warning', confirmButtonText: '仍然离开', cancelButtonText: '留下继续等' }
    )
      .then(() => router.push('/invoice'))
      .catch(() => {})
  } else {
    router.push('/invoice')
  }
}

onMounted(() => loadReimburseOptions())

onBeforeUnmount(() => {
  pollTimers.forEach((t) => clearInterval(t))
  pollTimers.clear()
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.upload-card {
  max-width: 680px;
  margin: 0 auto;
}

.card-wrapper {
  margin-bottom: $spacing-card-gap;
}

.upload-area {
  width: 100%;

  :deep(.el-upload-dragger) {
    padding: 40px;
    border-radius: $radius-card;
    border: 2px dashed $border-color;
    background: $bg-white;
    transition: border-color 0.3s;

    &:hover {
      border-color: $color-primary;
    }
  }
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.upload-text {
  text-align: center;

  .upload-title {
    font-size: $font-size-card-title;
    color: $text-regular;
    margin-bottom: 8px;

    em {
      color: $color-primary;
      font-style: normal;
      cursor: pointer;
    }
  }

  .upload-tip {
    font-size: $font-size-auxiliary;
    color: $text-placeholder;
  }
}

.uploaded-list {
  margin-top: 24px;
}

.section-title {
  font-size: $font-size-card-title;
  font-weight: $font-weight-card-title;
  color: $text-primary;
  margin-bottom: 12px;
}

.invoice-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: $bg-white;
  border-radius: 8px;
  border: 1px solid $border-light;
  margin-bottom: 8px;
}

.item-thumbnail {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background: $bg-page;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.item-info {
  flex: 1;
  min-width: 0;

  .item-type {
    font-size: $font-size-body;
    color: $text-primary;
    font-weight: 500;
  }

  .item-amount {
    font-size: 16px;
    font-weight: 600;
    color: $color-primary;
    margin-top: 2px;
  }

  .item-code {
    font-size: $font-size-auxiliary;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.item-status {
  flex-shrink: 0;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: $font-size-auxiliary;
  }

  .status-recognizing { color: $color-info; }
  .status-success { color: $color-success; }
  .status-failed { color: $color-danger; }
  .status-abnormal { color: $color-warning; }

  .item-poll-debug {
    margin-top: 4px;
    font-size: 12px;
    color: $text-secondary;
  }
}

.item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.upload-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid $border-light;
}

.upload-empty {
  text-align: center;
  padding: 40px 0;

  .empty-text {
    color: $text-placeholder;
  }
}
</style>
