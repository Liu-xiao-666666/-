<template>
  <view class="page" @tap="menuVisible = false">
    <view class="header">
      <view class="header-bg"></view>
      <image class="settings-icon" src="/static/image/微信图片_20260515142109_362_20.png"
             mode="aspectFit" @tap.stop="toggleMenu" />
      <view class="dropdown" v-if="menuVisible" @tap.stop>
        <view class="dropdown-item" @tap="showPrivacy">隐私政策</view>
        <view class="dropdown-item" @tap="showVersion">版本信息</view>
      </view>
      <view class="user-card">
        <image v-if="user?.avatar" :src="user.avatar" class="avatar" mode="aspectFill" @tap.stop="goProfile" />
        <view class="avatar-placeholder" v-else @tap.stop="goProfile">👤</view>
        <text class="nickname" @tap.stop="goProfile">{{ user?.nickname || '未登录' }}</text>
        <view class="phone">{{ user?.phone || '' }}</view>
      </view>
    </view>

    <view class="section">
      <text class="section-title">我的订单</text>
      <view class="order-grid">
        <view class="order-item" v-for="item in orderCats" :key="item.status"
              @tap="goOrderList(item.status)">
          <image class="order-icon" :src="item.icon" mode="aspectFit" />
          <text class="order-label">{{ item.label }}</text>
          <text class="order-badge" v-if="item.count > 0">{{ item.count }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <text class="section-title">最近订单</text>
      <view v-if="recentOrders.length === 0" class="empty-hint">暂无订单记录</view>
      <view v-for="order in recentOrders" :key="order.id" class="swipe-wrap">
        <view class="swipe-del" @tap.stop="handleDelOrder(order.id)">删除</view>
        <view
          class="recent-card"
          :class="{ open: recentSwipedId === order.id }"
          :style="{ transform: recentSwipedId === order.id ? 'translateX(-160rpx)' : 'translateX(0)' }"
          @touchstart="recentTouchStart($event, order.id)"
          @touchmove="recentTouchMove($event, order.id)"
          @touchend="recentTouchEnd"
          @tap="goOrderDetail(order.id)"
        >
          <view class="recent-header">
            <text class="recent-names">
              {{ order.details.map(d => d.dishName).join('、') }}
            </text>
            <text class="recent-status" :class="'status-' + order.status">
              {{ statusMap[order.status] }}
            </text>
          </view>
          <view class="recent-footer">
            <text class="recent-time">{{ order.createTime }}</text>
            <text class="recent-total">¥{{ order.total }}</text>
          </view>
          <view class="again-btn" @tap.stop="reorder(order)">再来一单</view>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="menu-item" @tap="goCart">
        <text>🛒 购物车</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goChat">
        <text>🍽️ 美食问问</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="logout">
        <text>退出登录</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="bottom-nav">
      <view class="nav-item" @tap="goDish">🍜 点餐</view>

      <view class="nav-item" @tap="goCart">🛒 购物车</view>
      <view class="nav-item active">👤 我的</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post } from '@/utils/request'

const user = ref(null)
const recentOrders = ref([])
const recentSwipedId = ref(null)
const recentStartX = ref(0)
const recentStartY = ref(0)
const orderCounts = ref({})
const statusMap = { 1: '待付款', 2: '已支付', 3: '配送中', 4: '待评价', 5: '已完成' }
const orderCats = [
  { status: 1, label: '待付款', icon: '/static/image/微信图片_20260515112250_357_20.png', count: 0 },
  { status: 3, label: '配送中', icon: '/static/image/微信图片_20260515112413_358_20.png', count: 0 },
  { status: 4, label: '待评价', icon: '/static/image/微信图片_20260515112440_359_20.png', count: 0 },
  { status: 7, label: '售后', icon: '/static/image/微信图片_20260515112546_360_20.png', count: 0 }
]

onMounted(() => {
  user.value = uni.getStorageSync('user')
  if (!user.value) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1000)
    return
  }
  fetchOrders()
})

