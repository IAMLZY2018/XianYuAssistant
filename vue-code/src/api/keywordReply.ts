import { request } from '@/utils/request';

export interface KeywordReplyContent {
  id: string | number;
  ruleId: string | number;
  replyText: string;
  replyImageUrl: string;
}

export interface KeywordReplyRule {
  id: string | number;
  xianyuAccountId: string | number;
  xyGoodsId: string;
  keyword: string;
  matchMode: number;
  isFallback: number;
  contents: KeywordReplyContent[];
}

export function getKeywordReplyRules(data: { xianyuAccountId: number; xyGoodsId: string }) {
  return request<KeywordReplyRule[]>({ url: '/keyword-reply/rules', method: 'POST', data });
}

export function addKeywordRule(data: { xianyuAccountId: number; xyGoodsId: string; keyword: string }) {
  return request<KeywordReplyRule>({ url: '/keyword-reply/addRule', method: 'POST', data });
}

export function deleteKeywordRule(data: { ruleId: string | number }) {
  return request({ url: '/keyword-reply/deleteRule', method: 'POST', data });
}

export function updateKeyword(data: { ruleId: string | number; keyword: string }) {
  return request({ url: '/keyword-reply/updateKeyword', method: 'POST', data });
}

export function updateKeywordRuleMatchMode(data: { ruleId: string | number; matchMode: number }) {
  return request({ url: '/keyword-reply/updateMatchMode', method: 'POST', data });
}

export function ensureFallbackRule(data: { xianyuAccountId: number; xyGoodsId: string }) {
  return request<KeywordReplyRule>({ url: '/keyword-reply/ensureFallbackRule', method: 'POST', data });
}

export function addKeywordContent(data: { ruleId: string | number; replyText?: string; replyImageUrl?: string }) {
  return request<KeywordReplyContent>({ url: '/keyword-reply/addContent', method: 'POST', data });
}

export function updateKeywordContent(data: { contentId: string | number; replyText?: string; replyImageUrl?: string }) {
  return request({ url: '/keyword-reply/updateContent', method: 'POST', data });
}

export function deleteKeywordContent(data: { contentId: string | number }) {
  return request({ url: '/keyword-reply/deleteContent', method: 'POST', data });
}
