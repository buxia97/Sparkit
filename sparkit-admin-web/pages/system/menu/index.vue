<template>
  <div>
    <div class="page-header">
      <div class="page-title">菜单管理</div>
      <div class="page-desc">管理菜单和接口权限，支持多级菜单和按钮级别权限控制</div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd({})">
          <el-icon><Plus /></el-icon>新增根菜单
        </el-button>
        <el-button @click="expandAll">展开全部</el-button>
        <el-button @click="collapseAll">折叠全部</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="menuTree" v-loading="loading" row-key="id" stripe default-expand-all>
        <el-table-column prop="menuName" label="菜单名称" min-width="200" />
        <el-table-column prop="icon" label="图标" width="80" align="center" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="perms" label="权限标识" min-width="160" />
        <el-table-column prop="path" label="路由地址" min-width="150" />
        <el-table-column prop="menuType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <span :class="menuTypeTag(row.menuType)">{{ menuTypeLabel(row.menuType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleAdd(row)">新增子级</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="formData.parentId"
            :data="menuTree"
            placeholder="无（根菜单）"
            clearable
            check-strictly
            :props="{ label: 'menuName', value: 'id' }"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="formData.menuType">
            <el-radio label="M">目录</el-radio>
            <el-radio label="C">菜单</el-radio>
            <el-radio label="B">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="formData.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="formData.menuType !== 'B'" label="图标">
          <el-input v-model="formData.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item v-if="formData.menuType !== 'B'" label="路由地址">
          <el-input v-model="formData.path" placeholder="请输入路由地址" />
        </el-form-item>
        <el-form-item v-if="formData.menuType !== 'B'" label="组件路径">
          <el-input v-model="formData.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="formData.perms" placeholder="如 system:admin:list" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '菜单管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/menu', title: '菜单管理', icon: 'Menu' })
  fetchData()
})

const menuTree = ref([])
const loading = ref(false)
const tableRef = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/menus/tree', { headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) menuTree.value = data.value.data
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const expandAll = () => { /* Element Plus table tree expand all */ }
const collapseAll = () => { /* Element Plus table tree collapse all */ }

const menuTypeLabel = (type) => ({ M: '目录', C: '菜单', B: '按钮' }[type] || type)
const menuTypeTag = (type) => ({ M: 'tag-info', C: 'tag-success', B: 'tag-warning' }[type] || '')

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({ id: '', parentId: '', menuType: 'C', menuName: '', icon: '', path: '', component: '', perms: '', sort: 0, status: 1 })
const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

const handleAdd = (parent) => {
  dialogTitle.value = '新增菜单'
  resetForm()
  formData.parentId = parent.id || ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑菜单'
  Object.assign(formData, {
    id: row.id, parentId: row.parentId || '', menuType: row.menuType,
    menuName: row.menuName, icon: row.icon || '', path: row.path || '',
    component: row.component || '', perms: row.perms || '', sort: row.sort || 0, status: row.status
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(formData, { id: '', parentId: '', menuType: 'C', menuName: '', icon: '', path: '', component: '', perms: '', sort: 0, status: 1 })
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id ? `/api/v1/admin/menus/${formData.id}` : '/api/v1/admin/menus'
      const method = formData.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, { method, body: formData, headers: { Authorization: `Bearer ${auth.token}` } })
      if (data.value?.code === 200) {
        ElMessage.success(formData.id ? '编辑成功' : '新增成功')
        dialogVisible.value = false
        fetchData()
      }
    } catch (e) { /* ignore */ } finally { submitting.value = false }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除菜单「${row.menuName}」？`, '删除确认', { type: 'warning' }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/menus/${row.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) { ElMessage.success('删除成功'); fetchData() }
  }).catch(() => {})
}
</script>