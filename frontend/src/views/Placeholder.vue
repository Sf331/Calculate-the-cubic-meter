<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div>
    <h3>{{ route.meta.title }}</h3>
    <p>该模块尚未实现。开发顺序见《编码计划书》第八节的批次计划。</p>
  </div>
</template>
