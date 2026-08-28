<template>
  <!--
    app-container 是 RuoYi 项目给页面内容区加的通用外层类名，
    作用：让页面四周留出合适边距，避免内容贴边。
  -->
  <div class="app-container">
    <!--
      el-card 是 Element Plus 的卡片组件，
      作用：把一块内容包在一个带阴影的卡片里，视觉上更整齐。
    -->
    <el-card>
      <!-- 页面标题 -->
      <h1>Hello World</h1>
      <p>这是一个学习 RuoYi 前端组件的示例页面。</p>

      <!--
        ===================================================================
        1. el-button 按钮组件
        ===================================================================
        type="primary"：蓝色主按钮
        @click="handleClick"：点击时调用 script 里的 handleClick 函数
        plain：朴素按钮样式（边框样式）
      -->
      <el-divider content-position="left">1. 按钮 el-button</el-divider>
      <el-button type="primary" @click="handleClick">点我弹出提示</el-button>
      <el-button plain @click="handleReset">重置表单</el-button>

      <!--
        ===================================================================
        2. el-form 表单 + el-input 输入框 + el-select 下拉框
        ===================================================================
        :model="form"：把表单数据和 script 里的 form 对象绑定
        label-width="100px"：每个表单项前面的标签宽度
        ref="formRef"：给表单起个名字，方便在 script 里调用它的方法（比如重置）
        :rules="rules"：表单校验规则
      -->
      <el-divider content-position="left">2. 表单 el-form / 输入框 el-input / 下拉框 el-select</el-divider>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 500px">
        <!--
          el-form-item：一个表单项
          label="姓名"：显示在前面的标签文字
          prop="name"：对应 form.name，也用于校验规则里的字段名
        -->
        <el-form-item label="姓名" prop="name">
          <!--
            v-model="form.name"：双向绑定
            输入框里的内容和 script 里 form.name 的值实时同步
          -->
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="年龄" prop="age">
          <!-- type="number" 让输入框只能输入数字 -->
          <el-input v-model.number="form.age" type="number" placeholder="请输入年龄" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <!--
            el-select：下拉选择框
            v-model="form.gender"：绑定选中的值
            placeholder="请选择"：未选择时显示的占位文字
          -->
          <el-select v-model="form.gender" placeholder="请选择性别" style="width: 100%">
            <!--
              el-option：下拉选项
              label="男"：用户看到的文字
              value="male"：实际保存到 form.gender 里的值
            -->
            <el-option label="男" value="male" />
            <el-option label="女" value="female" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <!--
            :loading="submitting"：控制按钮是否显示加载状态
            当 submitting 为 true 时，按钮会转圈，防止重复提交
          -->
          <el-button type="success" :loading="submitting" @click="handleSubmit">提交表单</el-button>
        </el-form-item>
      </el-form>

      <!--
        ===================================================================
        3. el-table 表格组件
        ===================================================================
        :data="tableData"：表格的数据源，是一个数组
        border：显示边框
        stripe：斑马纹（隔行变色）
      -->
      <el-divider content-position="left">3. 表格 el-table</el-divider>
      <el-table :data="tableData" border stripe>
        <!--
          el-table-column：一列
          prop="name"：对应数据里的字段名
          label="姓名"：表头显示的文字
          width="120"：列宽
        -->
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="gender" label="性别" width="80">
          <!--
            自定义列内容：用 #default="scope"
            scope.row 就是当前这一行的数据对象
          -->
          <template #default="scope">
            {{ scope.row.gender === 'male' ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <!--
              link 类型按钮：文字链接样式，适合放在表格操作列
              @click="handleEdit(scope.row)"：把当前行数据传过去
            -->
            <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!--
        ===================================================================
        4. el-dialog 弹窗组件
        ===================================================================
        v-model="dialogVisible"：控制弹窗显示/隐藏
        title="编辑信息"：弹窗顶部标题
        width="500px"：弹窗宽度
      -->
      <el-divider content-position="left">4. 弹窗 el-dialog</el-divider>
      <el-button @click="dialogVisible = true">打开弹窗</el-button>
      <el-dialog v-model="dialogVisible" title="编辑信息" width="500px">
        <p>当前编辑：{{ currentRow?.name || '未选择' }}</p>
        <p>这里可以放表单，让用户修改数据。</p>
        <template #footer>
          <!--
            #footer 是弹窗底部区域
            通常放"确定"和"取消"按钮
          -->
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="dialogVisible = false">确定</el-button>
        </template>
      </el-dialog>

      <!--
        ===================================================================
        5. RuoYi 自带组件：DictTag 字典标签
        ===================================================================
        DictTag 是 RuoYi 封装好的字典显示组件，放在 src/components/DictTag。
        作用：根据字典值自动显示对应的文字 + Element Plus 标签颜色。
        实际项目中，字典数据通常来自后端的 sys_dict_data 表。
        :options：字典选项数组，每项包含 label/value/elTagType
        :value：要匹配的字典值
      -->
      <el-divider content-position="left">5. RuoYi 组件：DictTag 字典标签</el-divider>
      <p>发票类型值 "0" 显示为：</p>
      <dict-tag :options="invoiceTypeOptions" value="0" />
      <p style="margin-top: 10px;">发票类型值 "1" 显示为：</p>
      <dict-tag :options="invoiceTypeOptions" value="1" />

      <!--
        ===================================================================
        6. RuoYi 自带组件：SvgIcon SVG 图标
        ===================================================================
        SvgIcon 是 RuoYi 封装的 svg 图标组件，放在 src/components/SvgIcon。
        它会自动到 src/assets/icons/svg 目录下找对应文件名的 svg。
        iconClass：svg 文件名，不需要写 #icon- 前缀
        className：额外的 CSS 类名，方便调大小
        color：图标颜色
      -->
      <el-divider content-position="left">6. RuoYi 组件：SvgIcon SVG 图标</el-divider>
      <svg-icon icon-class="dict" class-name="demo-icon" color="#409eff" />
      <svg-icon icon-class="user" class-name="demo-icon" color="#67c23a" />

      <!--
        ===================================================================
        7. RuoYi 自带组件：Pagination 分页
        ===================================================================
        Pagination 是 RuoYi 封装的分页组件，放在 src/components/Pagination。
        它封装了 Element Plus 的 el-pagination，统一了分页样式和行为。
        :total：总条数
        v-model:page：当前页码
        v-model:limit：每页条数
        @pagination：页码或每页条数变化时触发
      -->
      <el-divider content-position="left">7. RuoYi 组件：Pagination 分页</el-divider>
      <pagination
        :total="100"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="handlePageChange"
      />

      <!--
        ===================================================================
        8. RuoYi 自带组件：RightToolbar 右侧工具栏
        ===================================================================
        RightToolbar 是表格页面右上角常见的工具栏，放在 src/components/RightToolbar。
        作用：提供"显示/隐藏搜索"、"刷新"、"显示/隐藏列"三个按钮。
        :showSearch：是否显示搜索区域
        :columns：可控制显隐的列配置，每项是 { key, label, visible }
        @update:showSearch：点击搜索按钮时触发，参数是新的 showSearch 值
        @queryTable：点击刷新按钮时触发
      -->
      <el-divider content-position="left">8. RuoYi 组件：RightToolbar 右侧工具栏</el-divider>
      <p>当前搜索区域状态：{{ showSearch ? '显示' : '隐藏' }}</p>
      <right-toolbar
        :showSearch="showSearch"
        :columns="columns"
        @update:showSearch="showSearch = $event"
        @queryTable="handleRefresh"
      />
    </el-card>
  </div>
</template>

<script setup name="HelloWorld">
/**
 * script setup 是 Vue3 的写法，
 * 作用：让组件逻辑更简洁，不用写 export default {}
 * name="HelloWorld"：给组件起名字，方便调试
 */

// ref 用来定义响应式数据：数据变化，页面会自动更新
import { ref } from 'vue';

// 引入 Element Plus 的消息提示组件
import { ElMessage } from 'element-plus';

// 引入 RuoYi 项目自己封装的组件
// 它们都放在 src/components 目录下
// 本项目使用了 unplugin-vue-components，所以这些组件会被自动注册
// 但教学示例里显式 import，方便你看到它们从哪里来
import DictTag from '@/components/DictTag/index.vue';
import Pagination from '@/components/Pagination/index.vue';
import SvgIcon from '@/components/SvgIcon/index.vue';
import RightToolbar from '@/components/RightToolbar/index.vue';

/**
 * form 对象：存储表单数据
 * 初始值：姓名空字符串、年龄 18、性别空字符串
 */
const form = ref({
  name: '',
  age: 18,
  gender: ''
});

/**
 * formRef：给 el-form 组件的引用
 * 作用：在 script 里调用表单的方法，比如重置表单、触发表单校验
 */
const formRef = ref(null);

/**
 * submitting：控制提交按钮的加载状态
 * true：按钮显示加载动画，防止用户重复点击
 */
const submitting = ref(false);

/**
 * dialogVisible：控制弹窗显示/隐藏
 * true：弹窗显示；false：弹窗关闭
 */
const dialogVisible = ref(false);

/**
 * currentRow：存储当前点击编辑的行数据
 * 点击表格"编辑"按钮时赋值，弹窗里显示这行数据
 */
const currentRow = ref(null);

/**
 * 表单校验规则
 * required: true 表示必填
 * message: 不满足规则时显示的提示文字
 * trigger: 'blur' 表示输入框失去焦点时触发校验
 */
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
};

/**
 * tableData：表格数据源
 * 实际项目中，这个数据通常是从后端接口请求回来的
 */
const tableData = ref([
  { name: '张三', age: 25, gender: 'male', address: '北京市朝阳区' },
  { name: '李四', age: 28, gender: 'female', address: '上海市浦东新区' },
  { name: '王五', age: 22, gender: 'male', address: '广州市天河区' }
]);

/**
 * 按钮点击事件
 * ElMessage.success()：在页面右上角弹出绿色成功提示
 */
function handleClick() {
  ElMessage.success('你点击了按钮！');
}

/**
 * 重置表单
 * formRef.value.resetFields()：把表单所有字段恢复成初始值
 */
function handleReset() {
  formRef.value?.resetFields();
  ElMessage.info('表单已重置');
}

/**
 * 提交表单
 * formRef.value.validate()：触发表单校验
 * 如果校验通过，valid 为 true；否则为 false
 */
function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (valid) {
      submitting.value = true;
      // 模拟 1 秒网络请求
      setTimeout(() => {
        submitting.value = false;
        ElMessage.success(`提交成功：${form.value.name}，${form.value.age}岁`);
      }, 1000);
    } else {
      ElMessage.error('请检查表单填写是否正确');
    }
  });
}

