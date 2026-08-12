<template>
  <el-container class="layout">
    <el-aside width="220px">
      <div class="logo">🍜 天空外卖后台</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#FF6B35"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/dish">
          <el-icon><Food /></el-icon>
          <span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/order">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <span class="header-title">{{ route.meta?.title || '数据概览' }}</span>
        <el-button text @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { User, Food, Document, DataAnalysis } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

function logout() {
  localStorage.removeItem('admin')
  router.replace('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.el-aside { background: #304156; overflow: hidden; }
.logo {
  height: 60px; line-height: 60px; text-align: center;
  color: #fff; font-size: 18px; font-weight: 600;
}
.el-header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid #e6e6e6;
}
.header-title { font-size: 16px; font-weight: 600; }
.el-main { background: #f0f2f5; }
</style>
