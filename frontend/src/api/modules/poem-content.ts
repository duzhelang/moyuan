import request from '@/utils/request'

export interface PoemContentCache {
  content: string
  source: string
}

export function getCachedContent(poemTitle: string, poetName: string, contentType: string) {
  return request.get<PoemContentCache>('/poem-content/cache', {
    params: { poemTitle, poetName, contentType }
  })
}

export function saveCachedContent(data: {
  poemTitle: string
  poetName: string
  contentType: string
  content: string
  source: string
}) {
  return request.post<void>('/poem-content/cache', data)
}
