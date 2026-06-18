<template>
  <div class="icon-select">
    <el-input
      v-model="searchKey"
      placeholder="搜索图标"
      clearable
      prefix-icon="Search"
      size="small"
    />
    <div class="icon-grid">
      <div
        v-for="item in filteredIcons"
        :key="item"
        class="icon-item"
        :class="{ active: modelValue === item }"
        @click="$emit('update:modelValue', item)"
      >
        <el-icon :size="20"><component :is="item" /></el-icon>
        <span class="icon-name">{{ item }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' }
})

defineEmits(['update:modelValue'])

const searchKey = ref('')

const icons = [
  'Odometer', 'Setting', 'User', 'UserFilled', 'Avatar', 'Menu', 'OfficeBuilding',
  'Briefcase', 'Tools', 'Collection', 'CollectionTag', 'Location', 'Document',
  'Notebook', 'FolderOpened', 'Folder', 'Money', 'Bell', 'Cpu', 'Clock',
  'EditPen', 'Tickets', 'List', 'Search', 'Plus', 'Delete', 'Edit', 'Check',
  'Close', 'ArrowDown', 'ArrowRight', 'ArrowLeft', 'ArrowUp', 'Expand', 'Fold',
  'HomeFilled', 'Star', 'StarFilled', 'Warning', 'WarningFilled', 'InfoFilled',
  'CircleCheck', 'CircleCheckFilled', 'CircleClose', 'CircleCloseFilled',
  'Lock', 'Unlock', 'Key', 'Link', 'Share', 'Download', 'Upload', 'Picture',
  'VideoCamera', 'VideoPlay', 'Headset', 'Microphone', 'Message', 'ChatDotRound',
  'ChatLineSquare', 'ChatRound', 'Postcard', 'DataAnalysis', 'Histogram',
  'PieChart', 'TrendCharts', 'Coordinate', 'MagicStick', 'Operation',
  'Switch', 'SwitchButton', 'TurnOff', 'Refresh', 'RefreshRight', 'RefreshLeft',
  'Loading', 'Top', 'Bottom', 'Right', 'Back', 'DArrowLeft', 'DArrowRight',
  'Promotion', 'Sell', 'ShoppingCart', 'ShoppingBag', 'Goods', 'Present',
  'PriceTag', 'Discount', 'Wallet', 'CreditCard', 'BankCard', 'Coin',
  'Calendar', 'Timer', 'AlarmClock', 'Stopwatch', 'Sunny', 'Sunrise', 'Sunset',
  'Moon', 'MoonNight', 'PartlyCloudy', 'Cloudy', 'Lightning', 'Rain',
  'Ship', 'Van', 'Bicycle', 'Guide', 'Aim', 'Compass', 'MapLocation',
  'Connection', 'Monitor', 'Mobile', 'Phone', 'Cellphone', 'Iphone',
  'Printer', 'Mouse', 'Keyboard', 'Crop', 'FullScreen', 'ScaleToOriginal',
  'CopyDocument', 'CopyDocument', 'DocumentCopy', 'DocumentAdd', 'DocumentRemove',
  'DocumentChecked', 'DocumentDelete', 'Files', 'Notebook', 'Reading',
  'Tickets', 'Stamp', 'Medal', 'Trophy', 'TrophyBase', 'Flag'
]

const filteredIcons = computed(() => {
  if (!searchKey.value) return icons
  const kw = searchKey.value.toLowerCase()
  return icons.filter(i => i.toLowerCase().includes(kw))
})
</script>