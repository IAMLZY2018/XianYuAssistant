# 销售额趋势图快速参考

## 🎨 配色方案

```css
/* 主题色 */
--chart-primary: #534AB7;
--chart-primary-light: rgba(83, 74, 183, 0.08);
--chart-primary-shadow: rgba(83, 74, 183, 0.15);

/* 渐变 */
--chart-gradient: linear-gradient(180deg, #534AB7 0%, #4a3fa5 100%);
--chart-bg-gradient: linear-gradient(135deg, rgba(83, 74, 183, 0.08) 0%, rgba(83, 74, 183, 0.04) 100%);
```

## 📐 尺寸规范

```css
/* 图表高度 */
--chart-height-desktop: 280px;
--chart-height-tablet: 240px;
--chart-height-mobile: 200px;

/* 柱子宽度 */
--bar-width-max: 40px;
--bar-width-tablet: 32px;
--bar-width-mobile: 28px;

/* 圆角 */
--chart-radius: 16px;
--button-radius: 6px;
--pill-radius: 999px;

/* 间距 */
--chart-padding: 20px 24px;
--control-gap: 8px;
```

## 🎭 动画时长

```css
/* 过渡 */
--transition-fast: 0.2s;
--transition-normal: 0.35s;

/* 动画 */
--animation-bar: 0.6s;
--animation-line: 1.2s;
--animation-dot: 0.4s;

/* 延迟 */
--animation-delay-step: 0.05s;
```

## 🔧 常用类名

### 控制按钮
```html
<!-- 维度切换 -->
<div class="dp__dimension-toggle">
  <button class="dp__dim-btn dp__dim-btn--active">日</button>
  <button class="dp__dim-btn">周</button>
</div>

<!-- 日期快捷选择 -->
<div class="dp__quick-dates">
  <button class="dp__quick-btn dp__quick-btn--active">近一周</button>
  <button class="dp__quick-btn">近一月</button>
</div>

<!-- 图表类型切换 -->
<div class="dp__chart-toggle">
  <button class="dp__toggle-btn dp__toggle-btn--active">柱状</button>
  <button class="dp__toggle-btn">折线</button>
</div>
```

### 图表容器
```html
<!-- 柱状图 -->
<div class="dp__bar-chart">
  <div class="dp__bar-group">
    <div class="dp__bar-wrap">
      <div class="dp__bar dp__bar--orange" style="height: 120px"></div>
    </div>
    <span class="dp__bar-label">10-15</span>
    <span class="dp__bar-count">¥1,234</span>
  </div>
</div>

<!-- 折线图 -->
<div class="dp__line-chart">
  <svg viewBox="0 0 320 180" class="dp__line-svg">
    <!-- SVG 内容 -->
  </svg>
  <div class="dp__legend">
    <span class="dp__legend-item">
      <span class="dp__legend-dot dp__legend-dot--orange"></span>
      销售额
    </span>
  </div>
</div>
```

## 📱 响应式断点

```css
/* 小屏手机 */
@media screen and (max-width: 480px) {
  /* 单列布局 */
}

/* 手机 */
@media screen and (max-width: 768px) {
  /* 垂直布局 */
}

/* 平板 */
@media screen and (min-width: 769px) and (max-width: 1024px) {
  /* 2列布局 */
}

/* 桌面 */
@media screen and (min-width: 1025px) {
  /* 多列布局 */
}

/* 大屏 */
@media screen and (min-width: 1440px) {
  /* 宽屏优化 */
}
```

## 🎯 交互状态

### 按钮状态
```css
/* 默认 */
.dp__quick-btn {
  border: 0.5px solid var(--d-border-strong);
  background: var(--d-surface);
  color: var(--d-text-secondary);
}

/* 悬停 */
.dp__quick-btn:hover {
  background: rgba(0, 0, 0, 0.03);
  border-color: rgba(0, 0, 0, 0.15);
}

/* 激活 */
.dp__quick-btn--active {
  border: 2px solid #534AB7;
  color: #534AB7;
  font-weight: 500;
}

/* 焦点 */
.dp__quick-btn:focus-visible {
  outline: 2px solid #534AB7;
  outline-offset: 2px;
}
```

### 图表元素状态
```css
/* 柱子悬停 */
.dp__bar:hover {
  filter: brightness(1.08);
  box-shadow: 0 4px 16px rgba(83, 74, 183, 0.25);
  transform: translateY(-2px);
}

/* 圆点悬停 */
.dp__line-svg circle:hover {
  r: 5;
  filter: drop-shadow(0 3px 8px rgba(83, 74, 183, 0.3));
}
```

## 🎬 动画示例

### 柱状图生长动画
```css
@keyframes barGrow {
  from {
    transform: scaleY(0);
    opacity: 0;
  }
  to {
    transform: scaleY(1);
    opacity: 1;
  }
}

.dp__bar {
  animation: barGrow 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) backwards;
  transform-origin: bottom;
}

/* 依次延迟 */
.dp__bar-group:nth-child(1) .dp__bar { animation-delay: 0.05s; }
.dp__bar-group:nth-child(2) .dp__bar { animation-delay: 0.1s; }
/* ... */
```

### 折线图绘制动画
```css
@keyframes lineDrawIn {
  from {
    stroke-dashoffset: 1000;
    opacity: 0;
  }
  to {
    stroke-dashoffset: 0;
    opacity: 1;
  }
}

.dp__line-svg path {
  stroke-dasharray: 1000;
  animation: lineDrawIn 1.2s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}
```

