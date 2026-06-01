import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, logoutApi, getUserInfoApi } from '@/api/user'
import router from '@/router'

interface UserInfo {
  id: number
  username: string
  realName: string
  deptId: number
  role: number
  phone: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const login = async (username: string, password: string) => {
    const res = await loginApi({ username, password })
    const { token: tk, user } = res.data
    token.value = tk
    userInfo.value = user
    localStorage.setItem('token', tk)
    localStorage.setItem('role', String(user.role))
  }

  const logout = async () => {
    try { await logoutApi() } catch { /* ignore */ }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    router.push('/login')
  }

  const fetchUserInfo = async () => {
    try {
      const res = await getUserInfoApi()
      userInfo.value = res.data
      // 同步 role 到 localStorage（供路由守卫读取，兼容老用户无 role 缓存）
      localStorage.setItem('role', String(res.data.role))
    } catch {
      logout()
    }
  }

  return { token, userInfo, login, logout, fetchUserInfo }
})
