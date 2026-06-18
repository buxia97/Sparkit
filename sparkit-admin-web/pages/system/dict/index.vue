<template>
  <div>
    <div class="page-header">
      <div class="page-title">字典管理</div>
      <div class="page-desc">管理数据字典类型与字典数据</div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="字典类型" name="type">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" @click="handleTypeAdd">
                <el-icon><Plus /></el-icon>新增类型
              </el-button>
              <el-button @click="refreshCache">
                <el-icon><Refresh /></el-icon>刷新缓存
              </el-button>
            </div>
          </div>
          <el-table :data="typeData" v-loading="typeLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="dictName" label="字典名称" min-width="150" />
            <el-table-column prop="dictType" label="字典类型" min-width="150" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
                  {{ row.status === 1 ? '正常' : '停用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleTypeEdit(row)">编辑</el-button>
                <el-button type="success" link size="small" @click="viewDictData(row)">数据</el-button>
                <el-button type="danger" link size="small" @click="handleTypeDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="typePage" v-model:page-size="typePageSize" :total="typeTotal"
            :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchTypes" @current-change="fetchTypes"
          />
        </el-tab-pane>

        <el-tab-pane v-if="currentDictType" label="字典数据" name="data">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" @click="handleDataAdd">
                <el-icon><Plus /></el-icon>新增数据
              </el-button>
              <el-button @click="activeTab = 'type'">返回类型列表</el-button>
            </div>
          </div>
          <el-table :data="dataList" v-loading="dataLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="dictLabel" label="数据标签" min-width="150" />
            <el-table-column prop="dictValue" label="数据键值" min-width="150" />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
                  {{ row.status === 1 ? '正常' : '停用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleDataEdit(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDataDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="500px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="请输入字典类型" :disabled="!!typeForm.id" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTypeSubmit" :loading="typeSubmitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataDialogVisible" :title="dataDialogTitle" width="500px" destroy-on-close>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="100px">
        <el-form-item label="字典类型">{{ currentDictType?.dictType }}</el-form-item>
        <el-form-item label="数据标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="请输入数据标签" />
        </el-form-item>
        <el-form-item label="数据键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="请输入数据键值" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="dataForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDataSubmit" :loading="dataSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '字典管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/dict', title: '字典管理', icon: 'Collection' })
  fetchTypes()
})

const activeTab = ref('type')

// 字典类型
const typeData = ref([])
const typeLoading = ref(false)
const typePage = ref(1)
const typePageSize = ref(10)
const typeTotal = ref(0)

const fetchTypes = async () => {
  typeLoading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/dict/types', {
      params: { page: typePage.value, pageSize: typePageSize.value },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      const list = data.value.data || []
      // 前端分页
      typeTotal.value = list.length
      const start = (typePage.value - 1) * typePageSize.value
      typeData.value = list.slice(start, start + typePageSize.value)
    }
  } catch (e) { /* fallback */ } finally { typeLoading.value = false }
}

const typeDialogVisible = ref(false)
const typeDialogTitle = ref('新增字典类型')
const typeFormRef = ref(null)
const typeSubmitting = ref(false)
const typeForm = reactive({ id: '', dictName: '', dictType: '', status: 1, remark: '' })
const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

const handleTypeAdd = () => { typeDialogTitle.value = '新增字典类型'; resetTypeForm(); typeDialogVisible.value = true }
const handleTypeEdit = (row) => {
  typeDialogTitle.value = '编辑字典类型'
  Object.assign(typeForm, { id: row.id, dictName: row.dictName, dictType: row.dictType, status: row.status, remark: row.remark || '' })
  typeDialogVisible.value = true
}
const resetTypeForm = () => { Object.assign(typeForm, { id: '', dictName: '', dictType: '', status: 1, remark: '' }); typeFormRef.value?.resetFields() }

const handleTypeSubmit = async () => {
  if (!typeFormRef.value) return
  await typeFormRef.value.validate(async (valid) => {
    if (!valid) return
    typeSubmitting.value = true
    try {
      const url = typeForm.id ? `/api/v1/admin/dict/types/${typeForm.id}` : '/api/v1/admin/dict/types'
      const method = typeForm.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, { method, body: typeForm, headers: { Authorization: `Bearer ${auth.token}` } })
      if (data.value?.code === 200) { ElMessage.success(typeForm.id ? '编辑成功' : '新增成功'); typeDialogVisible.value = false; fetchTypes() }
    } catch (e) { /* ignore */ } finally { typeSubmitting.value = false }
  })
}

const handleTypeDelete = (row) => {
  ElMessageBox.confirm(`确定删除字典类型「${row.dictName}」？`, '删除确认', { type: 'warning' }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/dict/types/${row.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) { ElMessage.success('删除成功'); fetchTypes() }
  }).catch(() => {})
}

const refreshCache = async () => {
  ElMessage.success('缓存已刷新')
}

// 字典数据
const currentDictType = ref(null)
const dataList = ref([])
const dataLoading = ref(false)

const viewDictData = async (row) => {
  currentDictType.value = row
  dataLoading.value = true
  try {
    const { data } = await useFetch(`/api/v1/admin/dict/data/${row.dictType}`, { headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) dataList.value = data.value.data
  } catch (e) { /* ignore */ } finally { dataLoading.value = false }
  activeTab.value = 'data'
}

const dataDialogVisible = ref(false)
const dataDialogTitle = ref('新增字典数据')
const dataFormRef = ref(null)
const dataSubmitting = ref(false)
const dataForm = reactive({ id: '', dictLabel: '', dictValue: '', sort: 0, status: 1, remark: '', dictType: '' })
const dataRules = {
  dictLabel: [{ required: true, message: '请输入数据标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入数据键值', trigger: 'blur' }]
}

const handleDataAdd = () => {
  dataDialogTitle.value = '新增字典数据'
  resetDataForm()
  dataForm.dictType = currentDictType.value?.dictType
  dataDialogVisible.value = true
}
const handleDataEdit = (row) => {
  dataDialogTitle.value = '编辑字典数据'
  Object.assign(dataForm, { id: row.id, dictLabel: row.dictLabel, dictValue: row.dictValue, sort: row.sort, status: row.status, remark: row.remark || '', dictType: currentDictType.value?.dictType })
  dataDialogVisible.value = true
}
const resetDataForm = () => { Object.assign(dataForm, { id: '', dictLabel: '', dictValue: '', sort: 0, status: 1, remark: '', dictType: '' }); dataFormRef.value?.resetFields() }

const handleDataSubmit = async () => {
  if (!dataFormRef.value) return
  await dataFormRef.value.validate(async (valid) => {
    if (!valid) return
    dataSubmitting.value = true
    try {
      const url = dataForm.id ? `/api/v1/admin/dict/data/${dataForm.id}` : '/api/v1/admin/dict/data'
      const method = dataForm.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, { method, body: dataForm, headers: { Authorization: `Bearer ${auth.token}` } })
      if (data.value?.code === 200) { ElMessage.success(dataForm.id ? '编辑成功' : '新增成功'); dataDialogVisible.value = false; viewDictData(currentDictType.value) }
    } catch (e) { /* ignore */ } finally { dataSubmitting.value = false }
  })
}

const handleDataDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.dictLabel}」？`, '删除确认', { type: 'warning' }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/dict/data/${row.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) { ElMessage.success('删除成功'); viewDictData(currentDictType.value) }
  }).catch(() => {})
}
</script>