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
          <div class="modal-body">
            <div v-if="goodsDetail" class="detail-content">
              <div class="detail-left">
                <div class="main-image">
                  <img
                    v-if="images.length > 0"
                    :src="images[currentImageIndex]"
                    class="native-image"
                  />
                  <div v-else class="native-empty"><span>暂无图片</span></div>
                </div>
                <div v-if="images.length > 1" class="thumbnails">
                  <div
                    v-for="(img, index) in images"
                    :key="index"
                    class="thumbnail"
                    :class="{ active: currentImageIndex === index }"
                    @click="selectImage(index)"
                  >
                    <img :src="img" class="native-image" />
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
                  <span class="tag" :class="getStatusType(goodsDetail.item.status) === 'success' ? 'tag--success' : getStatusType(goodsDetail.item.status) === 'warning' ? 'tag--warning' : 'tag--info'">
                    {{ getStatusText(goodsDetail.item.status) }}
                  </span>
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
                  <button class="btn-glass btn-glass--success" @click="handleConfigAutoDelivery">配置自动发货</button>
                  <button class="btn-glass btn-glass--danger" @click="handleDelete">删除商品</button>
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
  background: rgba(0,0,0,0.20);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

.modal-container {
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(40px) saturate(2);
  -webkit-backdrop-filter: blur(40px) saturate(2);
  border: 1px solid rgba(255,255,255,0.75);
  border-radius: 20px;
  width: 100%;
  max-width: 720px;
  max-height: 88vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-container.is-mobile {
  max-width: 94vw;
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
  color: #1c1c1e;
  margin: 0;
}

.modal-close {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: rgba(28,28,30,.55);
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close:hover {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
}

.modal-body {
  flex: 1;
  padding: 0 20px 20px;
  overflow-y: auto;
  min-height: 0;
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
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border: none;
}

.btn-secondary {
  background: rgba(255,255,255,0.55);
  border: 1px solid rgba(255,255,255,0.75);
  color: #1c1c1e;
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
}

.btn-secondary:hover {
  background: rgba(255,255,255,0.72);
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
  background: rgba(0,0,0,0.03);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image .native-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
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
  border-color: #0A84FF;
}

.thumbnail .native-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  color: #1c1c1e;
  margin: 0;
  line-height: 1.4;
}

.goods-id {
  font-size: 12px;
  color: rgba(28,28,30,.55);
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
  color: #FF453A;
}

.description {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.description-title {
  font-size: 13px;
  font-weight: 600;
  color: #1c1c1e;
}

.description-content {
  font-size: 13px;
  color: rgba(28,28,30,.55);
  line-height: 1.6;
  background: rgba(255,255,255,0.38);
  padding: 10px 12px;
  border-radius: 8px;
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border: 1px solid rgba(255,255,255,0.35);
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
  color: rgba(28,28,30,.55);
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
  background: rgba(48,209,88,.2);
  color: #30D158;
}

.detail-mode-tag--reply {
  background: rgba(191,90,242,.15);
  color: #BF5AF2;
}

.detail-mode-tag--off {
  background: rgba(120,120,128,.12);
  color: rgba(28,28,30,.55);
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

.btn-glass { display: inline-flex; align-items: center; justify-content: center; gap: 6px; padding: 8px 16px; border-radius: 100px; font-size: 13px; font-weight: 590; cursor: pointer; transition: opacity .15s, transform .12s; border: none; font-family: inherit; user-select: none; white-space: nowrap; }
.btn-glass:active { opacity: .80; transform: scale(.96); }
.btn-glass--primary { background: rgba(10,132,255,0.85); color: #fff; border: 1px solid rgba(255,255,255,0.35); box-shadow: 0 4px 16px rgba(10,132,255,0.35), 0 8px 32px rgba(0,0,0,0.08); }
.btn-glass--default { background: rgba(255,255,255,0.70); color: #0A84FF; border: 1px solid rgba(255,255,255,0.85); box-shadow: 0 8px 32px rgba(0,0,0,0.08); }
.btn-glass--success { background: rgba(48,209,88,0.85); color: #fff; border: 1px solid rgba(255,255,255,0.35); }
.btn-glass--warning { background: rgba(255,159,10,0.85); color: #fff; border: 1px solid rgba(255,255,255,0.35); }
.btn-glass--danger { color: #FF453A; background: rgba(255,69,58,0.15); border: 1px solid rgba(255,69,58,0.2); }
.tag { display: inline-flex; align-items: center; padding: 2px 10px; border-radius: 100px; font-size: 12px; font-weight: 500; }
.tag--success { background: rgba(48,209,88,0.12); color: #30D158; }
.tag--warning { background: rgba(255,159,10,0.12); color: #FF9F0A; }
.tag--info { background: rgba(120,120,128,0.12); color: rgba(28,28,30,.55); }
.tag--danger { background: rgba(255,69,58,0.12); color: #FF453A; }
.native-image { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.native-empty { display: flex; align-items: center; justify-content: center; color: rgba(28,28,30,.55); font-size: 13px; padding: 20px; }
</style>
