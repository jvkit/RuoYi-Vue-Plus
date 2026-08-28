-- 采购管理菜单
insert into sys_menu values ('12000', '采购管理', '0', '8', 'procurement', '', '', '1', '0', 'M', '0', '0', '', 'shopping-cart', 103, 1, sysdate(), NULL, NULL, '采购管理目录');
insert into sys_menu values ('12001', '采购申请', '12000', '1', 'procurement', 'workflow/procurement/index', '', '1', '0', 'C', '0', '0', 'workflow:procurement:list', 'edit', 103, 1, sysdate(), NULL, NULL, '采购申请菜单');
insert into sys_menu values ('12002', '采购申请查询', '12001', '1', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:procurement:query', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12003', '采购申请新增', '12001', '2', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:procurement:add', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12004', '采购申请修改', '12001', '3', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:procurement:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12005', '采购申请删除', '12001', '4', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:procurement:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
insert into sys_menu values ('12006', '采购申请导出', '12001', '5', '#', '', '', '1', '0', 'F', '0', '0', 'workflow:procurement:export', '#', 103, 1, sysdate(), NULL, NULL, '');
