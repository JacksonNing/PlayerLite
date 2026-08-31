## Context

应用已经在 `design-system` 中建立 `PlayerLiteBrandPalettes(light, dark)`、Material `ColorScheme` 和 `PlayerLiteVisualTokens`，`app` 与 `feature-discovery` 也都有主题 wrapper。但生产入口目前只接收 `darkTheme: Boolean` 并默认调用 `isSystemInDarkTheme()`，没有用户主题模式、持久化状态或跨 Activity 的单一真相源。

应用由多个独立 `ComponentActivity.setContent` 根组成；搜索页位于 `feature-discovery`，通过 `SearchHostDependencies` 从 app 获取宿主依赖。`CompositionLocal` 只能在单个 composition 内传播，无法自行保证 Main、Search、Player、Settings 和各详情页使用同一主题选择。

现有主题 contract 仍有两类泄漏：一是 `ColorScheme` 转换和 `surfaceRaised` 内部保留固定色值，未来皮肤不能完整替换 palette；二是播放列表浮层、更多操作浮层、minibar 和少数普通页面绕过共享 token，直接读取浅色色板、系统模式或硬编码颜色。播放器展开页、详情 hero 和封面叠字中的固定深色/白色则属于有意的内容对比体系，需要与普通容器区分。

项目现有偏好存储使用 SharedPreferences，没有 DataStore 或 AppCompat DayNight 依赖；播放服务运行在独立 `:playback` 进程，主题属于 app UI 进程的展示状态，不应进入播放进程或跨进程契约。

## Goals / Non-Goals

**Goals:**

- 提供 `SYSTEM`、`LIGHT`、`DARK` 三态主题模式，默认 `SYSTEM`，并在 Activity 重建和进程重启后恢复。
- 让所有 app Activity 根与独立搜索页消费同一个主题选择，并在设置变更后即时重组，无需手动重建 Activity。
- 让普通页面、通用 sheet 和 minibar 通过 Material 语义色或 `PlayerLiteVisualTokens` 获取主题相关颜色。
- 将皮肤身份与明暗模式建模为正交维度，本期落下默认皮肤和静态解析边界，使未来新增皮肤无需改写页面。
- 统一普通页面的系统栏明暗策略，同时保留 hero、歌曲详情和播放器按内容亮度覆盖的能力。
- 保持现有默认行为和数据兼容；未知或损坏的偏好值安全回退。

**Non-Goals:**

- 本期不提供第二套皮肤、皮肤选择器、`skin_id` 持久化、远程皮肤、动态插件注册或 palette 下载。
- 不把播放器展开页、详情 hero、封面叠字和 scrim 等有意固定的内容对比色机械改为随主题变化。
- 不引入 DataStore、AppCompatDelegate、第三方主题框架，也不重写完整设计系统。
- 不修改播放服务、网络 API、数据库 schema 或跨进程播放契约。

## Decisions

### 1. 共享层定义 ThemeSelection，模式与皮肤保持正交

在 `design-system` 放置无 Android 存储依赖的纯主题模型：

```kotlin
enum class ThemeMode(val wireValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

@JvmInline
value class SkinId(val value: String) {
    companion object {
        val Default = SkinId("default")
    }
}

data class ThemeSelection(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val skinId: SkinId = SkinId.Default
)
```

`ThemeMode.resolveDarkTheme(systemDark)` 是唯一的明暗解析规则：`SYSTEM` 使用系统值，`LIGHT` 固定为 false，`DARK` 固定为 true。持久化使用稳定的 `wireValue`，不保存 enum ordinal 或 `name`；空值和未知值回退 `SYSTEM`。

同时定义不可变的 `PlayerLiteSkin(id, palettes)` 和静态 `PlayerLiteSkinCatalog`。本期 catalog 只有默认皮肤，未知 `SkinId` 回退默认皮肤；未来新增内置皮肤只需增加 catalog entry。解析顺序固定为：

```text
ThemeSelection
  ├─ mode + systemDark → resolvedDarkTheme
  └─ skinId → PlayerLiteSkin → light/dark palette
      → Material ColorScheme → PlayerLiteVisualTokens → feature/page tokens
```

选择静态 catalog 而不是可变全局 registry，是因为本期没有动态皮肤来源；它已经验证 skin × mode 边界，又不会提前引入插件生命周期、并发注册和远程资源管理。

### 2. app 持有唯一的 ThemePreferencesRepository

在 app 层新增 `ThemePreferencesRepository`，由 `AppContainer.Services` 持有唯一实例。repository 使用独立的 `theme_preferences` SharedPreferences，对外暴露只读 `StateFlow<ThemeSelection>` 和当前同步快照，并提供 `setThemeMode(mode)`。