function fetchOrders() {
  get('/cart/orders', { userId: user.value.id }).then(res => {
    const orders = res.data
    recentOrders.value = orders.slice(0, 3)
    const counts = {}
    orders.forEach(o => { counts[o.status] = (counts[o.status] || 0) + 1 })
    orderCats.forEach(cat => { cat.count = counts[cat.status] || 0 })
    // 售后数量 = 订单中 afterSaleStatus 为 1（处理中）的数量
    const asCount = orders.filter(o => o.afterSaleStatus === 1).length
    const asCat = orderCats.find(c => c.status === 7)
    if (asCat) asCat.count = asCount
  })
}

function goOrderList(status) {
  if (status === 7) {
    uni.navigateTo({ url: '/pages/aftersale/aftersale' })
    return
  }
  uni.redirectTo({ url: '/pages/order/list?status=' + (status || '') })
}
function goOrderDetail(id) {
  if (recentSwipedId.value) { recentSwipedId.value = null; return }
  uni.navigateTo({ url: '/pages/order/order?orderId=' + id })
}
function goDish() { uni.redirectTo({ url: '/pages/dish/dish' }) }
function goCart() { uni.redirectTo({ url: '/pages/cart/cart' }) }
function goOrders() { uni.redirectTo({ url: '/pages/order/list' }) }
function goProfile() { uni.navigateTo({ url: '/pages/my/profile' }) }
function goChat() { uni.navigateTo({ url: '/pages/chat/chat' }) }

function reorder(order) {
  if (!order.details) return
  const adds = order.details.map(d =>
    post('/cart/add', { userId: user.value.id, dishId: d.dishId, quantity: d.quantity })
  )
  Promise.all(adds).then(() => {
    uni.showToast({ title: '已加入购物车', icon: 'success' })
    setTimeout(() => uni.redirectTo({ url: '/pages/cart/cart' }), 800)
  })
}

const menuVisible = ref(false)

function toggleMenu() {
  menuVisible.value = !menuVisible.value
}

function showPrivacy() {
  menuVisible.value = false
  uni.showModal({
    title: '隐私政策',
    content: '我们重视您的隐私。本应用仅收集必要的账号信息用于订单服务，不会将您的数据分享给第三方。',
    showCancel: false
  })
}

function showVersion() {
  menuVisible.value = false
  uni.showModal({
    title: '版本信息',
    content: '天空外卖 v1.0.0\n技术栈：Spring Boot + UniApp + Vue3',
    showCancel: false
  })
}

function recentTouchStart(e, id) {
  recentStartX.value = e.touches[0].clientX
  recentStartY.value = e.touches[0].clientY
}
function recentTouchMove(e, id) {
  const dx = e.touches[0].clientX - recentStartX.value
  const dy = e.touches[0].clientY - recentStartY.value
  if (Math.abs(dy) > Math.abs(dx)) return
  recentSwipedId.value = dx < -30 ? id : dx > 30 ? null : recentSwipedId.value
}
function recentTouchEnd() {}

function handleDelOrder(id) {
  uni.showModal({
    title: '确认删除',
    content: '确定删除该订单吗？',
    success: r => {
      if (r.confirm) {
        uni.request({
          url: '/api/cart/orders/' + id, method: 'DELETE',
          success: () => {
            uni.showToast({ title: '已删除', icon: 'success' })
            recentSwipedId.value = null
            fetchOrders()
          }
        })
      } else {
        recentSwipedId.value = null
      }
    }
  })
}

