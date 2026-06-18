<template>
  <div class="header-search">
    <el-icon class="search-icon"><Search /></el-icon>
    <input
      v-model="keyword"
      type="text"
      placeholder="搜索菜单、功能..."
      @focus="showResults = true"
      @blur="handleBlur"
      @input="handleSearch"
    />
    <div v-if="showResults && filteredResults.length > 0" class="search-results">
      <div
        v-for="item in filteredResults"
        :key="item.path"
        class="search-result-item"
        @mousedown.prevent="goTo(item.path)"
      >
        <div class="result-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="result-info">
          <div class="result-title">{{ item.title }}</div>
          <div class="result-path">{{ item.path }}</div>
        </div>
      </div>
    </div>
    <div v-if="showResults && keyword && filteredResults.length === 0" class="search-results">
      <div class="search-result-item" style="justify-content: center; color: var(--text-secondary);">
        未找到匹配结果
      </div>
    </div>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { searchData } from '~/composables/menuConfig'

const router = useRouter()
const keyword = ref('')
const showResults = ref(false)
const filteredResults = ref([])

const handleSearch = () => {
  if (!keyword.value.trim()) {
    filteredResults.value = []
    return
  }
  const kw = keyword.value.toLowerCase()
  filteredResults.value = searchData.filter(item =>
    item.title.toLowerCase().includes(kw) ||
    item.path.toLowerCase().includes(kw) ||
    item.keywords.some(k => k.toLowerCase().includes(kw))
  ).slice(0, 8)
}

const handleBlur = () => {
  setTimeout(() => { showResults.value = false }, 200)
}

const goTo = (path) => {
  keyword.value = ''
  showResults.value = false
  router.push(path)
}
</script>