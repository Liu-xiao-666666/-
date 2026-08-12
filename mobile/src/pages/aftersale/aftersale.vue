<template>
  <view class="page">
    <view class="header">🔧 售后</view>

    <view v-if="orders.length === 0" class="empty">
      <text class="empty-icon">📋</text>
      <text>暂无已完成订单</text>
    </view>

    <view class="list" v-else>
      <view v-for="order in orders" :key="order.id" class="swipe-wrap">
        <view class="swipe-del" @tap.stop="handleDelete(order.id)">删除</view>
        <!-- 订单卡片 -->
        <view
          class="card"
          :class="{ open: swipedId === order.id }"
          :style="{ transform: swipedId === order.id ? 'translateX(-160rpx)' : 'translateX(0)' }"
          @touchstart="onTouchStart($event, order.id)"
          @touchmove="onTouchMove($event, order.id)"
          @touchend="onTouchEnd"
          @tap="toggleExpand(order.id)"
        >
          <view class="card-header">
            <text class="order-no">订单号：{{ order.orderNo }}</text>
            <text class="as-tag" :class="'as-' + (order.afterSaleStatus || 0)">
              {{ afterSaleMap[order.afterSaleStatus || 0] }}
            </text>
          </view>
          <view v-for="d in order.details" :key="d.id" class="detail-row">
            <text>{{ d.dishName }}<text v-if="d.size" class="detail-size">({{ d.size === 'small' ? '小份' : '大份' }})</text></text>
            <text>x{{ d.quantity }}</text>
          </view>
          <view class="card-footer">
            <text class="time">{{ order.createTime }}</text>
            <text class="total">¥{{ order.total }}</text>
          </view>

          <!-- 展开的售后表单，@tap.stop 阻止冒泡到卡片导致收起 -->
          <view v-if="expandedId === order.id" class="as-form" @tap.stop>
            <view v-if="order.afterSaleStatus === 1" class="as-done">
              <text class="as-done-icon">⏳</text>
              <text>售后处理中，请耐心等待</text>
              <text class="as-reason">反馈内容：{{ order.afterSaleReason }}</text>
            </view>
            <view v-else>
              <text class="as-label">问题类型</text>
              <view class="type-grid">
                <view
                  v-for="t in issueTypes" :key="t"
                  class="type-item"
                  :class="{ selected: selectedType === t }"
                  @tap.stop="selectedType = t"
                >{{ t }}</view>
              </view>
              <text class="as-label">详细描述</text>
              <textarea v-model="reason" class="as-textarea" placeholder="请描述您遇到的问题…" />
              <view class="as-submit" @tap.stop="submitAfterSale(order)">提交申请</view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/utils/request'

const orders = ref([])
const expandedId = ref(null)
const selectedType = ref('')
const reason = ref('')
const swipedId = ref(null)
const startX = ref(0)
const startY = ref(0)
const issueTypes = ['少送菜品', '菜品错误', '质量问题', '配送问题', '其他']
const afterSaleMap = { 0: '无售后', 1: '处理中', 2: '已处理' }

onMounted(() => {
  const user = uni.getStorageSync('user')
  if (!user) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1000)
    return
  }
  get('/cart/orders', { userId: user.id }).then(res => {
    orders.value = (res.data || []).filter(o => o.status === 5).reverse()
  })
})

function toggleExpand(id) {
  if (swipedId.value) { swipedId.value = null; return }
  expandedId.value = expandedId.value === id ? null : (selectedType.value = '', reason.value = '', id)
}

function onTouchStart(e, id) {
  startX.value = e.touches[0].clientX
  startY.value = e.touches[0].clientY
}
function onTouchMove(e, id) {
  const dx = e.touches[0].clientX - startX.value
  const dy = e.touches[0].clientY - startY.value
  if (Math.abs(dy) > Math.abs(dx)) return
  if (dx < -30) swipedId.value = id
  else if (dx > 30) swipedId.value = null
}
function onTouchEnd() {}

