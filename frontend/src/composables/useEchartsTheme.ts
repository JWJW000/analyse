import type { EChartsOption } from 'echarts'

export const ethicsSraTheme = {
  color: ['#1a365d', '#2c5282', '#d69e2e', '#38a169', '#dd6b20', '#e53e3e', '#3182ce'],
  backgroundColor: 'transparent',
  textStyle: {
    fontFamily: '"PingFang SC", "Microsoft YaHei", sans-serif',
    color: '#2d3748',
  },
  title: {
    textStyle: {
      color: '#2d3748',
      fontWeight: 600,
    },
  },
  legend: {
    textStyle: {
      color: '#718096',
    },
  },
  tooltip: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#e2e8f0',
    textStyle: {
      color: '#2d3748',
    },
  },
  categoryAxis: {
    axisLine: {
      lineStyle: {
        color: '#e2e8f0',
      },
    },
    axisTick: {
      lineStyle: {
        color: '#e2e8f0',
      },
    },
    axisLabel: {
      color: '#718096',
    },
  },
  valueAxis: {
    axisLine: {
      lineStyle: {
        color: '#e2e8f0',
      },
    },
    axisTick: {
      lineStyle: {
        color: '#e2e8f0',
      },
    },
    axisLabel: {
      color: '#718096',
    },
    splitLine: {
      lineStyle: {
        color: '#f0f0f0',
      },
    },
  },
}

export function useEchartsTheme() {
  function getOption(base?: EChartsOption): EChartsOption {
    return {
      ...ethicsSraTheme,
      ...base,
    }
  }

  return {
    theme: ethicsSraTheme,
    getOption,
  }
}