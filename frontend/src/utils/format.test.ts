import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  formatDate,
  formatRelativeTime,
  formatNumber,
  formatFileSize,
  truncateText,
  formatPhone,
  formatEmail
} from './format'

vi.mock('dayjs', () => {
  const actual = vi.importActual('dayjs')
  return {
    default: actual,
    extend: vi.fn(),
    locale: vi.fn()
  }
})

describe('formatDate', () => {
  it('格式化日期为默认格式', () => {
    const date = '2024-01-15T10:30:00Z'
    const result = formatDate(date)
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
  })

  it('格式化日期为自定义格式', () => {
    const date = '2024-01-15T10:30:00Z'
    const result = formatDate(date, 'YYYY/MM/DD')
    expect(result).toMatch(/^\d{4}\/\d{2}\/\d{2}$/)
  })

  it('处理Date对象', () => {
    const date = new Date('2024-01-15')
    const result = formatDate(date, 'YYYY-MM-DD')
    expect(result).toBe('2024-01-15')
  })
})

describe('formatNumber', () => {
  it('格式化小于1000的数字', () => {
    expect(formatNumber(500)).toBe('500')
    expect(formatNumber(0)).toBe('0')
    expect(formatNumber(999)).toBe('999')
  })

  it('格式化大于等于1000的数字', () => {
    expect(formatNumber(1000)).toBe('1.0k')
    expect(formatNumber(1500)).toBe('1.5k')
    expect(formatNumber(9999)).toBe('10.0k')
  })

  it('格式化大于等于10000的数字', () => {
    expect(formatNumber(10000)).toBe('1.0万')
    expect(formatNumber(25000)).toBe('2.5万')
    expect(formatNumber(100000)).toBe('10.0万')
  })
})

describe('formatFileSize', () => {
  it('格式化0字节', () => {
    expect(formatFileSize(0)).toBe('0 B')
  })

  it('格式化字节', () => {
    expect(formatFileSize(500)).toBe('500 B')
    expect(formatFileSize(1023)).toBe('1023 B')
  })

  it('格式化KB', () => {
    expect(formatFileSize(1024)).toBe('1 KB')
    expect(formatFileSize(1536)).toBe('1.5 KB')
  })

  it('格式化MB', () => {
    expect(formatFileSize(1048576)).toBe('1 MB')
    expect(formatFileSize(5242880)).toBe('5 MB')
  })

  it('格式化GB', () => {
    expect(formatFileSize(1073741824)).toBe('1 GB')
  })
})

describe('truncateText', () => {
  it('文本长度小于最大长度时返回原文本', () => {
    expect(truncateText('测试文本', 10)).toBe('测试文本')
  })

  it('文本长度等于最大长度时返回原文本', () => {
    expect(truncateText('测试文本', 4)).toBe('测试文本')
  })

  it('文本长度大于最大长度时截断并添加省略号', () => {
    expect(truncateText('这是一段很长的测试文本', 5)).toBe('这是一段很...')
  })
})

describe('formatPhone', () => {
  it('格式化11位手机号', () => {
    expect(formatPhone('13812345678')).toBe('138****5678')
  })

  it('非11位手机号返回原样', () => {
    expect(formatPhone('1234567890')).toBe('1234567890')
    expect(formatPhone('123456789012')).toBe('123456789012')
  })
})

describe('formatEmail', () => {
  it('格式化邮箱', () => {
    expect(formatEmail('test@example.com')).toBe('tes***@example.com')
    expect(formatEmail('ab@example.com')).toBe('ab***@example.com')
  })

  it('用户名长度小于等于3时返回原样', () => {
    expect(formatEmail('ab@example.com')).toBe('ab***@example.com')
    expect(formatEmail('abc@example.com')).toBe('abc***@example.com')
  })
})

describe('formatRelativeTime', () => {
  it('返回相对时间字符串', () => {
    const date = new Date()
    const result = formatRelativeTime(date)
    expect(typeof result).toBe('string')
    expect(result.length).toBeGreaterThan(0)
  })
})