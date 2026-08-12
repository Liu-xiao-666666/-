<template>
  <view class="page">
    <view class="header">🚀 配送中</view>

    <view class="content">
      <view class="rider-anim">
        <text class="rider">🛵</text>
      </view>

      <view class="timer-wrap">
        <text class="timer-label">预计送达</text>
        <text class="timer">{{ countdown }}s</text>
      </view>

      <view class="info-card">
        <view class="info-row"><text class="label">订单号</text><text>{{ order?.orderNo }}</text></view>
        <view class="info-row"><text class="label">配送地址</text><text>{{ order?.address }}</text></view>
        <view class="info-row"><text class="label">联系电话</text><text>{{ order?.phone }}</text></view>
        <view class="info-row"><text class="label">订单金额</text><text class="price">¥{{ order?.total }}</text></view>
      </view>

      <view class="detail-list" v-if="order?.details">
        <view v-for="d in order.details" :key="d.id" class="detail-row">
          <text>{{ d.dishName }}<text v-if="d.size" class="detail-size">({{ d.size === 'small' ? '小份' : '大份' }})</text></text>
          <text>x{{ d.quantity }}</text>
          <text>¥{{ (d.price * d.quantity).toFixed(2) }}</text>
        </view>
      </view>

      <view v-if="delivered" class="eval-box">
        <text class="eval-title">订单已送达，请评价</text>
        <view class="stars">
          <text v-for="i in 5" :key="i" class="star"
                :class="{ filled: i <= rating }"
                @tap="rating = i">{{ i <= rating ? '★' : '☆' }}</text>
        </view>
        <textarea v-model="comment" class="comment-input" placeholder="写点评价吧（可选）" />
        <view class="eval-btn" @tap="submitReview">提交评价</view>
      </view>

      <view v-if="reviewed" class="done-box">
        <text class="done-icon">🎉</text>
        <text class="done-text">评价完成，感谢您的支持！</text>
        <view class="back-btn" @tap="goOrders">返回订单</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const order = ref(null)
const countdown = ref(0)
const delivered = ref(false)
const reviewed = ref(false)
const rating = ref(5)
const comment = ref('')
let timer = null

onMounted(() => {
  const id = getCurrentPages().slice(-1)[0]?.options?.orderId
  if (!id) return
  fetchOrder(id)
})

function fetchOrder(id) {
  uni.request({
    url: '/api/cart/orders?userId=' + uni.getStorageSync('user').id,
    success(res) {
      const orders = res.data.data
      order.value = orders.find(o => o.id === parseInt(id))
      if (order.value) {
        if (order.value.status === 3) {
          const elapsed = Math.floor((Date.now() - new Date(order.value.deliveryTime).getTime()) / 1000)
          countdown.value = Math.max(0, order.value.eta - elapsed)
          if (countdown.value > 0) startTimer()
          else showDelivered()
        } else if (order.value.status === 4) {
          showDelivered()
        } else if (order.value.status >= 5) {
          delivered.value = true
          reviewed.value = true
        }
      }
    }
  })
}

function startTimer() {
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      showDelivered()
    }
  }, 1000)
}

function showDelivered() {
  delivered.value = true
  uni.request({
    url: '/api/cart/orders/' + order.value.id + '/delivered',
    method: 'PUT'
  })
}

function submitReview() {
  uni.request({
    url: '/api/cart/orders/' + order.value.id + '/review',
    method: 'PUT',
    data: { rating: rating.value, review: comment.value },
    success() {
      reviewed.value = true
      uni.showToast({ title: '评价成功', icon: 'success' })
    }
  })
}

function goOrders() {
  uni.redirectTo({ url: '/pages/order/list' })
}

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #FFF8F5; padding-bottom: 120rpx; }
.header {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 36rpx; font-weight: 700;
  text-align: center; padding: 32rpx 0;
}
.content { padding: 24rpx; }
.rider-anim { text-align: center; padding: 40rpx 0 20rpx; }
.rider { font-size: 80rpx; }
.timer-wrap { text-align: center; padding: 20rpx 0; }
.timer-label { font-size: 26rpx; color: #999; display: block; }
.timer { font-size: 72rpx; font-weight: 700; color: #FF6B35; display: block; margin-top: 8rpx; }

.info-card {
  background: #fff; border-radius: 20rpx; padding: 24rpx;
  margin-bottom: 20rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
}
.info-row { display: flex; justify-content: space-between; padding: 12rpx 0; font-size: 26rpx; }
.label { color: #999; }
.price { color: #FF6B35; font-weight: 600; }

.detail-list { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 20rpx; }
.detail-row { display: flex; justify-content: space-between; align-items: center; padding: 8rpx 0; font-size: 26rpx; }
.detail-size { font-size: 20rpx; color: #FF6B35; }

.eval-box {
  background: #fff; border-radius: 20rpx; padding: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
}
.eval-title { font-size: 30rpx; font-weight: 600; display: block; text-align: center; margin-bottom: 20rpx; }
.stars { text-align: center; margin-bottom: 20rpx; }
.star { font-size: 48rpx; color: #DDD; margin: 0 8rpx; }
.star.filled { color: #FFB800; }
.comment-input {
  width: 100%; height: 120rpx; background: #F5F5F5;
  border-radius: 12rpx; padding: 16rpx; font-size: 26rpx; box-sizing: border-box;
}
.eval-btn {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 30rpx; font-weight: 600;
  text-align: center; padding: 24rpx; border-radius: 50rpx; margin-top: 20rpx;
}

.done-box { text-align: center; padding: 60rpx 0; }
.done-icon { font-size: 100rpx; display: block; }
.done-text { font-size: 30rpx; color: #2D2D2D; display: block; margin: 20rpx 0; }
.back-btn {
  display: inline-block; background: #FF6B35; color: #fff;
  font-size: 28rpx; padding: 16rpx 48rpx; border-radius: 40rpx; margin-top: 20rpx;
}
</style>
