## MODIFIED Requirements

### Requirement: 应用提供共享主题色 token
系统 SHALL 为首页、搜索页、播放器展开页、播放列表半浮层、更多操作浮层与 minibar 提供共享主题色 token；默认皮肤的浅色方案以 `primary #e53935`、`secondary #616161`、`tertiary #0087a0`、`neutral #f9f9fb` 作为核心基准，深色方案 SHALL 提供语义等价映射。palette SHALL 完整提供生成 Material `ColorScheme` 与产品视觉 token 所需的 surface、content、outline、error 和强调色角色，而不是在 converter、组件或页面内补写默认皮肤常量。

#### Scenario: 浅色方案映射核心语义
- **WHEN** 系统以浅色主题渲染首页、搜索页、播放器展开页、播放列表半浮层、更多操作浮层或 minibar
- **THEN** 主操作与激活态使用 `primary`
- **AND** 次级信息与辅助文字使用 `secondary`
- **AND** 辅助强调或补充状态使用 `tertiary`
- **AND** 大面积背景与浅底容器使用 `neutral` 或其派生中性色
- **AND** surface 及其 content 色来自当前浅色 palette 的完整映射

#### Scenario: 深色方案保持语义等价
- **WHEN** 系统以深色主题渲染同一组页面和通用组件
- **THEN** 系统为主操作、次级信息、辅助强调、中性基底、surface 和 content 提供语义等价的深色映射
- **AND** 不直接把浅色 `neutral`、浅色 surface 或浅色 content 色用于深色大面积背景和普通文字

### Requirement: 应用通过共享主题 contract 暴露默认皮肤
系统 SHALL 通过稳定的共享主题 contract 暴露默认皮肤，使 `app`、`feature-discovery` 与 `feature-player` 使用同一主题源。主题模式与皮肤身份 SHALL 保持为两个独立维度；每个皮肤 SHALL 提供配对的 light/dark palette，并通过统一解析顺序生成 Material scheme 和产品视觉 token。

#### Scenario: 多模块复用同一主题源
- **WHEN** app 主壳层、独立搜索页和播放器宿主分别初始化 Compose 主题
- **THEN** 它们从同一主题选择解析出相同的默认皮肤与 light/dark color scheme
- **AND** 页面级 theme wrapper 只负责各自的 typography 或局部 token 组合，不复制核心色板或重新解析用户模式

#### Scenario: 后续新增皮肤无需回到页面重写颜色
- **WHEN** 后续需要新增另一套品牌皮肤
- **THEN** 系统通过为稳定皮肤标识注册配对的 light/dark palette 完成主题接入
- **AND** 现有主题模式继续只决定明暗变体而不承载皮肤身份
- **AND** 首页、搜索页、播放器和播放列表的页面代码不需要新增皮肤分支或颜色常量

#### Scenario: 未知皮肤安全回退默认皮肤
- **WHEN** 主题解析收到当前版本不认识的皮肤标识
- **THEN** 系统使用默认皮肤完成 light/dark 解析
- **AND** 页面继续通过相同语义 token 正常渲染

### Requirement: 页面通过语义 token 复用共享主题
系统 SHALL 通过 Material `ColorScheme`、稳定的产品语义 token 和由其派生的 feature-local token 复用共享主题，以保证首页、搜索页、播放器和播放列表中的搜索框、banner、结果列表、minibar、通用 sheet 与高亮项保持一致的视觉语言。普通页面和共享组件不得直接读取默认 light/dark palette，也不得为受主题控制的背景、容器、文字、图标、分隔、操作或状态语义新增固定色值。

#### Scenario: 主强调色仅用于关键操作与激活状态
- **WHEN** 页面渲染主操作按钮、当前选中类型、当前播放项或显式高亮入口
- **THEN** 系统使用当前皮肤的同一主强调语义突出这些元素
- **AND** 不让多个高饱和色同时竞争主视觉注意力

#### Scenario: 中性色用于容器与弱分隔
- **WHEN** 页面渲染搜索框、榜单容器、胶囊底栏、sheet 背景、minibar 或弱分隔线
- **THEN** 系统使用当前明暗变体的中性色与派生层级表达容器关系
- **AND** 文本、图标与分隔从同一 scheme 或产品 token 获取足够可读的 content 色

#### Scenario: 深色通用浮层不再使用浅色色板
- **WHEN** 深色主题渲染播放列表浮层、更多操作浮层或 minibar
- **THEN** 这些通用容器使用当前深色 scheme 和产品视觉 token
- **AND** 不直接读取默认浅色 palette 或固定白色 surface

#### Scenario: 内容对比色保留明确例外边界
- **WHEN** 播放器沉浸式背景、封面叠字、图片遮罩或详情 hero 需要与内容保持固定对比
- **THEN** 系统允许该内容视觉继续使用明确命名和验证过的固定对比色
- **AND** 该例外不得被复用于普通页面 surface、文字、图标或分隔语义
