import request from '@/utils/request';
import { SupplierForm, SupplierQuery, SupplierVO } from './types';
import { AxiosPromise } from 'axios';

// 查询采购供应商列表
export function listSupplier(query: SupplierQuery): AxiosPromise<SupplierVO[]> {
  return request({
    url: '/procurement/supplier/list',
    method: 'get',
    params: query
  });
}

// 查询采购供应商详细
export function getSupplier(id: string | number): AxiosPromise<SupplierVO> {
  return request({
    url: '/procurement/supplier/' + id,
    method: 'get'
  });
}

// 新增采购供应商
export function addSupplier(data: SupplierForm) {
  return request({
    url: '/procurement/supplier',
    method: 'post',
    data: data
  });
}

// 修改采购供应商
export function updateSupplier(data: SupplierForm) {
  return request({
    url: '/procurement/supplier',
    method: 'put',
    data: data
  });
}

// 删除采购供应商
export function delSupplier(id: string | number | Array<string | number>) {
  return request({
    url: '/procurement/supplier/' + id,
    method: 'delete'
  });
}

// 导出采购供应商
export function exportSupplier(query: SupplierQuery) {
  return request({
    url: '/procurement/supplier/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  });
}
