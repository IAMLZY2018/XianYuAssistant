<script setup lang="ts">
import { ref, computed } from 'vue'
import { uploadImage } from '@/api/image'
import { ElMessage } from 'element-plus'
import IconImage from '@/components/icons/IconImage.vue'
import IconClose from '@/components/icons/IconClose.vue'

interface Props {
  accountId: number
  modelValue?: string
  maxFileSize?: number
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'success', url: string): void
  (e: 'error', error: string): void
}

const props = withDefaults(defineProps<Props>(), {
  maxFileSize: 10
})

const emit = defineEmits<Emits>()

const uploading = ref(false)
const uploadProgress = ref(0)
const previewUrl = computed(() => props.modelValue)
const fileInput = ref<HTMLInputElement | null>(null)

const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  
  // 检查文件大小
  const maxSize = props.maxFileSize * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`图片大小不能超过${props.maxFileSize}MB`)
    return
  }
  
  await doUpload(file)
  target.value = ''
}

const doUpload = async (file: File) => {
  uploading.value = true
  uploadProgress.value = 0
  
  try {
    // 模拟上传进度
    const progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += 10
      }
    }, 100)
    
    const res = await uploadImage(props.accountId, file)
    
    clearInterval(progressInterval)
    uploadProgress.value = 100
    
    if (res && res.code === 200 && res.data) {
      const cdnUrl = res.data
      emit('update:modelValue', cdnUrl)
      emit('success', cdnUrl)
      ElMessage.success('图片上传成功')
    } else {
      throw new Error(res?.msg || '上传失败')
    }
  } catch (error: any) {
    emit('error', error.message || '上传失败')
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    setTimeout(() => {
      uploading.value = false
      uploadProgress.value = 0
    }, 500)
  }
}

const handleDrop = async (event: DragEvent) => {
  event.preventDefault()
  const file = event.dataTransfer?.files[0]
  if (!file) return
  
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  
  const maxSize = props.maxFileSize * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`图片大小不能超过${props.maxFileSize}MB`)
    return
  }
  
  await doUpload(file)
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
}

const clearImage = () => {
  emit('update:modelValue', '')
}
</script>

<template>
  <div class="image-uploader">
    <!-- 已上传图片预览 -->
    <div v-if="previewUrl" class="image-preview">
      <img :src="previewUrl" alt="预览图片" />
      <button class="image-preview__remove" @click="clearImage">
        <IconClose />
      </button>
    </div>
    
    <!-- 上传区域 -->
    <div
      v-else
      class="image-upload-area"
      :class="{ 'image-upload-area--uploading': uploading }"
      @drop="handleDrop"
      @dragover="handleDragOver"
      @click="(fileInput as HTMLInputElement)?.click()"
    >
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleFileSelect"
      />
      
      <div v-if="uploading" class="upload-progress">
        <div class="upload-progress__bar">
          <div class="upload-progress__fill" :style="{ width: uploadProgress + '%' }"></div>
        </div>
        <span class="upload-progress__text">上传中 {{ uploadProgress }}%</span>
      </div>
      
      <div v-else class="upload-hint">
        <IconImage />
        <span>点击或拖拽上传图片</span>
        <span class="upload-hint__size">最大{{ maxFileSize }}MB</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.image-uploader {
  width: 100%;
}

.image-preview {
  position: relative;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e5e5;
  background: #f5f5f7;
}

.image-preview img {
  width: 100%;
  display: block;
  max-height: 200px;
  object-fit: contain;
}

.image-preview__remove {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.image-preview__remove:hover {
  background: rgba(0, 0, 0, 0.8);
}

.image-preview__remove svg {
  width: 14px;
  height: 14px;
}

.image-upload-area {
  width: 100%;
  height: 120px;
  border: 2px dashed #d4d4d4;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.image-upload-area:hover {
  border-color: #1a1a1a;
  background: #f5f5f7;
}

.image-upload-area--uploading {
  pointer-events: none;
  border-color: #007aff;
}

.upload-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 13px;
}

.upload-hint svg {
  width: 28px;
  height: 28px;
  color: #999;
}

.upload-hint__size {
  font-size: 11px;
  color: #999;
}

.upload-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 80%;
}

.upload-progress__bar {
  width: 100%;
  height: 6px;
  background: #e5e5e5;
  border-radius: 3px;
  overflow: hidden;
}

.upload-progress__fill {
  height: 100%;
  background: #007aff;
  transition: width 0.2s;
}

.upload-progress__text {
  font-size: 12px;
  color: #007aff;
}
</style>
