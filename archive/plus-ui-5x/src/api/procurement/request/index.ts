import request from '@/utils/request';
import { RequestForm, RequestQuery, RequestVO } from './types';
import { AxiosPromise } from 'axios';

// 查询采购申请列表
export function listRequest(query: RequestQuery): AxiosPromise<RequestVO[]> {
  return request({
    url: '/procurement/request/list',
    method: 'get',
    params: query
  });
}

// 查询采购申请详细
export function getRequest(id: string | number): AxiosPromise<RequestVO> {
  return request({
    url: '/procurement/request/' + id,
    method: 'get'
  });
}

// 新增采购申请
export function addRequest(data: RequestForm) {
  return request({
    url: '/procurement/request',
    method: 'post',
    data: data
  });
}

// 修改采购申请
export function updateRequest(data: RequestForm) {
  return request({
    url: '/procurement/request',
    method: 'put',
    data: data
  });
}

// 删除采购申请
export function delRequest(id: string | number | Array<string | number>) {
  return request({
    url: '/procurement/request/' + id,
    method: 'delete'
  });
}

// 导出采购申请
export function exportRequest(query: RequestQuery) {
  return request({
    url: '/procurement/request/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  });
}

// 提交采购申请并启动流程
export function submitRequest(data: RequestForm) {
  return request({
    url: '/procurement/request/submit',
    method: 'post',
    data: data
  });
}