本期只持久化 `theme_mode`；`ThemeSelection.skinId` 始终为 `SkinId.Default`。未来换肤 change 可以增加 `skin_id` 和 `setSkin()`，而无需改变主题消费者、Search host contract 或页面 token 调用方式。

repository 构造时同步读取偏好并初始化 StateFlow；写入模式后更新同一条 flow。缺失 key 等价于 `SYSTEM`，非法值回退 `SYSTEM`。主题偏好不复用 `player_playback_preferences`，避免 presentation 状态进入独立播放进程。

选择 SharedPreferences 而不是 DataStore，是因为这里只存一个小型枚举，仓库已有 SharedPreferences + StateFlow 模式，且无需新增依赖。repository 隔离了存储实现，未来需要时可在不改变上层 contract 的情况下迁移。

### 3. 生产根入口收集全局选择，纯主题 renderer 保持可测试

保留 `PlayerLiteTheme(darkTheme, palettes)` 和 `SearchFeatureTheme(darkTheme, palettes)` 作为纯 renderer，测试和 Preview 可以显式传入确定值。app 新增 `PlayerLiteAppTheme`：

1. 收集 `ThemePreferencesRepository.selectionFlow`；
2. 只在此边界调用 `isSystemInDarkTheme()`；
3. 解析 `ThemeSelection` 得到 `resolvedDarkTheme` 与默认 skin palette；
4. 调用纯 renderer 注入 Material scheme 与视觉 token。

Main、Settings、Player、Login、Local、Liked、Recent、Song、WebImport 以及 `BasePlaybackDetailActivity` 等 app 根入口统一改用 `PlayerLiteAppTheme`。

Search 不反向依赖 app。`SearchHostDependencies` 增加共享的 `StateFlow<ThemeSelection>`，由 `PlayerLiteApplication` 传入同一个 repository flow；`SearchActivity` 收集后调用纯 `SearchFeatureTheme`。`feature-player` 没有独立 Activity 根，继续从 app composition 消费共享 token。

设置切换后由 StateFlow 触发各活跃 composition 重组，不调用 `Activity.recreate()`。系统配置变化时，`SYSTEM` 重新解析系统明暗；显式 `LIGHT/DARK` 不得被页面内的 `isSystemInDarkTheme()` 覆盖。

只使用 CompositionLocal 作为方案被否决，因为它不能跨多个 Activity；直接使用 AppCompatDelegate 也被否决，因为会引入新依赖、强制 Activity 重建，并扩大对播放器沉浸式视觉和平台资源主题的影响。

### 4. palette 负责可换肤基值，页面只消费语义 token

扩展 `PlayerLiteBrandPalette`，覆盖当前仍隐藏在 converter 或 visual token 里的皮肤相关值，至少包括 surface/surfaceRaised 以及 `onPrimary`、`onSecondary`、`onTertiary`、`onError`。默认皮肤显式给出 light/dark 两套完整值；`ColorScheme` 与 `PlayerLiteVisualTokens` 不再依赖固定默认皮肤常量。

颜色使用分为三层：

1. `PlayerLiteBrandPalette`：皮肤作者使用的基值，只在 design-system 的解析层出现。
2. Material `ColorScheme` 与 `PlayerLiteVisualTokens`：普通页面和共享组件使用的稳定语义角色。
3. feature-local tokens：仅在确有局部语义时存在，并且必须从共享 scheme/token 派生。

页面和共享组件不得直接读取 `DefaultBrandPalettes.light/dark`，也不得为背景、容器、文本、图标、分隔、操作或状态语义新增固定 hex。迁移时按以下规则分类：

- 普通 surface、文字、图标、分隔、操作和可复用状态色：迁移到共享语义 token。
- feature 内重复出现的局部角色：建立由共享 token 派生的 feature token。
- 封面叠字、图片 scrim、播放器沉浸式背景等内容对比色：允许保留固定值，并通过命名或测试明确其例外语义。

本期优先修正已确认的泄漏：Playlist sheet、Player more-actions sheet、Shared minibar、Recent 页面直接系统判断，以及 Player 退出时按系统模式恢复系统栏。不会做全仓库 `Color.White/Black` 机械替换。

直接把 palette 暴露给页面的方案被否决，因为会让未来每套皮肤继续渗入业务 UI；复制完整 Material 3 全角色 palette 也被否决，本期只补当前 contract 实际需要且会随皮肤变化的角色。

### 5. 设置页只投影全局状态，不创建第二真相源

`SettingsUiState` 增加 `SettingsAppearanceUiState(themeMode)`。`SettingsViewModel` 注入同一个 `ThemePreferencesRepository`，收集 selection flow 并投影到设置 UI；`updateThemeMode(mode)` 只调用 repository，不再维护独立的主题 MutableStateFlow。

