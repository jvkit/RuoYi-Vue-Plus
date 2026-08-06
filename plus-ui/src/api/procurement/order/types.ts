export interface OrderItemVO {
  id: number | string;
  orderId: number | string;
  requestItemId: number | string;
  itemName: string;
  spec: string;
  brand: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  amount: number;
  sortNo: number;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface OrderVO extends BaseEntity {
  id: number | string;
  orderNo: string;
  title: string;
  requestId: number | string;
  requestCode: string;
  projectId: number | string;
  projectName: string;
  supplierId: number | string;
  supplierName: string;
  amount: number;
  status: string;
  orderDate: string;
  deliveryDate: string;
  remark: string;
  items: OrderItemVO[];
}

export interface OrderItemForm {
  id: number | string | undefined;
  orderId: number | string | undefined;
  requestItemId: number | string | undefined;
  itemName: string;
  spec: string;
  brand: string;
  unit: string;
  quantity: number | undefined;
  unitPrice: number | undefined;
  amount: number | undefined;
  sortNo: number | undefined;
  remark: string;
}

export interface OrderForm {
  id: number | string | undefined;
  orderNo: string;
  title: string;
  requestId: number | string | undefined;
  projectId: number | string | undefined;
  supplierId: number | string | undefined;
  amount: number | undefined;
  status: string;
  orderDate: string;
  deliveryDate: string;
  remark: string;
  items: OrderItemForm[];
}

export interface OrderQuery extends PageQuery {
  orderNo: string;
  title: string;
  requestId: number | string | undefined;
  projectId: number | string | undefined;
  supplierId: number | string | undefined;
  status: string;
}
