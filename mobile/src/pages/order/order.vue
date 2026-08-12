<template>
  <view class="page">
    <view class="header">📋 订单详情</view>

    <view v-if="order" class="content">
      <view class="card status-card">
        <text class="status-text">{{ statusMap[order.status] }}</text>
        <text class="order-no">订单号：{{ order.orderNo }}</text>
        <text class="order-time">下单时间：{{ order.createTime }}</text>
      </view>

      <view class="card info-card">
        <text class="section-title">配送信息</text>
        <text class="info-text">地址：{{ order.address || '默认地址' }}</text>
        <text class="info-text">电话：{{ order.phone || user?.phone }}</text>
        <text class="info-text" v-if="order.remark">备注：{{ order.remark }}</text>
      </view>

      <view class="card">
        <text class="section-title">菜品明细</text>
        <view v-for="d in order.details" :key="d.id" class="detail-item">
          <text class="detail-name">{{ d.dishName }}<text v-if="d.size" class="detail-size">({{ d.size === 'small' ? '小份' : '大份' }})</text></text>
          <text class="detail-qty">x{{ d.quantity }}</text>
          <text class="detail-price">¥{{ (d.price * d.quantity).toFixed(2) }}</text>
        </view>
        <view class="total-line">
          <text class="total-label">合计</text>
          <text class="total-amount">¥{{ order.total }}</text>
        </view>
      </view>

      <view class="back-btn" @tap="goOrders">返回订单列表</view>
    </view>

    <view v-else-if="!loading" class="empty">
      <text>订单不存在</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const order = ref(null)
const user = ref(null)
const loading = ref(true)
const statusMap = { 1: '待付款', 2: '已支付', 3: '配送中', 4: '待评价', 5: '已完成' }

onMounted(() => {
  user.value = uni.getStorageSync('user')
  const orderId = getOrderId()
  if (orderId) {
    uni.request({
      url: '/api/cart/orders?userId=' + user.value.id,
      method: 'GET',
      success(res) {
        const orders = res.data.data
        order.value = orders.find(o => o.id == orderId)
        loading.value = false
      }
    })
  }
})

function getOrderId() {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  return page?.options?.orderId
}

function goOrders() {
  uni.redirectTo({ url: '/pages/order/list' })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #FFF8F5; }
.header {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 36rpx; font-weight: 700;
  text-align: center; padding: 32rpx 0;
}
.content { padding: 20rpx 24rpx; }
.card {
  background: #fff; border-radius: 20rpx; padding: 28rpx;
  margin-bottom: 20rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
}
.status-card { display: flex; flex-direction: column; align-items: center; }
.status-text { font-size: 40rpx; font-weight: 700; color: #FF6B35; }
.order-no { font-size: 24rpx; color: #999; margin-top: 12rpx; }
.order-time { font-size: 24rpx; color: #999; margin-top: 4rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #2D2D2D; margin-bottom: 16rpx; display: block; }
.info-text { font-size: 26rpx; color: #666; margin-bottom: 8rpx; display: block; }
.detail-item { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f5; }
.detail-name { flex: 1; font-size: 28rpx; color: #2D2D2D; }
.detail-size { font-size: 22rpx; color: #FF6B35; }
.detail-qty { font-size: 26rpx; color: #999; margin: 0 20rpx; }
.detail-price { font-size: 28rpx; font-weight: 600; color: #FF6B35; }
.total-line { display: flex; justify-content: space-between; align-items: center; margin-top: 20rpx; padding-top: 20rpx; border-top: 2rpx solid #f0f0f0; }
.total-label { font-size: 30rpx; font-weight: 600; }
.total-amount { font-size: 36rpx; font-weight: 700; color: #FF6B35; }
.back-btn { text-align: center; padding: 40rpx; color: #FF6B35; font-size: 28rpx; }
.empty { text-align: center; padding: 200rpx 0; color: #999; font-size: 28rpx; }
</style>
