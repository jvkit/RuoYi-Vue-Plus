import request from '@/utils/request';
import { OrderForm, OrderQuery, OrderVO } from './types';
import { AxiosPromise } from 'axios';

// 查询采购订单列表
export function listOrder(query: OrderQuery): AxiosPromise<OrderVO[]> {
  return request({
    url: '/procurement/order/list',
    method: 'get',
    params: query
  });
}

// 查询采购订单详细
export function getOrder(id: string | number): AxiosPromise<OrderVO> {
  return request({
    url: '/procurement/order/' + id,
    method: 'get'
  });
}

// 新增采购订单
export function addOrder(data: OrderForm) {
  return request({
    url: '/procurement/order',
    method: 'post',
    data: data
  });
}

// 修改采购订单
export function updateOrder(data: OrderForm) {
  return request({
    url: '/procurement/order',
    method: 'put',
    data: data
  });
}

// 删除采购订单
export function delOrder(id: string | number | Array<string | number>) {
  return request({
    url: '/procurement/order/' + id,
    method: 'delete'
  });
}

// 导出采购订单
export function exportOrder(query: OrderQuery) {
  return request({
    url: '/procurement/order/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  });
}

// 查询已审批通过的采购申请
export function approvedRequestList(): AxiosPromise<any[]> {
  return request({
    url: '/procurement/request/approvedList',
    method: 'get'
  });
}
