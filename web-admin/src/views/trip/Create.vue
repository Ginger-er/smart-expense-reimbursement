<template>
  <div class="page-container">
    <div class="header-row">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>返回
      </el-button>
      <h1 class="page-title" style="margin: 0 0 0 8px">{{ isEdit ? '编辑出差申请' : '新建出差申请' }}</h1>
    </div>

    <div class="card-wrapper">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="申请人">
          <el-input :model-value="applicantLabel" disabled />
        </el-form-item>
        <el-form-item label="目的地" prop="destination">
          <el-input v-model="form.destination" placeholder="请输入出差目的地" />
        </el-form-item>
        <el-form-item label="出差事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请描述出差事由" maxlength="200" show-word-limit />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="开始日期" prop="startDate">
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束日期" prop="endDate">
            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="预算（元）" prop="budget">
          <el-input-number v-model="form.budget" :min="0" :precision="2" :step="100" style="width: 240px" />
        </el-form-item>
      </el-form>
    </div>

    <div class="footer-actions">
      <el-button @click="$router.back()">取消</el-button>
      <el-button type="primary" plain :loading="saving" @click="handleSave">保存草稿</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { createTrip, updateTrip, getTripDetail, submitTrip } from '@/api/trip'
import { getDeptList } from '@/api/dept'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()

const tripId = ref<number | null>(null)
const isEdit = computed(() => tripId.value != null)

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
  destination: '',
  reason: '',
  startDate: '',
  endDate: '',
  budget: 0
})

const rules: FormRules = {
  destination: [{ required: true, message: '请输入出差目的地', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入出差事由', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  budget: [{ required: true, message: '请输入预算', trigger: 'blur' }]
}

const saving = ref(false)
const submitting = ref(false)

const buildPayload = () => ({
  id: tripId.value ?? undefined,
  userId: userStore.userInfo?.id,
  destination: form.destination,
  purpose: form.reason,
  startDate: form.startDate,
  endDate: form.endDate,
  budgetAmount: form.budget
})

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.endDate && form.startDate && form.endDate < form.startDate) {
      ElMessage.warning('结束日期不能早于开始日期')
      return
    }
    saving.value = true
    try {
      if (isEdit.value) {
        await updateTrip(buildPayload())
        ElMessage.success('草稿已更新')
      } else {
        await createTrip(buildPayload())
        ElMessage.success('草稿已保存')
      }
      router.push('/trip')
    } catch {
      /* 错误已由拦截器提示 */
    } finally {
      saving.value = false
    }
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.endDate && form.startDate && form.endDate < form.startDate) {
      ElMessage.warning('结束日期不能早于开始日期')
      return
    }
    submitting.value = true
    try {
      let id: number
      if (isEdit.value) {
        const updated: any = await updateTrip(buildPayload())
        id = updated.data.id
      } else {
        const created: any = await createTrip(buildPayload())
        id = created.data.id
      }
      await submitTrip(id)
      ElMessage.success('出差申请已提交')
      router.push('/trip')
    } catch {
      /* 错误已由拦截器提示 */
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  loadDepts()
  const id = route.query.id
  if (!id) return
  tripId.value = Number(id)
  try {
    const res: any = await getTripDetail(tripId.value)
    const t = res.data
    Object.assign(form, {
      destination: t.destination || '',
      reason: t.purpose || '',
      startDate: t.startDate || '',
      endDate: t.endDate || '',
      budget: t.budgetAmount ?? 0
    })
  } catch {
    ElMessage.error('加载出差申请失败')
    router.push('/trip')
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.header-row {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
</style>
