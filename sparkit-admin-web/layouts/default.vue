<template>
  <div class="admin-layout">
    <Sidebar :collapsed="collapsed" />
    <div class="admin-main">
      <HeaderBar :collapsed="collapsed" @toggle-collapse="collapsed = !collapsed" />
      <PageTabs />
      <div class="admin-content">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
const collapsed = ref(false)
</script>