<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">🍜 天空外卖后台</h1>
      <p class="subtitle">管理员登录</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Phone, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })
const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function handleLogin() {
  formRef.value.validate(valid => {
    if (!valid) return
    loading.value = true
    request.post('/user/login', form).then(res => {
      if (res.data.status !== 1) {
        ElMessage.error('非管理员账号')
        return
      }
      localStorage.setItem('admin', JSON.stringify(res.data))
      router.replace('/')
    }).finally(() => { loading.value = false })
  })
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #FF6B35 0%, #FF8C5A 50%, #FFB088 100%);
}
.login-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.title { font-size: 28px; color: #303133; text-align: center; margin-bottom: 8px; }
.subtitle { font-size: 14px; color: #909399; text-align: center; margin-bottom: 32px; }
.login-btn { width: 100%; }
</style>
