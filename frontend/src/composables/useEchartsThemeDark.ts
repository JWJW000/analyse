import type { EChartsOption } from 'echarts'

export const ethicsSraThemeDark = {
  color: ['#4a90d9', '#63b3ed', '#f6e05e', '#48bb78', '#ed8936', '#fc8181', '#63b3ed'],
  backgroundColor: 'transparent',
  textStyle: {
    fontFamily: '"PingFang SC", "Microsoft YaHei", sans-serif',
    color: '#f7fafc',
  },
  title: {
    textStyle: {
      color: '#f7fafc',
      fontWeight: 600,
    },
  },
  legend: {
    textStyle: {
      color: '#a0aec0',
    },
  },
  tooltip: {
    backgroundColor: 'rgba(45, 55, 72, 0.95)',
    borderColor: '#4a5568',
    textStyle: {
      color: '#f7fafc',
    },
  },
  categoryAxis: {
    axisLine: {
      lineStyle: {
        color: '#4a5568',
      },
    },
    axisTick: {
      lineStyle: {
        color: '#4a5568',
      },
    },
    axisLabel: {
      color: '#a0aec0',
    },
  },
  valueAxis: {
    axisLine: {
      lineStyle: {
        color: '#4a5568',
      },
    },
    axisTick: {
      lineStyle: {
        color: '#4a5568',
      },
    },
    axisLabel: {
      color: '#a0aec0',
    },
    splitLine: {
      lineStyle: {
        color: '#2d3748',
      },
    },
  },
}

export function useEchartsThemeDark() {
  function getOption(base?: EChartsOption): EChartsOption {
    return {
      ...ethicsSraThemeDark,
      ...base,
    }
  }

  return {
    theme: ethicsSraThemeDark,
    getOption,
  }
}