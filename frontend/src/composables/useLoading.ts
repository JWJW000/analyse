import { ref } from 'vue'

const isInitialLoading = ref(true)
const isPageLoading = ref(false)

export function useLoading() {
  function finishInitialLoad() {
    setTimeout(() => {
      isInitialLoading.value = false
    }, 800)
  }

  function startPageLoad() {
    isPageLoading.value = true
  }

  function finishPageLoad() {
    isPageLoading.value = false
  }

  return {
    isInitialLoading,
    isPageLoading,
    finishInitialLoad,
    startPageLoad,
    finishPageLoad,
  }
}
