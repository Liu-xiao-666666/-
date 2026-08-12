<template>
  <view class="page">
    <view class="header">🍜 菜品列表</view>

    <view class="search-bar">
      <view class="search-input">
        <text class="search-icon">🔍</text>
        <input v-model="keyword" class="search-field" placeholder="搜索菜品名称" />
      </view>
    </view>

    <scroll-view scroll-x class="tabs">
      <view
        v-for="c in categories" :key="c"
        class="tab" :class="{ active: activeCat === c }"
        @tap="activeCat = c"
      >{{ c }}</view>
    </scroll-view>

    <view class="list">
      <view v-for="dish in filteredDishes" :key="dish.id" class="card">
        <image v-if="dish.image" :src="dish.image" class="card-img" mode="aspectFill" />
        <view class="card-img-placeholder" v-else>🍽️</view>
        <view class="card-body">
          <view class="card-title">{{ dish.name }}</view>
          <view class="card-desc" v-if="dish.description">{{ dish.description }}</view>
          <view class="card-footer">
            <text class="card-price">¥{{ dish.priceLarge || dish.price }}</text>
            <text class="card-sales">已售{{ dish.sales }}</text>
          </view>
        </view>
        <view class="add-btn" @tap.stop="addToCart(dish)">+</view>
      </view>

      <view v-if="filteredDishes.length === 0" class="empty">暂无菜品</view>
    </view>

    <!-- 分量选择弹窗 -->
    <view v-if="sizeModal.visible" class="modal-mask" @tap="sizeModal.visible = false">
      <view class="modal" @tap.stop>
        <text class="modal-title">{{ sizeModal.dish?.name }}</text>
        <view class="size-options">
          <view
            v-for="opt in sizeOptions" :key="opt.size"
            class="size-option"
            :class="{ disabled: !opt.available }"
            @tap="opt.available && confirmAdd(opt.size)"
          >
            <text class="size-label">{{ opt.label }}</text>
            <text class="size-price" v-if="opt.available">¥{{ opt.price }}</text>
            <text class="size-price" v-else>暂无</text>
          </view>
        </view>
        <view class="modal-close" @tap="sizeModal.visible = false">取消</view>
      </view>
    </view>

    <view class="bottom-nav">
      <view class="nav-item active" @tap="() => {}">🍜 点餐</view>

      <view class="nav-item" @tap="goCart">🛒 购物车</view>
      <view class="nav-item" @tap="goMy">👤 我的</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { get, post } from '@/utils/request'

const dishes = ref([])
const activeCat = ref('全部')
const categories = ref(['全部'])
const keyword = ref('')
const user = ref(null)
const sizeModal = ref({ visible: false, dish: null })

onMounted(() => {
  user.value = uni.getStorageSync('user')
  fetchDishes()
})

function fetchDishes() {
  get('/dish/page', { page: 1, size: 50, status: 1 }).then(res => {
    dishes.value = res.data.records
    const cats = new Set(dishes.value.map(d => d.category))
    categories.value = ['全部', ...cats]
  })
}

/** 可用的分量选项 */
const sizeOptions = computed(() => {
  const dish = sizeModal.value.dish
  if (!dish) return []
  const list = []
  // 大份：有 priceLarge 用 priceLarge，否则用 price（始终可选）
  list.push({
    size: 'large',
    label: '大份',
    price: dish.priceLarge || dish.price,
    available: true
  })
  // 小份：只有设置了 priceSmall 才可选
  list.push({
    size: 'small',
    label: '小份',
    price: dish.priceSmall,
    available: !!dish.priceSmall
  })
  return list
})

function addToCart(dish) {
  if (!user.value) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  // 如果菜品设置了分量价格，弹出选择弹窗；否则默认大份直接加入
  if (dish.priceSmall || dish.priceLarge) {
    sizeModal.value = { visible: true, dish }
  } else {
    post('/cart/add', { userId: user.value.id, dishId: dish.id, quantity: 1, size: 'large' }).then(res => {
      if (res.code === 1) {
        uni.showToast({ title: dish.name + ' 已加入购物车', icon: 'success' })
      } else {
        uni.showToast({ title: res.message || '添加失败', icon: 'none' })
      }
    })
  }
}

