<template>
  <view class="page">
    <view class="header">📋 我的订单</view>

    <view v-if="orders.length === 0" class="empty">
      <text class="empty-icon">📋</text>
      <text>暂无订单</text>
      <text class="empty-link" @tap="goDish">去点餐</text>
    </view>

    <view class="list" v-else>
      <view v-for="order in orders" :key="order.id" class="swipe-wrap">
        <view class="delete-btn" @tap.stop="handleDelete(order.id)">删除</view>
        <view
          class="card"
          :class="{ open: swipedId === order.id }"
          :style="{ transform: swipedId === order.id ? 'translateX(-160rpx)' : 'translateX(0)' }"
          @touchstart="onTouchStart($event, order.id)"
          @touchmove="onTouchMove($event, order.id)"
          @touchend="onTouchEnd($event, order.id)"
          @tap="goDetail(order.id)"
        >
          <view class="card-header">
            <text class="order-no">订单号：{{ order.orderNo }}</text>
            <text class="status" :class="'status-' + order.status">{{ statusMap[order.status] }}</text>
          </view>
          <view v-for="d in order.details" :key="d.id" class="detail-row">
            <text class="detail-name">{{ d.dishName }}<text v-if="d.size" class="detail-size">({{ d.size === 'small' ? '小份' : '大份' }})</text></text>
            <text class="detail-qty">x{{ d.quantity }}</text>
            <text class="detail-price">¥{{ (d.price * d.quantity).toFixed(2) }}</text>
          </view>
          <view class="card-footer">
            <text class="time">{{ order.createTime }}</text>
            <text class="total">合计 ¥{{ order.total }}</text>
          </view>
          <view v-if="order.status === 1" class="pay-bar">
            <view class="pay-btn" @tap.stop="handlePay(order.id)">付 款</view>
          </view>
          <view v-if="order.status === 3" class="pay-bar">
            <view class="pay-btn" @tap.stop="goDelivery(order.id)">查看配送</view>
          </view>
          <view v-if="order.status === 4" class="pay-bar">
            <view class="pay-btn" @tap.stop="goDelivery(order.id)">去评价</view>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-nav">
      <view class="nav-item" @tap="goDish">🍜 点餐</view>
      <view class="nav-item" @tap="goCart">🛒 购物车</view>
      <view class="nav-item" @tap="goMy">👤 我的</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/utils/request'

const orders = ref([])
const allOrders = ref([])
const user = ref(null)
const statusFilter = ref(null)
const swipedId = ref(null)
const startX = ref(0)
const startY = ref(0)
const statusMap = { 1: '待付款', 2: '已支付', 3: '配送中', 4: '待评价', 5: '已完成' }

onMounted(() => {
  user.value = uni.getStorageSync('user')
  if (!user.value) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1000)
    return
  }
  const pages = getCurrentPages()
  const options = pages[pages.length - 1]?.options
  if (options?.status) statusFilter.value = parseInt(options.status)
  fetchOrders()
})

function fetchOrders() {
  get('/cart/orders', { userId: user.value.id }).then(res => {
    allOrders.value = res.data
    orders.value = statusFilter.value
      ? allOrders.value.filter(o => o.status === statusFilter.value)
      : allOrders.value
  })
}

function onTouchStart(e, id) {
  startX.value = e.touches[0].clientX
  startY.value = e.touches[0].clientY
}

function onTouchMove(e, id) {
  const dx = e.touches[0].clientX - startX.value
  const dy = e.touches[0].clientY - startY.value
  if (Math.abs(dy) > Math.abs(dx)) return
  if (dx < -30) {
    swipedId.value = id
  } else if (dx > 30) {
    swipedId.value = null
  }
}

function onTouchEnd(e, id) {
  // keep swiped state
}

function handlePay(id) {
  uni.showModal({
    title: '确认付款',
    content: '确定支付该订单吗？',
    success: r => {
      if (r.confirm) {
        uni.request({
          url: '/api/cart/orders/' + id + '/pay',
          method: 'PUT',
          success(res) {
            uni.showToast({ title: '付款成功', icon: 'success' })
            setTimeout(() => {
              uni.navigateTo({ url: '/pages/delivery/delivery?orderId=' + id })
            }, 800)
          }
        })
      }
    }
  })
}

function handleDelete(id) {
  uni.showModal({
    title: '确认删除',
    content: '确定删除该订单吗？',
    success: r => {
      if (r.confirm) {
        uni.request({
          url: `/api/cart/orders/${id}`, method: 'DELETE',
          success: () => {
            uni.showToast({ title: '已删除', icon: 'success' })
            swipedId.value = null
            fetchOrders()
          }
        })
      } else {
        swipedId.value = null
      }
    }
  })
}

function goDetail(id) {
  if (swipedId.value) { swipedId.value = null; return }
  uni.navigateTo({ url: '/pages/order/order?orderId=' + id })
}
function goDish() { uni.redirectTo({ url: '/pages/dish/dish' }) }
function goCart() { uni.redirectTo({ url: '/pages/cart/cart' }) }
function goDelivery(id) { uni.navigateTo({ url: '/pages/delivery/delivery?orderId=' + id }) }
function goMy() { uni.redirectTo({ url: '/pages/my/my' }) }
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #FFF8F5; }
.header {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 36rpx; font-weight: 700;
  text-align: center; padding: 32rpx 0;
}
.empty { text-align: center; padding: 200rpx 0; }
.empty-icon { font-size: 100rpx; display: block; }
.empty-link { font-size: 28rpx; color: #FF6B35; margin-top: 20rpx; display: block; }

.list { padding: 20rpx 24rpx 120rpx; }

.swipe-wrap {
  position: relative; margin-bottom: 20rpx;
  border-radius: 20rpx; overflow: hidden;
}

.delete-btn {
  position: absolute; right: 0; top: 0; bottom: 0;
  width: 160rpx; background: #FF3B30; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 28rpx; font-weight: 600;
}

.card {
  background: #fff; border-radius: 20rpx; padding: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
  position: relative; z-index: 1;
  transition: transform 0.2s;
}
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.order-no { font-size: 24rpx; color: #999; }
.status { font-size: 24rpx; font-weight: 600; }
.status-1 { color: #FF6B35; }
.status-2 { color: #409EFF; }
.status-3 { color: #E6A23C; }
.status-4 { color: #67C23A; }
.status-5 { color: #909399; }
.detail-row { display: flex; align-items: center; padding: 8rpx 0; }
.detail-name { flex: 1; font-size: 26rpx; color: #2D2D2D; }
.detail-size { font-size: 22rpx; color: #FF6B35; }
.detail-qty { font-size: 24rpx; color: #999; margin: 0 16rpx; }
.detail-price { font-size: 26rpx; color: #FF6B35; font-weight: 600; }
.card-footer { display: flex; justify-content: space-between; margin-top: 12rpx; padding-top: 12rpx; border-top: 1rpx solid #f5f5f5; }
.time { font-size: 22rpx; color: #BBB; }
.total { font-size: 28rpx; font-weight: 600; color: #2D2D2D; }
.pay-bar { display: flex; justify-content: flex-end; margin-top: 16rpx; }
.pay-btn {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 26rpx; font-weight: 600;
  padding: 12rpx 40rpx; border-radius: 40rpx;
}

.bottom-nav {
  position: fixed; bottom: 0; left: 0; right: 0;
  background: #fff; display: flex; justify-content: space-around;
  padding: 16rpx 0; padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06);
}
.nav-item { font-size: 24rpx; color: #999; text-align: center; padding: 8rpx 0; }
.nav-item.active { color: #FF6B35; font-weight: 600; }
</style>
