<template>
  <div>
    <div class="page-header">
      <div class="page-title">国际化管理</div>
      <div class="page-desc">管理系统多语言翻译文本，支持中文/英文/日文/韩文等</div>
    </div>

    <el-card>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="currentLang" placeholder="选择语言" @change="fetchI18n" style="width: 150px;">
            <el-option label="简体中文" value="zh-CN" />
            <el-option label="English" value="en-US" />
            <el-option label="日本語" value="ja-JP" />
            <el-option label="한국어" value="ko-KR" />
          </el-select>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增翻译
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-button @click="handleExport">导出当前语言</el-button>
          <el-upload
            :show-file-list="false"
            :before-upload="handleImport"
            accept=".csv"
          >
            <el-button>导入翻译</el-button>
          </el-upload>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="i18nKey" label="翻译键" min-width="200" show-overflow-tooltip />
        <el-table-column prop="i18nValue" label="翻译值" min-width="250">
          <template #default="{ row }">
            <el-input
              v-if="editingId === row.id"
              v-model="editingValue"
              @blur="handleSaveInline(row)"
              @keyup.enter="handleSaveInline(row)"
              size="small"
            />
            <span v-else>{{ row.i18nValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lang" label="语言" width="120" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEditInline(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增翻译弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增翻译" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="语言" prop="lang">
          <el-select v-model="formData.lang" style="width: 100%;">
            <el-option label="简体中文" value="zh-CN" />
            <el-option label="English" value="en-US" />
            <el-option label="日本語" value="ja-JP" />
            <el-option label="한국어" value="ko-KR" />
          </el-select>
        </el-form-item>
        <el-form-item label="翻译键" prop="i18nKey">
          <el-input v-model="formData.i18nKey" placeholder="如：common.save" />
        </el-form-item>
        <el-form-item label="翻译值" prop="i18nValue">
          <el-input v-model="formData.i18nValue" type="textarea" :rows="3" placeholder="请输入翻译文本" />
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
  meta: { title: '国际化管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/i18n', title: '国际化管理', icon: 'ChatLineSquare' })
  fetchI18n()
})

const currentLang = ref('zh-CN')
const tableData = ref([])
const loading = ref(false)

const fetchI18n = async () => {
  loading.value = true
  try {
    const { data } = await useFetch(`/api/v1/admin/i18n/${currentLang.value}`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      const map = data.value.data || {}
      tableData.value = Object.entries(map).map(([key, value], idx) => ({
        id: idx + 1,
        i18nKey: key,
        i18nValue: value,
        lang: currentLang.value
      }))
    }
  } catch (e) { /* ignore */ } finally { loading.value = false }
}

const editingId = ref(null)
const editingValue = ref('')

const handleEditInline = (row) => {
  editingId.value = row.id
  editingValue.value = row.i18nValue
}

const handleSaveInline = async (row) => {
  if (editingValue.value === row.i18nValue) {
    editingId.value = null
    return
  }
  try {
    await useFetch(`/api/v1/admin/i18n/${row.id}`, {
      method: 'PUT',
      body: { i18nKey: row.i18nKey, i18nValue: editingValue.value, lang: row.lang },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    row.i18nValue = editingValue.value
    ElMessage.success('保存成功')
  } catch (e) { /* ignore */ } finally { editingId.value = null }
}

const dialogVisible = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({ lang: 'zh-CN', i18nKey: '', i18nValue: '' })
const formRules = {
  lang: [{ required: true, message: '请选择语言', trigger: 'change' }],
  i18nKey: [{ required: true, message: '请输入翻译键', trigger: 'blur' }],
  i18nValue: [{ required: true, message: '请输入翻译值', trigger: 'blur' }]
}

const handleAdd = () => {
  Object.assign(formData, { lang: currentLang.value, i18nKey: '', i18nValue: '' })
  formRef.value?.resetFields()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await useFetch('/api/v1/admin/i18n', {
        method: 'POST',
        body: formData,
        headers: { Authorization: `Bearer ${auth.token}` }
      })
      ElMessage.success('新增成功')
      dialogVisible.value = false
      fetchI18n()
    } catch (e) { /* ignore */ } finally { submitting.value = false }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除翻译「${row.i18nKey}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    await useFetch(`/api/v1/admin/i18n/${row.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    ElMessage.success('删除成功')
    fetchI18n()
  }).catch(() => {})
}

const handleExport = () => {
  const csv = 'key,value\n' + tableData.value.map(r => `${r.i18nKey},${r.i18nValue}`).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `${currentLang.value}.csv`; a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

const handleImport = async (file) => {
  const text = await file.text()
  const lines = text.replace(/^\uFEFF/, '').split('\n').filter(l => l.trim())
  for (let i = 1; i < lines.length; i++) {
    const [key, value] = lines[i].split(',')
    if (key && value) {
      try {
        await useFetch('/api/v1/admin/i18n', {
          method: 'POST',
          body: { lang: currentLang.value, i18nKey: key, i18nValue: value },
          headers: { Authorization: `Bearer ${auth.token}` }
        })
      } catch (e) { /* ignore */ }
    }
  }
  ElMessage.success('导入成功')
  fetchI18n()
  return false
}
</script>