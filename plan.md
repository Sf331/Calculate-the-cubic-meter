STYLEKIT_STYLE_REFERENCE
style_name: 数据密集
style_slug: data-dense
style_source: /styles/data-dense

# Hard Prompt

## 什么时候用
当你希望 AI 严格按风格规则生成代码时使用。它是生产界面最稳的默认选择。

## 怎么用
- 把完整提示词复制到 ChatGPT、Claude、Cursor 或其他编码助手。
- 在提示词后追加具体产品、页面或组件需求。
- 生成后按禁止项和交互状态检查，确认没有风格漂移。

请严格遵守以下风格规则并保持一致性，禁止风格漂移。

## 执行要求

- 优先保证风格一致性，其次再做创意延展。
- 遇到冲突时以禁止项为最高优先级。
- 输出前自检：颜色、排版、间距、交互是否仍属于该风格。

## Style Rules

You are a Data Dense admin panel design expert.

## 绝对禁止

- 禁止使用大间距 p-6 以上（浪费数据展示空间）
- 禁止使用大圆角 rounded-xl 以上（占用像素）
- 禁止使用大字号 text-lg 以上作为表格数据
- 禁止使用装饰性渐变或阴影（增加视觉噪音）
- 禁止隐藏重要操作在下拉菜单中（常用操作必须直接可见）
- 禁止使用大面积空白（数据优先）

## 必须遵守

