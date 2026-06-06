import request from '@/utils/request'

export interface RhymeItem {
  id: number
  character: string
  rhymeGroup: string
  toneType: string
  rhymeCategory: string
  sortOrder: number
}

export function queryRhymeByCharacter(character: string) {
  return request.get<RhymeItem[]>('/rhyme/query', { params: { character } })
}

export function queryRhymeByGroup(group: string) {
  return request.get<RhymeItem[]>('/rhyme/group', { params: { group } })
}
