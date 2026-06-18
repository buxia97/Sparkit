<template>
  <div>
    <div class="page-header">
      <div class="page-title">角色管理</div>
      <div class="page-desc">管理系统角色，分配菜单和接口权限</div>
    </div>

    <div class="search-bar">
      <div class="search-item">
        <span class="search-label">角色名称</span>
        <el-input v-model="searchForm.keyword" placeholder="请输入角色名称" clearable style="width: 180px;" />
      </div>
      <div class="search-item">
        <span class="search-label">状态</span>
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </div>
      <div class="search-actions">
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增角色
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="roleKey" label="角色标识" min-width="150" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleAssignPerm(row)">分配权限</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData" @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="formData.roleKey" placeholder="请输入角色标识" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="450px" destroy-on-close>
      <div class="permission-tree">
        <el-tree
          ref="permTreeRef"
          :data="menuTree"
          show-checkbox
          node-key="id"
          :default-checked-keys="checkedMenuIds"
          :props="{ label: 'menuName', children: 'children' }"
          default-expand-all
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePerm" :loading="permSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '角色管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/role', title: '角色管理', icon: 'Avatar' })
  fetchData()
  fetchMenuTree()
})

const searchForm = reactive({ keyword: '', status: '' })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = ''; handleSearch() }

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/roles', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      const list = data.value.data || []
      // 前端分页
      const filtered = list.filter(r => {
        if (searchForm.keyword && !r.roleName.includes(searchForm.keyword)) return false
        if (searchForm.status !== '' && r.status !== searchForm.status) return false
        return true
      })
      total.value = filtered.length
      const start = (page.value - 1) * pageSize.value
      tableData.value = filtered.slice(start, start + pageSize.value)
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({ id: '', roleName: '', roleKey: '', sort: 0, status: 1, remark: '' })
const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }]
}

const handleAdd = () => { dialogTitle.value = '新增角色'; resetForm(); dialogVisible.value = true }
const handleEdit = (row) => {
  dialogTitle.value = '编辑角色'
  Object.assign(formData, { id: row.id, roleName: row.roleName, roleKey: row.roleKey, sort: row.sort, status: row.status, remark: row.remark || '' })
  dialogVisible.value = true
}
const resetForm = () => { Object.assign(formData, { id: '', roleName: '', roleKey: '', sort: 0, status: 1, remark: '' }); formRef.value?.resetFields() }

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id ? `/api/v1/admin/roles/${formData.id}` : '/api/v1/admin/roles'
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
  ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '删除确认', { type: 'warning' }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/roles/${row.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) { ElMessage.success('删除成功'); fetchData() }
  }).catch(() => {})
}

// 分配权限
const permDialogVisible = ref(false)
const permTreeRef = ref(null)
const menuTree = ref([])
const checkedMenuIds = ref([])
const currentRoleId = ref('')
const permSaving = ref(false)

const fetchMenuTree = async () => {
  try {
    const { data } = await useFetch('/api/v1/admin/menus/tree', { headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) menuTree.value = data.value.data
  } catch (e) { /* ignore */ }
}

const handleAssignPerm = async (row) => {
  currentRoleId.value = row.id
  checkedMenuIds.value = row.menuIds || []
  permDialogVisible.value = true
}

const handleSavePerm = async () => {
  permSaving.value = true
  try {
    const menuIds = permTreeRef.value?.getCheckedKeys() || []
    const halfCheckedKeys = permTreeRef.value?.getHalfCheckedKeys() || []
    const allIds = [...menuIds, ...halfCheckedKeys]
    const { data } = await useFetch(`/api/v1/admin/roles/${currentRoleId.value}/menus`, {
      method: 'PUT', body: { menuIds: allIds },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) { ElMessage.success('权限分配成功'); permDialogVisible.value = false }
  } catch (e) { /* ignore */ } finally { permSaving.value = false }
}
</script>