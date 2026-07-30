# 播放队列视觉验收

## 目标

- 目标稿：`/Users/wxy/.codex/generated_images/019f9eb0-e1e5-7e71-8be9-9441d7cb5f0c/call_Zw3E7t6MxnYtTJhiPuE8r609.png`
- 实现位置：`feature-player/src/main/java/com/wxy/playerlite/feature/player/ui/components/PlaylistSheet.kt`

## 验收环境

- 设备：Pixel 9 Pro 模拟器
- 视口：1280 × 2856 px
- 密度：480 dpi
- APK：`app/build/outputs/apk/debug/app-debug.apk`

## 实现证据

- 浏览态：`/tmp/playerlite-playlist-browse-qa-final.png`
- 悬浮菜单：`/tmp/playerlite-playlist-menu-qa.png`
- 排序态：`/tmp/playerlite-playlist-sort-qa-final.png`
- 目标稿与浏览态对比：`/tmp/playerlite-playlist-compare-browse.png`
- 目标稿与悬浮菜单对比：`/tmp/playerlite-playlist-compare-menu.png`
- 目标稿与排序态对比：`/tmp/playerlite-playlist-compare-sort.png`

## 状态覆盖

- 浏览态只显示歌曲信息和三点菜单，不显示拖拽手柄。
- 三点菜单以浮层展示，不改变当前歌曲项和后续歌曲项的位置。
- 排序态显示拖拽手柄，同时保留三点菜单，头部操作切换为“完成”。
- 当前播放项使用浅红背景、红色标题和均衡器图标。
- “移出播放队列”使用危险操作红色。

## 对比与修正记录

1. 第一轮发现常规手机宽度下头部操作分成两排，已改为宽屏单排、窄屏自适应堆叠。
2. 第二轮发现当前歌曲已在可见区域时仍执行自动滚动，造成首行裁切；已改为仅在当前歌曲不在可见区域时滚动。
3. 最终并排对比确认：圆角、列表密度、分隔线、当前项层级、悬浮菜单和排序入口均符合目标方向，没有裁切、重叠或行位移。

## 自动化验证

- `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- 结果：`BUILD SUCCESSFUL`

## 最终结果

passed
