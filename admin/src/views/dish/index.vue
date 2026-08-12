<template>
  <div class="dish-page">
    <el-card>
      <el-form :inline="true" :model="query">
        <el-form-item>
          <el-input v-model="query.name" placeholder="菜品名称" clearable @keyup.enter="fetchList" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.category" placeholder="分类" clearable>
            <el-option label="热菜" value="热菜" />
            <el-option label="面食" value="面食" />
            <el-option label="饮品" value="饮品" />
            <el-option label="小吃" value="小吃" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="状态" clearable>
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" @click="openDialog()">新增菜品</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="dishes" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="菜品名称" width="120" />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column label="价格" width="130">
          <template #default="{ row }">
            <span v-if="row.priceSmall || row.priceLarge">
              <span v-if="row.priceSmall">小¥{{ row.priceSmall }} </span>
              <span v-if="row.priceLarge">大¥{{ row.priceLarge }}</span>
            </span>
            <span v-else>¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="image" label="图片" width="100">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" :preview-src-list="[row.image]" style="width:48px;height:48px" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="70" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width:100%">
            <el-option label="热菜" value="热菜" />
            <el-option label="面食" value="面食" />
            <el-option label="饮品" value="饮品" />
            <el-option label="小吃" value="小吃" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="小份价格">
          <el-input-number v-model="form.priceSmall" :min="0" :precision="2" style="width:100%" placeholder="不填则无小份选项" />
        </el-form-item>
        <el-form-item label="大份价格">
          <el-input-number v-model="form.priceLarge" :min="0" :precision="2" style="width:100%" placeholder="不填则用默认价格" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="upload-wrap">
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              style="display:none"
              @change="handleFileChange"
            />
            <el-button @click="$refs.fileInput.click()" :loading="uploading">
              {{ form.image ? '重新选择' : '点击上传' }}
            </el-button>
            <span v-if="!form.image" class="upload-hint">未选择图片</span>
            <div v-if="form.image" class="upload-preview">
              <el-image :src="form.image" style="width:80px;height:80px" fit="cover" :preview-src-list="[form.image]" />
              <el-button size="small" type="danger" @click="form.image = ''">移除</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可留空" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const dishes = ref([])
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const fileInput = ref()
const isEdit = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const query = reactive({ name: '', category: '', status: null })
const form = reactive({ id: null, name: '', category: '', price: 0, priceSmall: null, priceLarge: null, image: '', description: '', status: 1 })
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑菜品' : '新增菜品')

function fetchList() {
  loading.value = true
  const params = { page: page.value, size: size.value, name: query.name || undefined, category: query.category || undefined }
  if (query.status !== null && query.status !== '') params.status = query.status
  request.get('/dish/page', { params }).then(res => {
    dishes.value = res.data.records
    total.value = res.data.total
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  query.name = ''
  query.category = ''
  query.status = null
  page.value = 1
  fetchList()
}

function openDialog(row) {
  if (row) {
    isEdit.value = true
    Object.assign(form, row)
  } else {
    isEdit.value = false
    Object.assign(form, { id: null, name: '', category: '', price: 0, priceSmall: null, priceLarge: null, image: '', description: '', status: 1 })
  }
  dialogVisible.value = true
}

/** 文件选择后的上传处理 */
function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return

  // 校验是否为图片格式
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('文件格式错误，请选择图片文件（支持 JPG/PNG/GIF/WebP/BMP）')
    // 清空 input 以便重复选择同一个文件时仍能触发 change
    fileInput.value.value = ''
    return
  }

  uploading.value = true
  const formData = new FormData()
  formData.append('file', file)

  request.post('/file/upload', formData).then(res => {
    form.image = res.data
    ElMessage.success('上传成功')
  }).catch(() => {
    ElMessage.error('上传失败')
  }).finally(() => {
    uploading.value = false
    fileInput.value.value = ''
  })
}

function handleSubmit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const api = isEdit.value ? request.put('/dish', form) : request.post('/dish', form)
    api.then(() => {
      ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
      dialogVisible.value = false
      fetchList()
    }).finally(() => { submitting.value = false })
  })
}

function handleDelete(row) {
  ElMessageBox.confirm('确定删除该菜品吗？', '提示', { type: 'warning' }).then(() => {
    request.delete(`/dish/${row.id}`).then(() => {
      ElMessage.success('删除成功')
      fetchList()
    })
  }).catch(() => {})
}

onMounted(fetchList)
</script>

<style scoped>
.dish-page { max-width: 1400px; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }

.upload-wrap { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.upload-hint { color: #909399; font-size: 13px; }
.upload-preview { display: flex; align-items: center; gap: 8px; }
</style>
