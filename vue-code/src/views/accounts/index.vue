<script setup lang="ts">
import { useAccountManager } from './useAccountManager'
import AccountTable from './components/AccountTable.vue'
import AddAccountDialog from './components/AddAccountDialog.vue'
import ManualAddDialog from './components/ManualAddDialog.vue'
import QRLoginDialog from './components/QRLoginDialog.vue'
import DeleteConfirmDialog from './components/DeleteConfirmDialog.vue'

import IconQrCode from '@/components/icons/IconQrCode.vue'
import IconPlus from '@/components/icons/IconPlus.vue'
import IconSync from '@/components/icons/IconSync.vue'
import IconAccount from '@/components/icons/IconAccount.vue'

const {
  loading,
  accounts,
  dialogs,
  currentAccount,
  deleteAccountId,
  loadAccounts,
  showAddDialog,
  showManualAddDialog,
  showQRLoginDialog,
  editAccount,
  deleteAccount
} = useAccountManager();

loadAccounts();
</script>

<template>
  <div class="accounts">
    <!-- Page Header -->
    <header class="accounts__header">
      <div class="accounts__title-row">
        <div class="accounts__title-icon"><IconAccount /></div>
        <h1 class="accounts__title mobile-hidden">闲鱼账号</h1>
      </div>
      <div class="accounts__actions">
        <button class="btn btn--primary" @click="showQRLoginDialog">
          <IconQrCode />
          <span>扫码添加</span>
        </button>
        <button class="btn btn--secondary" @click="showManualAddDialog">
          <IconPlus />
          <span>手动添加</span>
        </button>
      </div>
    </header>

    <!-- Content Card -->
    <section class="accounts__content">
      <div class="accounts__toolbar">
        <span class="accounts__list-title">账号列表</span>
        <button
          class="btn btn--ghost"
          :class="{ 'btn--loading': loading }"
          @click="loadAccounts"
          :disabled="loading"
        >
          <IconSync />
          <span>刷新</span>
        </button>
      </div>

      <div class="accounts__table-wrap">
        <AccountTable
          :accounts="accounts"
          :loading="loading"
          @edit="editAccount"
          @delete="deleteAccount"
        />
      </div>
    </section>

    <!-- Dialogs -->
    <AddAccountDialog
      v-model="dialogs.add"
      :account="currentAccount"
      @success="loadAccounts"
    />
    <ManualAddDialog v-model="dialogs.manualAdd" @success="loadAccounts" />
    <QRLoginDialog v-model="dialogs.qrLogin" @success="loadAccounts" />
    <DeleteConfirmDialog
      v-model="dialogs.deleteConfirm"
      :account-id="deleteAccountId"
      @success="loadAccounts"
    />
  </div>
</template>

<style scoped src="./accounts.css"></style>
