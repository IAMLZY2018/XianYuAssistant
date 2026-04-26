<script setup lang="ts">
import { useAutoReply } from './useAutoReply'
import './auto-reply.css'

import IconChat from '@/components/icons/IconChat.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconRobot from '@/components/icons/IconRobot.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconImage from '@/components/icons/IconImage.vue'
import IconSparkle from '@/components/icons/IconSparkle.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconPackage from '@/components/icons/IconPackage.vue'
import IconClipboard from '@/components/icons/IconClipboard.vue'
import IconSearch from '@/components/icons/IconSearch.vue'

import GoodsDetailDialog from '../goods/components/GoodsDetailDialog.vue'

const {
  saving,
  accounts,
  selectedAccountId,
  goodsList,
  selectedGoods,
  goodsTotal,
  goodsLoading,
  goodsListRef,
  detailDialogVisible,
  selectedGoodsId,
  rightTab,
  dataContent,
  uploading,
  dataList,
  dataLoading,
  dataVisible,
  chatMessages,
  chatInput,
  chatSending,
  chatListRef,
  isMobile,
  mobileView,
  confirmDialog,
  delaySeconds,
  configLoading,
  configSaving,
  recordsVisible,
  recordsList,
  recordsLoading,
  recordsTotal,
  recordsPage,
  recordsPageSize,
  recordDetailVisible,
  recordDetail,
  contextExpanded,
  handleAccountChange,
  selectGoods,
  toggleAutoReply,
  toggleContextOn,
  handleUploadData,
  handleQueryData,
  handleDeleteData,
  handleSendChat,
  handleChatKeydown,
  handleGoodsScroll,
  goBackToGoods,
  viewGoodsDetail,
  handleDialogConfirm,
  handleDialogCancel,
  formatTime,
  formatPrice,
  getStatusText,
  getStatusClass,
  updateDelaySeconds,
  toggleRecords,
  loadRecords,
  viewRecordDetail,
  handleRecordsPageChange,
  parseTriggerContext
} = useAutoReply()
</script>

