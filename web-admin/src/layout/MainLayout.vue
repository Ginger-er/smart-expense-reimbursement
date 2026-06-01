<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <aside class="layout-sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <!-- Logo -->
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 36 36" fill="none">
            <rect width="36" height="36" rx="10" fill="url(#sGrad)" />
            <path d="M11 13h14M11 19h9M11 25h5" stroke="white" stroke-width="2" stroke-linecap="round" />
            <circle cx="25" cy="23" r="4.5" fill="#fff" opacity="0.95" />
            <path d="M23.5 22l2 2 3-2.5" stroke="#0f172a" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
            <defs><linearGradient id="sGrad" x1="0" y1="0" x2="36" y2="36"><stop stop-color="#1a1a2e"/><stop offset="1" stop-color="#4f46e5"/></linearGradient></defs>
          </svg>
        </div>
        <div class="brand-text" v-show="!appStore.sidebarCollapsed">
          <span class="brand-name">SmartExpense</span>
          <span class="brand-sub">智能差旅报销</span>
        </div>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <div class="nav-section" v-show="!appStore.sidebarCollapsed">
          <span class="nav-section-title">导航</span>
        </div>

        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          router
          background-color="transparent"
          class="sidebar-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>工作台</span>
          </el-menu-item>

          <el-menu-item v-if="userRole >= 2" index="/approval">
            <el-icon><Checked /></el-icon>
            <span>审批中心</span>
            <span class="menu-badge">{{ pendingApproval }}</span>
          </el-menu-item>
        </el-menu>

        <div class="nav-section" v-show="!appStore.sidebarCollapsed">
          <span class="nav-section-title">业务</span>
        </div>

        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          router
          background-color="transparent"
          class="sidebar-menu"
        >
          <el-sub-menu index="reimb-group">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>报销管理</span>
            </template>
            <el-menu-item index="/reimbursement">报销列表</el-menu-item>
            <el-menu-item index="/reimbursement/create">新建报销</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="trip-group">
            <template #title>
              <el-icon><Promotion /></el-icon>
              <span>出差申请</span>
            </template>
            <el-menu-item index="/trip">申请列表</el-menu-item>
            <el-menu-item index="/trip/create">新建申请</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="invoice-group">
            <template #title>
              <el-icon><Tickets /></el-icon>
              <span>发票管理</span>
            </template>
            <el-menu-item index="/invoice">发票列表</el-menu-item>
            <el-menu-item index="/invoice/upload">上传发票</el-menu-item>
          </el-sub-menu>
        </el-menu>

        <div class="nav-section" v-show="!appStore.sidebarCollapsed">
          <span class="nav-section-title">系统</span>
        </div>

        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          router
          background-color="transparent"
          class="sidebar-menu"
        >
          <el-menu-item v-if="userRole >= 2" index="/report">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据报表</span>
          </el-menu-item>

          <el-sub-menu v-if="userRole === 4" index="sys-group">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/user">用户管理</el-menu-item>
            <el-menu-item index="/system/dept">部门管理</el-menu-item>
            <el-menu-item index="/system/operlog">操作日志</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </nav>

      <!-- 折叠按钮 -->
      <div class="sidebar-footer" @click="appStore.toggleSidebar()">
        <el-icon :size="18"><Fold v-if="!appStore.sidebarCollapsed" /><Expand v-else /></el-icon>
        <span v-if="!appStore.sidebarCollapsed">收起</span>
      </div>
    </aside>

    <!-- 右侧主体 -->
    <div class="layout-body">
      <!-- 顶栏 -->
      <header class="layout-header">
        <div class="header-left">
          <div class="breadcrumb">
            <span class="breadcrumb-root" @click="router.push('/dashboard')">首页</span>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-current">{{ currentPageTitle }}</span>
          </div>
        </div>

        <div class="header-right">
          <!-- 消息通知 -->
          <button class="header-icon-btn" @click="handleOpenNotice">
            <el-icon :size="18"><Message /></el-icon>
            <span class="notify-badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </button>

          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="header-user">
              <el-avatar :size="34" class="user-avatar">
                {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-meta" v-if="userStore.userInfo">
                <span class="user-name">{{ userStore.userInfo.realName }}</span>
                <span class="user-role">{{ roleLabel(userStore.userInfo.role) }}</span>
              </div>
              <el-icon :size="14" class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 消息通知对话框 -->
    <el-dialog v-model="notifyVisible" title="消息通知" width="460px">
      <div class="notify-dialog-list">
        <div v-for="n in notifications" :key="n.id" class="notify-item" :class="{ unread: n.isRead !== 1 }" @click="handleNotify(n)">
          <div class="notify-main">
            <span class="notify-text">{{ n.title }}</span>
            <span class="notify-content">{{ n.content }}</span>
          </div>
          <span class="notify-time">{{ formatDate(n.createTime, 'MM-DD HH:mm') }}</span>
        </div>
        <div v-if="!notifications.length" class="notify-empty">暂无消息</div>
      </div>
      <template #footer>
        <el-button @click="handleMarkAllRead" :disabled="!notifications.length">全部已读</el-button>
      </template>
    </el-dialog>

    <!-- 个人信息对话框 -->
    <el-dialog v-model="profileVisible" title="个人信息" width="440px">
      <div class="profile-box">
        <div class="profile-head">
          <el-avatar :size="60" class="profile-avatar">
            {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="profile-head-meta">
            <div class="profile-name">{{ userStore.userInfo?.realName }}</div>
            <div class="profile-role">{{ roleLabel(userStore.userInfo?.role ?? 1) }}</div>
          </div>
        </div>
        <el-form label-width="80px">
          <el-form-item label="姓名">
            <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input :model-value="userStore.userInfo?.username" disabled />
          </el-form-item>
          <el-form-item label="所属部门">
            <el-input :model-value="deptLabel(userStore.userInfo?.deptId)" disabled />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 系统设置对话框 -->
    <el-dialog v-model="settingsVisible" title="系统设置" width="440px">
      <el-form label-width="90px">
        <el-form-item label="消息通知">
          <el-switch v-model="settings.notify" />
        </el-form-item>
        <el-form-item label="邮件提醒">
          <el-switch v-model="settings.email" />
        </el-form-item>
        <el-form-item label="免打扰模式">
          <el-switch v-model="settings.dnd" />
        </el-form-item>
        <el-divider style="margin: 16px 0" />
        <el-form-item label="旧密码">
          <el-input v-model="settings.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="settings.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="settings.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { updatePassword, updateProfile } from '@/api/user'
import { getDeptList } from '@/api/dept'
import { getDashboardStats } from '@/api/dashboard'
import { getNoticeList, getUnreadCount, markRead, markAllRead } from '@/api/notice'
import { formatDate } from '@/utils/format'
import {
  HomeFilled, Checked, Document, Promotion, Tickets,
  DataAnalysis, Setting, Fold, Expand, Message, ArrowDown, User, SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

// 刷新页面后 userInfo 会丢失（存内存），这里恢复，否则新建出差/报销时 userId 为空导致后端 500
onMounted(() => {
  userStore.fetchUserInfo()
  loadPendingCount()
  loadNotice()
  loadDepts()
})

const notifications = ref<any[]>([])
const unreadCount = ref(0)

// 待处理数量（用于侧边栏角标）
const pendingApproval = ref(0)

// 待审批角标：与审批中心口径一致（报销/出差 status 1,2）
const loadPendingCount = async () => {
  try {
    const res: any = await getDashboardStats()
    pendingApproval.value = res.data?.pendingApproval ?? 0
  } catch {
    /* 忽略 */
  }
}

// 站内通知：列表 + 未读数
const loadNotice = async () => {
  try {
    const res: any = await getNoticeList()
    notifications.value = res.data || []
    const unread: any = await getUnreadCount()
    unreadCount.value = unread.data ?? 0
  } catch {
    /* 忽略 */
  }
}

const handleOpenNotice = () => {
  loadNotice()
  notifyVisible.value = true
}

const handleNotify = async (n: any) => {
  if (n.isRead !== 1) {
    try {
      await markRead(n.id)
      n.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      /* 忽略 */
    }
  }
  notifyVisible.value = false
  if (n.link) router.push(n.link)
}

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/reimbursement')) return '/reimbursement'
  if (path.startsWith('/trip')) return '/trip'
  if (path.startsWith('/invoice')) return '/invoice'
  if (path.startsWith('/system/operlog')) return '/system/operlog'
  if (path.startsWith('/system')) return '/system/user'
  return path
})

const currentPageTitle = computed(() => {
  const meta = route.meta as { title?: string }
  return (meta?.title as string) || '工作台'
})

const userRole = computed(() => userStore.userInfo?.role ?? 1)

const roleLabel = (role: number) => {
  const map: Record<number, string> = { 1: '员工', 2: '领导', 3: '财务', 4: '管理员' }
  return map[role] || '员工'
}

const notifyVisible = ref(false)
const profileVisible = ref(false)
const settingsVisible = ref(false)
const settings = reactive({
  notify: true,
  email: true,
  dnd: false,
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const profileForm = reactive({
  realName: '',
  phone: ''
})

const handleMarkAllRead = async () => {
  try {
    await markAllRead()
    notifications.value.forEach(n => n.isRead = 1)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch {
    /* 忽略 */
  }
}

const deptOptions = ref<{ id: number; deptName: string }[]>([])

const loadDepts = async () => {
  try {
    const res: any = await getDeptList()
    deptOptions.value = res.data || []
  } catch {
    /* 忽略 */
  }
}

const deptLabel = (id?: number) => {
  if (id == null) return '—'
  return deptOptions.value.find(d => d.id === id)?.deptName || '—'
}

const openProfile = () => {
  profileForm.realName = userStore.userInfo?.realName || ''
  profileForm.phone = userStore.userInfo?.phone || ''
  profileVisible.value = true
}

const saveProfile = async () => {
  const realName = profileForm.realName?.trim()
  if (!realName) {
    ElMessage.warning('姓名不能为空')
    return
  }
  try {
    await updateProfile({ realName, phone: profileForm.phone })
    if (userStore.userInfo) {
      userStore.userInfo.realName = realName
      userStore.userInfo.phone = profileForm.phone
    }
    ElMessage.success('个人信息已更新')
    profileVisible.value = false
  } catch {
    /* 错误已由拦截器提示 */
  }
}

const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      openProfile()
      break
    case 'settings':
      settingsVisible.value = true
      break
    case 'logout':
      userStore.logout().then(() => router.push('/login'))
      break
  }
}

const saveSettings = async () => {
  // 填写了新密码才执行改密码（校验旧密码）
  if (settings.newPassword) {
    if (!settings.oldPassword) {
      ElMessage.warning('请输入旧密码')
      return
    }
    if (settings.newPassword !== settings.confirmPassword) {
      ElMessage.warning('两次输入的新密码不一致')
      return
    }
    try {
      await updatePassword({ oldPassword: settings.oldPassword, newPassword: settings.newPassword })
      ElMessage.success('密码修改成功，下次登录请使用新密码')
      settings.oldPassword = ''
      settings.newPassword = ''
      settings.confirmPassword = ''
    } catch {
      /* 错误已由拦截器提示（如旧密码错误） */
      return
    }
  }
  ElMessage.success('设置已保存')
  settingsVisible.value = false
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

// ===== 侧边栏 =====
.layout-sidebar {
  width: $sidebar-width;
  background: $bg-sidebar;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.04);

  &.collapsed {
    width: 68px;
  }
}

// ---- 品牌区 ----
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.brand-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.brand-text {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  white-space: nowrap;

  .brand-name {
    font-size: 15px;
    font-weight: 700;
    color: #fff;
    letter-spacing: -0.2px;
  }

  .brand-sub {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.35);
    margin-top: 1px;
  }
}

// ---- 导航区 ----
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
}

