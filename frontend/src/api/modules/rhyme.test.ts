import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import { queryRhymeByCharacter, queryRhymeByGroup } from './rhyme'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('rhyme API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('queryRhymeByCharacter', () => {
    it('调用GET /rhyme/query', async () => {
      await queryRhymeByCharacter('东')
      expect(request.get).toHaveBeenCalledWith('/rhyme/query', {
        params: { character: '东' }
      })
    })
  })

  describe('queryRhymeByGroup', () => {
    it('调用GET /rhyme/group', async () => {
      await queryRhymeByGroup('东')
      expect(request.get).toHaveBeenCalledWith('/rhyme/group', {
        params: { group: '东' }
      })
    })
  })
})