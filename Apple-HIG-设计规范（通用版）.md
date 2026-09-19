# Apple HIG 设计规范（通用版）

> 用途：把 Apple Human Interface Guidelines 落成一套**可直接粘贴、跨项目复用**的设计规范。
> 适用：Web / uni-app / 小程序 / React Native / 原生 iOS 均可，按 §6 换单位与字体即可。
> 用法三步：① 复制 §3.7 的 token 块进全局样式 → ② 按 §2、§4 出界面 → ③ 提交前过 §8 清单。

---

## 1. 三条适配前提（照搬 HIG 前必须先决定）

| 维度 | HIG 原生 | 通用做法 |
|---|---|---|
| **单位** | pt | 以 375pt 设计宽为基准，Web 用 px/rem，rpx 体系用 **1pt = 2rpx**。全项目统一一种，别混 |
| **字体** | SF Pro | iOS/macOS 用 `-apple-system` 即天然 SF；其他平台走系统字体。**不要打包字体文件**（体积 + 授权） |
| **材质** | 毛玻璃 | **默认实色**。仅在平台支持且提供降级时才用 `backdrop-filter`，绝不作为视觉依赖 |

---

## 2. 四原则 → 执行要点

| 原则 | 怎么做 |
|---|---|
| **Clarity 清晰** | 一屏一主题；字号只分 4 档（大标题/标题/正文/说明），靠**字重**而非无限加字号造层级 |
| **Deference 尊重内容** | 内容为主角：图大字少、去边框、少渐变；chrome 越少越好 |
| **Depth 层次** | 用**同心圆角 + 轻阴影**表达层级，不靠重色块；弹层遮罩统一 `rgba(0,0,0,.4)` |
| **Consistency 一致** | 全部走 token，禁止写死色值/字号；同一语义只有一种颜色 |

---

## 3. 设计 Token

### 3.1 颜色（Light）

| 语义 | 值 |
|---|---|
| 主色 Primary | `#007AFF` |
| 成功 Success | `#34C759` |
| 警告 Warning | `#FF9500` |
| 危险 Danger | `#FF3B30` |
| 信息 Info | `#5856D6` |
| 强调 Pink | `#FF2D55` |
| 文本-主 | `#000000`（中文长文可用 `#333333`，见 §7） |
| 文本-次 | `rgba(60,60,67,.60)` |
| 文本-辅 | `rgba(60,60,67,.30)` |
| 文本-占位 | `rgba(60,60,67,.18)` |
| 背景-主 | `#FFFFFF` |
| 背景-次（分组底） | `#F2F2F7` |
| 分割线 | `rgba(60,60,67,.29)` |

### 3.2 颜色（Dark）

| 语义 | 值 |
|---|---|
| 主色 | `#0A84FF` |
| 成功 | `#30D158` |
| 警告 | `#FF9F0A` |
| 危险 | `#FF453A` |
| 信息 | `#5E5CE6` |
| 文本-主 | `#FFFFFF` |
| 文本-次 | `rgba(235,235,245,.60)` |
| 文本-辅 | `rgba(235,235,245,.30)` |
| 背景-主 | `#000000` |
| 背景-次 | `#1C1C1E` |
| 分割线 | `rgba(84,84,88,.60)` |

### 3.3 字号阶（pt / px / rpx）

| 名称 | pt | px | rpx | 用途 |
|---|---|---|---|---|
| Large Title | 34 | 34 | 68 | 页面大标题 |
| Title 1 | 28 | 28 | 56 | 页面主标题 |
| Title 2 | 22 | 22 | 44 | 区块标题 |
| Title 3 | 20 | 20 | 40 | 卡片大标题 |
| Body / Headline | 17 | 17 | 34 | **正文默认**（Headline 同尺寸 + 600 字重） |
| Subheadline | 15 | 15 | 30 | 副标题 |
| Footnote | 13 | 13 | 26 | 说明 |
| Caption 1 | 12 | 12 | 24 | 标签/时间 |
| Caption 2 | 11 | 11 | 22 | 最小，仅角标 |

字重只用 400 / 500 / 600；行高 1.2–1.5。

### 3.4 间距（8pt 网格）

| pt | 4 | 8 | 12 | 16 | 20 | 24 | 32 | 40 | 48 |
|---|---|---|---|---|---|---|---|---|---|
| px | 4 | 8 | 12 | 16 | 20 | 24 | 32 | 40 | 48 |
| **rpx** | 8 | 16 | 24 | 32 | 40 | 48 | 64 | 80 | 96 |

