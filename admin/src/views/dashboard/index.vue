<template>
  <div v-loading="loading" class="dashboard">
    <div class="stat-cards">
      <div class="stat-card blue"><div class="stat-val">{{ data.userCount }}</div><div class="stat-label">用户总数</div></div>
      <div class="stat-card green"><div class="stat-val">{{ data.dishCount }}</div><div class="stat-label">在售菜品</div></div>
      <div class="stat-card orange"><div class="stat-val">{{ data.orderCount }}</div><div class="stat-label">总订单数</div></div>
      <div class="stat-card purple"><div class="stat-val">¥{{ fmt(data.totalRevenue) }}</div><div class="stat-label">总营收</div></div>
    </div>

    <div class="stat-cards today">
      <div class="stat-card today-card">
        <div class="stat-val today-num">{{ data.todayOrderCount }}</div>
        <div class="stat-label">今日订单</div>
      </div>
      <div class="stat-card today-card">
        <div class="stat-val today-money">¥{{ fmt(data.todayRevenue) }}</div>
        <div class="stat-label">今日营收</div>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <div class="card-title">订单状态分布</div>
          <div class="status-bars">
            <div v-for="(count, status) in data.statusCounts" :key="status" class="bar-row">
              <span class="bar-label">{{ statusMap[status] }}</span>
              <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(count) }" :class="'bar-' + status"></div></div>
              <span class="bar-val">{{ count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="card-title">热销菜品 TOP5</div>
          <div class="rank-list" v-if="data.topDishes">
            <div v-for="(dish, i) in data.topDishes" :key="dish.id" class="rank-row">
              <span class="rank-num" :class="{ top3: i < 3 }">{{ i + 1 }}</span>
              <span class="rank-name">{{ dish.name }}</span>
              <span class="rank-sales">已售 {{ dish.sales }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <div class="card-title">评分分布</div>
      <div class="rating-row" v-if="data.ratingDist">
        <div v-for="(count, star) in data.ratingDist" :key="star" class="rating-item">
          <div class="star-count">{{ count }}</div>
          <div class="star-icons">{{ '★'.repeat(Number(star)) }}{{ '☆'.repeat(5 - Number(star)) }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const loading = ref(false)
const data = reactive({
  userCount: 0, dishCount: 0, orderCount: 0, totalRevenue: 0,
  todayOrderCount: 0, todayRevenue: 0,
  statusCounts: {}, topDishes: [], ratingDist: {}
})
const statusMap = { 1: '待付款', 2: '已支付', 3: '配送中', 4: '待评价', 5: '已完成' }

onMounted(() => {
  loading.value = true
  request.get('/dashboard').then(res => {
    Object.assign(data, res.data)
  }).finally(() => { loading.value = false })
})

function fmt(v) { return (v || 0).toFixed(2) }
function barWidth(count) {
  const max = Math.max(1, ...Object.values(data.statusCounts || {}))
  return (count / max * 100) + '%'
}
</script>

<style scoped>
.dashboard { max-width: 1400px; }

.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-cards.today { grid-template-columns: repeat(2, 1fr); }
.stat-card {
  border-radius: 12px; padding: 24px; color: #fff;
}
.stat-card.blue { background: linear-gradient(135deg, #409EFF, #337ECC); }
.stat-card.green { background: linear-gradient(135deg, #67C23A, #529B2E); }
.stat-card.orange { background: linear-gradient(135deg, #E6A23C, #CF9236); }
.stat-card.purple { background: linear-gradient(135deg, #9B59B6, #8E44AD); }
.today-card { background: linear-gradient(135deg, #FF6B35, #FF8C5A); }
.stat-val { font-size: 36px; font-weight: 700; }
.stat-label { font-size: 14px; opacity: 0.85; margin-top: 6px; }
.today-num { font-size: 48px; }
.today-money { font-size: 32px; }

.card-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #303133; }

.bar-row { display: flex; align-items: center; margin-bottom: 14px; }
.bar-label { width: 60px; font-size: 13px; color: #666; flex-shrink: 0; }
.bar-track { flex: 1; height: 20px; background: #F0F0F0; border-radius: 10px; overflow: hidden; margin: 0 12px; }
.bar-fill { height: 100%; border-radius: 10px; transition: width 0.4s; }
.bar-1 { background: #E6A23C; } .bar-2 { background: #909399; } .bar-3 { background: #409EFF; }
.bar-4 { background: #E6A23C; } .bar-5 { background: #67C23A; }
.bar-val { font-size: 13px; font-weight: 600; color: #303133; width: 24px; text-align: right; }

.rank-row { display: flex; align-items: center; padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.rank-row:last-child { border-bottom: none; }
.rank-num { width: 24px; height: 24px; border-radius: 4px; background: #DCDFE6; color: #fff; text-align: center; line-height: 24px; font-size: 13px; margin-right: 12px; }
.rank-num.top3 { background: #FF6B35; }
.rank-name { flex: 1; font-size: 14px; }
.rank-sales { font-size: 13px; color: #999; }

.rating-row { display: flex; justify-content: space-around; }
.rating-item { text-align: center; }
.star-count { font-size: 28px; font-weight: 700; color: #303133; }
.star-icons { font-size: 16px; color: #FFB800; margin-top: 4px; }
</style>
