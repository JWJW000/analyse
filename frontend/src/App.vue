<script setup lang="ts">
import { ref, onMounted, provide } from 'vue'
import { RouterView } from 'vue-router'
import InitialLoader from '@/components/common/InitialLoader.vue'
import SearchModal from '@/components/common/SearchModal.vue'

const searchOpen = ref(false)
const isLoading = ref(true)

provide('isLoading', isLoading)

onMounted(() => {
  setTimeout(() => {
    isLoading.value = false
  }, 1500)
})
</script>

<template>
  <InitialLoader v-if="isLoading" />
  
  <template v-else>
    <router-view v-slot="{ Component, route }">
      <transition name="page" mode="out-in">
        <!-- 保护：部分路由切换/异步解析期间 Component 可能为空，Transition 卸载时会触发运行时异常 -->
        <component :is="Component || 'div'" :key="route.fullPath || route.path" />
      </transition>
    </router-view>
    
    <SearchModal v-model:open="searchOpen" />
  </template>
</template>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
}
</style>