function handleDelete(id) {
  uni.showModal({
    title: '确认删除',
    content: '确定删除该订单吗？',
    success: r => {
      if (r.confirm) {
        uni.request({
          url: '/api/cart/orders/' + id, method: 'DELETE',
          success: () => {
            uni.showToast({ title: '已删除', icon: 'success' })
            orders.value = orders.value.filter(o => o.id !== id)
            swipedId.value = null
          }
        })
      } else {
        swipedId.value = null
      }
    }
  })
}

function submitAfterSale(order) {
  if (!selectedType.value) {
    uni.showToast({ title: '请选择问题类型', icon: 'none' })
    return
  }
  if (!reason.value.trim()) {
    uni.showToast({ title: '请填写问题描述', icon: 'none' })
    return
  }
  const fullReason = '[' + selectedType.value + '] ' + reason.value.trim()
  uni.request({
    url: '/api/cart/orders/' + order.id + '/aftersale',
    method: 'PUT',
    data: { reason: fullReason },
    success(res) {
      if (res.data.code === 1) {
        uni.showToast({ title: '提交成功', icon: 'success' })
        order.afterSaleStatus = 1
        order.afterSaleReason = fullReason
        expandedId.value = null
      } else {
        uni.showToast({ title: res.data.message || '提交失败', icon: 'none' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #FFF8F5; padding-bottom: 40rpx; }
.header {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 36rpx; font-weight: 700;
  text-align: center; padding: 32rpx 0;
}
.empty { text-align: center; padding: 200rpx 0; color: #999; font-size: 28rpx; }
.empty-icon { font-size: 100rpx; display: block; margin-bottom: 20rpx; }

.list { padding: 20rpx 24rpx; }

.swipe-wrap {
  position: relative; margin-bottom: 20rpx;
  border-radius: 20rpx; overflow: hidden;
}
.swipe-del {
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
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.order-no { font-size: 24rpx; color: #999; }
.as-tag { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 20rpx; font-weight: 600; }
.as-0 { background: #F5F5F5; color: #999; }
.as-1 { background: #FFF3E0; color: #E6A23C; }
.as-2 { background: #E8F5E9; color: #67C23A; }
.detail-row { display: flex; justify-content: space-between; padding: 6rpx 0; font-size: 26rpx; color: #2D2D2D; }
.detail-size { font-size: 22rpx; color: #FF6B35; }
.card-footer { display: flex; justify-content: space-between; margin-top: 10rpx; padding-top: 10rpx; border-top: 1rpx solid #f5f5f5; }
.time { font-size: 22rpx; color: #BBB; }
.total { font-size: 28rpx; font-weight: 600; }

/* 售后表单 */
.as-form { margin-top: 20rpx; padding-top: 20rpx; border-top: 2rpx solid #f0f0f0; }
.as-label { font-size: 26rpx; font-weight: 600; color: #2D2D2D; display: block; margin-bottom: 12rpx; }
.type-grid { display: flex; flex-wrap: wrap; gap: 16rpx; margin-bottom: 24rpx; }
.type-item {
  padding: 12rpx 24rpx; border-radius: 30rpx; font-size: 24rpx;
  background: #F5F5F5; color: #666;
  &.selected { background: #FF6B35; color: #fff; }
}
.as-textarea {
  width: 100%; height: 140rpx; background: #F5F5F5; border-radius: 12rpx;
  padding: 16rpx; font-size: 26rpx; box-sizing: border-box; margin-bottom: 20rpx;
}
.as-submit {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 28rpx; font-weight: 600;
  text-align: center; padding: 20rpx; border-radius: 40rpx;
}
.as-done { text-align: center; padding: 20rpx 0; }
.as-done-icon { font-size: 60rpx; display: block; margin-bottom: 12rpx; }
.as-reason { font-size: 24rpx; color: #999; display: block; margin-top: 12rpx; }
</style>
