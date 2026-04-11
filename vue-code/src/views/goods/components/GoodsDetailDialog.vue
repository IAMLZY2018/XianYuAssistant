<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { getGoodsDetail, updateAutoDeliveryStatus, updateAutoReplyStatus, deleteItem } from '@/api/goods';
import { showSuccess, showError, showConfirm } from '@/utils';
import type { GoodsItemWithConfig } from '@/api/goods';

interface Props {
  modelValue: boolean;
  goodsId: string;
  accountId: number | null;
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'refresh'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();
const router = useRouter();

const loading = ref(false);
const goodsDetail = ref<GoodsItemWithConfig | null>(null);
const currentImageIndex = ref(0);
const images = ref<string[]>([]);

// 响应式检测
const isMobile = ref(false);
const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768;
};

// 计算对话框宽度
const dialogWidth = computed(() => {
  return isMobile.value ? '95%' : '750px';
});

// 加载商品详情
const loadDetail = async () => {
  if (!props.goodsId) return;

  loading.value = true;
  try {
    const response = await getGoodsDetail(props.goodsId);
    if (response.code === 0 || response.code === 200) {
      goodsDetail.value = response.data?.itemWithConfig || null;
      
      // 解析图片列表
      if (goodsDetail.value?.item.infoPic) {
        try {
          const infoPicArray = JSON.parse(goodsDetail.value.item.infoPic);
          images.value = infoPicArray.map((pic: any) => pic.url);
        } catch (e) {
          console.error('解析图片列表失败:', e);
          images.value = [];
        }
      }
      
      // 如果没有图片，使用封面图
      if (images.value.length === 0 && goodsDetail.value?.item.coverPic) {
        images.value = [goodsDetail.value.item.coverPic];
      }
      
      currentImageIndex.value = 0;
    } else {
      throw new Error(response.msg || '获取商品详情失败');
    }
  } catch (error: any) {
    console.error('加载商品详情失败:', error);
  } finally {
    loading.value = false;
  }
};

// // 切换自动发货
// const handleToggleAutoDelivery = async (value: boolean) => {
//   if (!props.accountId || !goodsDetail.value) return;
//
//   try {
//     const response = await updateAutoDeliveryStatus({
//       xianyuAccountId: props.accountId,
//       xyGoodsId: goodsDetail.value.item.xyGoodId,
//       xianyuAutoDeliveryOn: value ? 1 : 0
//     });
//
//     if (response.code === 0 || response.code === 200) {
//       showSuccess(`自动发货${value ? '开启' : '关闭'}成功`);
//       goodsDetail.value.xianyuAutoDeliveryOn = value ? 1 : 0;
//       emit('refresh');
//     } else {
//       throw new Error(response.msg || '操作失败');
//     }
//   } catch (error: any) {
//     showError('操作失败: ' + error.message);
//     // 恢复开关状态
//     if (goodsDetail.value) {
//       goodsDetail.value.xianyuAutoDeliveryOn = value ? 0 : 1;
//     }
//   }
// };
//
// // 切换自动回复
// const handleToggleAutoReply = async (value: boolean) => {
//   if (!props.accountId || !goodsDetail.value) return;
//
//   try {
//     const response = await updateAutoReplyStatus({
//       xianyuAccountId: props.accountId,
//       xyGoodsId: goodsDetail.value.item.xyGoodId,
//       xianyuAutoReplyOn: value ? 1 : 0
//     });
//
//     if (response.code === 0 || response.code === 200) {
//       showSuccess(`自动回复${value ? '开启' : '关闭'}成功`);
//       goodsDetail.value.xianyuAutoReplyOn = value ? 1 : 0;
//       emit('refresh');
//     } else {
//       throw new Error(response.msg || '操作失败');
//     }
//   } catch (error: any) {
//     showError('操作失败: ' + error.message);
//     // 恢复开关状态
//     if (goodsDetail.value) {
//       goodsDetail.value.xianyuAutoReplyOn = value ? 0 : 1;
//     }
//   }
// };

// 配置自动发货
const handleConfigAutoDelivery = () => {
  if (!goodsDetail.value) return;

  router.push({
    path: '/auto-delivery',
    query: {
      accountId: props.accountId?.toString(),
      goodsId: goodsDetail.value.item.xyGoodId
    }
  });
  handleClose();
};

