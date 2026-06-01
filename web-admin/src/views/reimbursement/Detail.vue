<template>
  <div class="page-container" v-loading="loading">
    <template v-if="detail">
      <!-- 顶部标题栏 -->
      <div class="detail-header">
        <div class="header-left">
          <el-button text @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <h1 class="page-title" style="margin-bottom: 0; margin-left: 8px">
            {{ detail.reimburseNo }}
          </h1>
          <el-tag :type="reimbursementStatus(detail.status).tag" size="large" style="margin-left: 12px">
            {{ reimbursementStatus(detail.status).label }}
          </el-tag>
        </div>
        <div class="header-amount">
          <span class="amount-label">报销总金额</span>
          <span class="amount-large">&yen;{{ formatYuan(detail.totalAmount ?? 0) }}</span>
          <span v-if="detail.status === 5 && detail.payTime" class="pay-info">
            打款人 {{ detail.payUserName || '—' }} · {{ formatDate(detail.payTime, 'YYYY-MM-DD HH:mm') }}
          </span>
        </div>
      </div>

      <!-- 驳回原因 -->
      <el-alert
        v-if="detail.status === 4 && detail.rejectReason"
        :title="`驳回原因：${detail.rejectReason}`"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <!-- 左右分栏 -->
      <div class="detail-layout">
        <!-- 左侧：信息区 -->
        <div class="detail-left">
          <!-- 基本信息卡片 -->
          <div class="card-wrapper card-gap">
            <h3 class="section-title">基本信息</h3>
            <div class="trip-info-grid">
              <div class="info-item">
                <span class="info-label">申请人</span>
                <span class="info-value">{{ detail.applicantName || '—' }} / {{ detail.deptName || '—' }}</span>
              </div>
            </div>
          </div>

          <!-- 费用构成 -->
          <div class="card-wrapper card-gap">
            <h3 class="section-title">费用构成</h3>
            <div class="fee-breakdown" v-if="feeBreakdown.length">
              <div v-for="f in feeBreakdown" :key="f.name" class="fee-item">
                <span class="fee-name">{{ f.name }}</span>
                <div class="fee-bar">
                  <div class="fee-fill" :style="{ width: feePct(f) + '%' }" />
                </div>
                <span class="fee-amount">&yen;{{ formatYuan(f.amount) }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无费用数据" :image-size="60" />
          </div>

          <!-- 发票明细表格 -->
          <div class="card-wrapper card-gap">
            <div class="flex-between" style="margin-bottom: 16px">
              <h3 class="section-title" style="margin-bottom: 0">发票明细（{{ detail.invoices?.length || 0 }}张）</h3>
              <el-upload
                v-if="isOwner && detail.status === 0"
                :show-file-list="false"
                :http-request="handleUpload"
                :accept="'.jpg,.jpeg,.png,.pdf'"
                multiple
              >
                <el-button size="small" type="primary" :loading="uploading">
                  <el-icon><Plus /></el-icon>
                  上传发票
                </el-button>
              </el-upload>
            </div>
            <el-table :data="detail.invoices" style="width: 100%">
              <el-table-column prop="type" label="发票类型" width="90">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain">{{ invoiceType(row.type).label || '其他' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sellerName" label="销售方" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.sellerName || '—' }}</template>
              </el-table-column>
              <el-table-column prop="amount" label="金额" width="110">
                <template #default="{ row }">
                  <span class="amount-text">&yen;{{ formatYuan(row.amount ?? 0) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="invoiceDate" label="开票日期" width="105">
                <template #default="{ row }">{{ row.invoiceDate || '—' }}</template>
              </el-table-column>
              <el-table-column prop="ocrStatus" label="状态" width="95">
                <template #default="{ row }">
                  <el-tag :type="invoiceOcrStatus(row.ocrStatus).tag" size="small">{{ invoiceOcrStatus(row.ocrStatus).label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <div class="inv-op">
                    <el-button v-if="row.ocrStatus === 2" text type="primary" size="small" @click="openManual(row)">填写</el-button>
                    <el-button v-if="isOwner && detail.status === 0" text type="danger" size="small" @click="handleDeleteInvoice(row)">移除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 右侧：审批流 -->
        <div class="detail-right">
          <!-- 审批进度 -->
          <div class="card-wrapper card-gap">
            <h3 class="section-title">审批进度</h3>
            <el-timeline class="approval-timeline">
              <el-timeline-item
                v-for="node in detail.approvalRecords"
                :key="node.id"
                :timestamp="formatDate(node.createTime, 'YYYY-MM-DD HH:mm')"
                placement="top"
                :color="approvalAction(node.action).color"
              >
                <div class="timeline-content">
                  <div class="timeline-title">
                    {{ node.approverName || '审批人' }}
                    <el-tag size="small" :type="approvalAction(node.action).tag">
                      {{ approvalAction(node.action).label }}
                    </el-tag>
                  </div>
                  <p class="timeline-comment" v-if="node.comment">{{ node.comment }}</p>
                </div>
              </el-timeline-item>
              <el-empty v-if="!detail.approvalRecords?.length" description="暂无审批记录" :image-size="60" />
            </el-timeline>
          </div>

          <!-- 提交报销单 -->
          <div class="card-wrapper" v-if="isOwner && detail.status === 0">
            <h3 class="section-title">提交报销单</h3>
            <p class="submit-hint">提交前请上传并关联发票，报销金额由发票自动汇总。</p>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交审批</el-button>
          </div>

          <!-- 审批操作区 -->
          <div class="card-wrapper" v-if="isCurrentApprover">
            <h3 class="section-title">审批操作</h3>
            <div class="approval-actions">
              <el-input
                v-model="approvalComment"
                type="textarea"
                :rows="3"
                placeholder="请输入审批意见（驳回时必填）"
                style="margin-bottom: 12px"
              />
              <div class="approval-buttons">
                <el-button type="success" :loading="approving" @click="handleApprove">通过</el-button>
                <el-button type="danger" :loading="rejecting" @click="handleReject">驳回</el-button>
              </div>
            </div>
          </div>

          <!-- 打款操作区 -->
          <div class="card-wrapper" v-if="isFinance && detail.status === 3">
            <h3 class="section-title">打款</h3>
            <p class="submit-hint">审批已通过，确认后执行打款操作。</p>
            <el-button type="primary" :loading="paying" @click="handlePay">确认打款</el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 手动填写发票弹窗（OCR 识别失败时补全金额） -->
    <el-dialog v-model="manualVisible" title="填写发票信息" width="480px">
      <el-form v-if="manualItem" label-width="90px">
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
          <el-input v-model="manualForm.sellerName" placeholder="选填" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatYuan, formatDate } from '@/utils/format'
import { reimbursementStatus, approvalAction, invoiceType, invoiceOcrStatus } from '@/utils/status'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { getReimbursementDetail, approveReimbursement, submitReimbursement, payReimbursement } from '@/api/reimbursement'
import { uploadInvoice, confirmInvoice, deleteInvoice } from '@/api/invoice'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

interface InvoiceItem {
  id: number
  type: number
  invoiceNo?: string
  sellerName?: string
  amount: number
  invoiceDate?: string
  ocrStatus: number
}

interface ApprovalNode {
  id: number
  approverName?: string
  action: number
  comment?: string
  createTime: string
}

interface DetailData {
  id: number
  userId?: number
  reimburseNo: string
  totalAmount?: number
  invoiceCount?: number
  status: number
  rejectReason?: string
  createTime?: string
  payTime?: string
  payUserName?: string
  applicantName?: string
  deptName?: string
  invoices: InvoiceItem[]
  approvalRecords: ApprovalNode[]
}

const loading = ref(false)
const detail = ref<DetailData | null>(null)
const approvalComment = ref('')
const approving = ref(false)
const rejecting = ref(false)

const feeBreakdown = computed(() => {
  const names: Record<number, string> = { 1: '交通费', 2: '住宿费', 3: '餐饮费', 4: '其他' }
  const map = new Map<number, number>()
  ;(detail.value?.invoices || []).forEach((inv) => {
    const t = inv.type
    map.set(t, (map.get(t) || 0) + Number(inv.amount || 0))
  })
  return Array.from(map.entries()).map(([t, amount]) => ({ name: names[t] || '其他', amount }))
})

const feePct = (f: { name: string; amount: number }) => {
  const total = feeBreakdown.value.reduce((s, x) => s + x.amount, 0) || 1
  return Math.round((f.amount / total) * 100)
}

const isCurrentApprover = computed(() => {
  const role = userStore.userInfo?.role ?? 1
  const s = detail.value?.status
  if (s === 1) return role === 2 || role === 3 || role === 4
  if (s === 2) return role === 3 || role === 4 // 二级审批仅财务/管理员
  return false
})

const isOwner = computed(() => userStore.userInfo?.id === detail.value?.userId)

const submitting = ref(false)
const paying = ref(false)

const isFinance = computed(() => {
  const role = userStore.userInfo?.role ?? 1
  return role === 3 || role === 4
})

const loadDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res: any = await getReimbursementDetail(id)
    detail.value = res.data
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    await submitReimbursement(detail.value!.id)
    ElMessage.success('已提交审批')
    loadDetail()
  } catch {
    /* 错误已由拦截器提示（如未关联发票） */
  } finally {
    submitting.value = false
  }
}

const handleApprove = async () => {
  await ElMessageBox.confirm('确认审批通过？', '提示', { type: 'success' })
  approving.value = true
  try {
    await approveReimbursement(detail.value!.id, 1, approvalComment.value.trim())
    ElMessage.success('审批通过')
    approvalComment.value = ''
    loadDetail()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    approving.value = false
  }
}

const handleReject = async () => {
  if (!approvalComment.value.trim()) {
    ElMessage.warning('驳回时请输入审批意见')
    return
  }
  await ElMessageBox.confirm('确认驳回该报销申请？', '提示', { type: 'warning' })
  rejecting.value = true
  try {
    await approveReimbursement(detail.value!.id, 2, approvalComment.value.trim())
    ElMessage.success('已驳回')
    approvalComment.value = ''
    loadDetail()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    rejecting.value = false
  }
}

const handlePay = async () => {
  await ElMessageBox.confirm('确认对该报销单打款？', '提示', { type: 'warning' })
  paying.value = true
  try {
    await payReimbursement(detail.value!.id)
    ElMessage.success('已打款')
    loadDetail()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    paying.value = false
  }
}

// ===== 发票上传 / 移除 / 手动填写 =====
const uploading = ref(false)

const handleUpload = async (options: any) => {
  const file = options.file as File
  if (file.size && file.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件超过 10MB 限制')
    return
  }
  uploading.value = true
  try {
    await uploadInvoice(file, { reimbursementId: detail.value!.id })
    ElMessage.success('发票已上传并关联到本报销单')
    await loadDetail()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    uploading.value = false
  }
}

const handleDeleteInvoice = async (row: any) => {
  await ElMessageBox.confirm('确定从本报销单移除这张发票？移除后将永久删除该发票记录。', '提示', { type: 'warning' })
  try {
    await deleteInvoice(row.id)
    ElMessage.success('已移除')
    await loadDetail()
  } catch {
    /* 错误已由拦截器提示 */
  }
}

const manualVisible = ref(false)
const manualLoading = ref(false)
const manualItem = ref<InvoiceItem | null>(null)
const manualForm = reactive({
  type: 4,
  amount: 0,
  sellerName: '',
  invoiceDate: ''
})

const openManual = (row: any) => {
  manualItem.value = row
  manualForm.type = row.type || 4
  manualForm.amount = Number(row.amount || 0)
  manualForm.sellerName = row.sellerName || ''
  manualForm.invoiceDate = row.invoiceDate || ''
  manualVisible.value = true
}

const confirmManual = async () => {
  if (!manualItem.value) return
  manualLoading.value = true
  try {
    await confirmInvoice({
      id: manualItem.value.id,
      invoiceNo: manualItem.value.invoiceNo || '',
      type: manualForm.type,
      amount: manualForm.amount,
      sellerName: manualForm.sellerName,
      invoiceDate: manualForm.invoiceDate || null
    })
    ElMessage.success('已保存')
    manualVisible.value = false
    await loadDetail()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    manualLoading.value = false
  }
}

onMounted(() => loadDetail())
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  .header-left {
    display: flex;
    align-items: center;
  }
}