function logout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: r => {
      if (r.confirm) {
        uni.removeStorageSync('user')
        uni.redirectTo({ url: '/pages/login/login' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #FFF8F5; padding-bottom: 120rpx; }
.header {
  position: relative;
  padding: 60rpx 32rpx 32rpx;
}
.header-bg {
  position: absolute; top: 0; left: 0; right: 0; height: 400rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  border-radius: 0 0 60rpx 60rpx;
}
.settings-icon {
  position: absolute; top: 20rpx; right: 24rpx; z-index: 2;
  width: 44rpx; height: 44rpx;
}

.dropdown {
  position: absolute; top: 72rpx; right: 24rpx; z-index: 3;
  background: #fff; border-radius: 12rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.12);
  overflow: hidden;
}
.dropdown-item {
  padding: 22rpx 36rpx; font-size: 26rpx; color: #2D2D2D;
  border-bottom: 1rpx solid #f0f0f0;
}
.dropdown-item:last-child { border-bottom: none; }

.user-card {
  position: relative; z-index: 1;
  display: flex; flex-direction: column; align-items: center;
  padding-top: 40rpx; width: 100%;
}
.avatar, .avatar-placeholder {
  width: 120rpx; height: 120rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 60rpx; border: 4rpx solid rgba(255,255,255,0.6);
  background: rgba(255,255,255,0.2);
}
.nickname { font-size: 36rpx; font-weight: 700; color: #fff; margin-top: 16rpx; }
.phone { font-size: 26rpx; color: rgba(255,255,255,0.8); margin-top: 4rpx; display: block; }

.section {
  background: #fff; border-radius: 20rpx; margin: 20rpx 24rpx;
  padding: 24rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
}
.section-title { font-size: 30rpx; font-weight: 600; color: #2D2D2D; margin-bottom: 20rpx; display: block; }

.order-grid { display: flex; justify-content: space-around; }
.order-item {
  display: flex; flex-direction: column; align-items: center;
  position: relative;
}
.order-icon { width: 48rpx; height: 48rpx; }
.order-label { font-size: 22rpx; color: #666; margin-top: 8rpx; }
.order-badge {
  position: absolute; top: -8rpx; right: -16rpx;
  background: #FF3B30; color: #fff; font-size: 20rpx;
  min-width: 32rpx; height: 32rpx; border-radius: 16rpx;
  text-align: center; line-height: 32rpx; padding: 0 8rpx;
}

.empty-hint { text-align: center; color: #999; font-size: 26rpx; padding: 40rpx 0; }

.swipe-wrap {
  position: relative; overflow: hidden; border-radius: 0;
}
.swipe-del {
  position: absolute; right: 0; top: 0; bottom: 0;
  width: 160rpx; background: #FF3B30; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 26rpx; font-weight: 600;
}
.recent-card {
  padding: 20rpx 0; border-bottom: 1rpx solid #f5f5f5;
  position: relative; z-index: 1; background: #fff;
  transition: transform 0.2s;
}
.recent-card:last-child { border-bottom: none; }
.recent-header { display: flex; justify-content: space-between; align-items: center; }
.recent-names { font-size: 28rpx; color: #2D2D2D; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recent-status { font-size: 24rpx; font-weight: 600; margin-left: 16rpx; flex-shrink: 0; }
.status-1 { color: #FF6B35; }
.status-2 { color: #409EFF; }
.status-3 { color: #E6A23C; }
.status-4 { color: #67C23A; }
.status-5 { color: #909399; }
.recent-footer { display: flex; justify-content: space-between; margin-top: 8rpx; }
.recent-time { font-size: 22rpx; color: #BBB; }
.recent-total { font-size: 26rpx; font-weight: 600; color: #2D2D2D; }
.again-btn {
  position: absolute; right: 0; bottom: 20rpx;
  background: #FFF0E5; color: #FF6B35; font-size: 22rpx;
  padding: 6rpx 20rpx; border-radius: 20rpx;
}

.menu-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f5;
  font-size: 28rpx; color: #2D2D2D;
}
.menu-item:last-child { border-bottom: none; }
.menu-arrow { font-size: 32rpx; color: #BBB; }

.bottom-nav {
  position: fixed; bottom: 0; left: 0; right: 0;
  background: #fff; display: flex; justify-content: space-around;
  padding: 16rpx 0; padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06);
}
.nav-item { font-size: 24rpx; color: #999; }
.nav-item.active { color: #FF6B35; font-weight: 600; }
</style>
