import { request } from '@/utils/request';

export interface DataPanelStats {
  orderCount: number;
  deliverySuccessCount: number;
  deliveryFailCount: number;
  aiReplyCount: number;
  hasData: boolean;
}

export interface DataPanelTrend {
  dates: string[];
  deliverySuccess: number[];
  deliveryFail: number[];
  aiReplies: number[];
}

export function getDataPanelStats(date?: string) {
  return request<DataPanelStats>({
    url: '/data-panel/stats',
    method: 'POST',
    data: { date: date || undefined }
  });
}

export function getDataPanelTrend() {
  return request<DataPanelTrend>({
    url: '/data-panel/trend',
    method: 'POST',
    data: {}
  });
}