.amount-label {
  font-size: $font-size-auxiliary;
  color: $text-secondary;
  margin-right: 8px;
}

.amount-large {
  font-size: 26px;
  font-weight: 700;
  color: $color-primary;
  letter-spacing: -0.5px;
}

.pay-info {
  display: block;
  margin-top: 6px;
  font-size: $font-size-auxiliary;
  color: $text-secondary;
}

.detail-layout {
  display: flex;
  gap: $spacing-card-gap;
}

.detail-left {
  flex: 0 0 65%;
  min-width: 0;
}

.detail-right {
  flex: 0 0 35%;
}

.trip-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-item {
  .info-label {
    font-size: $font-size-auxiliary;
    color: $text-secondary;
    display: block;
    margin-bottom: 4px;
  }

  .info-value {
    font-size: $font-size-body;
    color: $text-primary;
  }
}

.section-title {
  font-size: $font-size-card-title;
  font-weight: $font-weight-card-title;
  color: $text-primary;
  margin-bottom: 16px;
}

.amount-text {
  font-weight: 600;
  color: $color-primary;
}

.fee-breakdown {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.fee-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fee-name {
  width: 60px;
  flex-shrink: 0;
  font-size: $font-size-body;
  color: $text-primary;
}

.fee-bar {
  flex: 1;
  height: 8px;
  background: #f5f5f7;
  border-radius: 4px;
  overflow: hidden;
}

.fee-fill {
  height: 100%;
  background: linear-gradient(90deg, $color-accent, #818cf8);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.fee-amount {
  width: 100px;
  text-align: right;
  flex-shrink: 0;
  font-size: $font-size-body;
  font-weight: 600;
  color: $text-primary;
}

.approval-timeline {
  :deep(.el-timeline-item__node) {
    width: 12px;
    height: 12px;
  }

  :deep(.el-timeline-item__timestamp) {
    font-size: $font-size-auxiliary;
    color: $text-secondary;
  }
}

.timeline-content {
  .timeline-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: $font-size-body;
    color: $text-primary;
  }

  .timeline-comment {
    font-size: $font-size-auxiliary;
    color: $text-secondary;
    margin-top: 4px;
  }
}

.approval-buttons {
  display: flex;
  gap: 12px;

  .el-button {
    flex: 1;
  }
}

.submit-hint {
  font-size: $font-size-auxiliary;
  color: $text-secondary;
  line-height: 1.6;
  margin-bottom: 12px;
}

.inv-op {
  display: flex;
  align-items: center;
  gap: 2px;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
</style>
