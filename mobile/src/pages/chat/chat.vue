<template>
  <view class="page">
    <view class="header">🍽️ 美食问问</view>

    <scroll-view class="chat-list" scroll-y :scroll-top="scrollTop" :scroll-with-animation="true">
      <view v-if="messages.length === 0" class="welcome">
        <text class="welcome-icon">🍳</text>
        <text class="welcome-text">今天想吃什么？</text>
        <text class="welcome-hint">试试问我：有什么推荐的？/ 想吃辣的配什么？</text>
        <view class="quick-asks">
          <view v-for="q in quickQuestions" :key="q" class="quick-btn" @tap="sendQuick(q)">{{ q }}</view>
        </view>
      </view>

      <view v-for="(msg, i) in messages" :key="i" class="msg-row" :class="msg.role === 'user' ? 'user' : 'bot'">
        <view class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🍳' }}</view>
        <view class="msg-bubble">
          <text>{{ msg.content }}</text>
          <view v-if="msg.cartUpdated" class="goto-cart" @tap="goCart">🛒 查看购物车 →</view>
        </view>
      </view>

      <view v-if="typing" class="msg-row bot">
        <view class="msg-avatar">🍳</view>
        <view class="msg-bubble typing"><text>...</text></view>
      </view>
    </scroll-view>

    <view class="input-bar">
      <input v-model="input" class="input-field" placeholder="输入你想吃的..." confirm-type="send"
             @confirm="send" :disabled="typing" />
      <view class="send-btn" @tap="send" :class="{ disabled: typing || !input.trim() }">发送</view>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'

const messages = ref([])
const input = ref('')
const typing = ref(false)
const scrollTop = ref(0)
const user = ref(null)
const quickQuestions = ['今天有什么推荐？', '想吃辣的配什么？', '推荐几个清淡的菜', '我的订单到哪了？']

onMounted(() => {
  user.value = uni.getStorageSync('user')
  if (!user.value) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1000)
  }
})

function send() {
  const text = input.value.trim()
  if (!text || typing.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  scrollBottom()
  typing.value = true

  uni.request({
    url: '/api/chat',
    method: 'POST',
    data: { userId: user.value.id, message: text },
    success(res) {
      if (res.data.code === 1) {
        messages.value.push({ role: 'bot', content: res.data.data.reply, cartUpdated: res.data.data.cartUpdated })
      } else {
        messages.value.push({ role: 'bot', content: '抱歉，出了点问题~' })
      }
    },
    fail() {
      messages.value.push({ role: 'bot', content: '网络异常，请稍后再试~' })
    },
    complete() {
      typing.value = false
      scrollBottom()
    }
  })
}

function sendQuick(q) {
  input.value = q
  send()
}

function scrollBottom() {
  nextTick(() => { scrollTop.value = 99999 })
}

function goCart() {
  uni.redirectTo({ url: '/pages/cart/cart' })
}
</script>

<style lang="scss" scoped>
.page { display: flex; flex-direction: column; height: 100vh; background: #FFF8F5; }
.header {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 36rpx; font-weight: 700;
  text-align: center; padding: 32rpx 0;
}
.chat-list { flex: 1; padding: 24rpx; overflow-y: auto; }

.welcome { text-align: center; padding: 80rpx 0; }
.welcome-icon { font-size: 80rpx; display: block; }
.welcome-text { font-size: 32rpx; font-weight: 600; color: #2D2D2D; display: block; margin-top: 16rpx; }
.welcome-hint { font-size: 24rpx; color: #999; display: block; margin-top: 8rpx; }
.quick-asks { display: flex; flex-wrap: wrap; justify-content: center; gap: 12rpx; margin-top: 24rpx; }
.quick-btn {
  background: #FFF0E5; color: #FF6B35; font-size: 24rpx;
  padding: 12rpx 24rpx; border-radius: 30rpx;
}

.msg-row { display: flex; margin-bottom: 24rpx; align-items: flex-start; }
.msg-row.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 60rpx; height: 60rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 32rpx; flex-shrink: 0; background: #fff;
}
.msg-bubble {
  max-width: 480rpx; padding: 18rpx 24rpx; border-radius: 20rpx;
  font-size: 28rpx; line-height: 1.6; word-break: break-all;
}
.user .msg-bubble { background: #FF6B35; color: #fff; margin-right: 12rpx; border-top-right-radius: 4rpx; }
.bot .msg-bubble { background: #fff; color: #2D2D2D; margin-left: 12rpx; border-top-left-radius: 4rpx; }
.typing .msg-bubble { padding: 12rpx 32rpx; }

.input-bar {
  display: flex; align-items: center; padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.04);
}
.input-field {
  flex: 1; height: 72rpx; background: #F5F5F5; border-radius: 36rpx;
  padding: 0 28rpx; font-size: 28rpx;
}
.send-btn {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 26rpx; font-weight: 600;
  padding: 16rpx 28rpx; border-radius: 36rpx; margin-left: 16rpx;
}
.send-btn.disabled { opacity: 0.4; }

.goto-cart {
  margin-top: 12rpx; padding: 12rpx 0; text-align: center;
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 24rpx; border-radius: 30rpx;
}
</style>
