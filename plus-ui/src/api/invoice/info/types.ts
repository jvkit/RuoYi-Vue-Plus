export interface InvoiceInfoVO extends BaseEntity {
  id: number | string;
  invoiceCode: string;
  invoiceNumber: string;
  invoiceType: string;
  amount: number;
  taxAmount: number;
  totalAmount: number;
  invoiceDate: string;
  sellerName: string;
  buyerName: string;
  status: string;
  verifyStatus: string;
  verifyTime: string;
  finQueryNo: string;
  orderNo: string;
  remark: string;
}

export interface InvoiceInfoForm {
  id: number | string | undefined;
  invoiceCode: string;
  invoiceNumber: string;
  invoiceType: string;
  amount: number | undefined;
  taxAmount: number | undefined;
  totalAmount: number | undefined;
  invoiceDate: string;
  sellerName: string;
  buyerName: string;
  status: string;
  verifyStatus: string;
  verifyTime: string;
  finQueryNo: string;
  orderNo: string;
  remark: string;
}

export interface InvoiceInfoQuery extends PageQuery {
  invoiceNumber: string;
  sellerName: string;
  buyerName: string;
  invoiceType: string;
  status: string;
}
