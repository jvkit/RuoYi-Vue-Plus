<template>
  <div class="p-2">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>发票上传与 AI 识别</span>
        </div>
      </template>

      <!-- 步骤 1：上传图片 -->
      <el-steps :active="step" finish-status="success" class="mb-4">
        <el-step title="上传发票图片" />
        <el-step title="AI 识别填表" />
        <el-step title="确认提交" />
      </el-steps>

      <el-alert
        v-if="result.status"
        :title="statusTitle"
        :type="statusType"
        :description="result.aiOpinion"
        show-icon
        :closable="false"
        class="mb-4"
      />

      <el-form ref="invoiceFormRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="发票图片" prop="imageFile">
          <el-upload
            ref="uploadRef"
            action="#"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept="image/*"
          >
            <el-button type="primary">选择发票图片</el-button>
            <template #tip>
              <div class="el-upload__tip">上传图片后可点击"AI 识别"自动填表，或直接手动填写下方表单</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="extracting" @click="handleExtract">
            {{ extracted ? '重新识别' : 'AI 识别' }}
          </el-button>
        </el-form-item>

        <el-divider content-position="left">发票信息（可手动填写或修正识别结果）</el-divider>

        <el-row :gutter="20">
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

        <el-row :gutter="20">
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

        <el-row :gutter="20">
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

        <el-row :gutter="20">
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

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联订单" prop="orderNo">
              <el-input v-model="form.orderNo" placeholder="请输入关联订单号（选填）" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>

        <el-form-item>
          <el-button type="success" :loading="submitting" @click="handleSubmit">
            确认并提交审核
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="InvoiceEmployee" lang="ts">
import { extractInvoice, addInvoiceInfo, aiReviewInvoice, checkDuplicate } from '@/api/invoice/info';
import { InvoiceInfoForm } from '@/api/invoice/info/types';
import { ElMessageBox } from 'element-plus';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { invoice_type } = toRefs<any>(proxy?.useDict('invoice_type'));

const invoiceFormRef = ref<ElFormInstance>();
const uploadRef = ref<any>();
const imageFile = ref<File | undefined>();
const extracted = ref(false);
const extracting = ref(false);
const submitting = ref(false);
const step = ref(0);

