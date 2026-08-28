export interface BomItemVO extends BaseEntity {
  id: number | string;
  projectId: number | string;
  projectName: string;
  category: string;
  name: string;
  spec: string;
  brand: string;
  qty: number;
  unit: string;
  estPrice: number;
  estTotal: number;
  supplierId: number | string;
  supplierName: string;
  status: number;
  remark: string;
}

export interface BomItemForm {
  id: number | string | undefined;
  projectId: number | string | undefined;
  category: string;
  name: string;
  spec: string;
  brand: string;
  qty: number | undefined;
  unit: string;
  estPrice: number | undefined;
  estTotal: number | undefined;
  supplierId: number | string | undefined;
  status: number;
  remark: string;
}

export interface BomItemQuery extends PageQuery {
  projectId: number | string | undefined;
  category: string;
  name: string;
  brand: string;
  supplierId: number | string | undefined;
  status: number | undefined;
}
