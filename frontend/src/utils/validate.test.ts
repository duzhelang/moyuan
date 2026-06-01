import { describe, it, expect } from 'vitest'
import {
  isValidUsername,
  isValidPassword,
  isValidEmail,
  isValidPhone,
  isValidIdCard,
  isValidUrl,
  isEmpty,
  isNumeric,
  hasSpecialChar,
  isStrongPassword
} from './validate'

describe('isValidUsername', () => {
  it('有效用户名', () => {
    expect(isValidUsername('test_user')).toBe(true)
    expect(isValidUsername('user123')).toBe(true)
    expect(isValidUsername('abcd')).toBe(true)
    expect(isValidUsername('a'.repeat(16))).toBe(true)
  })

  it('无效用户名', () => {
    expect(isValidUsername('abc')).toBe(false)
    expect(isValidUsername('a'.repeat(17))).toBe(false)
    expect(isValidUsername('user@name')).toBe(false)
    expect(isValidUsername('user name')).toBe(false)
    expect(isValidUsername('')).toBe(false)
  })
})

describe('isValidPassword', () => {
  it('有效密码', () => {
    expect(isValidPassword('password')).toBe(true)
    expect(isValidPassword('Pass123')).toBe(true)
    expect(isValidPassword('P@ssw0rd')).toBe(true)
    expect(isValidPassword('a'.repeat(20))).toBe(true)
  })

  it('无效密码', () => {
    expect(isValidPassword('abc')).toBe(false)
    expect(isValidPassword('a'.repeat(21))).toBe(false)
    expect(isValidPassword('')).toBe(false)
  })
})

describe('isValidEmail', () => {
  it('有效邮箱', () => {
    expect(isValidEmail('test@example.com')).toBe(true)
    expect(isValidEmail('user.name@domain.co.uk')).toBe(true)
    expect(isValidEmail('user+tag@example.org')).toBe(true)
  })

  it('无效邮箱', () => {
    expect(isValidEmail('test@')).toBe(false)
    expect(isValidEmail('@example.com')).toBe(false)
    expect(isValidEmail('test@example')).toBe(false)
    expect(isValidEmail('test.example.com')).toBe(false)
    expect(isValidEmail('')).toBe(false)
  })
})

describe('isValidPhone', () => {
  it('有效手机号', () => {
    expect(isValidPhone('13812345678')).toBe(true)
    expect(isValidPhone('15912345678')).toBe(true)
    expect(isValidPhone('18812345678')).toBe(true)
    expect(isValidPhone('17712345678')).toBe(true)
  })

  it('无效手机号', () => {
    expect(isValidPhone('12345678901')).toBe(false)
    expect(isValidPhone('1381234567')).toBe(false)
    expect(isValidPhone('138123456789')).toBe(false)
    expect(isValidPhone('')).toBe(false)
  })
})

describe('isValidIdCard', () => {
  it('有效身份证号', () => {
    expect(isValidIdCard('123456789012345')).toBe(true)
    expect(isValidIdCard('123456789012345678')).toBe(true)
    expect(isValidIdCard('12345678901234567X')).toBe(true)
    expect(isValidIdCard('12345678901234567x')).toBe(true)
  })

  it('无效身份证号', () => {
    expect(isValidIdCard('12345678901234')).toBe(false)
    expect(isValidIdCard('1234567890123456')).toBe(false)
    expect(isValidIdCard('1234567890123456789')).toBe(false)
    expect(isValidIdCard('')).toBe(false)
  })
})

describe('isValidUrl', () => {
  it('有效URL', () => {
    expect(isValidUrl('https://example.com')).toBe(true)
    expect(isValidUrl('http://example.com/path?query=1')).toBe(true)
    expect(isValidUrl('ftp://example.com')).toBe(true)
    expect(isValidUrl('https://sub.domain.example.com:8080/path')).toBe(true)
  })

  it('无效URL', () => {
    expect(isValidUrl('example.com')).toBe(false)
    expect(isValidUrl('://example.com')).toBe(false)
    expect(isValidUrl('')).toBe(false)
  })
})

describe('isEmpty', () => {
  it('空值', () => {
    expect(isEmpty(null)).toBe(true)
    expect(isEmpty(undefined)).toBe(true)
    expect(isEmpty('')).toBe(true)
    expect(isEmpty('   ')).toBe(true)
    expect(isEmpty([])).toBe(true)
    expect(isEmpty({})).toBe(true)
  })

  it('非空值', () => {
    expect(isEmpty('hello')).toBe(false)
    expect(isEmpty([1, 2, 3])).toBe(false)
    expect(isEmpty({ key: 'value' })).toBe(false)
    expect(isEmpty(0)).toBe(false)
    expect(isEmpty(false)).toBe(false)
  })
})

describe('isNumeric', () => {
  it('数字字符串', () => {
    expect(isNumeric('123')).toBe(true)
    expect(isNumeric('12.34')).toBe(true)
    expect(isNumeric('-123')).toBe(true)
    expect(isNumeric('0')).toBe(true)
    expect(isNumeric('1e10')).toBe(true)
  })

  it('非数字字符串', () => {
    expect(isNumeric('abc')).toBe(false)
    expect(isNumeric('')).toBe(false)
    expect(isNumeric('12abc')).toBe(false)
    expect(isNumeric('NaN')).toBe(false)
  })
})

describe('hasSpecialChar', () => {
  it('包含特殊字符', () => {
    expect(hasSpecialChar('test@123')).toBe(true)
    expect(hasSpecialChar('pass#word')).toBe(true)
    expect(hasSpecialChar('hello!')).toBe(true)
    expect(hasSpecialChar('test$%^')).toBe(true)
  })

  it('不包含特殊字符', () => {
    expect(hasSpecialChar('test123')).toBe(false)
    expect(hasSpecialChar('hello world')).toBe(false)
    expect(hasSpecialChar('')).toBe(false)
  })
})

describe('isStrongPassword', () => {
  it('强密码', () => {
    expect(isStrongPassword('Pass@1234')).toBe(true)
    expect(isStrongPassword('Str0ng!Pass')).toBe(true)
    expect(isStrongPassword('C0mplex@Pass')).toBe(true)
  })

  it('弱密码', () => {
    expect(isStrongPassword('password')).toBe(false)
    expect(isStrongPassword('PASSWORD')).toBe(false)
    expect(isStrongPassword('12345678')).toBe(false)
    expect(isStrongPassword('Pass@12')).toBe(false)
    expect(isStrongPassword('Pass@123456')).toBe(false)
  })
})