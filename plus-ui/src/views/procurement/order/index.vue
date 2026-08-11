<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="订单编号" prop="orderNo">
              <el-input v-model="queryParams.orderNo" placeholder="请输入订单编号" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="标题" prop="title">
              <el-input v-model="queryParams.title" placeholder="请输入标题" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="项目" prop="projectId">
              <el-select v-model="queryParams.projectId" placeholder="请选择项目" clearable style="width: 180px">
                <el-option v-for="item in projectOptions" :key="item.id" :label="item.projectName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="queryParams.supplierId" placeholder="请选择供应商" clearable style="width: 180px">
                <el-option v-for="item in supplierOptions" :key="item.id" :label="item.supplierName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 140px">
                <el-option label="草稿" value="draft" />
                <el-option label="已下单" value="ordered" />
                <el-option label="部分收货" value="partial_received" />
                <el-option label="已收货" value="received" />
                <el-option label="已取消" value="cancelled" />
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
            <el-button v-hasPermi="['procurement:order:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['procurement:order:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['procurement:order:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['procurement:order:export']" type="warning" plain icon="Download" @click="handleExport">导出</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="orderList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-if="false" label="主键" align="center" prop="id" />
        <el-table-column label="订单编号" align="center" prop="orderNo" width="170" :show-overflow-tooltip="true" />
        <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
        <el-table-column label="申请编号" align="center" prop="requestCode" width="160" :show-overflow-tooltip="true" />
        <el-table-column label="项目" align="center" prop="projectName" :show-overflow-tooltip="true" />
        <el-table-column label="供应商" align="center" prop="supplierName" :show-overflow-tooltip="true" />
        <el-table-column label="总金额" align="center" prop="amount" width="120">
          <template #default="scope">
            <span>{{ (Number(scope.row.amount) || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="110">
          <template #default="scope">
            <dict-tag :options="statusOptions" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="下单日期" align="center" prop="orderDate" width="120">
          <template #default="scope">
            <span>{{ scope.row.orderDate ? scope.row.orderDate.substring(0, 10) : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['procurement:order:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['procurement:order:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <!-- 添加或修改采购订单对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="900px" append-to-body>
      <el-form ref="orderFormRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="订单标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入订单标题" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联申请" prop="requestId">
              <el-select v-model="form.requestId" placeholder="请选择已审批申请" clearable style="width: 100%" @change="onRequestChange">
                <el-option v-for="item in requestOptions" :key="item.id" :label="item.requestCode" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="项目" prop="projectId">
              <el-select v-model="form.projectId" placeholder="请选择项目" clearable style="width: 100%">
                <el-option v-for="item in projectOptions" :key="item.id" :label="item.projectName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商" clearable style="width: 100%">
                <el-option v-for="item in supplierOptions" :key="item.id" :label="item.supplierName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="下单日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计到货" prop="deliveryDate">
              <el-date-picker v-model="form.deliveryDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="订单状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="草稿" value="draft" />
                <el-option label="已下单" value="ordered" />
                <el-option label="部分收货" value="partial_received" />
                <el-option label="已收货" value="received" />
                <el-option label="已取消" value="cancelled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" maxlength="500" />
        </el-form-item>
      </el-form>

      <el-card shadow="never" class="mt-2">
        <template #header>
          <div class="card-header">
            <span>订单明细</span>
            <el-button v-hasPermi="['procurement:order:add']" type="primary" link icon="Plus" @click="addItemRow">添加明细</el-button>
          </div>
        </template>
        <el-table :data="form.items" border size="small">
          <el-table-column label="品名" align="center" min-width="140">
            <template #default="{ $index }">
              <el-input v-model="form.items[$index].itemName" placeholder="品名" />
            </template>
          </el-table-column>
          <el-table-column label="规格" align="center" min-width="120">
            <template #default="{ $index }">
              <el-input v-model="form.items[$index].spec" placeholder="规格" />
            </template>
          </el-table-column>
          <el-table-column label="品牌" align="center" min-width="100">
            <template #default="{ $index }">
              <el-input v-model="form.items[$index].brand" placeholder="品牌" />
            </template>
          </el-table-column>
          <el-table-column label="单位" align="center" width="80">
            <template #default="{ $index }">
              <el-input v-model="form.items[$index].unit" placeholder="单位" />
            </template>
          </el-table-column>
          <el-table-column label="数量" align="center" width="110">
            <template #default="{ $index }">
              <el-input-number v-model="form.items[$index].quantity" :min="0" :precision="4" :controls="false" style="width: 100%" @change="calcItem($index)" />
            </template>
          </el-table-column>
          <el-table-column label="单价" align="center" width="110">
            <template #default="{ $index }">
              <el-input-number v-model="form.items[$index].unitPrice" :min="0" :precision="4" :controls="false" style="width: 100%" @change="calcItem($index)" />
            </template>
          </el-table-column>
          <el-table-column label="金额" align="center" width="110">
            <template #default="{ $index }">
              <span>{{ (form.items[$index].amount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" align="center" min-width="100">
            <template #default="{ $index }">
              <el-input v-model="form.items[$index].remark" placeholder="备注" />
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="80">
            <template #default="{ $index }">
              <el-button link type="danger" icon="Delete" @click="removeItemRow($index)"></el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="mt-2 text-right font-bold">合计：{{ totalAmount.toFixed(2) }} 元</div>
      </el-card>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ProcurementOrder" lang="ts">
import { listOrder, getOrder, delOrder, addOrder, updateOrder } from '@/api/procurement/order';
import { approvedRequestList } from '@/api/procurement/order/index';
import { OrderForm, OrderQuery, OrderVO, OrderItemForm } from '@/api/procurement/order/types';
import { listProject } from '@/api/procurement/project';
import { ProjectVO } from '@/api/procurement/project/types';
import { listSupplier } from '@/api/procurement/supplier';
import { SupplierVO } from '@/api/procurement/supplier/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const orderList = ref<OrderVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const projectOptions = ref<ProjectVO[]>([]);
const supplierOptions = ref<SupplierVO[]>([]);
const requestOptions = ref<any[]>([]);

const statusOptions = ref([
  { label: '草稿', value: 'draft', elTagType: 'info' },
  { label: '已下单', value: 'ordered', elTagType: 'primary' },
  { label: '部分收货', value: 'partial_received', elTagType: 'warning' },
  { label: '已收货', value: 'received', elTagType: 'success' },
  { label: '已取消', value: 'cancelled', elTagType: 'danger' }
]);

const queryFormRef = ref<ElFormInstance>();
const orderFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const emptyItem = (): OrderItemForm => ({
  id: undefined,
  orderId: undefined,
  requestItemId: undefined,
  itemName: '',
  spec: '',
  brand: '',
  unit: '',
  quantity: undefined,
  unitPrice: undefined,
  amount: 0,
  sortNo: undefined,
  remark: ''
});

const initFormData: OrderForm = {
  id: undefined,
  orderNo: '',
  title: '',
  requestId: undefined,
  projectId: undefined,
  supplierId: undefined,
  amount: 0,
  status: 'draft',
  orderDate: '',
  deliveryDate: '',
  remark: '',
  items: []
};

const data = reactive<PageData<OrderForm, OrderQuery>>({
  form: JSON.parse(JSON.stringify(initFormData)),
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: '',
    title: '',
    requestId: undefined,
    projectId: undefined,
    supplierId: undefined,
    status: ''
  },
  rules: {
    title: [{ required: true, message: '订单标题不能为空', trigger: 'blur' }],
    projectId: [{ required: true, message: '项目不能为空', trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

const totalAmount = computed(() => {
  if (!form.value.items) return 0;
  return form.value.items.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);
});

const getList = async () => {
  loading.value = true;
  const res = await listOrder(queryParams.value);
  orderList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const loadOptions = async () => {
  const [projectRes, supplierRes, requestRes] = await Promise.all([
    listProject({ pageNum: 1, pageSize: 1000 } as any),
    listSupplier({ pageNum: 1, pageSize: 1000 } as any),
    approvedRequestList()
  ]);
  projectOptions.value = projectRes.rows || [];
  supplierOptions.value = supplierRes.rows || [];
  requestOptions.value = requestRes.data || [];
};

const onRequestChange = (val: number | string | undefined) => {
  if (!val) return;
  const req = requestOptions.value.find((item) => item.id === val);
  if (req) {
    form.value.projectId = req.projectId;
    form.value.supplierId = req.supplierId;
  }
};

const cancel = () => {
  reset();
  dialog.visible = false;
};

const reset = () => {
  form.value = JSON.parse(JSON.stringify(initFormData));
  orderFormRef.value?.resetFields();
};

const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

const resetQuery = () => {
  queryFormRef.value?.resetFields();
  handleQuery();
};

const handleSelectionChange = (selection: OrderVO[]) => {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
};

const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加采购订单';
};

const handleUpdate = async (row?: OrderVO) => {
  reset();
  const id = row?.id || ids.value[0];
  const res = await getOrder(id);
  form.value = { ...JSON.parse(JSON.stringify(initFormData)), ...res.data, items: res.data.items || [] };
  dialog.visible = true;
  dialog.title = '修改采购订单';
};

const addItemRow = () => {
  if (!form.value.items) {
    form.value.items = [];
  }
  form.value.items.push(emptyItem());
};

const removeItemRow = (index: number) => {
  form.value.items.splice(index, 1);
};

const calcItem = (index: number) => {
  const item = form.value.items[index];
  const qty = Number(item.quantity) || 0;
  const price = Number(item.unitPrice) || 0;
  item.amount = Number((qty * price).toFixed(2));
};

const submitForm = () => {
  orderFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    if (!form.value.items || form.value.items.length === 0) {
      proxy?.$modal.msgError('请至少添加一条订单明细');
      return;
    }
    form.value.id ? await updateOrder(form.value) : await addOrder(form.value);
    proxy?.$modal.msgSuccess('操作成功');
    dialog.visible = false;
    await getList();
  });
};

const handleDelete = async (row?: OrderVO) => {
  const idList = row?.id || ids.value;
  await proxy?.$modal.confirm('是否确认删除订单编号为"' + idList + '"的数据项？');
  await delOrder(idList);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

const handleExport = () => {
  proxy?.download('procurement/order/export', queryParams.value, `order_${new Date().getTime()}.xlsx`);
};

onMounted(() => {
  getList();
  loadOptions();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.text-right {
  text-align: right;
}
.font-bold {
  font-weight: bold;
}
</style>
