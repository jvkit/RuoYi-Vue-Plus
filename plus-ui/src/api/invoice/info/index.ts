import request from '@/utils/request';
import { InvoiceInfoForm, InvoiceInfoQuery, InvoiceInfoVO } from './types';
import { AxiosPromise } from 'axios';

// 查询发票信息列表
export function listInvoiceInfo(query: InvoiceInfoQuery): AxiosPromise<InvoiceInfoVO[]> {
  return request({
    url: '/invoice/info/list',
    method: 'get',
    params: query
  });
}

// 查询发票信息详细
export function getInvoiceInfo(id: string | number): AxiosPromise<InvoiceInfoVO> {
  return request({
    url: '/invoice/info/' + id,
    method: 'get'
  });
}

// 新增发票信息
export function addInvoiceInfo(data: InvoiceInfoForm) {
  return request({
    url: '/invoice/info',
    method: 'post',
    data: data
  });
}

// 修改发票信息
export function updateInvoiceInfo(data: InvoiceInfoForm) {
  return request({
    url: '/invoice/info',
    method: 'put',
    data: data
  });
}

// 删除发票信息
export function delInvoiceInfo(id: string | number | Array<string | number>) {
  return request({
    url: '/invoice/info/' + id,
    method: 'delete'
  });
}

// 导出发票信息
export function exportInvoiceInfo(query: InvoiceInfoQuery) {
  return request({
    url: '/invoice/info/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  });
}

// AI 审核发票（提交或重新提交）
export function aiReviewInvoice(id: string | number, imageFile?: File) {
  const formData = new FormData();
  if (imageFile) {
    formData.append('imageFile', imageFile);
  }
  return request({
    url: '/invoice/info/ai-review/' + id,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

// 获取发票 AI 审核结果
export function getAiReviewResult(id: string | number) {
  return request({
    url: '/invoice/info/ai-review/' + id,
    method: 'get'
  });
}

// 上传发票图片，AI 识别字段并返回审核意见
export function extractInvoice(imageFile: File) {
  const formData = new FormData();
  formData.append('imageFile', imageFile);
  return request({
    url: '/invoice/info/extract',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

// 查重：检查发票代码+号码是否已存在
export function checkDuplicate(invoiceCode: string, invoiceNumber: string) {
  return request({
    url: '/invoice/info/check-duplicate',
    method: 'get',
    params: { invoiceCode, invoiceNumber }
  });
}

// 查验发票真伪（mock）
export function verifyInvoice(id: string | number) {
  return request({
    url: '/invoice/info/verify/' + id,
    method: 'post'
  });
}

// 财务查询单号反查
export function queryByFinNo(finQueryNo: string) {
  return request({
    url: '/invoice/info/query/' + finQueryNo,
    method: 'get'
  });
}
