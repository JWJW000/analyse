import { onUnmounted, ref, watch, type Ref } from 'vue'

/**
 * 将数字从当前值缓动到目标值，用于统计卡片展示。
 */
export function useAnimatedNumber(source: Ref<number | null | undefined>, duration = 720) {
  const display = ref(0)
  let raf = 0

  function animateTo(target: number) {
    const from = display.value
    const start = performance.now()
    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / duration)
      const eased = 1 - (1 - t) ** 3
      display.value = Math.round(from + (target - from) * eased)
      if (t < 1) {
        raf = requestAnimationFrame(tick)
      } else {
        display.value = target
      }
    }
    cancelAnimationFrame(raf)
    raf = requestAnimationFrame(tick)
  }

  watch(
    source,
    (v) => {
      const n = typeof v === 'number' && !Number.isNaN(v) ? Math.max(0, v) : 0
      animateTo(n)
    },
    { immediate: true },
  )

  onUnmounted(() => cancelAnimationFrame(raf))

  return display
}