const result = reactive({
  status: '',
  aiOpinion: ''
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
  verifyStatus: '',
  verifyTime: '',
  finQueryNo: '',
  orderNo: '',
  remark: ''
};

const data = reactive({
  form: { ...initFormData },
  rules: {
    invoiceNumber: [{ required: true, message: '发票号码不能为空', trigger: 'blur' }],
    invoiceType: [{ required: true, message: '发票类型不能为空', trigger: 'change' }],
    invoiceDate: [{ required: true, message: '开票日期不能为空', trigger: 'change' }],
    amount: [{ required: true, message: '不含税金额不能为空', trigger: 'change' }],
    taxAmount: [{ required: true, message: '税额不能为空', trigger: 'change' }]
  }
});

const { form, rules } = toRefs(data);

const statusTitle = computed(() => {
  if (result.status === 'submitted') return 'AI 审核通过';
  if (result.status === 'rejected') return 'AI 审核驳回';
  return '';
});

const statusType = computed(() => {
  if (result.status === 'submitted') return 'success';
  if (result.status === 'rejected') return 'error';
  return 'info';
});

const handleFileChange = (file: any) => {
  imageFile.value = file.raw;
  extracted.value = false;
  result.status = '';
  result.aiOpinion = '';
  step.value = 0;
};

const handleFileRemove = () => {
  imageFile.value = undefined;
  extracted.value = false;
  result.status = '';
  result.aiOpinion = '';
  step.value = 0;
};

const handleExtract = () => {
  if (!imageFile.value) {
    proxy?.$modal.msgWarning('请先上传发票图片');
    return;
  }

  extracting.value = true;
  result.status = '';
  result.aiOpinion = '';

  extractInvoice(imageFile.value)
    .then((res: any) => {
      const data = res.data;
      form.value.invoiceCode = data.invoiceCode || '';
      form.value.invoiceNumber = data.invoiceNumber || '';
      form.value.invoiceType = data.invoiceType || 'normal';
      form.value.amount = data.amount;
      form.value.taxAmount = data.taxAmount;
      form.value.totalAmount = data.totalAmount;
      form.value.invoiceDate = data.invoiceDate || '';
      form.value.sellerName = data.sellerName || '';
      form.value.buyerName = data.buyerName || '';

      // 显示 AI 预审意见
      result.status = data.passed ? 'submitted' : 'rejected';
      result.aiOpinion = data.opinion || '暂无审核意见';

      extracted.value = true;
      step.value = 1;
      proxy?.$modal.msgSuccess('AI 识别完成，请检查并修正识别结果');
    })
    .catch((error: any) => {
      console.error('识别失败', error);
      proxy?.$modal.msgError('AI 识别失败，请重试');
    })
    .finally(() => {
      extracting.value = false;
    });
};

const handleSubmit = () => {
  invoiceFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;

    submitting.value = true;
    try {
      // 1. 查重：检查发票代码+号码是否已存在
      if (form.value.invoiceCode && form.value.invoiceNumber) {
        const dupRes = await checkDuplicate(form.value.invoiceCode, form.value.invoiceNumber);
        const dup = dupRes.data;
        if (dup && dup.isDuplicate) {
          const statusMap: Record<string, string> = {
            draft: '草稿',
            submitted: '已提交',
            approved: '已认证',
            rejected: '已驳回'
          };
          const statusText = statusMap[dup.duplicateStatus] || dup.duplicateStatus || '未知';
          const lines = [
            `发票代码：${dup.duplicateInvoiceCode}`,
            `发票号码：${dup.duplicateInvoiceNumber}`,
            `状态：${statusText}`,
            `销售方：${dup.duplicateSellerName || '—'}`,
            `价税合计：${dup.duplicateTotalAmount ?? '—'}`,
            dup.duplicateFinQueryNo ? `财务查询单号：${dup.duplicateFinQueryNo}` : null,
            dup.duplicateOrderNo ? `⚠️ 该发票已在关联订单「${dup.duplicateOrderNo}」下提交` : null,
            `记录ID：${dup.duplicateId}`
          ].filter(Boolean);
          const msg = `检测到重复发票，已有记录如下：\n\n${lines.join('\n')}\n\n是否仍要继续提交？`;
          try {
            await ElMessageBox.confirm(msg, '查重提示 — 重复发票', {
              confirmButtonText: '继续提交',
              cancelButtonText: '取消',
              type: 'warning'
            });
          } catch {
            // 用户取消
            submitting.value = false;
            return;
          }
        }
      }

      // 2. 创建发票
      const submitData = {
        ...form.value,
        totalAmount: Number(form.value.amount || 0) + Number(form.value.taxAmount || 0)
      };
      const addRes = await addInvoiceInfo(submitData);
      const invoiceId = addRes.data.id;

      // 3. 调用 AI 审核（携带同一张图片）
      const reviewRes = await aiReviewInvoice(invoiceId, imageFile.value);
      const reviewData = reviewRes.data;

      // 4. 显示最终结果
      result.status = reviewData.passed ? 'submitted' : 'rejected';
      result.aiOpinion = reviewData.opinion || '暂无审核意见';
      step.value = 2;

      proxy?.$modal.msgSuccess(reviewData.passed ? '发票已提交并通过 AI 审核' : 'AI 审核已驳回，请根据意见修改后重新提交');
    } catch (error) {
      console.error('提交失败', error);
      proxy?.$modal.msgError('提交或 AI 审核失败，请稍后重试');
    } finally {
      submitting.value = false;
    }
  });
};

const handleReset = () => {
  form.value = { ...initFormData };
  result.status = '';
  result.aiOpinion = '';
  extracted.value = false;
  imageFile.value = undefined;
  step.value = 0;
  uploadRef.value?.clearFiles?.();
  invoiceFormRef.value?.resetFields();
};
</script>

<style scoped>
.card-header {
  font-weight: bold;
  font-size: 16px;
}
</style>
