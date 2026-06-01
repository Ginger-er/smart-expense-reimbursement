import request from './request'

// 通知列表
export function getNoticeList() {
  return request.get('/notice/list')
}

// 未读数
export function getUnreadCount() {
  return request.get('/notice/unread')
}

// 标记单条已读
export function markRead(id: number) {
  return request.post(`/notice/read/${id}`)
}

// 全部已读
export function markAllRead() {
  return request.post('/notice/read-all')
}