.nav-section {
  padding: 16px 22px 6px;

  .nav-section-title {
    font-size: 10px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 1.2px;
    color: rgba(255, 255, 255, 0.28);
  }
}

.sidebar-menu {
  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    margin: 2px 12px;
    border-radius: 10px;
    height: 42px;
    line-height: 42px;
    color: rgba(255, 255, 255, 0.55);
    font-size: 13px;
    transition: all 0.2s ease;

    .el-icon {
      width: 20px;
      font-size: 16px;
      margin-right: 10px;
      flex-shrink: 0;
    }

    &:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.06) !important;
    }
  }

  :deep(.el-menu-item) {
    &.is-active {
      color: #fff !important;
      background: linear-gradient(90deg, rgba(79, 70, 229, 0.24), rgba(79, 70, 229, 0.05)) !important;
      font-weight: 600;
      box-shadow: inset 0 0 0 1px rgba(79, 70, 229, 0.28);

      .el-icon {
        color: $color-accent;
      }
    }
  }

  :deep(.el-sub-menu__title) {
    .el-sub-menu__icon-arrow {
      color: rgba(255, 255, 255, 0.3);
    }
  }

  :deep(.el-menu) {
    background: transparent !important;

    .el-menu-item {
      padding-left: 58px !important;
      margin: 1px 12px;
      font-size: 13px;
      height: 36px;
      line-height: 36px;

      &.is-active {
        background: rgba(79, 70, 229, 0.14) !important;
        box-shadow: none;
      }
    }
  }
}

