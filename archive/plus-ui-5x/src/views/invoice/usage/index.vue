<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="发票ID" prop="invoiceId">
              <el-input v-model="queryParams.invoiceId" placeholder="请输入发票ID" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="业务类型" prop="bizType">
              <el-select v-model="queryParams.bizType" placeholder="业务类型" clearable>
                <el-option v-for="dict in biz_type_options" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="业务单号" prop="bizNo">
              <el-input v-model="queryParams.bizNo" placeholder="请输入业务单号" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </transition>
    <el-card shadow="hover">
      <template #header>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">删除</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="usageList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="主键" align="center" prop="id" v-if="false" />
        <el-table-column label="发票ID" align="center" prop="invoiceId" />
        <el-table-column label="业务类型" align="center" prop="bizType">
          <template #default="scope">
            <el-tag :type="bizTypeTag(scope.row.bizType)">{{ bizTypeText(scope.row.bizType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="业务单号" align="center" prop="bizNo" :show-overflow-tooltip="true" />
        <el-table-column label="使用人" align="center" prop="usedBy" />
        <el-table-column label="使用时间" align="center" prop="usedTime" width="180" />
        <el-table-column label="使用金额" align="center" prop="usedAmount" />
        <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="删除" placement="top">
              <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <!-- 新增使用记录对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="500px" append-to-body>
      <el-form ref="usageFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="发票ID" prop="invoiceId">
          <el-input v-model="form.invoiceId" placeholder="请输入发票ID" />
        </el-form-item>
        <el-form-item label="业务类型" prop="bizType">
          <el-select v-model="form.bizType" placeholder="请选择业务类型" style="width: 100%">
            <el-option v-for="dict in biz_type_options" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务单号" prop="bizNo">
          <el-input v-model="form.bizNo" placeholder="请输入业务单号" />
        </el-form-item>
        <el-form-item label="使用金额" prop="usedAmount">
          <el-input-number v-model="form.usedAmount" :precision="2" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="InvoiceUsage" lang="ts">
import { listInvoiceUsage, addInvoiceUsage, delInvoiceUsage } from '@/api/invoice/usage';
import { InvoiceUsageForm, InvoiceUsageQuery, InvoiceUsageVO } from '@/api/invoice/usage/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const biz_type_options = [
  { label: '报销', value: 'reimbursement' },
  { label: '付款', value: 'payment' },
  { label: '采购', value: 'purchase' }
];

const usageList = ref<InvoiceUsageVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const queryFormRef = ref<ElFormInstance>();
const usageFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: InvoiceUsageForm = {
  id: undefined,
  invoiceId: undefined,
  bizType: '',
  bizNo: '',
  usedAmount: undefined,
  remark: ''
};

const data = reactive({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    invoiceId: '',
    bizType: '',
    bizNo: ''
  } as InvoiceUsageQuery,
  rules: {
    invoiceId: [{ required: true, message: '发票ID不能为空', trigger: 'blur' }],
    bizType: [{ required: true, message: '业务类型不能为空', trigger: 'change' }],
    bizNo: [{ required: true, message: '业务单号不能为空', trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 业务类型标签类型 */
const bizTypeTag = (type: string) => {
  const map: Record<string, string> = {
    reimbursement: '',
    payment: 'warning',
    purchase: 'success'
  };
  return map[type] || 'info';
};

/** 业务类型文本 */
const bizTypeText = (type: string) => {
  const map: Record<string, string> = {
    reimbursement: '报销',
    payment: '付款',
    purchase: '采购'
  };
  return map[type] || type;
};

/** 查询使用记录列表 */
const getList = async () => {
  loading.value = true;
  const res = await listInvoiceUsage(queryParams.value);
  usageList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

/** 取消按钮 */
const cancel = () => {
  reset();
  dialog.visible = false;
};

/** 表单重置 */
const reset = () => {
  form.value = { ...initFormData };
  usageFormRef.value?.resetFields();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

/** 多选框选中数据 */
const handleSelectionChange = (selection: InvoiceUsageVO[]) => {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加使用记录';
};

/** 提交按钮 */
const submitForm = () => {
  usageFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      await addInvoiceUsage(form.value);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

/** 删除按钮操作 */
const handleDelete = async (row?: InvoiceUsageVO) => {
  const idList = row?.id || ids.value;
  await proxy?.$modal.confirm('是否确认删除使用记录编号为"' + idList + '"的数据项？');
  await delInvoiceUsage(idList);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  getList();
});
</script>