function confirmAdd(size) {
  const dish = sizeModal.value.dish
  sizeModal.value.visible = false
  post('/cart/add', { userId: user.value.id, dishId: dish.id, quantity: 1, size }).then(res => {
    if (res.code === 1) {
      uni.showToast({ title: (dish?.name || '') + ' 已加入购物车', icon: 'success' })
    } else {
      uni.showToast({ title: res.message || '添加失败', icon: 'none' })
    }
  })
}

const filteredDishes = computed(() => {
  let list = dishes.value
  if (activeCat.value !== '全部') list = list.filter(d => d.category === activeCat.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(d => d.name.toLowerCase().includes(kw))
  }
  return list
})

function goCart() { uni.redirectTo({ url: '/pages/cart/cart' }) }
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
.search-bar { padding: 16rpx 24rpx 0; }
.search-input {
  display: flex; align-items: center; background: #fff;
  border-radius: 40rpx; padding: 14rpx 28rpx;
}
.search-icon { font-size: 28rpx; margin-right: 12rpx; }
.search-field { flex: 1; font-size: 26rpx; color: #2D2D2D; }

.tabs { white-space: nowrap; background: #fff; padding: 16rpx 24rpx 20rpx; display: flex; }
.tab {
  display: inline-block; padding: 12rpx 28rpx; margin-right: 16rpx;
  border-radius: 40rpx; font-size: 26rpx; color: #666; background: #F5F5F5;
  &.active { background: #FF6B35; color: #fff; }
}
.list { padding: 20rpx 24rpx 120rpx; }
.card {
  display: flex; align-items: center; background: #fff;
  border-radius: 20rpx; margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-img, .card-img-placeholder {
  width: 200rpx; height: 200rpx; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 60rpx; background: #FFF0E5;
}
.card-body { flex: 1; padding: 20rpx 24rpx; display: flex; flex-direction: column; justify-content: space-between; }
.card-title { font-size: 32rpx; font-weight: 600; color: #2D2D2D; }
.card-desc { font-size: 24rpx; color: #999; margin-top: 6rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-footer { display: flex; justify-content: space-between; align-items: center; }
.card-price { font-size: 34rpx; font-weight: 700; color: #FF6B35; }
.card-sales { font-size: 22rpx; color: #BBB; }
.add-btn {
  width: 56rpx; height: 56rpx; border-radius: 50%;
  background: #FF6B35; color: #fff; font-size: 36rpx; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; margin-right: 20rpx;
}
.empty { text-align: center; padding: 100rpx 0; color: #999; font-size: 28rpx; }

.bottom-nav {
  position: fixed; bottom: 0; left: 0; right: 0;
  background: #fff; display: flex; justify-content: space-around;
  padding: 16rpx 0; padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06);
}
.nav-item { font-size: 24rpx; color: #999; text-align: center; }
.nav-item.active { color: #FF6B35; font-weight: 600; }

/* 分量选择弹窗 */
.modal-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.45); z-index: 1000;
  display: flex; align-items: flex-end; justify-content: center;
}
.modal {
  width: 100%; background: #fff; border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx 32rpx 32rpx;
  padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
}
.modal-title { font-size: 34rpx; font-weight: 700; color: #2D2D2D; text-align: center; display: block; margin-bottom: 32rpx; }
.size-options { display: flex; gap: 24rpx; margin-bottom: 32rpx; }
.size-option {
  flex: 1; background: #FFF8F5; border: 2rpx solid #FFE0D0;
  border-radius: 16rpx; padding: 28rpx 0; text-align: center;
  &.disabled { opacity: 0.4; }
}
.size-label { font-size: 30rpx; font-weight: 600; color: #2D2D2D; display: block; }
.size-price { font-size: 36rpx; font-weight: 700; color: #FF6B35; display: block; margin-top: 8rpx; }
.modal-close { text-align: center; font-size: 28rpx; color: #999; padding: 20rpx 0; }
</style>
