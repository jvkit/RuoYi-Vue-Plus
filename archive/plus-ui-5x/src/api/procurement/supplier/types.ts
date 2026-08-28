export interface SupplierVO extends BaseEntity {
  id: number | string;
  supplierCode: string;
  supplierName: string;
  contactName: string;
  contactPhone: string;
  address: string;
  bankName: string;
  bankAccount: string;
  status: number;
  remark: string;
}

export interface SupplierForm {
  id: number | string | undefined;
  supplierCode: string;
  supplierName: string;
  contactName: string;
  contactPhone: string;
  address: string;
  bankName: string;
  bankAccount: string;
  status: number;
  remark: string;
}

export interface SupplierQuery extends PageQuery {
  supplierCode: string;
  supplierName: string;
  contactName: string;
  status: number | undefined;
}
