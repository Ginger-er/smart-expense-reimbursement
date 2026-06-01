import request from './request'

// 部门列表
export function getDeptList() {
  return request.get('/dept/list')
}

// 部门树
export function getDeptTree() {
  return request.get('/dept/tree')
}

// 创建部门
export function createDept(data: any) {
  return request.post('/dept/create', data)
}

// 更新部门
export function updateDept(data: any) {
  return request.put('/dept/update', data)
}

// 删除部门
export function deleteDept(id: number) {
  return request.delete(`/dept/${id}`)
}
