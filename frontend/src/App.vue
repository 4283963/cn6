<template>
  <div class="app-container">
    <el-container class="layout-container">
      <el-header class="app-header">
        <div class="header-content">
          <div class="logo-area">
            <el-icon :size="32" color="#409EFF">
              <Van />
            </el-icon>
            <h1 class="app-title">飞机飞行传感器数据回溯系统</h1>
          </div>
          <el-menu
            :default-active="activeMenu"
            mode="horizontal"
            router
            class="nav-menu"
            background-color="transparent"
            text-color="#ffffff"
            active-text-color="#ffd04b"
          >
            <el-menu-item index="/">
              <el-icon><UploadFilled /></el-icon>
              <span>数据上传</span>
            </el-menu-item>
            <el-menu-item index="/history">
              <el-icon><DataAnalysis /></el-icon>
              <span>数据回溯</span>
            </el-menu-item>
            <el-menu-item index="/flights">
              <el-icon><List /></el-icon>
              <span>航班列表</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
      <el-footer class="app-footer">
        <p>Flight Sensor Data Management System © 2026</p>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped lang="scss">
.app-container {
  min-height: 100vh;
  background: #f0f2f5;
}

.layout-container {
  min-height: 100vh;
}

.app-header {
  background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 50%, #2563eb 100%);
  padding: 0;
  height: 70px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;

  .header-content {
    width: 100%;
    max-width: 1600px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
  }

  .logo-area {
    display: flex;
    align-items: center;
    gap: 12px;

    .app-title {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: #fff;
      letter-spacing: 1px;
    }
  }

  .nav-menu {
    border: none;
    height: 70px;
    display: flex;
    align-items: center;

    :deep(.el-menu-item) {
      height: 70px;
      line-height: 70px;
      border-bottom: 3px solid transparent;
      font-size: 15px;
      margin: 0 8px;
      padding: 0 20px;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
      }

      &.is-active {
        background: rgba(255, 255, 255, 0.15);
        border-bottom-color: #ffd04b;
      }
    }
  }
}

.app-main {
  padding: 24px;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
}

.app-footer {
  background: #1f2937;
  color: #9ca3af;
  text-align: center;
  padding: 20px;
  height: auto;

  p {
    margin: 0;
    font-size: 13px;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