设置页在账户入口之后、播放与缓存之前增加“外观”分组，主题模式行展示当前值并打开三项单选界面。使用单选而不是夜间模式开关，是因为开关无法表达“跟随系统”第三态。皮肤入口本期不展示。

### 6. 系统栏按 resolved theme 恢复，内容驱动页面保留覆盖权

普通页面使用统一 helper，根据 `resolvedDarkTheme` 设置 status/navigation bar 图标明暗，并与 edge-to-edge 背景保持一致。helper 可在 Activity `onCreate` 早期使用 repository 当前快照初始化，在 composition 中随 flow 更新。

优先级如下：

1. 普通页面：resolved theme；
2. 详情 hero、歌曲详情、播放器：按实际 backdrop/content luminance 覆盖；
3. 覆盖结束或离开页面：恢复 resolved theme，而不是重新读取系统主题。

本期不设置 `configChanges` 绕过系统 uiMode 重建，也不全量改造 XML `values-night`。`Theme.PlayerLite` 的启动窗口仍可能在“用户强制深色、系统浅色”时短暂不一致，必须真机观察；若可见且不可接受，再做窄范围启动窗口方案，不能以引入 AppCompat 全局夜间模式作为默认解法。

## Risks / Trade-offs

- [多 Activity 根入口遗漏] → 建立入口清单并覆盖 Main、Search、Player、Settings、各独立页和详情基类；验证用户模式与系统模式相反时仍保持一致。
- [设置页与全局主题形成两个状态源] → repository 是唯一可写源，SettingsViewModel 只投影 selection flow。
- [Search 使用另一套偏好] → 通过既有 host dependencies 传入同一 flow，feature 不直接访问 app 存储。
- [未来皮肤仍被 converter 固定色限制] → 将 converter/visual tokens 中会随皮肤变化的固定值纳入 palette，并用自定义测试 palette 验证端到端传播。
- [把内容对比色误判为主题泄漏] → 按“普通 UI 语义色 / 内容对比色”分类迁移，播放器和 hero 使用显式例外清单与回归测试。
- [系统栏策略互相覆盖] → 定义普通主题、内容亮度覆盖、退出恢复的明确优先级，同时覆盖 status 与 navigation bar。
- [启动窗口短暂浅色] → 在 onCreate 尽早应用同步快照并做真机验证；只有观察到问题后才增加启动窗口资源方案。
- [偏好损坏或未来版本值不兼容] → 稳定 wire value、未知值回退 `SYSTEM`/默认 skin，不保存 enum ordinal。
- [为未来换肤过度设计] → 本期仅实现不可变默认 skin 与静态 catalog，不实现动态 registry、皮肤资源或选择持久化。

## Migration Plan

1. 在 design-system 增加 `ThemeMode`、`SkinId`、`ThemeSelection`、默认 `PlayerLiteSkin` 和纯解析测试；扩展 palette 角色，但先保持默认渲染结果不变。
2. 在 app 新增 SharedPreferences-backed repository，并注册到 `AppContainer.Services`；缺失偏好默认 `SYSTEM`。
3. 增加 `PlayerLiteAppTheme`，逐一迁移 app Activity 根；通过 `SearchHostDependencies` 把相同 selection flow 传给搜索页。
4. 在 Settings state/ViewModel/UI 增加外观分组和三态选择，验证切换后的即时传播、Activity 重建和进程重启恢复。
5. 迁移已确认的固定浅色消费者和直接系统主题判断，补齐默认 skin 的 light/dark 语义 token。
6. 统一普通页面系统栏恢复逻辑，并保留 hero、歌曲详情和播放器的内容亮度覆盖。
7. 运行 design-system、feature-discovery、app 定向测试，以及项目要求的 playback-service/app 单测和 app assemble；在真机覆盖系统明暗与用户选择相反、手势/三键导航、横竖屏和 Activity 跳转。

该变更没有旧数据迁移：未保存 `theme_mode` 的安装自然落到 `SYSTEM`，保持当前行为。回滚时旧版本会忽略新的独立偏好文件；若需要紧急关闭功能，可隐藏设置入口并让 resolver 固定返回 `SYSTEM`，无需删除用户数据或修改播放偏好。

## Open Questions

- 真机上“用户强制 DARK、系统 LIGHT”的启动窗口是否出现可感知闪白；只有存在稳定证据时才决定是否补充最小启动窗口资源策略。
- 首页快捷入口的 pastel 背景应作为普通 surface 提供 dark variant，还是作为内容装饰色保留；specs 阶段需要按现有 homepage 行为明确边界，避免实现阶段临时判断。
