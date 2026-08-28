import request from '@/utils/request';
import { BomItemForm, BomItemQuery, BomItemVO } from './types';
import { AxiosPromise } from 'axios';

// 查询采购BOM列表
export function listBomItem(query: BomItemQuery): AxiosPromise<BomItemVO[]> {
  return request({
    url: '/procurement/bom/list',
    method: 'get',
    params: query
  });
}

// 查询采购BOM详细
export function getBomItem(id: string | number): AxiosPromise<BomItemVO> {
  return request({
    url: '/procurement/bom/' + id,
    method: 'get'
  });
}

// 新增采购BOM
export function addBomItem(data: BomItemForm) {
  return request({
    url: '/procurement/bom',
    method: 'post',
    data: data
  });
}

// 修改采购BOM
export function updateBomItem(data: BomItemForm) {
  return request({
    url: '/procurement/bom',
    method: 'put',
    data: data
  });
}

// 删除采购BOM
export function delBomItem(id: string | number | Array<string | number>) {
  return request({
    url: '/procurement/bom/' + id,
    method: 'delete'
  });
}

// 导出采购BOM
export function exportBomItem(query: BomItemQuery) {
  return request({
    url: '/procurement/bom/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  });
}
