import { ref, reactive, computed } from 'vue'
import { getCardSecretList, deleteCardSecret, batchDeleteCardSecret, importCardSecrets, addCardSecret, updateCardSecret, type CardSecretQueryReq, type CardSecretResp } from '@/api/card-secret'
import { getAccountList } from '@/api/account'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useCardSecret() {
    const loading = ref(false)
    const cardSecretList = ref<CardSecretResp[]>([])
    const total = ref(0)
    const accounts = ref<any[]>([])
    
    const queryParams = reactive<CardSecretQueryReq>({
        pageNum: 1,
        pageSize: 10,
        xianyuAccountId: null,
        xyGoodsId: '',
        isUsed: null,
        cardContent: ''
    })

    const dialogs = reactive({
        edit: false,
        import: false
    })

    const editForm = reactive({
        id: undefined as number | undefined,
        xianyuAccountId: null as number | null,
        xyGoodsId: '',
        cardContent: '',
        isUsed: 0
    })

    const importForm = reactive({
        xianyuAccountId: null as number | null,
        xyGoodsId: '',
        secretsText: ''
    })

    const totalPages = computed(() => Math.ceil(total.value / queryParams.pageSize))

    const loadAccounts = async () => {
        try {
            const response = await getAccountList()
            accounts.value = response.data?.accounts || []
        } catch (error) {
            console.error('加载账号列表失败:', error)
        }
    }

    const loadCardSecrets = async () => {
        loading.value = true
        try {
            const response = await getCardSecretList(queryParams)
            if (response.data) {
                cardSecretList.value = response.data.records || []
                total.value = response.data.total || 0
            }
        } catch (error) {
            console.error('查询卡密列表失败:', error)
            ElMessage.error('查询卡密列表失败')
        } finally {
            loading.value = false
        }
    }

    const handleReset = () => {
        queryParams.pageNum = 1
        queryParams.xianyuAccountId = null
        queryParams.xyGoodsId = ''
        queryParams.isUsed = null
        queryParams.cardContent = ''
        loadCardSecrets()
    }

    const handlePageChange = (page: number) => {
        queryParams.pageNum = page
        loadCardSecrets()
    }

    const handleAdd = () => {
        editForm.id = undefined
        editForm.xianyuAccountId = queryParams.xianyuAccountId ?? null
        editForm.xyGoodsId = queryParams.xyGoodsId ?? ''
        editForm.cardContent = ''
        editForm.isUsed = 0
        dialogs.edit = true
    }

    const handleEdit = (row: CardSecretResp) => {
        editForm.id = row.id
        editForm.xianyuAccountId = row.xianyuAccountId
        editForm.xyGoodsId = row.xyGoodsId
        editForm.cardContent = row.cardContent
        editForm.isUsed = row.isUsed
        dialogs.edit = true
    }

    const saveEdit = async () => {
        if (!editForm.xianyuAccountId || !editForm.xyGoodsId || !editForm.cardContent) {
            ElMessage.warning('请填写完整信息')
            return
        }
        try {
            if (editForm.id) {
                await updateCardSecret(editForm)
                ElMessage.success('更新成功')
            } else {
                await addCardSecret(editForm)
                ElMessage.success('添加成功')
            }
            dialogs.edit = false
            loadCardSecrets()
        } catch (error: any) {
            if (!error.messageShown) {
                ElMessage.error('操作失败')
            }
        }
    }

    const handleImport = () => {
        importForm.xianyuAccountId = queryParams.xianyuAccountId ?? null
        importForm.xyGoodsId = queryParams.xyGoodsId ?? ''
        importForm.secretsText = ''
        dialogs.import = true
    }

    const saveImport = async () => {
        if (!importForm.xianyuAccountId || !importForm.xyGoodsId || !importForm.secretsText) {
            ElMessage.warning('请填写完整信息')
            return
        }
        const secrets = importForm.secretsText.split('\n').map(s => s.trim()).filter(s => s !== '')
        if (secrets.length === 0) {
            ElMessage.warning('请输入有效的卡密内容')
            return
        }
        try {
            const res = await importCardSecrets({
                xianyuAccountId: importForm.xianyuAccountId,
                xyGoodsId: importForm.xyGoodsId,
                secrets: secrets
            })
            ElMessage.success(`成功导入 ${res.data} 条卡密`)
            dialogs.import = false
            loadCardSecrets()
        } catch (error: any) {
            if (!error.messageShown) {
                ElMessage.error('导入失败')
            }
        }
    }

    const handleDelete = async (id: number) => {
        try {
            await ElMessageBox.confirm('确定要删除这条卡密吗？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            })
            await deleteCardSecret(id)
            ElMessage.success('删除成功')
            loadCardSecrets()
        } catch (error: any) {
            if (error !== 'cancel') {
                ElMessage.error('删除失败')
            }
        }
    }

    const selectedIds = ref<number[]>([])

    const handleSelectionChange = (selection: CardSecretResp[]) => {
        selectedIds.value = selection.map(item => item.id)
    }

    const handleBatchDelete = async () => {
        if (selectedIds.value.length === 0) return
        try {
            await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条卡密吗？`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            })
            await batchDeleteCardSecret(selectedIds.value)
            ElMessage.success('批量删除成功')
            selectedIds.value = []
            loadCardSecrets()
        } catch (error: any) {
            if (error !== 'cancel') {
                ElMessage.error('批量删除失败')
            }
        }
    }

    return {
        loading,
        cardSecretList,
        total,
        totalPages,
        queryParams,
        accounts,
        dialogs,
        editForm,
        importForm,
        loadAccounts,
        loadCardSecrets,
        handleReset,
        handlePageChange,
        handleAdd,
        handleEdit,
        saveEdit,
        handleImport,
        saveImport,
        handleDelete,
        handleBatchDelete,
        handleSelectionChange,
        selectedIds
    }
}