- 页面左右边距 **16pt**；卡片间距 16–24rpx / px；区块间距 40–64。

### 3.5 圆角（同心规则）

规则：**内圆角 + 内边距 = 外圆角**（嵌套时保持视觉同心）。

| 用途 | pt | rpx |
|---|---|---|
| 小组件 | 8 | 16 |
| 卡片（标准） | 12–16 | 24–32 |
| 大容器 / 抽屉顶部 | 20–24 | 40–48 |
| 胶囊 | 9999 | 9999 |

### 3.6 动效

| 用途 | 时长 | 缓动 |
|---|---|---|
| 按压反馈 | 100ms | `cubic-bezier(0,0,.58,1)` |
| 悬停/聚焦 | 200ms | 同上 |
| 常规过渡 | 300ms | `cubic-bezier(.25,.1,.25,1)` |
| 复杂动画 | 500ms | 同上 |

- 按压统一 `transform: scale(.97)`。
- 必须支持减弱动效：`@media (prefers-reduced-motion: reduce)`。

### 3.7 可直接粘贴的 token 块

```css
:root {
  /* 颜色 */
  --color-primary: #007AFF;
  --color-success: #34C759;
  --color-warning: #FF9500;
  --color-danger:  #FF3B30;
  --color-info:    #5856D6;
  --color-pink:    #FF2D55;

  --label-primary:   #000000;
  --label-secondary: rgba(60, 60, 67, 0.60);
  --label-tertiary:  rgba(60, 60, 67, 0.30);
  --label-quaternary:rgba(60, 60, 67, 0.18);

  --bg-primary:   #FFFFFF;
  --bg-secondary: #F2F2F7;
  --separator:    rgba(60, 60, 67, 0.29);

  /* 字体 */
  --font-system: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text",
                 "Helvetica Neue", "PingFang SC", "Microsoft YaHei", sans-serif;

  /* 字号（px；rpx 体系 ×2） */
  --text-large-title: 34px;  --text-title1: 28px; --text-title2: 22px;
  --text-title3: 20px;       --text-body: 17px;   --text-subhead: 15px;
  --text-footnote: 13px;     --text-caption1: 12px; --text-caption2: 11px;

  /* 间距（8pt 网格） */
  --space-1: 4px;  --space-2: 8px;  --space-3: 12px; --space-4: 16px;
  --space-5: 20px; --space-6: 24px; --space-8: 32px; --space-10: 40px; --space-12: 48px;

  /* 圆角 */
  --radius-sm: 8px; --radius-md: 12px; --radius-lg: 16px;
  --radius-xl: 20px; --radius-2xl: 24px; --radius-full: 9999px;

  /* 阴影 */
  --shadow-card: 0 1px 3px rgba(0,0,0,.06), 0 4px 12px rgba(0,0,0,.04);
  --shadow-pop:  0 8px 32px rgba(0,0,0,.16);

  /* 动效 */
  --ease-default: cubic-bezier(.25,.1,.25,1);
  --ease-out:     cubic-bezier(0,0,.58,1);
  --duration-fast: 200ms; --duration-normal: 300ms; --duration-slow: 500ms;

  /* 触控区 */
  --touch-min: 44px; /* rpx 体系用 88rpx */
}

@media (prefers-color-scheme: dark) {
  :root {
    --color-primary: #0A84FF; --color-success: #30D158; --color-warning: #FF9F0A;
    --color-danger: #FF453A;  --color-info: #5E5CE6;

    --label-primary:   #FFFFFF;
    --label-secondary: rgba(235,235,245,.60);
    --label-tertiary:  rgba(235,235,245,.30);
    --label-quaternary:rgba(235,235,245,.18);

    --bg-primary:   #000000;
    --bg-secondary: #1C1C1E;
    --separator:    rgba(84,84,88,.60);
  }
}

@media (prefers-reduced-motion: reduce) {
  * { animation-duration: .01ms !important; transition-duration: .01ms !important; }
}
```

---

## 4. 组件规范（平台无关）

