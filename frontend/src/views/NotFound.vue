<template>
  <div class="page">
    <h3>404</h3>
    <p>页面不存在。</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.page {
  width: 320px;
  margin: 80px auto;
}
</style>
