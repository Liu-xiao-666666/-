<template>
  <view class="page">
    <view class="header">个人信息</view>

    <view class="content">
      <view class="avatar-section" @tap="changeAvatar">
        <image v-if="form.avatar" :src="form.avatar" class="avatar" mode="aspectFill" />
        <view class="avatar-placeholder" v-else>👤</view>
        <text class="avatar-hint">点击更换头像</text>
      </view>

      <view class="field">
        <text class="field-label">昵称</text>
        <input class="field-input" v-model="form.nickname" placeholder="输入昵称" />
      </view>

      <view class="field">
        <text class="field-label">手机号</text>
        <input class="field-input" :value="user?.phone" disabled />
      </view>

      <view class="divider">
        <text class="divider-text">修改密码</text>
      </view>

      <view class="field">
        <text class="field-label">原密码</text>
        <input class="field-input" v-model="form.oldPassword" placeholder="输入原密码" password />
      </view>
      <view class="field">
        <text class="field-label">新密码</text>
        <input class="field-input" v-model="form.newPassword" placeholder="输入新密码（6-20位）" password />
      </view>
      <view class="field">
        <text class="field-label">确认密码</text>
        <input class="field-input" v-model="form.confirmPassword" placeholder="再次输入新密码" password />
      </view>

      <view class="save-btn" @tap="handleSave">保存</view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'

const user = ref(null)
const form = reactive({ id: null, nickname: '', avatar: '', oldPassword: '', newPassword: '', confirmPassword: '' })

onMounted(() => {
  user.value = uni.getStorageSync('user')
  if (!user.value) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1000)
    return
  }
  form.id = user.value.id
  form.nickname = user.value.nickname || ''
  form.avatar = user.value.avatar || ''
})

function changeAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success(res) {
      uni.showLoading({ title: '上传中' })
      uni.uploadFile({
        url: '/api/file/upload',
        filePath: res.tempFilePaths[0],
        name: 'file',
        success(r) {
          const data = JSON.parse(r.data)
          if (data.code === 1) {
            form.avatar = data.data
          } else {
            uni.showToast({ title: '上传失败', icon: 'none' })
          }
        },
        fail() {
          uni.showToast({ title: '上传失败', icon: 'none' })
        },
        complete() {
          uni.hideLoading()
        }
      })
    }
  })
}

function handleSave() {
  if (!form.nickname.trim()) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }

  // 如果填了密码字段，校验并修改密码
  if (form.oldPassword) {
    if (!form.newPassword) {
      uni.showToast({ title: '请输入新密码', icon: 'none' })
      return
    }
    if (form.newPassword.length < 6) {
      uni.showToast({ title: '新密码至少6位', icon: 'none' })
      return
    }
    if (form.newPassword !== form.confirmPassword) {
      uni.showToast({ title: '两次密码不一致', icon: 'none' })
      return
    }
    uni.request({
      url: '/api/user/password',
      method: 'PUT',
      data: { userId: form.id, oldPassword: form.oldPassword, newPassword: form.newPassword },
      success(res) {
        if (res.data.code === 1) {
          uni.showToast({ title: '密码修改成功', icon: 'success' })
          form.oldPassword = ''
          form.newPassword = ''
          form.confirmPassword = ''
        } else {
          uni.showToast({ title: res.data.message, icon: 'none' })
        }
      }
    })
    return
  }

  // 只修改昵称/头像
  uni.request({
    url: '/api/user/profile',
    method: 'PUT',
    data: { id: form.id, nickname: form.nickname, avatar: form.avatar },
    success(res) {
      if (res.data.code === 1) {
        const updated = res.data.data
        user.value.nickname = updated.nickname
        user.value.avatar = updated.avatar
        uni.setStorageSync('user', user.value)
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1000)
      } else {
        uni.showToast({ title: res.data.message, icon: 'none' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #FFF8F5; }
.header {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 36rpx; font-weight: 700;
  text-align: center; padding: 32rpx 0;
}

.content { padding: 32rpx 24rpx; }

.avatar-section {
  display: flex; flex-direction: column; align-items: center;
  padding: 40rpx 0; margin-bottom: 24rpx;
}
.avatar, .avatar-placeholder {
  width: 140rpx; height: 140rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 70rpx; background: #FFF0E5;
}
.avatar-hint { font-size: 24rpx; color: #999; margin-top: 12rpx; }

.field {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  margin-bottom: 16rpx; display: flex; align-items: center;
}
.field-label { font-size: 28rpx; color: #2D2D2D; width: 120rpx; flex-shrink: 0; }
.field-input { flex: 1; font-size: 28rpx; color: #2D2D2D; }

.divider {
  display: flex; align-items: center; padding: 32rpx 0 16rpx;
}
.divider-text { font-size: 26rpx; color: #999; }

.save-btn {
  background: linear-gradient(135deg, #FF6B35, #FF8C5A);
  color: #fff; font-size: 32rpx; font-weight: 600;
  text-align: center; padding: 28rpx; border-radius: 50rpx;
  margin-top: 32rpx;
}
</style>
