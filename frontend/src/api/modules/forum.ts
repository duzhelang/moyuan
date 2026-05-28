import request from '@/utils/request'
import type { ForumPost, Comment, PageResult } from '@/types/model'
import type { ForumPostCreateRequest, ForumPostUpdateRequest, CommentCreateRequest } from '@/types/api'

export function getForumPostList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get<PageResult<ForumPost>>('/forum/posts', { params })
}

export function getForumPostById(id: number) {
  return request.get<ForumPost>(`/forum/posts/${id}`)
}

export function createForumPost(data: ForumPostCreateRequest) {
  return request.post<ForumPost>('/forum/posts', data)
}

export function updateForumPost(data: ForumPostUpdateRequest) {
  return request.put<ForumPost>(`/forum/posts/${data.id}`, data)
}

export function deleteForumPost(id: number) {
  return request.delete<void>(`/forum/posts/${id}`)
}

export function likeForumPost(id: number) {
  return request.post<void>(`/forum/posts/${id}/like`)
}

export function getComments(targetId: number, targetType: number, params?: { page?: number; size?: number }) {
  return request.get<PageResult<Comment>>('/comments', { 
    params: { targetId, targetType, ...params } 
  })
}

export function createComment(data: CommentCreateRequest) {
  return request.post<Comment>('/comments', data)
}

export function deleteComment(id: number) {
  return request.delete<void>(`/comments/${id}`)
}

export function likeComment(id: number) {
  return request.post<void>(`/comments/${id}/like`)
}