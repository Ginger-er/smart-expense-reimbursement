import request from './request'
import type { AxiosResponse } from 'axios'

interface LoginRes {
  token: string
  user: {
    id: number
    username: string
    realName: string
    deptId: number
    role: number
    phone: string
  }
}

// 登录
export function loginApi(data: { username: string; password: string }) {
  return request.post<any, AxiosResponse<LoginRes>>('/user/login', data)
}

// 登出
export function logoutApi() {
  return request.post('/user/logout')
}

// 用户自助注册（默认员工角色）
export function registerUser(data: { username: string; password: string; realName: string; deptId: number }) {
  return request.post('/user/register', data)
}

// 获取当前用户信息
export function getUserInfoApi() {
  return request.get('/user/info')
}

// 获取用户列表
export function getUserList(params: { pageNum?: number; pageSize?: number; keyword?: string; deptId?: number; status?: number }) {
  return request.get('/user/list', { params })
}

// 创建用户
export function createUser(data: any) {
  return request.post('/user/create', data)
}

// 更新用户
export function updateUser(data: any) {
  return request.put('/user/update', data)
}

// 删除用户
export function deleteUser(id: number) {
  return request.delete(`/user/${id}`)
}

// 修改当前用户密码
export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return request.post('/user/password', data)
}

// 更新当前用户个人信息（姓名/手机号）
export function updateProfile(data: { realName: string; phone?: string }) {
  return request.put('/user/profile', data)
}
