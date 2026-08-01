export interface InvoiceUsageVO {
  id: string | number;
  invoiceId: string | number;
  bizType: string;
  bizNo: string;
  usedBy: string | number;
  usedTime: string;
  usedAmount: number;
  remark: string;
}

export interface InvoiceUsageForm {
  id?: string | number;
  invoiceId?: string | number;
  bizType?: string;
  bizNo?: string;
  usedAmount?: number;
  remark?: string;
}

export interface InvoiceUsageQuery {
  pageNum?: number;
  pageSize?: number;
  invoiceId?: string | number;
  bizType?: string;
  bizNo?: string;
}
