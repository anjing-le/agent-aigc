# UI Design Guide

本文档定义 Anjing 脚手架的默认 UI 气质：极简、清晰、线条感、轻玻璃。它不是视觉灵感板，而是后续页面可复用、可维护的界面约束。

## 设计原则

- Less is more：只展示完成当前任务必需的信息。
- 界面先服务工作流，再服务装饰；不要用大段说明解释页面怎么用。
- 次级信息优先 hover、tooltip、popover、抽屉或详情页承载。
- 线条优先使用 dashed，边界轻，不制造厚重卡片感。
- 玻璃质感必须保证可读性：透明度、模糊和阴影只增强层次，不牺牲文字对比。
- 页面区域不套娃卡片；卡片只用于重复项、弹窗、表格容器或真正需要框定的工具。

## 视觉基线

- 背景：低饱和灰白或暗灰，允许细网格，但不使用大面积渐变装饰。
- 表面：使用 `--default-glass-surface`、`--default-glass-strong-surface`、`--default-glass-subtle-surface`。
- 边框：使用 `--default-glass-border` 或 `--default-border-dashed`，边框样式优先 `border-dashed`。
- 模糊：使用 `--default-glass-filter`，不要在组件里散落不同的 blur 数值。
- 圆角：常规工具和卡片不超过 8px，除非已有组件规范要求。
- 主色：只作为状态和动作提示，不把页面做成单一色系。

## 组件约束

- 表单：输入项少文案，placeholder 和校验足够时不额外堆描述。
- 按钮：明确命令用文字，工具型操作优先图标 + tooltip。
- 登录页：居中、轻玻璃、少文字；角色、忘记密码、注册、游客访问等次级操作允许 hover 展开或 tooltip 呈现。
- 表格页：密度优先，筛选、操作和状态应方便扫描，不做营销式大 hero。
- 弹窗/抽屉：只放完成任务需要的字段，复杂说明放到 tooltip 或文档链接。

## 样式入口

- 玻璃、虚线和灰度 token 定义在 `frontend/src/assets/styles/core/tailwind.css`。
- 全局容器、卡片、表格卡片的玻璃效果定义在 `frontend/src/assets/styles/core/app.scss` 和 `frontend/src/assets/styles/core/el-ui.scss`。
- 登录页样式在 `frontend/src/views/auth/login/style.css` 和同目录 `index.vue` scoped style 中维护。

## 防破窗规则

- 新页面不要直接写一套新的玻璃变量；先复用全局 token。
- 新边框默认 dashed；只有强状态、选中态或第三方组件限制时才使用 solid。
- 不新增解释型大段文案；能 hover 的次级信息就 hover。
- 不使用 decorative orb、bokeh blob、大面积紫蓝渐变或高饱和单色主题。
- 移动端先保证不遮挡、不溢出、不靠缩放字体硬撑。

## 验证方式

- `node scripts/check-scaffold-governance.js` 校验 UI 指南、全局玻璃 token、登录页玻璃/虚线入口和 legacy API 边界。
- 前端明显视觉改动后，至少检查桌面和移动端登录页，确认文字不重叠、控件可点击、玻璃表面不影响可读性。