<template>
  <div class="ar">
    <!-- Header -->
    <div class="ar__header">
      <div class="ar__title-row">
        <div class="ar__title-icon">
          <IconChat />
        </div>
        <h1 class="ar__title">自动回复</h1>
      </div>
      <div class="ar__actions">
        <div class="ar__select-wrap">
          <select
            v-model="selectedAccountId"
            class="ar__select"
            @change="handleAccountChange"
          >
            <option :value="null" disabled>选择账号</option>
            <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
              {{ acc.accountNote || acc.unb }}
            </option>
          </select>
          <span class="ar__select-icon">
            <IconChevronDown />
          </span>
        </div>
      </div>
    </div>

    <!-- Body -->
    <div class="ar__body">
      <!-- Goods Panel (Left) -->
      <div
        class="ar__goods-panel"
        :class="{ 'ar__goods-panel--hidden': isMobile && mobileView === 'config' }"
      >
        <div class="ar__goods-toolbar">
          <span class="ar__goods-toolbar-title">商品列表</span>
          <span v-if="goodsTotal > 0" class="ar__goods-toolbar-count">共 {{ goodsTotal }} 件</span>
        </div>

        <div
          class="ar__goods-list"
          ref="goodsListRef"
          @scroll="handleGoodsScroll"
        >
          <!-- Loading first page -->
          <div v-if="goodsLoading && goodsList.length === 0" class="ar__loading">
            <div class="ar__spinner"></div>
            <span>加载中...</span>
          </div>

          <!-- Goods items -->
          <div
            v-for="goods in goodsList"
            :key="goods.item.xyGoodId"
            class="ar__goods-item"
            :class="{ 'ar__goods-item--active': selectedGoods?.item.xyGoodId === goods.item.xyGoodId }"
            @click="selectGoods(goods)"
          >
            <img
              :src="goods.item.coverPic"
              :alt="goods.item.title"
              class="ar__goods-cover"
            />
            <div class="ar__goods-info">
              <div class="ar__goods-title">{{ goods.item.title }}</div>
              <div class="ar__goods-meta">
                <span class="ar__goods-price">{{ formatPrice(goods.item.soldPrice) }}</span>
                <span
                  class="ar__goods-status"
                  :class="`ar__goods-status--${getStatusClass(goods.item.status)}`"
                >
                  {{ getStatusText(goods.item.status) }}
                </span>
                <span
                  v-if="goods.xianyuAutoReplyOn === 1"
                  class="ar__goods-auto-badge ar__goods-auto-badge--on"
                >
                  <IconSparkle />
                  自动
                </span>
              </div>
            </div>
          </div>

          <!-- Loading more -->
          <div v-if="goodsLoading && goodsList.length > 0" class="ar__loading">
            <div class="ar__spinner"></div>
            <span>加载中...</span>
          </div>

          <!-- No more data -->
          <div
            v-if="!goodsLoading && goodsList.length > 0 && goodsList.length >= goodsTotal"
            class="ar__no-more"
          >
            已加载全部
          </div>

          <!-- Empty -->
          <div v-if="!goodsLoading && goodsList.length === 0" class="ar__empty">
            <IconPackage />
            <span class="ar__empty-text">暂无商品</span>
          </div>
        </div>
      </div>

      <!-- Config Panel (Right) -->
      <div
        class="ar__config-panel"
        :class="{ 'ar__config-panel--hidden': isMobile && mobileView === 'goods' }"
      >
        <!-- Mobile back button -->
        <div v-if="isMobile && selectedGoods" class="ar__config-header">
          <button class="ar__back-btn" @click="goBackToGoods">
            <IconChevronLeft />
            返回
          </button>
          <img
            v-if="selectedGoods"
            :src="selectedGoods.item.coverPic"
            :alt="selectedGoods.item.title"
            class="ar__config-goods-cover"
          />
          <div class="ar__config-goods-info">
            <div class="ar__config-goods-title">{{ selectedGoods.item.title }}</div>
            <div class="ar__config-goods-sub">{{ formatPrice(selectedGoods.item.soldPrice) }}</div>
          </div>
        </div>

        <!-- Desktop config header -->
        <div v-if="!isMobile && selectedGoods" class="ar__config-header">
          <img
            :src="selectedGoods.item.coverPic"
            :alt="selectedGoods.item.title"
            class="ar__config-goods-cover"
          />
          <div class="ar__config-goods-info">
            <div class="ar__config-goods-title">{{ selectedGoods.item.title }}</div>
            <div class="ar__config-goods-sub">{{ formatPrice(selectedGoods.item.soldPrice) }}</div>
          </div>
          <button class="btn btn--ghost btn--sm" @click="viewGoodsDetail">
            <IconImage />
            <span class="mobile-hidden">详情</span>
          </button>
        </div>

        <!-- Empty state -->
        <div v-if="!selectedGoods" class="ar__config-empty">
          <IconChat />
          <span class="ar__config-empty-text">选择商品以配置自动回复</span>
        </div>

        <!-- Config content -->
        <div v-if="selectedGoods" class="ar__config-scroll">
          <!-- Auto Reply Toggle -->
          <div class="ar__config-section">
            <div class="ar__config-section-title">回复设置</div>

            <div class="ar__toggle-row">
              <div class="ar__toggle-info">
                <div class="ar__toggle-label">自动回复</div>
                <div class="ar__toggle-hint">买家咨询时基于AI知识库自动回复</div>
              </div>
              <label class="ar__switch">
                <input
                  type="checkbox"
                  :checked="selectedGoods.xianyuAutoReplyOn === 1"
                  @change="toggleAutoReply(($event.target as HTMLInputElement).checked)"
                />
                <span class="ar__switch-track"></span>
                <span class="ar__switch-thumb"></span>
              </label>
            </div>

            <div v-if="selectedGoods.xianyuAutoReplyOn === 1" class="ar__toggle-row">
              <div class="ar__toggle-info">
                <div class="ar__toggle-label">携带上下文</div>
                <div class="ar__toggle-hint">将会话中买家和卖家的历史消息一起发送给大模型</div>
              </div>
              <label class="ar__switch">
                <input
                  type="checkbox"
                  :checked="selectedGoods.xianyuAutoReplyContextOn === 1"
                  @change="toggleContextOn(($event.target as HTMLInputElement).checked)"
                />
                <span class="ar__switch-track"></span>
                <span class="ar__switch-thumb"></span>
              </label>
            </div>

            <!-- Delay Config (show when auto reply is enabled) -->
            <div v-if="selectedGoods.xianyuAutoReplyOn === 1" class="ar__delay-config">
              <div class="ar__delay-label">回复延时</div>
              <div class="ar__delay-input-wrap">
                <input
                  type="number"
                  v-model.number="delaySeconds"
                  class="ar__delay-input"
                  min="5"
                  max="120"
                  :disabled="configSaving"
                />
                <span class="ar__delay-unit">秒</span>
              </div>
              <button
                class="ar__delay-save-btn"
                :disabled="configSaving"
                @click="updateDelaySeconds"
              >
                保存
              </button>
              <div class="ar__delay-hint">买家发送消息后等待指定时间，若无新消息则自动回复</div>
            </div>
          </div>

          <!-- Tab Switch: Data / Chat -->
          <div class="ar__tab-group">
            <button
              class="ar__tab-btn"
              :class="{ 'ar__tab-btn--active': rightTab === 'data' }"
              @click="rightTab = 'data'"
            >
              <IconClipboard />
              知识资料
            </button>
            <button
              class="ar__tab-btn"
              :class="{ 'ar__tab-btn--active': rightTab === 'chat' }"
              @click="rightTab = 'chat'"
            >
              <IconRobot />
              AI 对话
            </button>
          </div>

          <!-- ====== 知识资料视图 ====== -->
          <template v-if="rightTab === 'data'">
            <!-- Upload view -->
            <div v-if="!dataVisible" class="ar__config-section">
              <div class="ar__config-section-title">添加资料</div>
              <div class="ar__toggle-hint" style="margin-bottom: 8px;">
                上传商品相关资料到AI知识库，AI将基于这些资料自动回复买家咨询
              </div>

              <textarea
                v-model="dataContent"
                class="ar__textarea"
                placeholder="请输入商品资料内容，如商品介绍、规格参数、使用说明、常见问题等"
                maxlength="5000"
              ></textarea>
              <div class="ar__textarea-footer">
                <span class="ar__textarea-hint">支持文本内容，将存入AI知识库</span>
                <span class="ar__textarea-count">{{ dataContent.length }} / 5000</span>
              </div>

              <div class="ar__save-row">
                <button
                  class="btn btn--primary"
                  :class="{ 'btn--loading': uploading }"
                  :disabled="uploading"
                  @click="handleUploadData"
                >
                  <IconCheck />
                  添加资料
                </button>
                <button
                  class="btn btn--secondary"
                  :class="{ 'btn--loading': dataLoading }"
                  :disabled="dataLoading"
                  @click="dataVisible = true; handleQueryData()"
                >
                  <IconSearch />
                  查看现有资料
                </button>
                <button
                  class="btn btn--secondary"
                  @click="toggleRecords"
                >
                  <IconSearch />
                  查看回复记录
                </button>
              </div>
            </div>

            <!-- Existing data view (replaces upload view) -->
            <div v-else class="ar__data-section">
              <div class="ar__data-section-header">
                <span class="ar__data-section-title">现有资料</span>
                <span v-if="!dataLoading && dataList.length > 0" class="ar__data-section-count">共 {{ dataList.length }} 条</span>
                <button class="btn btn--ghost btn--sm" style="margin-left: auto;" @click="dataVisible = false">
                  返回上传
                </button>
              </div>

              <div class="ar__data-scroll">
                <div v-if="dataLoading" class="ar__loading">
                  <div class="ar__spinner"></div>
                  <span>加载中...</span>
                </div>

                <div v-else-if="dataList.length === 0" class="ar__data-empty">
                  <span class="ar__data-empty-text">暂无资料</span>
                </div>

                <!-- Desktop: Table view -->
                <table v-else-if="!isMobile" class="ar__data-table">
                  <thead class="ar__data-table-head">
                    <tr>
                      <th class="ar__data-table-th ar__data-table-th--index">#</th>
                      <th class="ar__data-table-th ar__data-table-th--content">资料内容</th>
                      <th class="ar__data-table-th ar__data-table-th--time">创建时间</th>
                      <th class="ar__data-table-th ar__data-table-th--action">操作</th>
                    </tr>
                  </thead>
                  <tbody class="ar__data-table-body">
                    <tr v-for="(item, index) in dataList" :key="item.documentId" class="ar__data-table-tr">
                      <td class="ar__data-table-td ar__data-table-td--index">{{ index + 1 }}</td>
                      <td class="ar__data-table-td ar__data-table-td--content">
                        <span class="ar__data-content-text">{{ item.content }}</span>
                      </td>
                      <td class="ar__data-table-td ar__data-table-td--time">{{ formatTime(item.createTime) }}</td>
                      <td class="ar__data-table-td ar__data-table-td--action">
                        <button class="ar__data-del-btn" @click="handleDeleteData(item.documentId)">删除</button>
                      </td>
                    </tr>
                  </tbody>
                </table>

                <!-- Mobile: Card view -->
                <div v-else class="ar__data-card-list">
                  <div v-for="(item, index) in dataList" :key="item.documentId" class="ar__data-card">
                    <div class="ar__data-card-header">
                      <span class="ar__data-card-index">#{{ index + 1 }}</span>
                      <span class="ar__data-card-time">{{ formatTime(item.createTime) }}</span>
                    </div>
                    <div class="ar__data-card-content">{{ item.content }}</div>
                    <div class="ar__data-card-footer">
                      <button class="ar__data-del-btn" @click="handleDeleteData(item.documentId)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- ====== AI 对话视图 ====== -->
          <template v-if="rightTab === 'chat'">
            <div class="ar__chat-container">
              <!-- Chat messages -->
              <div
                v-if="chatMessages.length > 0"
                class="ar__chat-list"
                ref="chatListRef"
              >
                <div
                  v-for="msg in chatMessages"
                  :key="msg.id"
                  class="ar__chat-msg"
                  :class="`ar__chat-msg--${msg.role}`"
                >
                  <div class="ar__chat-bubble" :class="{ 'ar__chat-bubble--loading': msg.loading }">
                    <template v-if="msg.loading">
                      <div class="ar__chat-dots">
                        <span class="ar__chat-dot"></span>
                        <span class="ar__chat-dot"></span>
                        <span class="ar__chat-dot"></span>
                      </div>
                    </template>
                    <template v-else>
                      {{ msg.content }}
                    </template>
                  </div>
                </div>
              </div>

              <!-- Chat empty -->
              <div v-if="chatMessages.length === 0" class="ar__chat-empty">
                <IconRobot />
                <span class="ar__chat-empty-text">AI 对话</span>
                <span class="ar__chat-empty-hint">基于商品知识库回答问题，输入消息开始对话</span>
              </div>

              <!-- Chat input -->
              <div class="ar__chat-input-area">
                <textarea
                  v-model="chatInput"
                  class="ar__chat-input"
                  placeholder="输入消息..."
                  rows="1"
                  :disabled="chatSending"
                  @keydown="handleChatKeydown"
                ></textarea>
                <button
                  class="ar__chat-send-btn"
                  :disabled="!chatInput.trim() || chatSending"
                  @click="handleSendChat"
                >
                  <IconSend />
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- Goods Detail Dialog -->
    <GoodsDetailDialog
      v-model="detailDialogVisible"
      :goods-id="selectedGoodsId"
      :account-id="selectedAccountId"
    />

    <!-- Records List Dialog -->
    <Transition name="overlay-fade">
      <div
        v-if="recordsVisible"
        class="ar__dialog-overlay"
        @click.self="recordsVisible = false"
      >
        <div class="ar__records-dialog">
          <div class="ar__records-dialog-header">
            <h3 class="ar__records-dialog-title">自动回复记录</h3>
            <span v-if="!recordsLoading" class="ar__records-dialog-count">共 {{ recordsTotal }} 条</span>
            <button class="ar__detail-dialog-close" @click="recordsVisible = false">&times;</button>
          </div>
          <div class="ar__records-dialog-body">
            <div v-if="recordsLoading" class="ar__loading">
              <div class="ar__spinner"></div>
              <span>加载中...</span>
            </div>

            <div v-else-if="recordsList.length === 0" class="ar__records-empty">
              <span>暂无回复记录</span>
            </div>

            <template v-else>
              <div
                v-for="record in recordsList"
                :key="record.id"
                class="ar__record-card"
              >
                <div class="ar__record-card-header">
                  <span class="ar__record-time">{{ formatTime(record.createTime) }}</span>
                  <span
                    class="ar__record-state"
                    :class="{
                      'ar__record-state--success': record.state === 1,
                      'ar__record-state--fail': record.state === -1,
                      'ar__record-state--pending': record.state === 0
                    }"
                  >
                    {{ record.state === 1 ? '成功' : record.state === -1 ? '失败' : '待回复' }}
                  </span>
                </div>

                <!-- User questions (max 3) -->
                <div class="ar__record-questions">
                  <template v-if="parseTriggerContext(record.triggerContext)?.triggerMessages?.length">
                    <div
                      v-for="(msg, idx) in parseTriggerContext(record.triggerContext).triggerMessages.slice(0, 3)"
                      :key="idx"
                      class="ar__record-question"
                    >
                      <span class="ar__record-question-label">Q{{ idx + 1 }}</span>
                      <span class="ar__record-question-text">{{ msg.msgContent }}</span>
                    </div>
                    <div
                      v-if="parseTriggerContext(record.triggerContext).triggerMessages.length > 3"
                      class="ar__record-more"
                    >
                      还有 {{ parseTriggerContext(record.triggerContext).triggerMessages.length - 3 }} 条消息，点击详情查看
                    </div>
                  </template>
                  <template v-else>
                    <div class="ar__record-question">
                      <span class="ar__record-question-label">Q</span>
                      <span class="ar__record-question-text">{{ record.buyerMessage }}</span>
                    </div>
                  </template>
                </div>

                <!-- AI reply -->
                <div class="ar__record-reply">
                  <span class="ar__record-reply-label">A</span>
                  <span class="ar__record-reply-text">{{ record.replyContent || '—' }}</span>
                </div>

                <!-- Detail button -->
                <div class="ar__record-card-footer">
                  <button class="btn btn--ghost btn--sm" @click="viewRecordDetail(record)">
                    详情
                  </button>
                </div>
              </div>

              <!-- Pagination -->
              <div v-if="recordsTotal > recordsPageSize" class="ar__records-pagination">
                <button
                  class="ar__records-page-btn"
                  :disabled="recordsPage <= 1"
                  @click="handleRecordsPageChange(recordsPage - 1)"
                >上一页</button>
                <span class="ar__records-page-info">{{ recordsPage }} / {{ Math.ceil(recordsTotal / recordsPageSize) }}</span>
                <button
                  class="ar__records-page-btn"
                  :disabled="recordsPage >= Math.ceil(recordsTotal / recordsPageSize)"
                  @click="handleRecordsPageChange(recordsPage + 1)"
                >下一页</button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Record Detail Dialog -->
    <Transition name="overlay-fade">
      <div
        v-if="recordDetailVisible"
        class="ar__dialog-overlay"
        @click.self="recordDetailVisible = false"
      >
        <div class="ar__detail-dialog">
          <div class="ar__detail-dialog-header">
            <h3 class="ar__detail-dialog-title">回复记录详情</h3>
            <button class="ar__detail-dialog-close" @click="recordDetailVisible = false">&times;</button>
          </div>
          <div class="ar__detail-dialog-body" v-if="recordDetail">
            <div class="ar__detail-row">
              <span class="ar__detail-label">回复时间</span>
              <span class="ar__detail-value">{{ formatTime(recordDetail.createTime) }}</span>
            </div>
            <div class="ar__detail-row">
              <span class="ar__detail-label">状态</span>
              <span
                class="ar__detail-value"
                :class="{
                  'ar__record-state--success': recordDetail.state === 1,
                  'ar__record-state--fail': recordDetail.state === -1,
                  'ar__record-state--pending': recordDetail.state === 0
                }"
              >{{ recordDetail.state === 1 ? '成功' : recordDetail.state === -1 ? '失败' : '待回复' }}</span>
            </div>
            <div class="ar__detail-row">
              <span class="ar__detail-label">买家</span>
              <span class="ar__detail-value">{{ recordDetail.buyerUserName || recordDetail.buyerUserId }}</span>
            </div>

            <!-- All user questions -->
            <div class="ar__detail-section">
              <div class="ar__detail-section-title">用户问题</div>
              <template v-if="parseTriggerContext(recordDetail.triggerContext)?.triggerMessages?.length">
                <div
                  v-for="(msg, idx) in parseTriggerContext(recordDetail.triggerContext).triggerMessages"
                  :key="idx"
                  class="ar__detail-msg-item"
                >
                  <div class="ar__detail-msg-meta">
                    <span class="ar__detail-msg-sender">{{ msg.senderUserName || msg.senderUserId }}</span>
                    <span v-if="msg.messageTime" class="ar__detail-msg-time">{{ new Date(msg.messageTime).toLocaleString('zh-CN') }}</span>
                  </div>
                  <div class="ar__detail-msg-content">{{ msg.msgContent }}</div>
                </div>
              </template>
              <template v-else>
                <div class="ar__detail-msg-item">
                  <div class="ar__detail-msg-content">{{ recordDetail.buyerMessage }}</div>
                </div>
              </template>
            </div>

            <!-- AI reply -->
            <div class="ar__detail-section">
              <div class="ar__detail-section-title">AI 回复</div>
              <div class="ar__detail-reply-content">{{ recordDetail.replyContent || '—' }}</div>
            </div>

            <!-- RAG hit details -->
            <template v-if="parseTriggerContext(recordDetail.triggerContext)?.ragHitDetails?.length">
              <div class="ar__detail-section">
                <div class="ar__detail-section-title">RAG 命中资料</div>
                <div
                  v-for="(hit, idx) in parseTriggerContext(recordDetail.triggerContext).ragHitDetails"
                  :key="idx"
                  class="ar__detail-hit-item"
                >
                  <div class="ar__detail-hit-meta">
                    <span class="ar__detail-hit-doc">文档 #{{ idx + 1 }}</span>
                    <span v-if="hit.score" class="ar__detail-hit-score">相似度: {{ (hit.score * 100).toFixed(1) }}%</span>
                  </div>
                  <div class="ar__detail-hit-content">{{ hit.content }}</div>
                </div>
              </div>
            </template>

            <!-- Context messages (collapsible) -->
            <template v-if="parseTriggerContext(recordDetail.triggerContext)?.contextMessages">
              <div class="ar__detail-section">
                <div class="ar__detail-context-header" @click="(_e: Event) => { contextExpanded = !contextExpanded }">
                  <span class="ar__detail-section-title" style="margin-bottom:0">携带上下文</span>
                  <span class="ar__detail-context-toggle">{{ contextExpanded ? '收起' : '展开' }}</span>
                </div>
                <div v-if="contextExpanded" class="ar__detail-context-body">
                  <div
                    v-for="(line, idx) in parseTriggerContext(recordDetail.triggerContext).contextMessages.split('\n')"
                    :key="idx"
                    class="ar__detail-context-line"
                    :class="{ 'ar__detail-context-line--user': line.startsWith('user:'), 'ar__detail-context-line--assistant': line.startsWith('assistant:') }"
                  >{{ line }}</div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Confirm Dialog -->
    <Transition name="overlay-fade">
      <div
        v-if="confirmDialog.visible"
        class="ar__dialog-overlay"
        @click.self="handleDialogCancel"
      >
        <div class="ar__dialog">
          <div class="ar__dialog-header">
            <h3 class="ar__dialog-title">{{ confirmDialog.title }}</h3>
          </div>
          <div class="ar__dialog-body">
            <p class="ar__dialog-text">{{ confirmDialog.message }}</p>
          </div>
          <div class="ar__dialog-footer">
            <button
              class="ar__dialog-btn ar__dialog-btn--cancel"
              @click="handleDialogCancel"
            >
              取消
            </button>
            <button
              class="ar__dialog-btn"
              :class="confirmDialog.type === 'danger' ? 'ar__dialog-btn--danger' : 'ar__dialog-btn--primary'"
              @click="handleDialogConfirm"
            >
              确定
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.2s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}
</style>
