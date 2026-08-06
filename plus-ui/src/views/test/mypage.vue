<template>
  <div class="app-container">
    <el-card>
    <el-table :data="mydata" border>
             <el-table-column prop="name" label="姓名" width="120" />
             <el-table-column prop="age" label="年龄" width="80" />
             <el-table-column prop="gender" label="性别" width="80" />
             <el-table-column prop="address" label="地址" />

    </el-table>    

        <el-table :data="mydata" stripe align="center" header-align="center">
             <el-table-column prop="name" label="姓名" width="120" />
             <el-table-column prop="age" label="年龄" width="80" />
             <el-table-column prop="gender" label="性别" width="80" />
             <el-table-column prop="address" label="地址" />

    </el-table>    
    
    </el-card>
  </div>
</template>

<script setup name="mypage">
// 页面逻辑可以写在这里，现在先空着
const mydata= [
         { name: '张三', age: 20, gender: 'male', address: '北京市' },
         { name: '李四', age: 25, gender: 'female', address: '上海市' }
       ]
</script>

<style scoped>
h1 {
  color: #409eff;
}
</style>
