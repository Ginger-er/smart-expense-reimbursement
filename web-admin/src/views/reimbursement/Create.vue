<template>
  <div class="page-container">
    <div class="header-row">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>返回
      </el-button>
      <h1 class="page-title" style="margin: 0 0 0 8px">创建报销单</h1>
    </div>

    <!-- 基本信息 -->
    <div class="card-wrapper card-gap">
      <h3 class="section-title">基本信息</h3>
      <el-form :model="form" label-width="110px">
        <el-form-item label="申请人">
          <el-input :model-value="applicantLabel" disabled />
        </el-form-item>
        <el-form-item label="报销说明">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="选填，如本次报销的费用构成说明" />
        </el-form-item>
      </el-form>
      <p class="form-hint">报销金额由关联的发票自动汇总，提交前请先上传并关联发票。</p>
    </div>

    <!-- 底部操作 -->
    <div class="footer-actions">
      <el-button @click="$router.back()">取消</el-button>
      <el-button type="primary" plain :loading="saving" @click="handleSave">保存草稿</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交审批</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { createReimbursement, submitReimbursement } from '@/api/reimbursement'
import { getDeptList } from '@/api/dept'

const router = useRouter()
const userStore = useUserStore()

const deptMap = ref<Record<number, string>>({})

const loadDepts = async () => {
  try {
    const res: any = await getDeptList()
    const map: Record<number, string> = {}
    ;(res.data || []).forEach((d: any) => { map[d.id] = d.deptName })
    deptMap.value = map
  } catch {
    /* 忽略 */
  }
}

const applicantLabel = computed(() => {
  const u = userStore.userInfo
  if (!u) return '—'
  return `${u.realName} / ${deptMap.value[u.deptId || -1] || '未分配部门'}`
})

const form = reactive({
  remark: ''
})

const saving = ref(false)
const submitting = ref(false)

const handleSave = async () => {
  saving.value = true
  try {
    const created: any = await createReimbursement({ remark: form.remark })
    ElMessage.success('草稿已保存，请上传发票')
    router.push(`/reimbursement/detail/${created.data.id}`)
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const created: any = await createReimbursement({ remark: form.remark })
    try {
      await submitReimbursement(created.data.id)
      ElMessage.success('报销单已提交审批')
      router.push('/reimbursement')
    } catch {
      // 提交失败（未关联发票/金额未填写等），具体原因已由拦截器提示，跳转详情补全
      router.push(`/reimbursement/detail/${created.data.id}`)
    }
  } catch {
    /* 创建失败，错误已由拦截器提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDepts()
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.header-row {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.form-hint {
  font-size: $font-size-auxiliary;
  color: $text-secondary;
  line-height: 1.6;
  margin: 4px 0 0 110px;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
</style>
