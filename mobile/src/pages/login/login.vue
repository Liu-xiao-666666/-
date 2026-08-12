<template>
  <view class="page">
    <view class="header">
      <view class="header-blob"></view>
      <view class="header-blob blob-2"></view>
      <view class="header-content">
        <text class="logo">🍜 天空外卖</text>
        <text class="slogan">热乎美食，即刻送达</text>
      </view>
    </view>

    <view class="card">
      <text class="card-title">欢迎回来</text>
      <text class="card-sub">登录你的账号</text>

      <view class="form">
        <view class="input-group">
          <text class="input-icon">📱</text>
          <input
            class="input"
            v-model="form.phone"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            placeholder-class="placeholder"
          />
        </view>
        <view class="input-group">
          <text class="input-icon">🔒</text>
          <input
            class="input"
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            placeholder-class="placeholder"
          />
        </view>

        <button class="btn" :disabled="loading" :loading="loading" @tap="handleLogin">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </view>

      <view class="footer-link">
        <text class="link-text">还没有账号？</text>
        <text class="link-action" @tap="goRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { post } from '@/utils/request'

const form = reactive({ phone: '', password: '' })
const loading = ref(false)

function handleLogin() {
  if (form.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (!form.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }

  loading.value = true
  post('/user/login', { phone: form.phone, password: form.password })
    .then(res => {
      if (res.code === 1) {
        uni.showToast({ title: '登录成功', icon: 'success' })
        uni.setStorageSync('user', res.data)
        setTimeout(() => {
          uni.redirectTo({ url: '/pages/dish/dish' })
        }, 800)
      } else {
        uni.showToast({ title: res.message, icon: 'none' })
      }
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/register' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FF6B35 0%, #FFF8F5 50%);
}

.header {
  position: relative;
  height: 280rpx;
  overflow: hidden;
}
.header-blob {
  position: absolute;
  width: 500rpx;
  height: 500rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
  top: -200rpx;
  right: -150rpx;
}
.blob-2 {
  width: 300rpx;
  height: 300rpx;
  top: -80rpx;
  left: -100rpx;
}
.header-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 80rpx;
}
.logo {
  font-size: 48rpx;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 4rpx;
}
.slogan {
  font-size: 26rpx;
  color: rgba(255,255,255,0.8);
  margin-top: 12rpx;
}

.card {
  margin: -40rpx 32rpx 0;
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 40rpx rgba(255, 107, 53, 0.12);
}
.card-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #2D2D2D;
  display: block;
}
.card-sub {
  font-size: 26rpx;
  color: #8E8E93;
  margin-top: 8rpx;
  display: block;
}

.form { margin-top: 48rpx; }

.input-group {
  display: flex;
  align-items: center;
  background: #FFF8F5;
  border-radius: 20rpx;
  padding: 0 28rpx;
  height: 100rpx;
  margin-bottom: 24rpx;
  border: 2rpx solid transparent;

  &:focus-within {
    border-color: #FF6B35;
    background: #FFFFFF;
  }
}
.input-icon {
  font-size: 36rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
}
.input {
  flex: 1;
  font-size: 30rpx;
  color: #2D2D2D;
  height: 100%;
}
.placeholder { color: #C7C7CC; }

.btn {
  width: 100%;
  height: 100rpx;
  line-height: 100rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #FFFFFF;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 50rpx;
  border: none;
  margin-top: 32rpx;
  letter-spacing: 8rpx;
  &[disabled] { opacity: 0.7; }
}

.footer-link {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 36rpx;
}
.link-text { font-size: 26rpx; color: #8E8E93; }
.link-action {
  font-size: 26rpx;
  color: #FF6B35;
  font-weight: 600;
  margin-left: 8rpx;
}
</style>