// 删除商品
const handleDelete = async () => {
  if (!props.accountId || !goodsDetail.value) return;

  try {
    await showConfirm(
      `确定要删除商品 "${goodsDetail.value.item.title}" 吗？此操作不可恢复。`,
      '删除确认'
    );

    const response = await deleteItem({
      xianyuAccountId: props.accountId,
      xyGoodsId: goodsDetail.value.item.xyGoodId
    });

    if (response.code === 0 || response.code === 200) {
      showSuccess('商品删除成功');
      handleClose();
      emit('refresh');
    } else {
      throw new Error(response.msg || '删除失败');
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return;
    }
    showError('删除失败: ' + error.message);
  }
};

// 获取状态标签类型
const getStatusType = (status: number) => {
  const statusMap: Record<number, string> = {
    0: 'success',
    1: 'info',
    2: 'warning'
  };
  return statusMap[status] || 'info';
};

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '在售',
    1: '已下架',
    2: '已售出'
  };
  return statusMap[status] || '未知';
};

// 格式化价格
const formatPrice = (price: string) => {
  return price ? `¥${price}` : '-';
};

// 选择图片
const selectImage = (index: number) => {
  currentImageIndex.value = index;
};

// 关闭对话框
const handleClose = () => {
  emit('update:modelValue', false);
  goodsDetail.value = null;
  images.value = [];
};

// 监听对话框打开
watch(() => props.modelValue, (val) => {
  if (val) {
    loadDetail();
  }
});

onMounted(() => {
  checkScreenSize();
  window.addEventListener('resize', checkScreenSize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkScreenSize);
});
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="商品详情"
    :width="dialogWidth"
    @close="handleClose"
    class="goods-detail-dialog"
    :class="{ 'mobile-dialog': isMobile }"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <div v-loading="loading" class="goods-detail">
      <div v-if="goodsDetail" class="detail-content">
        <div class="detail-left">
          <!-- 主图 -->
          <div class="main-image">
            <el-image
              v-if="images.length > 0"
              :src="images[currentImageIndex]"
              fit="contain"
              :preview-src-list="images"
              :initial-index="currentImageIndex"
            />
            <el-empty v-else description="暂无图片" :image-size="100" />
          </div>
          
          <!-- 缩略图 -->
          <div v-if="images.length > 1" class="thumbnails">
            <div
              v-for="(img, index) in images"
              :key="index"
              class="thumbnail"
              :class="{ active: currentImageIndex === index }"
              @click="selectImage(index)"
            >
              <el-image :src="img" fit="cover" />
            </div>
          </div>
        </div>

        <div class="detail-right">
          <!-- 标题和ID -->
          <div class="title-section">
            <h3 class="goods-title">{{ goodsDetail.item.title }}</h3>
            <div class="goods-id">ID: {{ goodsDetail.item.xyGoodId }}</div>
          </div>

          <!-- 价格和状态 -->
          <div class="price-section">
            <span class="price">{{ formatPrice(goodsDetail.item.soldPrice) }}</span>
            <el-tag :type="getStatusType(goodsDetail.item.status)" size="large">
              {{ getStatusText(goodsDetail.item.status) }}
            </el-tag>
          </div>

          <!-- 商品描述 -->
          <div v-if="goodsDetail.item.detailInfo" class="description">
            <div class="description-title">商品描述</div>
            <div class="description-content">{{ goodsDetail.item.detailInfo }}</div>
          </div>

          <!-- 配置项 -->
          <div class="config-section">
            <div class="config-item">
              <span class="config-label">自动发货</span>
              <div class="switch-container">
                <el-switch
                  :model-value="goodsDetail.xianyuAutoDeliveryOn === 1"
                  disabled
                />
                <span class="switch-status">
                  {{ goodsDetail.xianyuAutoDeliveryOn === 1 ? '已开启' : '已关闭' }}
                </span>
              </div>
            </div>
                        
            <div class="config-item">
              <span class="config-label">自动回复</span>
              <div class="switch-container">
                <el-switch
                  :model-value="goodsDetail.xianyuAutoReplyOn === 1"
                  disabled
                />
                <span class="switch-status">
                  {{ goodsDetail.xianyuAutoReplyOn === 1 ? '已开启' : '已关闭' }}
                </span>
              </div>
            </div>
          </div>

          <!-- 时间信息 -->
          <div class="time-info">
            <div v-if="goodsDetail.item.createdTime" class="time-item">
              <span class="time-label">创建时间：</span>
              <span class="time-value">{{ goodsDetail.item.createdTime }}</span>
            </div>
            <div v-if="goodsDetail.item.updatedTime" class="time-item">
              <span class="time-label">更新时间：</span>
              <span class="time-value">{{ goodsDetail.item.updatedTime }}</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <el-button type="success" @click="handleConfigAutoDelivery">
              配置自动发货
            </el-button>
            <el-button type="danger" @click="handleDelete">
              删除商品
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.goods-detail {
  min-height: 350px;
}

