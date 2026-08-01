<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="发票号码" prop="invoiceNumber">
              <el-input v-model="queryParams.invoiceNumber" placeholder="请输入发票号码" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="销售方" prop="sellerName">
              <el-input v-model="queryParams.sellerName" placeholder="请输入销售方名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="购买方" prop="buyerName">
              <el-input v-model="queryParams.buyerName" placeholder="请输入购买方名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="发票类型" prop="invoiceType">
              <el-select v-model="queryParams.invoiceType" placeholder="发票类型" clearable>
                <el-option v-for="dict in invoice_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="状态" clearable>
                <el-option v-for="dict in invoice_status" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
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
            <el-button v-hasPermi="['invoice:info:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['invoice:info:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()">
              修改
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['invoice:info:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">
              删除
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['invoice:info:export']" type="warning" plain icon="Download" @click="handleExport">导出</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="invoiceList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-if="false" label="主键" align="center" prop="id" />
        <el-table-column label="发票代码" align="center" prop="invoiceCode" :show-overflow-tooltip="true" />
        <el-table-column label="发票号码" align="center" prop="invoiceNumber" :show-overflow-tooltip="true" />
        <el-table-column label="发票类型" align="center" prop="invoiceType">
          <template #default="scope">
            <dict-tag :options="invoice_type" :value="scope.row.invoiceType" />
          </template>
        </el-table-column>
        <el-table-column label="不含税金额" align="center" prop="amount" />
        <el-table-column label="税额" align="center" prop="taxAmount" />
        <el-table-column label="价税合计" align="center" prop="totalAmount" />
        <el-table-column label="开票日期" align="center" prop="invoiceDate" width="120" />
        <el-table-column label="销售方" align="center" prop="sellerName" :show-overflow-tooltip="true" />
        <el-table-column label="购买方" align="center" prop="buyerName" :show-overflow-tooltip="true" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="invoice_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="AI审核意见" align="center" prop="aiOpinion" :show-overflow-tooltip="true" width="200" />
        <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['invoice:info:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['invoice:info:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <!-- 添加或修改发票信息对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="600px" append-to-body>
      <el-form ref="invoiceFormRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="发票代码" prop="invoiceCode">
              <el-input v-model="form.invoiceCode" placeholder="请输入发票代码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发票号码" prop="invoiceNumber">
              <el-input v-model="form.invoiceNumber" placeholder="请输入发票号码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="发票类型" prop="invoiceType">
              <el-select v-model="form.invoiceType" placeholder="请选择发票类型" style="width: 100%">
                <el-option v-for="dict in invoice_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开票日期" prop="invoiceDate">
              <el-date-picker v-model="form.invoiceDate" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="不含税金额" prop="amount">
              <el-input-number v-model="form.amount" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税额" prop="taxAmount">
              <el-input-number v-model="form.taxAmount" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="销售方" prop="sellerName">
              <el-input v-model="form.sellerName" placeholder="请输入销售方名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购买方" prop="buyerName">
              <el-input v-model="form.buyerName" placeholder="请输入购买方名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in invoice_status" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
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

<script setup name="InvoiceInfo" lang="ts">
import { listInvoiceInfo, getInvoiceInfo, delInvoiceInfo, addInvoiceInfo, updateInvoiceInfo } from '@/api/invoice/info';
import { InvoiceInfoForm, InvoiceInfoQuery, InvoiceInfoVO } from '@/api/invoice/info/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { invoice_type, invoice_status } = toRefs<any>(proxy?.useDict('invoice_type', 'invoice_status'));

const invoiceList = ref<InvoiceInfoVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const queryFormRef = ref<ElFormInstance>();
const invoiceFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: InvoiceInfoForm = {
  id: undefined,
  invoiceCode: '',
  invoiceNumber: '',
  invoiceType: 'normal',
  amount: undefined,
  taxAmount: undefined,
  totalAmount: undefined,
  invoiceDate: '',
  sellerName: '',
  buyerName: '',
  status: 'draft',
  remark: ''
};

const data = reactive<PageData<InvoiceInfoForm, InvoiceInfoQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    invoiceNumber: '',
    sellerName: '',
    buyerName: '',
    invoiceType: '',
    status: ''
  },
  rules: {
    invoiceNumber: [{ required: true, message: '发票号码不能为空', trigger: 'blur' }],
    invoiceType: [{ required: true, message: '发票类型不能为空', trigger: 'change' }],
    invoiceDate: [{ required: true, message: '开票日期不能为空', trigger: 'change' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询发票信息列表 */
const getList = async () => {
  loading.value = true;
  const res = await listInvoiceInfo(queryParams.value);
  invoiceList.value = res.rows;
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
  invoiceFormRef.value?.resetFields();
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
const handleSelectionChange = (selection: InvoiceInfoVO[]) => {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加发票信息';
};

/** 修改按钮操作 */
const handleUpdate = async (row?: InvoiceInfoVO) => {
  reset();
  const id = row?.id || ids.value[0];
  const res = await getInvoiceInfo(id);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改发票信息';
};

/** 提交按钮 */
const submitForm = () => {
  invoiceFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      form.value.id ? await updateInvoiceInfo(form.value) : await addInvoiceInfo(form.value);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

/** 删除按钮操作 */
const handleDelete = async (row?: InvoiceInfoVO) => {
  const idList = row?.id || ids.value;
  await proxy?.$modal.confirm('是否确认删除发票信息编号为"' + idList + '"的数据项？');
  await delInvoiceInfo(idList);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download(
    'invoice/info/export',
    {
      ...queryParams.value
    },
    `invoice_info_${new Date().getTime()}.xlsx`
  );
};

onMounted(() => {
  getList();
});
</script>