/**
 * 编辑按钮点击事件
 * row：当前行的数据对象
 */
function handleEdit(row) {
  currentRow.value = row;
  dialogVisible.value = true;
}

/**
 * ============================================================
 * RuoYi 自带组件相关数据与方法
 * ============================================================
 */

// DictTag 字典标签需要的数据
// 实际项目中通常通过 useDict('invoice_type') 从后端字典获取
const invoiceTypeOptions = [
  { label: '增值税专用发票', value: '0', elTagType: 'success' },
  { label: '增值税普通发票', value: '1', elTagType: 'primary' }
];

// Pagination 分页组件需要的查询参数
const queryParams = ref({
  pageNum: 1,
  pageSize: 10
});

// 分页变化时触发
function handlePageChange({ page, limit }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = limit;
  ElMessage.info(`分页变化：第 ${page} 页，每页 ${limit} 条`);
}

// RightToolbar 搜索区域显示/隐藏状态
const showSearch = ref(true);

// RightToolbar 列显示/隐藏配置
// FieldOption 类型定义在 src/types/global.d.ts
const columns = ref([
  { key: 1, label: '姓名', visible: true },
  { key: 2, label: '年龄', visible: true },
  { key: 3, label: '地址', visible: false }
]);

// RightToolbar 点击刷新按钮时触发
function handleRefresh() {
  ElMessage.success('触发了刷新事件');
}
</script>

<style scoped>
/*
  scoped 表示这些样式只作用于当前组件，
  不会影响其他页面。
*/
h1 {
  color: #409eff;
}

/*
  demo-icon：给 SvgIcon 示例用的样式
  font-size 控制图标大小，margin-right 控制图标间距
*/
.demo-icon {
  font-size: 32px;
  margin-right: 16px;
}
</style>
