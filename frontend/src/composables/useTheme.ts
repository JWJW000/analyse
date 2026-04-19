import { ref, watch } from 'vue'
import { useStorage } from '@vueuse/core'

const isDark = useStorage('ethics-sra-theme', false)
const colorScheme = ref<'light' | 'dark'>('light')

function applyTheme(dark: boolean) {
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
  colorScheme.value = dark ? 'dark' : 'light'
}

function toggleTheme() {
  isDark.value = !isDark.value
}

function setTheme(dark: boolean) {
  isDark.value = dark
}

function initTheme() {
  applyTheme(isDark.value)
}

watch(isDark, (dark) => {
  applyTheme(dark)
}, { immediate: false })

export function useTheme() {
  return {
    isDark,
    colorScheme,
    applyTheme,
    toggleTheme,
    setTheme,
    initTheme,
  }
}
