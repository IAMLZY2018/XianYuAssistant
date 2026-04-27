import { request } from '@/utils/request'

/**
 * 卡密查询请求
 */
export interface CardSecretQueryReq {
    pageNum: number;
    pageSize: number;
    xianyuAccountId?: number | null;
    xyGoodsId?: string;
    isUsed?: number | null;
    cardContent?: string;
}

/**
 * 卡密信息响应
 */
export interface CardSecretResp {
    id: number;
    xianyuAccountId: number;
    accountNote: string;
    xyGoodsId: string;
    goodsTitle: string;
    cardContent: string;
    isUsed: number;
    orderId: string;
    createTime: string;
    updateTime: string;
}

export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}

/**
 * 分页查询卡密列表
 */
export function getCardSecretList(data: CardSecretQueryReq) {
    return request<PageResult<CardSecretResp>>({
        url: '/card-secret/list',
        method: 'POST',
        data
    })
}

/**
 * 批量导入卡密
 */
export function importCardSecrets(data: {
    xianyuAccountId: number;
    xyGoodsId: string;
    secrets: string[];
}) {
    return request<number>({
        url: '/card-secret/import',
        method: 'POST',
        data
    })
}

/**
 * 添加单条卡密
 */
export function addCardSecret(data: any) {
    return request({
        url: '/card-secret/add',
        method: 'POST',
        data
    })
}

/**
 * 更新单条卡密
 */
export function updateCardSecret(data: any) {
    return request({
        url: '/card-secret/update',
        method: 'POST',
        data
    })
}

/**
 * 删除卡密
 */
export function deleteCardSecret(id: number) {
    return request({
        url: '/card-secret/delete',
        method: 'POST',
        params: { id }
    })
}

/**
 * 批量删除卡密
 */
export function batchDeleteCardSecret(ids: number[]) {
    return request({
        url: '/card-secret/batch-delete',
        method: 'POST',
        data: ids
    })
}
