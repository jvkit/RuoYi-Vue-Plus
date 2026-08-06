<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true">
            <el-form-item label="项目" prop="projectId">
              <el-select v-model="queryParams.projectId" placeholder="请选择项目" clearable style="width: 180px">
                <el-option v-for="item in projectOptions" :key="item.id" :label="item.projectName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="物料分类" prop="category">
              <el-input v-model="queryParams.category" placeholder="请输入物料分类" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="品名" prop="name">
              <el-input v-model="queryParams.name" placeholder="请输入品名" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="queryParams.brand" placeholder="请输入品牌" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="queryParams.supplierId" placeholder="请选择供应商" clearable style="width: 180px">
                <el-option v-for="item in supplierOptions" :key="item.id" :label="item.supplierName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
                <el-option label="待采购" :value="0" />
                <el-option label="已下单" :value="1" />
                <el-option label="已到货" :value="2" />
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
            <el-button v-hasPermi="['procurement:bom:add']" type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['procurement:bom:edit']" type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['procurement:bom:remove']" type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button v-hasPermi="['procurement:bom:export']" type="warning" plain icon="Download" @click="handleExport">导出</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="bomItemList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column v-if="false" label="主键" align="center" prop="id" />
        <el-table-column label="项目名称" align="center" prop="projectName" :show-overflow-tooltip="true" width="150" />
        <el-table-column label="物料分类" align="center" prop="category" />
        <el-table-column label="品名" align="center" prop="name" :show-overflow-tooltip="true" />
        <el-table-column label="规格型号" align="center" prop="spec" :show-overflow-tooltip="true" />
        <el-table-column label="品牌" align="center" prop="brand" />
        <el-table-column label="数量" align="center" prop="qty" />
        <el-table-column label="单位" align="center" prop="unit" />
        <el-table-column label="预估单价" align="center" prop="estPrice" />
        <el-table-column label="预估总价" align="center" prop="estTotal" />
        <el-table-column label="建议供应商" align="center" prop="supplierName" :show-overflow-tooltip="true" />
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template #default="scope">
            <el-tag :type="statusTag(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ proxy.parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-tooltip content="修改" placement="top">
              <el-button v-hasPermi="['procurement:bom:edit']" link type="primary" icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button v-hasPermi="['procurement:bom:remove']" link type="primary" icon="Delete" @click="handleDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <!-- 添加或修改BOM对话框 -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="700px" append-to-body>
      <el-form ref="bomFormRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="项目" prop="projectId">
              <el-select v-model="form.projectId" placeholder="请选择项目" style="width: 100%">
                <el-option v-for="item in projectOptions" :key="item.id" :label="item.projectName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料分类" prop="category">
              <el-input v-model="form.category" placeholder="请输入物料分类" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="品名" prop="name">
              <el-input v-model="form.name" placeholder="请输入品名" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="form.brand" placeholder="请输入品牌" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="规格型号" prop="spec">
              <el-input v-model="form.spec" placeholder="请输入规格型号" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="请输入单位" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="数量" prop="qty">
              <el-input-number v-model="form.qty" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预估单价" prop="estPrice">
              <el-input-number v-model="form.estPrice" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="建议供应商" prop="supplierId">
              <el-select v-model="form.supplierId" placeholder="请选择供应商" clearable style="width: 100%">
                <el-option v-for="item in supplierOptions" :key="item.id" :label="item.supplierName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">待采购</el-radio>
                <el-radio :value="1">已下单</el-radio>
                <el-radio :value="2">已到货</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" maxlength="500" />
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

<script setup name="ProcurementBom" lang="ts">
import { listBomItem, getBomItem, delBomItem, addBomItem, updateBomItem } from '@/api/procurement/bom';
import { BomItemForm, BomItemQuery, BomItemVO } from '@/api/procurement/bom/types';
import { listProject } from '@/api/procurement/project';
import { ProjectVO } from '@/api/procurement/project/types';
import { listSupplier } from '@/api/procurement/supplier';
import { SupplierVO } from '@/api/procurement/supplier/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const bomItemList = ref<BomItemVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const projectOptions = ref<ProjectVO[]>([]);
const supplierOptions = ref<SupplierVO[]>([]);

const queryFormRef = ref<ElFormInstance>();
const bomFormRef = ref<ElFormInstance>();
const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: BomItemForm = {
  id: undefined,
  projectId: undefined,
  category: '',
  name: '',
  spec: '',
  brand: '',
  qty: undefined,
  unit: '',
  estPrice: undefined,
  estTotal: undefined,
  supplierId: undefined,
  status: 0,
  remark: ''
};

const data = reactive<PageData<BomItemForm, BomItemQuery>>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    category: '',
    name: '',
    brand: '',
    supplierId: undefined,
    status: undefined
  },
  rules: {
    projectId: [{ required: true, message: '项目不能为空', trigger: 'change' }],
    name: [{ required: true, message: '品名不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 状态标签类型 */
const statusTag = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success' };
  return map[status] || 'info';
};

/** 状态文本 */
const statusText = (status: number) => {
  const map: Record<number, string> = { 0: '待采购', 1: '已下单', 2: '已到货' };
  return map[status] || '未知';
};

/** 加载项目下拉 */
const loadProjects = async () => {
  const res = await listProject({ pageNum: 1, pageSize: 1000 } as any);
  projectOptions.value = res.rows || [];
};

/** 加载供应商下拉 */
const loadSuppliers = async () => {
  const res = await listSupplier({ pageNum: 1, pageSize: 1000 } as any);
  supplierOptions.value = res.rows || [];
};

/** 查询采购BOM列表 */
const getList = async () => {
  loading.value = true;
  const res = await listBomItem(queryParams.value);
  bomItemList.value = res.rows;
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
  bomFormRef.value?.resetFields();
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
const handleSelectionChange = (selection: BomItemVO[]) => {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加BOM';
};

/** 修改按钮操作 */
const handleUpdate = async (row?: BomItemVO) => {
  reset();
  const id = row?.id || ids.value[0];
  const res = await getBomItem(id);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改BOM';
};

/** 提交按钮 */
const submitForm = () => {
  bomFormRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      form.value.id ? await updateBomItem(form.value) : await addBomItem(form.value);
      proxy?.$modal.msgSuccess('操作成功');
      dialog.visible = false;
      await getList();
    }
  });
};

/** 删除按钮操作 */
const handleDelete = async (row?: BomItemVO) => {
  const idList = row?.id || ids.value;
  await proxy?.$modal.confirm('是否确认删除BOM编号为"' + idList + '"的数据项？');
  await delBomItem(idList);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download('procurement/bom/export', queryParams.value, `bom_${new Date().getTime()}.xlsx`);
};

onMounted(() => {
  loadProjects();
  loadSuppliers();
  getList();
});
</script>
