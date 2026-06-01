<template>
  <div class="page-container">
    <div class="flex-between" style="margin-bottom: 16px">
      <h1 class="page-title" style="margin-bottom: 0">用户管理</h1>
      <span class="page-tip">用户通过登录页自助注册，此处仅查看与调整角色；默认仅显示启用账号</span>
    </div>

    <div class="card-wrapper card-gap">
      <el-form :model="query" inline>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="用户名/姓名" clearable style="width: 200px" @change="handleQuery" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 160px" @change="handleQuery">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="handleQuery">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card-wrapper">
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="deptName" label="部门" width="120" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleTag(row.role)" size="small" effect="plain">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pwdModified" label="密码" width="110">
          <template #default="{ row }">
            <el-tag :type="row.pwdModified === 1 ? 'success' : 'warning'" size="small" effect="plain">
              {{ row.pwdModified === 1 ? '已修改' : '初始密码' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime ? formatDate(row.createTime, 'YYYY-MM-DD HH:mm') : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="留空默认 123456，员工登录后自行修改" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择部门" style="width: 100%">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="员工" :value="1" />
            <el-option label="领导" :value="2" />
            <el-option label="财务" :value="3" />
            <el-option label="管理员" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser } from '@/api/user'
import { getDeptTree } from '@/api/dept'
import { formatDate } from '@/utils/format'

interface UserRow {
  id: number
  username: string
  realName: string
  deptId: number
  deptName: string
  role: number
  phone: string
  status: number
  pwdModified: number
  createTime: string
}

interface DeptOption {
  id: number
  deptName: string
}

const loading = ref(false)
const total = ref(0)
const tableData = ref<UserRow[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const deptOptions = ref<DeptOption[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  deptId: undefined as number | undefined,
  status: undefined as number | undefined
})

const form = reactive({
  id: 0, username: '', password: '', realName: '', deptId: undefined as number | undefined, role: 1, phone: ''
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')

const roleLabel = (role: number) => ({ 1: '员工', 2: '领导', 3: '财务', 4: '管理员' } as Record<number, string>)[role] || String(role)
const roleTag = (role: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  const m: Record<number, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    1: 'info', 2: 'warning', 3: 'success', 4: 'danger'
  }
  return m[role] || 'info'
}

const loadDepts = async () => {
  try {
    const res: any = await getDeptTree()
    deptOptions.value = res.data || []
  } catch { /* 忽略 */ }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getUserList({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      deptId: query.deptId,
      status: query.status
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
const handleReset = () => { query.keyword = ''; query.deptId = undefined; query.status = undefined; query.pageNum = 1; fetchData() }

const openDialog = (row?: any) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, { id: row.id, username: row.username, password: '', realName: row.realName, deptId: row.deptId, role: row.role, phone: row.phone })
  } else {
    Object.assign(form, { id: 0, username: '', password: '', realName: '', deptId: undefined, role: 1, phone: '' })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        username: form.username,
        realName: form.realName,
        deptId: form.deptId,
        role: form.role,
        phone: form.phone
      }
      if (isEdit.value) {
        await updateUser({ id: form.id, ...payload })
      } else {
        await createUser({ ...payload, password: form.password })
      }
      ElMessage.success(isEdit.value ? '修改成功' : '创建成功')
      dialogVisible.value = false
      fetchData()
    } catch {
      /* 错误已由拦截器提示 */
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.realName}？（将禁用该账号）`, '提示', { type: 'warning' })
  } catch {
    return // 取消
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    /* 错误已由拦截器提示 */
  }
}

onMounted(() => {
  loadDepts()
  fetchData()
})
</script>

<style lang="scss" scoped>
.page-tip {
  font-size: 13px;
  color: #8a8a94;
}

.op-btns {
  display: flex;
  align-items: center;
  gap: 2px;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
</style>
