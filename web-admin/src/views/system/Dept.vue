<template>
  <div class="page-container">
    <div class="flex-between" style="margin-bottom: 16px">
      <h1 class="page-title" style="margin-bottom: 0">部门管理</h1>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增部门
      </el-button>
    </div>

    <div class="card-wrapper">
      <el-table
        :data="tableData"
        v-loading="loading"
        row-key="id"
        default-expand-all
        style="width: 100%"
      >
        <el-table-column prop="name" label="部门名称" min-width="180" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime ? formatDate(row.createTime, 'YYYY-MM-DD HH:mm') : '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button text type="primary" size="small" @click="handleAddChild(row)">添加子部门</el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" :image-size="80" />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="deptTree"
            :props="{ label: 'name' }"
            node-key="id"
            placeholder="请选择上级部门（不选则为顶级）"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
import { Plus } from '@element-plus/icons-vue'
import { getDeptList, createDept, updateDept, deleteDept } from '@/api/dept'
import { formatDate } from '@/utils/format'

interface DeptRow {
  id: number
  deptName: string
  name: string
  parentId: number
  leaderId: number | null
  sortOrder: number
  sort: number
  status: number
  createTime: string
  children?: DeptRow[]
}

const loading = ref(false)
const tableData = ref<DeptRow[]>([])
const deptTree = ref<DeptRow[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: 0, deptName: '', parentId: undefined as number | undefined, sortOrder: 0
})

const formRules: FormRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑部门' : '新增部门')

const buildTree = (list: any[]): DeptRow[] => {
  const map = new Map<number, DeptRow>()
  list.forEach(d => {
    map.set(d.id, {
      id: d.id,
      deptName: d.deptName,
      name: d.deptName,
      parentId: d.parentId ?? 0,
      leaderId: d.leaderId ?? null,
      sortOrder: d.sortOrder ?? 0,
      sort: d.sortOrder ?? 0,
      status: d.status,
      createTime: d.createTime,
      children: []
    })
  })
  const roots: DeptRow[] = []
  map.forEach(node => {
    const parent = node.parentId ? map.get(node.parentId) : undefined
    if (parent) parent.children!.push(node)
    else roots.push(node)
  })
  return roots
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getDeptList()
    const tree = buildTree(res.data || [])
    tableData.value = tree
    deptTree.value = tree
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, { id: 0, deptName: '', parentId: undefined, sortOrder: 0 })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  Object.assign(form, { id: row.id, deptName: row.deptName, parentId: row.parentId || undefined, sortOrder: row.sortOrder })
  dialogVisible.value = true
}

const handleAddChild = (row: any) => {
  isEdit.value = false
  Object.assign(form, { id: 0, deptName: '', parentId: row.id, sortOrder: 0 })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        deptName: form.deptName,
        parentId: form.parentId || 0,
        sortOrder: form.sortOrder
      }
      if (isEdit.value) {
        await updateDept({ id: form.id, ...payload })
      } else {
        await createDept(payload)
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
  await ElMessageBox.confirm(`确认删除部门 ${row.deptName}？`, '提示', { type: 'warning' })
  try {
    await deleteDept(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    /* 错误已由拦截器提示 */
  }
}

onMounted(() => fetchData())
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.op-btns {
  display: flex;
  align-items: center;
  gap: 2px;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
</style>
