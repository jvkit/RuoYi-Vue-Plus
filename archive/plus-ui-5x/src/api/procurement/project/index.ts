import request from '@/utils/request';
import { ProjectForm, ProjectQuery, ProjectVO } from './types';
import { AxiosPromise } from 'axios';

// 查询采购项目列表
export function listProject(query: ProjectQuery): AxiosPromise<ProjectVO[]> {
  return request({
    url: '/procurement/project/list',
    method: 'get',
    params: query
  });
}

// 查询采购项目详细
export function getProject(id: string | number): AxiosPromise<ProjectVO> {
  return request({
    url: '/procurement/project/' + id,
    method: 'get'
  });
}

// 新增采购项目
export function addProject(data: ProjectForm) {
  return request({
    url: '/procurement/project',
    method: 'post',
    data: data
  });
}

// 修改采购项目
export function updateProject(data: ProjectForm) {
  return request({
    url: '/procurement/project',
    method: 'put',
    data: data
  });
}

// 删除采购项目
export function delProject(id: string | number | Array<string | number>) {
  return request({
    url: '/procurement/project/' + id,
    method: 'delete'
  });
}

// 导出采购项目
export function exportProject(query: ProjectQuery) {
  return request({
    url: '/procurement/project/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  });
}
