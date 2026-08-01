import request from '@/utils/request';

// 查询使用记录列表
export function listInvoiceUsage(query: any) {
  return request({
    url: '/invoice/usage/list',
    method: 'get',
    params: query
  });
}

// 查询使用记录详细
export function getInvoiceUsage(id: string | number) {
  return request({
    url: '/invoice/usage/' + id,
    method: 'get'
  });
}

// 新增使用记录
export function addInvoiceUsage(data: any) {
  return request({
    url: '/invoice/usage',
    method: 'post',
    data
  });
}

// 删除使用记录
export function delInvoiceUsage(id: string | number | Array<string | number>) {
  return request({
    url: '/invoice/usage/' + id,
    method: 'delete'
  });
}
