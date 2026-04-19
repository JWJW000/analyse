import { describe, expect, it } from 'vitest'
import axios, { AxiosError } from 'axios'

import { toApiError } from './http'

describe('toApiError', () => {
  it('uses backend envelope message when present', () => {
    const err = new AxiosError('Request failed with status code 400')
    err.response = {
      data: { ok: false, message: '用户名至少 3 位' },
      status: 400,
      statusText: 'Bad Request',
      headers: {},
      config: { headers: new axios.AxiosHeaders() },
    }

    expect(toApiError(err).message).toBe('用户名至少 3 位')
  })

  it('falls back to generic message for non-error values', () => {
    expect(toApiError(null).message).toBe('请求失败')
  })
})