| 组件 | 关键规则 |
|---|---|
| **主按钮** | 胶囊形，最小高度 44pt/88rpx，主色底白字 600 字重，按压 `scale(.98)` |
| **次按钮** | 同胶囊；主色文字 + `rgba(0,122,255,.1)` 底 |
| **卡片** | 白底（深色模式用 `--bg-secondary`）、标准圆角、轻阴影；分组卡=外层灰底 + 内层白条 + 分割线 |
| **标签栏 TabBar** | 固定底部，高 49pt/98rpx + 安全区；**必须带文字标签**（禁止纯图标）；选中实心图标、未选中线性；角标红胶囊 |
| **导航栏** | 高 44pt/88rpx；返回为圆形纯图标；右侧最多 1 组操作；主操作放右侧并用强调样式 |
| **列表/单元** | 行高 ≥ 44pt/88rpx；分组内分割线起点 60pt/120rpx 内缩；有下一级用 `>` 指示符 |
| **输入框** | 最小高度 44pt/88rpx，圆角 12px/24rpx；聚焦外圈 `0 0 0 4px rgba(0,122,255,.3)` |
| **抽屉/弹层** | 档位取 50% / 100%；顶部抓手 36×5pt 居中；遮罩 `rgba(0,0,0,.4)` |
| **操作表** | 破坏性操作放最后并标红；取消独立成组 |
| **提示框** | 仅用于重要确认；按钮 ≤ 3 个；用动词文案，不用"确定/OK" |
| **空态** | 统一插画 + 一句说明 + 一个动作按钮，不写裸文案 |
| **骨架屏** | 首屏加载必须有，避免白屏 |

---

## 5. 无障碍（不可省）

- **对比度**：正文 ≥ 4.5:1，大字 ≥ 3:1。（`#999` 这类灰在浅底上通常不达标，只能用于非关键信息。）
- **触控区**：≥ 44pt / 88rpx，视觉元素可小，外层补 padding。
- **图标**：装饰性加 `aria-hidden`，功能性给 `aria-label` 或配可见文字。
- **字号缩放**：不锁死行高与容器高度，系统大字号下关键信息不被截断。
- **动效**：支持 `prefers-reduced-motion`。

---

## 6. 跨端适配对照

| 目标平台 | 单位 | 字体 | 注意 |
|---|---|---|---|
| **Web / H5** | px / rem | `-apple-system` 字体栈 | 毛玻璃可用，但需 `@supports` 降级 |
| **uni-app** | rpx（1pt = 2rpx） | 不引入字体文件，走系统 | 用条件编译区分 App / 小程序；`env(safe-area-inset-bottom)` 补底部安全区 |
| **微信小程序** | rpx | 系统字体 | 无 `backdrop-filter`，不用毛玻璃；自定义 tabBar 需自己处理安全区 |
| **React Native** | dp（= pt） | `Platform.select` | 用 `PlatformColor` 或自定义主题；`SafeAreaView` 处理刘海 |
| **原生 iOS / SwiftUI** | pt | SF Pro + Dynamic Type | 直接用系统语义色与 `Material`，不要重造 |

**通例**：把 §3.7 的变量改名映射到目标框架的主题层，页面代码只引用语义名（`--color-primary`），不引用具体色值。

---

## 7. 反模式（明确别做）

1. **毛玻璃当默认** —— 跨端不一致、性能差，只做可选增强。
2. **无限字号** —— 层级靠字重与间距，档位固定。
3. **纯图标标签栏** —— 认知成本高，必须有文字。
4. **纯黑正文**（中文长文）—— 用 `#333` 更耐读，这是对 HIG 的合理偏离，需全项目统一。
5. **写死色值/字号** —— 一律走 token，否则主题与深色模式无法落地。
6. **锁死容器高度** —— 系统大字号会截断内容。
7. **破坏性操作二次确认里用"确定"** —— 用具体动词（"删除""放弃修改"）。

---

## 8. 交付前检查清单

- [ ] 颜色全部走语义 token，无写死色值
- [ ] 字号取自字号阶，无自由字号
- [ ] 间距落在 8pt 网格，单位统一（px 或 rpx，不混用）
- [ ] 卡片圆角符合同心规则
- [ ] 可点元素触控区 ≥ 44pt / 88rpx
- [ ] 底部固定元素处理了安全区
- [ ] 首屏有骨架屏，空数据有空态
- [ ] 正文对比度 ≥ 4.5:1
- [ ] 功能性图标有 `aria-label` 或可见文字
- [ ] 支持深色模式（若产品要求）与减弱动效
- [ ] 标签栏/导航栏符合 §4 尺度