- 使用白色 bg-white 搭配极浅蓝灰 bg-[#f8fafc] 交替行色
- 表格行高紧凑 py-2 text-sm 或 text-xs
- 状态标签使用颜色编码 bg-blue-50 text-blue-700 rounded px-1.5 py-0.5 text-xs
- 按钮使用小尺寸 px-2.5 py-1 text-xs rounded
- 间距统一使用 4px 递增：gap-1 gap-2 gap-3
- 表头使用 text-xs uppercase tracking-wide text-[#64748b] font-medium
- 使用 monospace 显示数字、ID、代码类数据
- 分隔线使用 border-[#e2e8f0] 细线

## Absolute Rules
- Maximum information density: py-2 or less for table rows
- All text in tables must be text-sm or text-xs
- Numbers and IDs must use font-mono
- Status must be color-coded (green=active, yellow=pending, red=error, blue=info)
- Buttons must be small: px-2.5 py-1 text-xs
- Table headers must be uppercase text-[10px] tracking-wide

## Responsive
- Mobile: horizontal scroll for tables, stacked cards for KPIs
- Desktop: full table view with inline actions

---

# Data Dense (数据密集) Design System

> 高密度后台管理面板风格，紧凑间距与小尺寸组件，优先展示数据表格与操作效率。

## 核心理念

Data Dense 是一种效率优先的管理界面设计语言。

核心理念：
- 信息密度最大化：在有限屏幕空间内展示最多有效信息
- 扫描效率：行高紧凑、对齐严格，支持快速垂直扫描
- 操作即时性：行内操作、快捷键、批量选择减少点击次数
- 状态可视化：颜色编码传递状态，无需阅读文字即可理解

设计原则：
- 视觉一致性：所有组件必须遵循统一的视觉语言，从色彩到字体到间距保持谐调
- 层次分明：通过颜色深浅、字号大小、留白空间建立清晰的信息层级
- 交互反馈：每个可交互元素都必须有明确的 hover、active、focus 状态反馈
- 响应式适配：设计必须在移动端、平板、桌面端上保持一致的体验
- 无障碍性：确保色彩对比度符合 WCAG 2.1 AA 标准，所有交互元素可键盘访问

---

## Token 字典（精确 Class 映射）

### 边框
```
宽度: border
颜色: border-[#e2e8f0]
圆角: rounded
```

### 阴影
```
小: shadow-none
中: shadow-sm
大: shadow-sm
悬停: hover:shadow-sm
聚焦: ring-1 ring-[#3b82f6]
```

### 交互效果
```
悬停位移: （无）
悬停缩放: hover:bg-[#f8fafc]
悬停透明度: （无）
过渡动画: transition-colors duration-150
按下状态: active:scale-95
```

### 字体
```
标题: font-semibold tracking-tight
正文: font-normal
等宽: font-mono
```

### 字号
```
Hero: text-xl md:text-2xl
H1: text-lg md:text-xl
H2: text-base md:text-lg
H3: text-sm md:text-base
正文: text-xs md:text-sm
小字: text-[10px]
```

### 间距
```
Section: py-3 md:py-4
容器: px-3 md:px-4
卡片: px-3 py-2.5
小间距: gap-1
中间距: gap-2
大间距: gap-3
```

### 颜色角色
```
背景主色: bg-white
背景辅色: bg-[#f8fafc]
背景强调色: bg-[#3b82f6]
正文主色: text-[#1e293b]
正文辅色: text-[#64748b]
正文弱化色: text-[#94a3b8]
按钮主色: bg-[#3b82f6] text-white hover:bg-[#2563eb]
按钮辅色: bg-white text-[#64748b] border border-[#e2e8f0]
```

---

## [FORBIDDEN] 绝对禁止

以下 class 在本风格中**绝对禁止使用**，生成时必须检查并避免：

### 禁止的 Class
- `rounded-xl`
- `rounded-2xl`
- `shadow-lg`
- `shadow-xl`
- `shadow-2xl`
- `p-6`
- `p-8`
- `text-xl`
- `text-2xl`

### 禁止的模式
- 匹配 `^rounded-(xl|2xl|3xl|full)`
- 匹配 `^shadow-(lg|xl|2xl)`
- 匹配 `^p-[6-9]`
- 匹配 `^bg-gradient-`

### 禁止原因
- `rounded-xl`: Data Dense uses small rounded corners only
- `shadow-lg`: Data Dense avoids decorative shadows
- `p-6`: Data Dense uses compact spacing (p-3 or less)
- `text-lg`: Data Dense uses small text sizes (text-sm or text-xs)

> WARNING: 如果你的代码中包含以上任何 class，必须立即替换。

---

## [REQUIRED] 必须包含

### 按钮必须包含
```
px-2.5 py-1
text-xs
rounded
transition-colors
```

### 卡片必须包含
```
bg-white
border border-[#e2e8f0]
rounded
```

### 输入框必须包含
```
px-2.5 py-1.5
text-xs
border border-[#e2e8f0]
rounded
focus:ring-1 focus:ring-[#3b82f6]
```

---

## [COMPARE] Data Dense 错误 vs 正确对比

以下错误示例只代表“未经过当前风格适配的通用默认值”，不要把错误示例当成视觉建议。

### 按钮

[WRONG] **错误示例**（通用组件库默认样式，不要直接复制）：
```html
<button class="{GENERIC_LIBRARY_BUTTON_DEFAULT}">
  点击我
</button>
```

[CORRECT] **正确示例**（使用当前风格的 token）：
```html
<button class="px-2.5 py-1 text-xs rounded transition-colors bg-[#3b82f6] text-white hover:bg-[#2563eb]">
  点击我
</button>
```

### 卡片

[WRONG] **错误示例**（未经当前风格适配的通用卡片）：
```html
<div class="{GENERIC_LIBRARY_CARD_DEFAULT}">
  <h3>{TITLE}</h3>
</div>
```

[CORRECT] **正确示例**（使用当前风格的 card token）：
```html
<div class="bg-white border border-[#e2e8f0] rounded px-3 py-2.5">
  <h3 class="font-semibold tracking-tight text-sm md:text-base">{TITLE}</h3>
</div>
```

### 输入框

[WRONG] **错误示例**（未经当前风格适配的通用输入框）：
```html
<input class="{GENERIC_LIBRARY_INPUT_DEFAULT}" />
```

[CORRECT] **正确示例**（使用当前风格的 input token）：
```html
<input class="px-2.5 py-1.5 text-xs border border-[#e2e8f0] rounded focus:ring-1 focus:ring-[#3b82f6]" placeholder="{PLACEHOLDER}" />
```

---

## [TEMPLATES] Data Dense 页面骨架模板

以下骨架只使用当前风格的 token。替换 `{PLACEHOLDER}` 时，不要移除或替换这些 token：

### 导航栏骨架
```html
<nav class="bg-white text-[#1e293b] border border-[#e2e8f0] px-3 md:px-4">
  <div class="flex items-center justify-between max-w-6xl mx-auto gap-2">
    <a href="/" class="font-semibold tracking-tight text-sm md:text-base">
      {LOGO_TEXT}
    </a>
    <div class="flex gap-2 font-normal text-[10px]">
      {NAV_LINKS}
    </div>
  </div>
</nav>
```

### Hero 区块骨架
```html
<section class="bg-[#3b82f6] text-[#1e293b] py-3 md:py-4 px-3 md:px-4">
  <div class="max-w-4xl mx-auto">
    <h1 class="font-semibold tracking-tight text-xl md:text-2xl">
      {HEADLINE}
    </h1>
    <p class="font-normal text-xs md:text-sm max-w-xl">
      {SUBHEADLINE}
    </p>
    <button class="px-2.5 py-1 text-xs rounded transition-colors bg-[#3b82f6] text-white hover:bg-[#2563eb]">
      {CTA_TEXT}
    </button>
  </div>
</section>
```

### 卡片网格骨架
```html
<section class="bg-white text-[#1e293b] py-3 md:py-4 px-3 md:px-4">
  <div class="max-w-6xl mx-auto">
    <h2 class="font-semibold tracking-tight text-base md:text-lg">{SECTION_TITLE}</h2>
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
      <!-- Card template - repeat for each card -->
      <div class="bg-white border border-[#e2e8f0] rounded px-3 py-2.5">
        <h3 class="font-semibold tracking-tight text-sm md:text-base">{CARD_TITLE}</h3>
        <p class="font-normal text-xs md:text-sm text-[#94a3b8]">{CARD_DESCRIPTION}</p>
      </div>
    </div>
  </div>
</section>
```

### 表单输入骨架
```html
<input class="px-2.5 py-1.5 text-xs border border-[#e2e8f0] rounded focus:ring-1 focus:ring-[#3b82f6]" placeholder="{PLACEHOLDER}" />
```

### 页脚骨架
```html
<footer class="bg-[#f8fafc] text-[#64748b] py-3 md:py-4 px-3 md:px-4">
  <div class="max-w-6xl mx-auto">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
      <div>
        <span class="font-semibold tracking-tight text-sm md:text-base">{LOGO_TEXT}</span>
        <p class="font-normal text-[10px]">{TAGLINE}</p>
      </div>
      <div>
        <h4 class="font-semibold tracking-tight text-sm md:text-base">{COLUMN_TITLE}</h4>
        <ul class="font-normal text-[10px]">
          {FOOTER_LINKS}
        </ul>
      </div>
    </div>
  </div>
</footer>
```

---

## [CHECKLIST] Data Dense 生成后自检清单

**输出代码前，逐项验证当前风格的 token 和规则。如有违反，先修正再交付：**

### Token 检查
- [ ] 按钮包含： `px-2.5 py-1 text-xs rounded transition-colors`
- [ ] 卡片包含： `bg-white border border-[#e2e8f0] rounded`
- [ ] 输入框包含： `px-2.5 py-1.5 text-xs border border-[#e2e8f0] rounded focus:ring-1 focus:ring-[#3b82f6]`

### 禁止项检查
- [ ] 没有使用 `rounded-xl`
- [ ] 没有使用 `rounded-2xl`
- [ ] 没有使用 `shadow-lg`
- [ ] 没有使用 `shadow-xl`
- [ ] 没有使用 `shadow-2xl`
- [ ] 没有使用 `p-6`
- [ ] 没有使用 `p-8`
- [ ] 没有使用 `text-xl`

### 风格规则检查
- [ ] 使用白色 bg-white 搭配极浅蓝灰 bg-[#f8fafc] 交替行色
- [ ] 表格行高紧凑 py-2 text-sm 或 text-xs
- [ ] 状态标签使用颜色编码 bg-blue-50 text-blue-700 rounded px-1.5 py-0.5 text-xs
- [ ] 按钮使用小尺寸 px-2.5 py-1 text-xs rounded
- [ ] 间距统一使用 4px 递增：gap-1 gap-2 gap-3

### 风格漂移检查
- [ ] 没有违反：禁止使用大间距 p-6 以上（浪费数据展示空间）
- [ ] 没有违反：禁止使用大圆角 rounded-xl 以上（占用像素）
- [ ] 没有违反：禁止使用大字号 text-lg 以上作为表格数据
- [ ] 没有违反：禁止使用装饰性渐变或阴影（增加视觉噪音）
- [ ] 没有违反：禁止隐藏重要操作在下拉菜单中（常用操作必须直接可见）

### 通用交付检查
- [ ] 响应式布局在手机、平板和桌面下稳定，没有横向溢出
- [ ] 所有交互元素有清晰焦点、可访问名称和 reduced-motion 方案
- [ ] 文本对比度达到 WCAG AA，且没有用颜色单独传递状态
- [ ] 结果仍然能够一眼识别为 Data Dense

---

## [EXAMPLES] 示例 Prompt

### 1. 管理后台仪表盘

高密度后台面板，含 KPI 卡片、数据表格、筛选栏

```
Build a data-dense admin dashboard with 4 compact KPI cards at top, filter toolbar with search and dropdowns, dense data table with inline status badges and action buttons, and pagination footer.
```

### 2. SaaS 着陆页

生成 数据密集风格的 SaaS 产品着陆页

```
Create a SaaS landing page using Data Dense style with hero section, feature grid, testimonials, pricing table, and footer.
```

### 3. 作品集展示

生成 数据密集风格的作品集页面

```
Create a portfolio showcase page using Data Dense style with project grid, about section, contact form, and consistent visual language.
```

## 绝对禁止（匹配即拒绝）

以下模式一旦出现，视为风格违规——不找借口，直接重写。

- 使用大间距 p-6 以上（浪费数据展示空间）
- 使用大圆角 rounded-xl 以上（占用像素）
- 使用大字号 text-lg 以上作为表格数据
- 使用装饰性渐变或阴影（增加视觉噪音）
- 隐藏重要操作在下拉菜单中（常用操作必须直接可见）
- 使用大面积空白（数据优先）

## 自检清单（交付前逐条确认）

如果任何一条不通过，说明风格漂移了——修改后再交付。

- [ ] 没有紫色到蓝色的渐变
- [ ] 没有使用 Inter / Roboto / Geist 等过度使用的字体
- [ ] 没有嵌套卡片（卡片里面套卡片）
- [ ] 没有在彩色背景上放灰色文字
- [ ] 正文对比度满足 WCAG AA（≥4.5:1）
- [ ] 没有 bounce / elastic 缓动曲线
- [ ] 动效有 prefers-reduced-motion 备选方案
- [ ] 正文行宽不超过 65-75 个字符
- [ ] 没有单侧粗边框装饰（border-left/right accent stripe）
- [ ] 没有渐变文字（background-clip: text）
- [ ] 没有把玻璃态（glassmorphism）当作默认风格
- [ ] 没有 tiny uppercase tracked eyebrow 放在每个 section 标题上面
- [ ] 禁止使用大间距 p-6 以上（浪费数据展示空间）
- [ ] 禁止使用大圆角 rounded-xl 以上（占用像素）
- [ ] 禁止使用大字号 text-lg 以上作为表格数据
- [ ] 禁止使用装饰性渐变或阴影（增加视觉噪音）
- [ ] 禁止隐藏重要操作在下拉菜单中（常用操作必须直接可见）