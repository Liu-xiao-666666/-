<template>
  <div class="order-page">
    <el-card>
      <el-form :inline="true" :model="query">
        <el-form-item><el-input v-model="query.phone" placeholder="手机号" clearable /></el-form-item>
        <el-form-item><el-input v-model="query.orderNo" placeholder="订单号" clearable /></el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="fetchList">
            <el-option label="待付款" :value="1" />
            <el-option label="已支付" :value="2" />
            <el-option label="配送中" :value="3" />
            <el-option label="待评价" :value="4" />
            <el-option label="已完成" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="orders" border stripe v-loading="loading" @expand-change="loadDetail">
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-table :data="row.details || []" size="small" border>
              <el-table-column prop="dishName" label="菜品" />
              <el-table-column prop="price" label="单价(¥)" width="100" />
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column label="小计(¥)" width="100">
                <template #default="{ row: d }">{{ (d.price * d.quantity).toFixed(2) }}</template>
              </el-table-column>
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="total" label="金额(¥)" width="100" />
        <el-table-column label="配送" width="100">
          <template #default="{ row }">{{ row.deliveryTime ? row.eta + 's' : '-' }}</template>
        </el-table-column>
        <el-table-column label="评分" width="70">
          <template #default="{ row }">{{ row.rating ? row.rating + '★' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="review" label="评价" min-width="100" show-overflow-tooltip />
        <el-table-column label="售后" width="260">
          <template #default="{ row }">
            <template v-if="row.afterSaleStatus === 1">
              <el-tag type="warning">处理中</el-tag>
              <el-popover placement="top" :width="280" trigger="click">
                <template #reference>
                  <el-button size="small" style="margin-left:6px">详情</el-button>
                </template>
                <div><strong>问题描述：</strong>{{ row.afterSaleReason }}</div>
              </el-popover>
              <el-button size="small" type="success" style="margin-left:6px" @click="resolveAfterSale(row)">解决</el-button>
            </template>
            <el-tag v-else-if="row.afterSaleStatus === 2" type="success">
              已处理
              <el-popover placement="top" :width="280" trigger="click">
                <template #reference>
                  <span style="cursor:pointer;margin-left:4px;text-decoration:underline">详情</span>
                </template>
                <div><strong>问题描述：</strong>{{ row.afterSaleReason }}</div>
              </el-popover>
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page" :page-size="size" :total="total"
          layout="total, prev, pager, next" @current-change="fetchList" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const orders = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const query = reactive({ phone: '', orderNo: '', status: null })
const statusMap = { 1: '待付款', 2: '已支付', 3: '配送中', 4: '待评价', 5: '已完成' }

onMounted(fetchList)

function fetchList() {
  loading.value = true
  const params = { page: page.value, size: size.value }
  if (query.phone) params.phone = query.phone
  if (query.orderNo) params.orderNo = query.orderNo
  if (query.status) params.status = query.status
  request.get('/orders', { params }).then(res => {
    orders.value = res.data.records
    total.value = res.data.total
  }).finally(() => { loading.value = false })
}

function loadDetail(row) {
  if (!row.details || row.details.length === 0) {
    request.get('/orders/' + row.id).then(res => { row.details = res.data.details })
  }
}

function resetQuery() {
  query.phone = ''
  query.orderNo = ''
  query.status = null
  page.value = 1
  fetchList()
}

function statusType(s) {
  return { 1: 'warning', 2: 'info', 3: 'primary', 4: 'warning', 5: 'success' }[s] || 'info'
}

function resolveAfterSale(row) {
  request.put('/orders/aftersale/resolve', { orderId: row.id }).then(res => {
    if (res.code === 1) {
      row.afterSaleStatus = 2
      ElMessage.success('已处理')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  }).catch(() => {
    ElMessage.error('操作失败，请稍后重试')
  })
}
</script>

<style scoped>
.order-page { max-width: 1400px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