.detail-content {
  display: flex;
  gap: 20px;
}

.detail-left {
  flex: 0 0 350px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.main-image {
  width: 100%;
  height: 350px;
  background: #f5f7fa;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.thumbnails {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.thumbnail {
  width: 60px;
  height: 60px;
  border-radius: 3px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.3s;
  flex-shrink: 0;
}

.thumbnail:hover {
  border-color: #409eff;
}

.thumbnail.active {
  border-color: #409eff;
}

.thumbnail :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.detail-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
  min-width: 0;
}

.title-section {
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.goods-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.goods-id {
  font-size: 12px;
  color: #909399;
  font-family: 'Courier New', Consolas, monospace;
  word-break: break-all;
}

.price-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.price {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

.description {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.description-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.description-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 150px;
  overflow-y: auto;
}

/* 隐藏描述内容的滚动条 */
.description-content::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.description-content {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}

.config-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.switch-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.switch-status {
  font-size: 13px;
  color: #909399;
}

.time-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.time-item {
  font-size: 12px;
  color: #909399;
}

.time-label {
  font-weight: 500;
}

.time-value {
  color: #606266;
}

.action-buttons {
  display: flex;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.action-buttons .el-button {
  flex: 1;
}

/* 响应式 */
@media (max-width: 768px) {
  .goods-detail {
    min-height: auto;
  }

  .detail-content {
    flex-direction: column;
    gap: 16px;
  }

  .detail-left {
    flex: none;
    width: 100%;
  }

  .main-image {
    height: 280px;
  }

  .thumbnails {
    gap: 8px;
  }

  .thumbnail {
    width: 50px;
    height: 50px;
  }

  .detail-right {
    gap: 12px;
  }

  .goods-title {
    font-size: 16px;
  }

  .price {
    font-size: 20px;
  }

  .description-content {
    max-height: 120px;
  }

  .config-label {
    font-size: 13px;
  }

  /* 手机端按钮保持一行显示 */
  .action-buttons {
    flex-direction: row;
    gap: 10px;
  }

  .action-buttons .el-button {
    flex: 1;
    width: auto;
  }
}

@media (max-width: 480px) {
  .main-image {
    height: 240px;
  }

  .thumbnail {
    width: 45px;
    height: 45px;
  }

  .goods-title {
    font-size: 15px;
  }

  .price {
    font-size: 18px;
  }

  .description {
    padding: 10px;
  }

  .description-content {
    font-size: 12px;
    max-height: 100px;
  }

  /* 小屏手机按钮也保持一行 */
  .action-buttons {
    flex-direction: row;
    gap: 8px;
  }

  .action-buttons .el-button {
    flex: 1;
  }
}
</style>

<style>
/* 全局样式：商品详情对话框 */
.goods-detail-dialog .el-dialog__body {
  padding: 16px 20px;
}

.goods-detail-dialog .el-dialog__header {
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
}

.goods-detail-dialog .el-dialog__footer {
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
}

/* 隐藏滚动条但保留滚动功能 */
.goods-detail-dialog .el-dialog__body::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.goods-detail-dialog .el-dialog__body {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}

/* 手机端弹窗样式：居中显示 */
.mobile-dialog {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  margin: 0 !important;
  border-radius: 16px !important;
  max-height: 85vh !important;
  max-width: 95vw !important;
  display: flex !important;
  flex-direction: column !important;
  /* 淡入效果 */
  animation: fadeIn 0.2s ease-out !important;
}

.mobile-dialog .el-dialog__header {
  padding: 16px 16px 12px !important;
  border-bottom: 1px solid #ebeef5;
}

.mobile-dialog .el-dialog__body {
  padding: 12px 16px !important;
  max-height: calc(85vh - 140px) !important;
  overflow-y: auto !important;
  flex: 1 !important;
}

.mobile-dialog .el-dialog__footer {
  padding: 10px 16px !important;
  border-top: 1px solid #ebeef5;
  flex-shrink: 0 !important;
}

/* 淡入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 遮罩层样式 */
.mobile-dialog + .el-overlay {
  background-color: rgba(0, 0, 0, 0.5) !important;
}

@media (max-width: 768px) {
  .goods-detail-dialog .el-dialog__body {
    padding: 12px 16px;
    max-height: calc(100vh - 120px);
    overflow-y: auto;
  }

  .goods-detail-dialog .el-dialog__header {
    padding: 12px 16px;
  }

  .goods-detail-dialog .el-dialog__footer {
    padding: 10px 16px;
  }
}
</style>
