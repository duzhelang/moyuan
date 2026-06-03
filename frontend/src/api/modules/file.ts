import request from '@/utils/request'

export function uploadFile(file: File, fileType?: string, relatedId?: number, relatedType?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (fileType) formData.append('fileType', fileType)
  if (relatedId) formData.append('relatedId', relatedId.toString())
  if (relatedType) formData.append('relatedType', relatedType)
  return request.post<{ url: string; fileName: string; fileKey: string }>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteFile(fileKey: string) {
  return request.delete<void>(`/files/${fileKey}`)
}

export function getFileInfo(fileKey: string) {
  return request.get<any>(`/files/${fileKey}`)
}
