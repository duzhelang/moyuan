import { describe, it, expect } from 'vitest'
import { buildPoetPrompt } from './ai-prompt'

describe('ai-prompt utils', () => {
  describe('buildPoetPrompt', () => {
    it('使用默认模板构建提示词', () => {
      const result = buildPoetPrompt('李白是谁？', {
        poetName: '李白'
      })
      
      expect(result).toContain('【系统设定】')
      expect(result).toContain('【用户问题】')
      expect(result).toContain('李白')
      expect(result).toContain('李白是谁？')
    })

    it('首次提问包含简要概括提示', () => {
      const result = buildPoetPrompt('介绍一下杜甫', {
        poetName: '杜甫',
        isFirst: true
      })
      
      expect(result).toContain('首次提问')
      expect(result).toContain('2-3句话')
    })

    it('非首次提问包含详细回答提示', () => {
      const result = buildPoetPrompt('杜甫的代表作？', {
        poetName: '杜甫',
        isFirst: false
      })
      
      expect(result).toContain('150字')
    })

    it('使用自定义promptTemplate', () => {
      const config = {
        promptTemplate: '你是{poetName}研究专家，回答不超过{maxLength}字',
        maxLength: 200,
        firstResponseLength: 100
      }
      
      const result = buildPoetPrompt('李白的生平', {
        poetName: '李白',
        config
      })
      
      expect(result).toContain('李白研究专家')
      expect(result).toContain('200字')
    })

    it('自定义模板首次提问使用firstResponseLength', () => {
      const config = {
        promptTemplate: '你是{poetName}研究专家，{styleHint}',
        maxLength: 200,
        firstResponseLength: 100
      }
      
      const result = buildPoetPrompt('李白是谁', {
        poetName: '李白',
        isFirst: true,
        config
      })
      
      expect(result).toContain('100字')
    })

    it('config为null时使用默认模板', () => {
      const result = buildPoetPrompt('测试问题', {
        poetName: '测试诗人',
        config: null
      })
      
      expect(result).toContain('古典诗词文化助手')
      expect(result).toContain('测试诗人')
    })
  })
})
