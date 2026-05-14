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
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="handleClose">
        <div class="modal-container" :class="{ 'is-mobile': isMobile }">
          <!-- Header -->
          <div class="modal-header">
            <h2 class="modal-title">商品详情</h2>
            <button class="modal-close" @click="handleClose">×</button>
          </div>

          <!-- Body -->
          <div class="modal-body" v-loading="loading">
            <div v-if="goodsDetail" class="detail-content">
              <div class="detail-left">
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
                <div class="title-section">
                  <h3 class="goods-title">{{ goodsDetail.item.title }}</h3>
                  <div class="goods-id">ID: {{ goodsDetail.item.xyGoodId }}</div>
                </div>

                <div class="price-section">
                  <span class="price">{{ formatPrice(goodsDetail.item.soldPrice) }}</span>
                  <el-tag :type="getStatusType(goodsDetail.item.status)" size="large">
                    {{ getStatusText(goodsDetail.item.status) }}
                  </el-tag>
                </div>

                <div v-if="goodsDetail.item.detailInfo" class="description">
                  <div class="description-title">商品描述</div>
                  <div class="description-content">{{ goodsDetail.item.detailInfo }}</div>
                </div>

                <div class="config-section">
                  <div class="config-item">
                    <span class="config-label">自动发货</span>
                    <div class="config-value">
                      <span v-if="goodsDetail.xianyuAutoDeliveryOn === 1" class="detail-mode-tag detail-mode-tag--delivery">{{ (goodsDetail.autoDeliveryType ?? 1) === 2 ? '卡密发货' : '文本发货' }}</span>
                      <span v-else class="detail-mode-tag detail-mode-tag--off">未开启</span>
                    </div>
                  </div>
                  <div class="config-item">
                    <span class="config-label">自动回复</span>
                    <div class="config-value">
                      <span v-if="goodsDetail.xianyuAutoReplyOn === 1" class="detail-mode-tag detail-mode-tag--reply">已开启</span>
                      <span v-else class="detail-mode-tag detail-mode-tag--off">未开启</span>
                    </div>
                  </div>
                </div>

                <div class="action-buttons">
                  <el-button type="success" @click="handleConfigAutoDelivery">配置自动发货</el-button>
                  <el-button type="danger" @click="handleDelete">删除商品</el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="handleClose">关闭</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

.modal-container {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 720px;
  max-height: 88vh;
  box-shadow: 0 32px 100px rgba(0, 0, 0, 0.14), 0 12px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-container.is-mobile {
  max-width: 94vw;
  border-radius: 20px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  flex-shrink: 0;
}

.modal-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.modal-close {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: #86868b;
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1d1d1f;
}

.modal-body {
  flex: 1;
  padding: 0 20px 20px;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  flex-shrink: 0;
}

.btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border: none;
}

.btn-secondary {
  background: rgba(0, 0, 0, 0.06);
  color: #1d1d1f;
}

.btn-secondary:hover {
  background: rgba(0, 0, 0, 0.1);
}

.detail-content {
  display: flex;
  gap: 20px;
}

.detail-left {
  flex: 0 0 300px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.main-image {
  width: 100%;
  height: 300px;
  background: #f5f5f7;
  border-radius: 12px;
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
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.thumbnail {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
  flex-shrink: 0;
}

.thumbnail.active {
  border-color: #007aff;
}

.thumbnail :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.detail-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.title-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.goods-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
  line-height: 1.4;
}

.goods-id {
  font-size: 12px;
  color: #86868b;
  font-family: 'SF Mono', Menlo, monospace;
}

.price-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #ff3b30;
}

.description {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.description-title {
  font-size: 13px;
  font-weight: 600;
  color: #1d1d1f;
}

.description-content {
  font-size: 13px;
  color: #6e6e73;
  line-height: 1.6;
  background: #f5f5f7;
  padding: 10px 12px;
  border-radius: 8px;
}

.config-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.config-label {
  font-size: 13px;
  color: #86868b;
  min-width: 70px;
}

.config-value {
  display: flex;
  align-items: center;
}

.detail-mode-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.detail-mode-tag--delivery {
  background: rgba(52, 199, 89, 0.1);
  color: #34c759;
}

.detail-mode-tag--reply {
  background: rgba(88, 86, 214, 0.1);
  color: #5856d6;
}

.detail-mode-tag--off {
  background: rgba(0, 0, 0, 0.05);
  color: #86868b;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

/* Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.3s cubic-bezier(0.32, 0.94, 0.6, 1), opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.92) translateY(8px);
  opacity: 0;
}

/* Mobile */
@media (max-width: 768px) {
  .modal-container {
    max-width: 94vw;
  }

  .detail-content {
    flex-direction: column;
  }

  .detail-left {
    flex: none;
    width: 100%;
  }

  .main-image {
    height: 250px;
  }
}
</style>
