<template>
  <div class="user-page">
    <el-card>
      <el-form :inline="true" :model="query">
        <el-form-item>
          <el-input v-model="query.phone" placeholder="手机号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="users" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="avatar" label="头像" width="100">
          <template #default="{ row }">
            <el-avatar v-if="row.avatar" :src="row.avatar" :size="32" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="160" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="togglingId === row.id"
              @change="toggleStatus(row)"
            />
            <span class="toggle-label">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const users = ref([])
const loading = ref(false)
const togglingId = ref(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const query = reactive({ phone: '' })

function fetchList() {
  loading.value = true
  request.get('/user/list', {
    params: { page: page.value, size: size.value, phone: query.phone || undefined }
  }).then(res => {
    users.value = res.data.records
    total.value = res.data.total
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  query.phone = ''
  page.value = 1
  fetchList()
}

function toggleStatus(row) {
  togglingId.value = row.id
  const newStatus = row.status === 1 ? 0 : 1
  request.put('/user/status', { id: row.id, status: newStatus }).then(() => {
    row.status = newStatus
  }).finally(() => { togglingId.value = null })
}

onMounted(fetchList)
</script>

<style scoped>
.user-page { max-width: 1200px; }
.toggle-label { margin-left: 8px; font-size: 13px; color: #909399; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
