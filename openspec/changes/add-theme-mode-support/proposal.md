## Why

应用已经具备共享的浅色与深色色板，但当前主题入口只跟随系统，部分页面和浮层仍直接使用固定浅色色值，导致深色环境下的视觉与可读性不一致。夜间模式需要先建立完整、统一的语义颜色使用边界，再提供用户可选择且可恢复的主题模式，避免在各页面分别维护主题状态和临时色值。

## What Changes

- 新增“跟随系统、浅色、深色”三种主题模式；默认保持跟随系统，并持久化用户选择，使模式在 Activity 重建和应用重启后恢复。
- 建立单一主题模式来源，并将解析后的浅色或深色状态一致传播到 `app`、`feature-discovery`、`feature-player` 的 Compose 根入口和共享组件。
- 扩展共享主题 contract 与语义颜色 token，统一受主题影响的背景、容器、文本、图标、分隔和强调色入口，补齐 light/dark 映射，避免页面继续固定引用浅色色板或硬编码主题相关颜色。
- 将“主题模式”与“皮肤”保持为两个正交维度，并在本阶段规划皮肤标识、palette 注册以及 skin × light/dark 的解析边界；默认皮肤先提供完整的 light/dark 变体，页面继续只消费稳定的语义 token。
- 在设置页新增外观分组或主题模式设置项，展示当前模式，并让用户切换后即时看到结果。
- 为主题模式解析、选择持久化、浅深色 token 映射以及关键页面和浮层补充自动化验证。
- 保留播放器展开页既有的沉浸式深色视觉；其中承担通用容器职责的播放列表浮层、更多操作浮层和 minibar 仍需遵循当前主题，封面叠字等有意固定的内容对比色不在本阶段机械替换。
- 本阶段的 specs/design SHALL 说明未来皮肤选择状态、偏好持久化和新增 palette 的接入路径；本期不交付非默认皮肤资源、用户可见的皮肤选择器或多皮肤持久化行为。

## Capabilities

### New Capabilities

- `theme-mode`: 定义跟随系统、浅色和深色模式的选择、解析、持久化及跨 Activity 和 feature 的一致生效行为。

### Modified Capabilities

- `ui-theme-tokens`: 扩展现有共享主题 contract，补齐常规页面、sheet 和 minibar 的语义颜色 light/dark 映射，要求受主题控制的颜色统一从共享 token 获取，并定义未来皮肤注册 palette、组合明暗变体而无需改写页面的扩展契约。
- `settings-page`: 增加外观相关设置分组或主题模式入口，展示当前选择并支持用户切换主题模式。

## Impact

- 影响 `design-system` 的共享 palette、`ColorScheme` 和视觉 token contract，以及 `app`、`feature-discovery`、`feature-player` 对该 contract 的消费方式。
- 主题模式状态不承载皮肤身份；本阶段需要在 design 中明确未来皮肤模型、palette 注册、偏好存储与迁移边界，确保新增皮肤时不需要迁移现有页面的颜色调用方式。
- 影响应用主题入口、独立 Compose Activity/feature wrapper、设置页 UI state 与本地偏好存储，以及播放列表浮层、更多操作浮层、minibar 等仍固定使用浅色值的共享视觉组件。
- 需要扩展主题、设置持久化和关键视觉消费者测试，覆盖三种模式、重建恢复和 light/dark 语义映射。
- 不改变网络 API、播放服务跨进程契约或数据库 schema，也不引入新的外部依赖。