## 🔍 调试技巧

### 查看动画
```css
/* 临时禁用动画 */
* {
  animation-duration: 0s !important;
  transition-duration: 0s !important;
}
```

### 查看边界
```css
/* 显示所有元素边框 */
.dp__chart-card * {
  outline: 1px solid red;
}
```

### 性能监控
```javascript
// 监控重绘
const observer = new PerformanceObserver((list) => {
  for (const entry of list.getEntries()) {
    console.log(entry);
  }
});
observer.observe({ entryTypes: ['paint', 'layout-shift'] });
```

## 📊 数据格式

### 柱状图数据
```typescript
interface BarChartData {
  labels: string[];      // ['10-15', '10-16', ...]
  revenues: number[];    // [1234, 2345, ...]
  dimension: 'day' | 'week' | 'month' | 'quarter';
  startDate: string;     // '2024-10-15'
  endDate: string;       // '2024-10-24'
}
```

### 折线图数据
```typescript
interface LineChartData {
  labels: string[];
  revenues: number[];
  dimension: string;
  startDate: string;
  endDate: string;
}
```

## 🛠️ 常见问题

### Q: 如何修改主题色？
```css
/* 在 dashboard.css 中全局替换 */
#534AB7 → 你的颜色
rgba(83, 74, 183, ...) → 你的颜色的 rgba 值
```

### Q: 如何调整图表高度？
```css
.dp__bar-chart {
  height: 你的高度px;
}

.dp__bar-wrap {
  height: 你的高度 - 80px;  /* 减去标签和数值的高度 */
}
```

### Q: 如何禁用动画？
```css
.dp__bar {
  animation: none;
}

.dp__line-svg path {
  animation: none;
}
```

### Q: 如何自定义按钮样式？
```css
/* 覆盖默认样式 */
.dp__quick-btn--active {
  background: 你的颜色;
  border-color: 你的颜色;
  color: #fff;
}
```

## 🎨 设计资源

### 图标
- 使用项目中的 Icon 组件
- 推荐尺寸: 14px - 18px
- 颜色: 继承父元素或使用主题色

### 字体
```css
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
```

### 阴影层级
```css
--d-shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02);
--d-shadow-md: 0 4px 12px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.04);
--d-shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
```

## 📝 代码片段

### Vue 组件使用
```vue
<template>
  <div class="dp__chart-card">
    <div class="dp__chart-head">
      <span class="dp__chart-title">销售额趋势</span>
      <div class="dp__chart-toggle">
        <button 
          :class="['dp__toggle-btn', { 'dp__toggle-btn--active': chartMode === 'bar' }]"
          @click="chartMode = 'bar'"
        >
          柱状
        </button>
        <button 
          :class="['dp__toggle-btn', { 'dp__toggle-btn--active': chartMode === 'line' }]"
          @click="chartMode = 'line'"
        >
          折线
        </button>
      </div>
    </div>
    
    <!-- 图表内容 -->
    <div v-if="chartMode === 'bar'" class="dp__bar-chart">
      <!-- 柱状图 -->
    </div>
    <div v-else class="dp__line-chart">
      <!-- 折线图 -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const chartMode = ref<'bar' | 'line'>('bar');
</script>
```

### 动态计算柱子高度
```typescript
const getBarHeight = (value: number, maxValue: number): number => {
  const maxHeight = 200; // 柱子容器高度
  return maxValue > 0 ? (value / maxValue) * maxHeight : 0;
};
```

### SVG 路径生成
```typescript
const generatePath = (data: number[], allData: number[][]): string => {
  if (!data.length) return '';
  
  const maxVal = Math.max(...allData.flat(), 1);
  const n = data.length;
  const xStep = 280 / (n - 1 || 1);
  
  return data.map((v, i) => {
    const x = 30 + i * xStep;
    const y = 150 - (v / maxVal) * 140;
    return `${i === 0 ? 'M' : 'L'}${x},${y}`;
  }).join(' ');
};
```

## 🚀 性能优化清单

- ✅ 使用 `transform` 和 `opacity` 实现动画
- ✅ 避免在动画中使用 `width`、`height`
- ✅ 使用 `will-change` 提示浏览器
- ✅ 合理设置动画时长 (< 1.5s)
- ✅ 使用 CSS 变量减少重复代码
- ✅ 图表数据按需加载
- ✅ 防抖和节流事件处理
- ✅ 使用 `requestAnimationFrame` 处理滚动

## 📚 相关文档

- [完整改进说明](./SALES_REVENUE_CHART_IMPROVEMENTS.md)
- [设计对比文档](./CHART_DESIGN_COMPARISON.md)
- [Vue 3 文档](https://vuejs.org/)
- [Element Plus 文档](https://element-plus.org/)
- [CSS 动画指南](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Animations)

## 💡 最佳实践

1. **保持一致性**: 使用统一的配色和间距
2. **响应式优先**: 从移动端开始设计
3. **性能优先**: 避免不必要的重绘和重排
4. **可访问性**: 支持键盘导航和屏幕阅读器
5. **渐进增强**: 基础功能优先，动画作为增强
6. **代码复用**: 使用 CSS 变量和混合
7. **注释清晰**: 复杂逻辑添加注释
8. **测试充分**: 多设备、多浏览器测试

---

**最后更新**: 2024-05-14  
**维护者**: 开发团队  
**版本**: 1.0.0
