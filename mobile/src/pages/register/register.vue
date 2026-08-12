<template>
  <view class="page">
    <view class="header">
      <view class="header-blob"></view>
      <view class="header-blob blob-2"></view>
      <view class="header-content">
        <text class="logo">🍜 天空外卖</text>
        <text class="slogan">创建账号，开始点餐</text>
      </view>
    </view>

    <view class="card">
      <text class="card-title">注册账号</text>
      <text class="card-sub">填写信息完成注册</text>

      <view class="form">
        <view class="input-group" :class="{ error: errors.phone }">
          <text class="input-icon">📱</text>
          <input
            class="input"
            v-model="form.phone"
            type="number"
            maxlength="11"
            placeholder="请输入11位手机号"
            placeholder-class="placeholder"
            @blur="validatePhone"
          />
        </view>
        <text class="error-tip" v-if="errors.phone">{{ errors.phone }}</text>

        <view class="input-group" :class="{ error: errors.password }">
          <text class="input-icon">🔒</text>
          <input
            class="input"
            v-model="form.password"
            type="password"
            placeholder="请设置6-20位密码"
            placeholder-class="placeholder"
            @blur="validatePassword"
          />
        </view>
        <text class="error-tip" v-if="errors.password">{{ errors.password }}</text>

        <view class="input-group" :class="{ error: errors.confirm }">
          <text class="input-icon">✓</text>
          <input
            class="input"
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            placeholder-class="placeholder"
            @blur="validateConfirm"
          />
        </view>
        <text class="error-tip" v-if="errors.confirm">{{ errors.confirm }}</text>

        <button class="btn" :disabled="loading" :loading="loading" @tap="handleRegister">
          {{ loading ? '注册中...' : '注 册' }}
        </button>
      </view>

      <view class="footer-link">
        <text class="link-text">已有账号？</text>
        <text class="link-action" @tap="goLogin">返回登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { post } from '@/utils/request'

const form = reactive({ phone: '', password: '', confirmPassword: '' })
const errors = reactive({ phone: '', password: '', confirm: '' })
const loading = ref(false)

function validatePhone() {
  if (!form.phone) {
    errors.phone = '请输入手机号'
  } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errors.phone = '手机号格式不正确'
  } else {
    errors.phone = ''
  }
}

function validatePassword() {
  if (!form.password) {
    errors.password = '请设置密码'
  } else if (form.password.length < 6 || form.password.length > 20) {
    errors.password = '密码长度需6-20位'
  } else {
    errors.password = ''
  }
}

function validateConfirm() {
  if (!form.confirmPassword) {
    errors.confirm = '请再次输入密码'
  } else if (form.confirmPassword !== form.password) {
    errors.confirm = '两次密码不一致'
  } else {
    errors.confirm = ''
  }
}

function validateAll() {
  validatePhone()
  validatePassword()
  validateConfirm()
  return !errors.phone && !errors.password && !errors.confirm
}

function handleRegister() {
  if (!validateAll()) return

  loading.value = true
  post('/user/register', { phone: form.phone, password: form.password })
    .then(res => {
      if (res.code === 1) {
        uni.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1000)
      } else {
        uni.showToast({ title: res.message, icon: 'none' })
      }
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function goLogin() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #FF6B35 0%, #FFF8F5 50%);
}

.header {
  position: relative;
  height: 260rpx;
  overflow: hidden;
}
.header-blob {
  position: absolute;
  width: 500rpx;
  height: 500rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
  top: -220rpx;
  right: -120rpx;
}
.blob-2 {
  width: 280rpx;
  height: 280rpx;
  top: -100rpx;
  left: -80rpx;
}
.header-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 70rpx;
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
  margin-top: 10rpx;
}

.card {
  margin: -30rpx 32rpx 0;
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 44rpx 40rpx;
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

.form { margin-top: 40rpx; }

.input-group {
  display: flex;
  align-items: center;
  background: #FFF8F5;
  border-radius: 20rpx;
  padding: 0 28rpx;
  height: 100rpx;
  margin-bottom: 8rpx;
  border: 2rpx solid transparent;

  &:focus-within {
    border-color: #FF6B35;
    background: #FFFFFF;
  }
  &.error {
    border-color: #FF3B30;
    background: #FFF5F5;
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

.error-tip {
  font-size: 22rpx;
  color: #FF3B30;
  padding-left: 28rpx;
  margin-bottom: 16rpx;
  display: block;
}

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
  margin-top: 24rpx;
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
