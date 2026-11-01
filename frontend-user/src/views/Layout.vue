<template>
  <div class="app-layout">
    <!-- 顶部导航 -->
    <header class="top-bar">
      <div class="top-bar-inner">
        <div class="logo">
          <span class="logo-icon">📊</span>
          <span class="logo-text gradient-text">数据分析</span>
        </div>
        <div class="top-right">
          <span class="username">{{ userInfo?.nickname || userInfo?.username }}</span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </div>
      </div>
    </header>

    <!-- 页面内容 -->
    <main class="page-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部 TabBar -->
    <nav class="tab-bar">
      <router-link
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        class="tab-item"
        :class="{ active: $route.path === tab.path }"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { computed } from 'vue'

const store = useUserStore()
const router = useRouter()
const userInfo = computed(() => store.userInfo)

const tabs = [
  { path: '/home',       icon: '🏠', label: '总览' },
  { path: '/trend',      icon: '📈', label: '趋势' },
  { path: '/funnel',     icon: '🔻', label: '漏斗' },
  { path: '/dimension',  icon: '🔬', label: '维度' },
  { path: '/comparison', icon: '⚖️', label: '对比' }
]

function handleLogout() {
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-base);
}

/* 顶部 */
.top-bar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  background: rgba(10, 14, 26, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  height: 56px;
}
.top-bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 20px;
}
.logo { display: flex; align-items: center; gap: 8px; }
.logo-icon { font-size: 22px; }
.logo-text { font-size: 17px; font-weight: 700; }
.top-right { display: flex; align-items: center; gap: 12px; }
.username { font-size: 13px; color: var(--text-sub); }
.logout-btn {
  padding: 5px 12px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-sub);
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all .2s;
}
.logout-btn:hover { border-color: var(--primary); color: var(--primary); }

/* 页面内容 */
.page-content {
  flex: 1;
  margin-top: 56px;
  padding-bottom: 70px;
  overflow-y: auto;
}

/* TabBar */
.tab-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  z-index: 100;
  display: flex;
  background: rgba(13, 22, 38, 0.95);
  backdrop-filter: blur(12px);
  border-top: 1px solid var(--border);
  height: 60px;
  padding-bottom: env(safe-area-inset-bottom);
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  cursor: pointer;
  transition: color .2s;
  color: var(--text-muted);
  text-decoration: none;
}
.tab-item.active { color: var(--primary); }
.tab-item.active .tab-icon { filter: drop-shadow(0 0 6px var(--primary)); }
.tab-icon { font-size: 20px; transition: filter .2s; }
.tab-label { font-size: 10px; font-weight: 500; }

/* 页面切换动画 */
.fade-enter-active, .fade-leave-active { transition: opacity .15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
