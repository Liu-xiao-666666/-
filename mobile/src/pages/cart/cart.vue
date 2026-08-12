<template>
  <view class="page">
    <view class="header">🛒 购物车</view>

    <view v-if="carts.length === 0" class="empty">
      <text class="empty-icon">🛒</text>
      <text class="empty-text">购物车空空如也</text>
      <text class="empty-link" @tap="goDish">去点餐</text>
    </view>

    <view v-else>
      <view class="list">
        <view v-for="item in carts" :key="item.id" class="card">
          <image v-if="item.dishImage" :src="item.dishImage" class="card-img" mode="aspectFill" />
          <view class="card-img-placeholder" v-else>🍽️</view>
          <view class="card-body">
            <view class="card-name-row">
              <text class="card-name">{{ item.dishName }}</text>
              <text class="size-tag">{{ item.size === 'small' ? '小份' : '大份' }}</text>
            </view>
            <text class="card-price">¥{{ item.dishPrice }}</text>
          </view>
          <view class="qty-ctrl">
            <view class="qty-btn" @tap="decrease(item)">−</view>
            <text class="qty-num">{{ item.quantity }}</text>
            <view class="qty-btn" @tap="increase(item)">+</view>
          </view>
        </view>
      </view>

      <view class="footer-bar">
        <view class="total">
          <text class="total-label">合计：</text>
          <text class="total-price">¥{{ totalPrice }}</text>
        </view>
        <view class="submit-btn" @tap="submitOrder">提交订单</view>
      </view>
    </view>

    <view class="bottom-nav">
      <view class="nav-item" @tap="goDish">🍜 点餐</view>

      <view class="nav-item active">🛒 购物车</view>
      <view class="nav-item" @tap="goMy">👤 我的</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post } from '@/utils/request'

const carts = ref([])
const user = ref(null)

onMounted(() => {
  user.value = uni.getStorageSync('user')
  if (!user.value) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1000)
    return
  }
  fetchCart()
})

function fetchCart() {
  get('/cart/list', { userId: user.value.id }).then(res => {
    carts.value = res.data
  })
}

const totalPrice = computed(() => {
  return carts.value.reduce((sum, c) => sum + c.dishPrice * c.quantity, 0).toFixed(2)
})

function increase(item) {
  post('/cart/add', { userId: user.value.id, dishId: item.dishId, quantity: 1, size: item.size || 'large' }).then(fetchCart)
}
function decrease(item) {
  if (item.quantity <= 1) {
    uni.request({ url: '/api/cart/' + item.id, method: 'DELETE', success: fetchCart })
  } else {
    uni.request({ url: '/api/cart/' + item.id + '?quantity=' + (item.quantity - 1), method: 'PUT', success: fetchCart })
  }
}

function submitOrder() {
  uni.showModal({
    title: '确认下单',
    content: '确定提交订单吗？',
    success: r => {
      if (!r.confirm) return
      post('/cart/submit', { userId: user.value.id, address: '默认地址', phone: user.value.phone }).then(res => {
        uni.showToast({ title: '下单成功', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({ url: '/pages/order/order?orderId=' + res.data.id })
        }, 800)
      })
    }
  })
}

function goDish() {
  uni.redirectTo({ url: '/pages/dish/dish' })
}
function goOrders() { uni.redirectTo({ url: '/pages/order/list' }) }
function goMy() { uni.redirectTo({ url: '/pages/my/my' }) }
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #FFF8F5; }
.header {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 36rpx; font-weight: 700;
  text-align: center; padding: 32rpx 0;
}
.empty { text-align: center; padding: 160rpx 0; }
.empty-icon { font-size: 100rpx; display: block; }
.empty-text { font-size: 28rpx; color: #999; margin-top: 20rpx; display: block; }
.empty-link { font-size: 28rpx; color: #FF6B35; margin-top: 20rpx; display: block; }

.list { padding: 20rpx 24rpx; }
.card {
  display: flex; align-items: center; background: #fff;
  border-radius: 20rpx; padding: 20rpx; margin-bottom: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-img, .card-img-placeholder {
  width: 100rpx; height: 100rpx; border-radius: 12rpx; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 44rpx; background: #FFF0E5;
}
.card-body { flex: 1; padding: 0 20rpx; }
.card-name-row { display: flex; align-items: center; gap: 8rpx; }
.card-name { font-size: 28rpx; font-weight: 600; color: #2D2D2D; }
.size-tag {
  font-size: 20rpx; color: #FF6B35; background: #FFF0E5;
  padding: 2rpx 10rpx; border-radius: 8rpx;
}
.card-price { font-size: 30rpx; font-weight: 700; color: #FF6B35; margin-top: 8rpx; display: block; }
.qty-ctrl { display: flex; align-items: center; }
.qty-btn {
  width: 48rpx; height: 48rpx; border-radius: 50%;
  background: #FFF0E5; color: #FF6B35; font-size: 32rpx;
  display: flex; align-items: center; justify-content: center;
  font-weight: 600;
}
.qty-num { width: 60rpx; text-align: center; font-size: 28rpx; font-weight: 600; }

.footer-bar {
  position: fixed; bottom: 100rpx; left: 0; right: 0;
  background: #fff; padding: 20rpx 32rpx;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 -4rpx 16rpx rgba(0,0,0,0.06);
}
.total-label { font-size: 28rpx; color: #666; }
.total-price { font-size: 40rpx; font-weight: 700; color: #FF6B35; }
.submit-btn {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 30rpx; font-weight: 600;
  padding: 16rpx 48rpx; border-radius: 40rpx;
}

.bottom-nav {
  position: fixed; bottom: 0; left: 0; right: 0;
  background: #fff; display: flex; justify-content: space-around;
  padding: 16rpx 0; padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06); z-index: 10;
}
.nav-item { font-size: 24rpx; color: #999; }
.nav-item.active { color: #FF6B35; font-weight: 600; }
</style>
