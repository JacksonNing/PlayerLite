## 1. 建立主题模式与皮肤 contract

- [x] 1.1 在 `design-system` 定义 `ThemeMode`、稳定 wire value、`SkinId`、`ThemeSelection` 和明暗解析函数，并用单元测试覆盖三种模式、系统明暗组合及非法值回退。
- [x] 1.2 定义不可变的 `PlayerLiteSkin` 与仅包含默认皮肤的静态 catalog，验证默认/未知 `SkinId` 均能解析到可用的 light/dark palette。
- [x] 1.3 扩展 `PlayerLiteBrandPalette`，补齐 surface、raised surface 和各强调/错误色的 content 角色，移除 `ColorScheme` converter 与 visual token 中依赖默认皮肤的固定色值。
- [x] 1.4 扩展 design-system 主题测试，验证默认皮肤渲染保持兼容，并使用自定义测试 palette 证明 light/dark scheme 和产品视觉 token 可完整替换。

## 2. 建立唯一主题偏好状态源

- [x] 2.1 在 app 层实现 `ThemePreferencesRepository` contract 与 SharedPreferences-backed 实现，使用独立 `theme_preferences/theme_mode`，对外提供同步快照和只读 `StateFlow<ThemeSelection>`。
- [x] 2.2 为 repository 增加测试，覆盖缺失值默认 `SYSTEM`、三种模式 round-trip、非法值回退、相同值幂等和新实例恢复已保存选择。
- [x] 2.3 将 repository 注册为 `AppContainer.Services` 的进程内唯一实例，并为 Activity 根、SettingsViewModel 和 Search host 提供同一实例或同一 selection flow。

## 3. 统一所有 Compose 根入口

- [x] 3.1 新增 `PlayerLiteAppTheme`，收集全局 selection flow，在唯一边界结合 `isSystemInDarkTheme()` 解析 mode 与默认 skin，并保留可显式传入 resolved 值的纯 `PlayerLiteTheme` 测试入口。
- [x] 3.2 将 Main、Settings、Player、Login、Local、Liked、Recent、Song、WebImport 和 `BasePlaybackDetailActivity` 等 app `setContent` 根迁移到 `PlayerLiteAppTheme`，核对没有独立 Activity 继续自行解析系统主题。
- [x] 3.3 扩展 `SearchHostDependencies` 传递同一 `StateFlow<ThemeSelection>`，让 SearchActivity/SearchFeatureTheme 使用相同解析结果且不反向依赖 app 存储。
- [x] 3.4 增加 app 与 feature-discovery 主题测试，验证用户模式与系统模式相反时，各根入口仍暴露一致的 Material scheme 和产品 token。

## 4. 在设置页提供主题模式选择

- [x] 4.1 为 `SettingsUiState` 增加 `SettingsAppearanceUiState`，向 SettingsViewModel 注入全局 repository，并让 ViewModel 只投影 selection flow 和转发 `setThemeMode`。
- [x] 4.2 在账户入口之后增加“外观”分组与“日间 / 夜间模式”行，展示当前值，并通过三项单选界面提供“跟随系统”“日间模式”“夜间模式”选择。
- [x] 4.3 扩展 SettingsViewModel 与设置页测试，验证初始状态、当前选中项、回调写入、即时重组以及登录/游客状态下外观分组均可见。

## 5. 迁移主题相关颜色到语义 token

- [x] 5.1 核验并保持 Playlist sheet 已有的 Material scheme / `PlayerLiteVisualTokens` 接入，在新的主题模式与默认皮肤 contract 下补齐或更新 light/dark 回归测试，确保不重新引入默认浅色色板。
- [x] 5.2 核验 Player more-actions sheet 在新的主题/皮肤 contract 下继续使用语义 token；将 Shared minibar 的固定白色 surface、content、分隔和进度颜色迁移到当前语义 token，并覆盖 light/dark 测试。
- [x] 5.3 移除 Recent 等普通页面中绕过用户模式的直接系统主题分支或固定主题色，改为消费已解析 scheme/token。
- [x] 5.4 审核普通页面中的主题相关固定色，重点将 `AccountVisualStyle.accent*` 在 Main/User Center 普通 surface 中迁移为共享或 feature-local 派生 token；为 `HomeDiscoveryLayoutSpec.dailyShortcutPalette` 明确 dark-aware palette 或仅限装饰用途的例外边界；保留播放器沉浸式背景、封面叠字、图片 scrim 和详情 hero 的明确例外。
- [x] 5.5 增加针对已确认泄漏的静态或单元回归检查，确保 feature UI 不再直接读取 `DefaultBrandPalettes.light/dark`，且普通 UI 不再通过 `AccountVisualStyle.accent*` 等 feature-local 固定色绕过主题 contract；继续允许内容对比用途的 `Color.White/Black`。

## 6. 统一系统栏与生命周期行为

- [x] 6.1 实现普通页面系统栏 helper，根据 repository 同步快照和 composition 内 resolved theme 设置 status/navigation bar 图标明暗，并接入所有普通 Activity 根。
- [x] 6.2 保留详情 hero、歌曲详情和播放器的内容亮度覆盖策略，修正覆盖结束或 Player dispose 时恢复 resolved theme，而不是重新调用系统主题判断。
- [x] 6.3 增加主题状态生命周期验证，覆盖设置切换即时生效、Activity 重建、打开新 Activity、应用进程重启，以及 `SYSTEM` 响应系统变化而显式模式保持不变。
- [x] 6.4 验证切换主题不会改变播放队列、播放状态、播放服务偏好或跨进程契约。

## 7. 集成验证与真机验收

- [x] 7.1 运行 `./gradlew :design-system:testDebugUnitTest :feature-discovery:testDebugUnitTest`，修复主题 contract、Search 传播和 light/dark token 回归。
- [x] 7.2 运行 `./gradlew :playback-service:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug`，确认项目要求的回归、打包和独立播放进程验证通过。
- [ ] 7.3 在真机覆盖系统浅/深色与三种用户模式的组合，逐一检查 Main、Search、Settings、Player、详情页和其他独立 Activity，以及横竖屏、手势导航和三键导航下的系统栏与 inset。
- [ ] 7.4 验证播放器展开页、详情 hero 和封面叠字仍保持预期沉浸式对比，同时 Playlist sheet、More-actions sheet 和 minibar 正确跟随当前主题。
- [ ] 7.5 观察“系统浅色 + 用户强制深色”的冷启动窗口；只有稳定复现可感知闪白时，基于现场证据增加最小启动窗口修正并重新执行真机回归。
