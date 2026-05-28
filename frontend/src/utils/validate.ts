export function isValidUsername(username: string): boolean {
  const reg = /^[a-zA-Z0-9_]{4,16}$/
  return reg.test(username)
}

export function isValidPassword(password: string): boolean {
  const reg = /^[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]{6,20}$/
  return reg.test(password)
}

export function isValidEmail(email: string): boolean {
  const reg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  return reg.test(email)
}

export function isValidPhone(phone: string): boolean {
  const reg = /^1[3-9]\d{9}$/
  return reg.test(phone)
}

export function isValidIdCard(idCard: string): boolean {
  const reg = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
  return reg.test(idCard)
}

export function isValidUrl(url: string): boolean {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

export function isEmpty(value: any): boolean {
  if (value === null || value === undefined) {
    return true
  }
  if (typeof value === 'string') {
    return value.trim() === ''
  }
  if (Array.isArray(value)) {
    return value.length === 0
  }
  if (typeof value === 'object') {
    return Object.keys(value).length === 0
  }
  return false
}

export function isNumeric(value: string): boolean {
  return !isNaN(parseFloat(value)) && isFinite(value as any)
}

export function hasSpecialChar(str: string): boolean {
  const reg = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/
  return reg.test(str)
}

export function isStrongPassword(password: string): boolean {
  const hasUpperCase = /[A-Z]/.test(password)
  const hasLowerCase = /[a-z]/.test(password)
  const hasNumbers = /\d/.test(password)
  const hasSpecialChar = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)
  const isLongEnough = password.length >= 8
  
  return hasUpperCase && hasLowerCase && hasNumbers && hasSpecialChar && isLongEnough
}