export interface RequestItemVO {
  id: number | string;
  requestId: number | string;
  itemName: string;
  spec: string;
  brand: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  bomItemId: number | string;
  sortNo: number;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface RequestVO extends BaseEntity {
  id: number | string;
  requestCode: string;
  title: string;
  projectId: number | string;
  projectName: string;
  supplierId: number | string;
  supplierName: string;
  amount: number;
  purchaseType: string;
  applyReason: string;
  status: string;
  processInstanceId: number | string;
  remark: string;
  items: RequestItemVO[];
}

export interface RequestItemForm {
  id: number | string | undefined;
  requestId: number | string | undefined;
  itemName: string;
  spec: string;
  brand: string;
  unit: string;
  quantity: number | undefined;
  unitPrice: number | undefined;
  amount: number | undefined;
  bomItemId: number | string | undefined;
  sortNo: number | undefined;
  remark: string;
}

export interface RequestForm {
  id: number | string | undefined;
  requestCode: string;
  title: string;
  projectId: number | string | undefined;
  supplierId: number | string | undefined;
  amount: number | undefined;
  purchaseType: string;
  applyReason: string;
  status: string;
  remark: string;
  items: RequestItemForm[];
}

export interface RequestQuery extends PageQuery {
  requestCode: string;
  title: string;
  projectId: number | string | undefined;
  supplierId: number | string | undefined;
  status: string;
  purchaseType: string;
}
