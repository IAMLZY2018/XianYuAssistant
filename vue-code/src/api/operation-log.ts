import { request } from '@/utils/request';

// 操作记录
export interface OperationLog {
  id: number;
  xianyuAccountId: number;
  operationType: string;
  operationModule: string;
  operationDesc: string;
  operationStatus: number;
  targetType?: string;
  targetId?: string;
  requestParams?: string;
  responseResult?: string;
  errorMessage?: string;
  ipAddress?: string;
  userAgent?: string;
  durationMs?: number;
  createTime: string | number;
}

// 查询操作记录请求
export interface QueryLogsRequest {
  accountId: number;
  operationType?: string;
  operationModule?: string;
  operationStatus?: number;
  page?: number;
  pageSize?: number;
}

// 查询操作记录响应
export interface QueryLogsResponse {
  logs: OperationLog[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

// 查询操作记录
export function queryOperationLogs(data: QueryLogsRequest) {
  return request<QueryLogsResponse>({
    url: '/operation-log/query',
    method: 'POST',
    data
  });
}

// 删除旧日志
export function deleteOldLogs(days: number) {
  return request<number>({
    url: '/operation-log/deleteOld',
    method: 'POST',
    data: { days }
  });
}