.menu-badge {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.75);
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  flex-shrink: 0;
}

// ---- 折叠按钮 ----
.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 48px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  font-size: 13px;
  flex-shrink: 0;
  transition: all 0.2s;

  &:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.06);
  }
}

// ===== 右侧主体 =====
.layout-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

// ===== 顶栏 =====
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $header-height;
  padding: 0 28px;
  background: $bg-white;
  border-bottom: 1px solid $border-color;
  flex-shrink: 0;
  z-index: 10;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-root {
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;

  &:hover {
    color: $color-primary;
  }
}

.breadcrumb-sep {
  font-size: 13px;
  color: $text-placeholder;
}

.breadcrumb-current {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon-btn {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-secondary;
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    color: $color-primary;
    background: $bg-page;
  }
}

.notify-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

// ---- 消息通知 ----
.notify-dialog-list {
  max-height: 360px;
  overflow-y: auto;
}

.notify-empty {
  padding: 48px 0;
  text-align: center;
  font-size: 13px;
  color: $text-placeholder;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 4px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.15s;

  &:hover {
    background: $bg-page;
  }

  &.unread .notify-text {
    color: $text-primary;
    font-weight: 600;
  }
}

.notify-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.notify-text {
  font-size: 13px;
  color: $text-regular;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-content {
  font-size: 12px;
  color: $text-secondary;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notify-time {
  font-size: 11px;
  color: $text-placeholder;
  flex-shrink: 0;
}

// ---- 个人信息 / 系统设置 ----
.profile-box {
  .profile-head {
    display: flex;
    align-items: center;
    gap: 14px;
    padding-bottom: 18px;
    margin-bottom: 14px;
    border-bottom: 1px solid $border-light;
  }

  .profile-avatar {
    background: linear-gradient(135deg, $color-primary, $color-accent);
    color: #fff;
    font-size: 22px;
    font-weight: 600;
  }

  .profile-head-meta {
    .profile-name {
      font-size: 17px;
      font-weight: 600;
      color: $text-primary;
    }

    .profile-role {
      font-size: 12px;
      color: $text-secondary;
      margin-top: 2px;
    }
  }
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 10px;
  transition: background 0.2s;

  &:hover {
    background: $bg-page;
  }
}

.user-avatar {
  background: linear-gradient(135deg, $color-primary, $color-primary-light);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

.user-meta {
  display: flex;
  flex-direction: column;

  .user-name {
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
    line-height: 1.2;
  }

  .user-role {
    font-size: 11px;
    color: $text-placeholder;
    line-height: 1.2;
  }
}

.user-arrow {
  color: $text-placeholder;
}

// ===== 内容区 =====
.layout-main {
  flex: 1;
  overflow-y: auto;
  background: $bg-page;
}
</style>
