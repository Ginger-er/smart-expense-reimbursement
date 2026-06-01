<template>
  <div class="page-container" v-loading="loading">
    <template v-if="detail">
      <div class="detail-header">
        <div class="header-left">
          <el-button text @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon>返回
          </el-button>
          <h1 class="page-title" style="margin: 0 0 0 8px">{{ detail.tripNo }}</h1>
          <el-tag :type="tripStatus(detail.status).tag" size="large" style="margin-left: 12px">
            {{ tripStatus(detail.status).label }}
          </el-tag>
        </div>
      </div>

      <el-alert
        v-if="rejectReason"
        :title="`驳回原因：${rejectReason}`"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <div class="detail-layout">
        <div class="detail-left">
          <div class="card-wrapper">
            <h3 class="section-title">基本信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">申请人</span>
                <span class="info-value">{{ detail.applicantName || '—' }} / {{ detail.deptName || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">目的地</span>
                <span class="info-value">{{ detail.destination || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">出差事由</span>
                <span class="info-value">{{ detail.purpose || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">出差时间</span>
                <span class="info-value">{{ detail.startDate }} 至 {{ detail.endDate }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">预算金额</span>
                <span class="info-value amount">&yen;{{ formatYuan(detail.budgetAmount ?? 0) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">创建时间</span>
                <span class="info-value">{{ detail.createTime ? formatDate(detail.createTime, 'YYYY-MM-DD HH:mm') : '—' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-right">
          <div class="card-wrapper">
            <h3 class="section-title">审批记录</h3>
            <el-timeline>
              <el-timeline-item
                v-for="node in detail.approvalRecords"
                :key="node.id"
                :timestamp="formatDate(node.createTime, 'YYYY-MM-DD HH:mm')"
                placement="top"
                :color="approvalAction(node.action).color"
              >
                <div class="timeline-title">
                  {{ node.approverName || '审批人' }}
                  <el-tag size="small" :type="approvalAction(node.action).tag">{{ approvalAction(node.action).label }}</el-tag>
                </div>
                <p class="timeline-comment" v-if="node.comment">{{ node.comment }}</p>
              </el-timeline-item>
              <el-empty v-if="!detail.approvalRecords?.length" description="暂无审批记录" :image-size="60" />
            </el-timeline>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { formatYuan, formatDate } from '@/utils/format'
import { tripStatus, approvalAction } from '@/utils/status'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getTripDetail } from '@/api/trip'

const route = useRoute()
const loading = ref(false)
const detail = ref<any>(null)

// 出差驳回原因存在审批记录 comment 中，取最近一条驳回记录
const rejectReason = computed(() => {
  const records = detail.value?.approvalRecords || []
  const rejected = records.find((r: any) => r.action === 2)
  return rejected?.comment || ''
})

const loadDetail = async () => {
  loading.value = true
  try {
    const res: any = await getTripDetail(Number(route.params.id))
    detail.value = res.data
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
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

.detail-layout {
  display: flex;
  gap: $spacing-card-gap;
}

.detail-left {
  flex: 0 0 60%;
  min-width: 0;
}

.detail-right {
  flex: 0 0 40%;
}

.section-title {
  font-size: $font-size-card-title;
  font-weight: $font-weight-card-title;
  color: $text-primary;
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-item {
  .info-label {
    display: block;
    font-size: $font-size-auxiliary;
    color: $text-secondary;
    margin-bottom: 4px;
  }

  .info-value {
    font-size: $font-size-body;
    color: $text-primary;
    word-break: break-all;

    &.amount {
      font-weight: 600;
      color: $color-primary;
    }
  }
}

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
</style>
