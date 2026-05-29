<script setup>
import Sidebar from './Sidebar.vue'
</script>

<template>
  <div class="app-layout">
    <Sidebar />
    <main class="main-content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100%;
}
.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
</style>
