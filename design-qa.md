# Login screen design QA

## Target

- Reference: `/Users/wxy/.codex/generated_images/019f9eb0-e1e5-7e71-8be9-9441d7cb5f0c/call_oqCk67AiwqD8ZmCoPx3o18Wy.png`
- Reference size: `853 × 1844 px`
- Selected direction: option 1

## Implementation

- Screenshot: `/tmp/playerlite-login-390x844-2.png`
- Device: Pixel 9 Pro emulator
- Target viewport: `390 × 844 dp`
- State: logged out, phone tab selected, empty fields, password hidden

## Comparison setup

- Comparison image: `/tmp/playerlite-login-design-qa-comparison.png`
- Android status and navigation chrome were cropped from the implementation capture.
- The reference and implementation were normalized to `390 × 844` before being placed side by side.
- Left side is the selected reference; right side is the Compose implementation.

## Findings

- P0: none
- P1: none
- P2: none
- The information hierarchy, red accent, compact tab indicator, outlined inputs, primary action, password visibility control, and privacy hint match the selected direction.
- The Android implementation intentionally keeps the project brand asset and platform typography rendering.
- System safe-area spacing differs from the chrome-free reference but does not introduce clipping, overlap, or broken hierarchy.
- The primary button remains a flat project token color rather than reproducing the generated reference's slight tonal variation.

## Interaction checks

- Phone and email methods remain selectable.
- Password visibility toggles between hidden and visible states.
- Skip and login actions remain enabled according to the existing state model.
- Long and keyboard-visible content remains scrollable.

## Comparison history

1. Initial implementation used a full-width selected-tab underline and had no password visibility action.
2. The selected indicator was reduced to `48 dp`, the visibility action was added, and the privacy icon was aligned with the reference.
3. The final implementation was recaptured at the target viewport and compared side by side with option 1.

final result: passed

# 最近播放页（方案 3）Design QA

## Comparison target

- Source visual truth path: `/Users/wxy/.codex/generated_images/01a05100-cdfa-7a42-ad63-77d1da568c46/exec-20354ae3-0301-48f5-8b3f-5e9e6814d85e.png`
- Real-device implementation screenshot: `/tmp/playerlite-recent-option3-final-1179x2556.png`
- Normalized implementation screenshot: `/tmp/playerlite-recent-option3-implementation-final-accessible.png`
- Full-view comparison evidence: `/tmp/playerlite-recent-option3-comparison-final-accessible.png`
- Focused comparison evidence: `/tmp/playerlite-recent-option3-focused-final-accessible.png`
- State: 已登录、最近播放页、歌曲分类已选中、真实接口数据已加载、更多菜单关闭。

## Viewport and normalization

- Source image: `852 × 1846 px`; its aspect ratio maps to the selected `393 × 852 dp` mobile viewport.
- Implementation: Android emulator `emulator-5554` temporarily set to `1179 × 2556 px` at `480 dpi` (`3×`), giving an exact `393 × 852 dp` Compose viewport.
- Comparison: the source was Lanczos-resampled to `1179 × 2556 px`; the implementation remained at native capture size. The comparison masks only the implementation's top status-bar area (`0–112 px`) and bottom navigation area (`2490–2556 px`) with the app canvas color. No product content was cropped, stretched, or retouched.
- The recent-play API is live and changed between captures. Song names, covers, albums, durations, and row count are therefore treated as dynamic data; the compared state, hierarchy, controls, and viewport are the same.

## Full-view comparison

- The approved structure is preserved: quiet back/title/refresh app bar, horizontally scrollable pill tabs, red selected-song state, continuous flat queue, two-digit ranking, rounded covers, title plus artist/album metadata, duration, overflow control, and thin inset dividers.
- The first three indices use an accessible red emphasis, while later indices use secondary text. No row cards, shadows, or detached panels were introduced.
- Major-region proportions, horizontal alignment, row density, cover size, divider rhythm, and above-the-fold item count materially match the source. A partial ninth row may appear at the bottom because the live queue contains more items than the static mock.

## Focused-region evidence

- `/tmp/playerlite-recent-option3-focused-final-accessible.png` compares the app bar, tab strip, first four rows, typography, cover masks, durations, and trailing controls at readable native scale.
- A focused comparison was retained because dense typography and icon alignment are easier to judge there than in the full-height composite.

## Required fidelity surfaces

- Fonts and typography: shared PlayerLite / Material typography is retained. The `20 sp` bold page title, semibold song titles, muted supporting text, two-digit indices, and compact durations reproduce the approved hierarchy without collisions.
- Spacing and layout rhythm: `16 dp` tab-strip margins, `8 dp` pill gaps, `12 dp` list start inset, `77.5 dp` minimum rows, `54 dp` artwork, `14 dp` artwork radius, and `48 dp` trailing actions match the source's compact continuous-list rhythm.
- Colors and visual tokens: the implementation uses PlayerLite's near-white canvas and shared text/divider roles. Light-theme selection and top-three emphasis use `#D32F2F`, which measures approximately `4.735:1` against the canvas; dark theme keeps the existing brand accent.
- Image quality and asset fidelity: live API artwork is center-cropped into consistent rounded squares and remains sharp. Missing artwork uses the closest Material rounded music-note icon; no handcrafted SVG, emoji, text glyph, or fake raster asset was introduced.
- Copy and content: fixed copy matches (`最近播放`, `本机`, `歌曲`, `视频`, `声音`, `歌单`, `专辑`, `播客`). Song copy and durations come from the live recent-play response rather than fabricated mock values.
- Icons: standard Material rounded back, refresh, music-note, and vertical-overflow icons are used at stable optical sizes; interactive icons retain platform-sized touch targets.
- Interaction and accessibility: tabs expose selectable-group and `Role.Tab` semantics, while their visible `32 dp` pills sit inside `48 dp` touch targets. Back, refresh, row playback, detail, and overflow behavior remain wired; generic unsupported rows stay read-only.

## Interaction checks

- Switched categories on the emulator and confirmed selected-tab state and content changes.
- Compose/Robolectric coverage verifies selected semantics, numbered song rows and durations, row playback, detail opening without click bubbling, and read-only generic rows/placeholders.
- The final implementation has no clipped persistent control, overlapping text, hidden action, or undersized primary touch target.

## Findings

- P0: none.
- P1: none.
- P2: none after iteration 3.
- Accepted variation: live server content differs from the generated source and may change between screenshots; the implementation intentionally does not freeze or fabricate recent-play history.
- P3 only: the accessible selection red is slightly darker than the generated mock's coral red, trading a small tonal difference for WCAG text contrast while retaining the same visual role.

## Comparison history

1. Iteration 1 (`/tmp/playerlite-recent-option3-comparison-pass1.png`): found P2 mismatches in oversized visible `48 dp` pills, horizontal list inset/trailing controls, typography, and duration density. Fixed by separating the `48 dp` touch target from the `32 dp` visible pill and tightening list/text measurements.
2. Iteration 2 (`/tmp/playerlite-recent-option3-comparison-pass2.png`): found P2 row-rhythm, artwork-size, and divider-weight drift (`81 dp` / `52 dp` versus approximately `78 dp` / `54 dp`). Corrected row height, cover size/radius, and divider thickness.
3. Iteration 3 (`/tmp/playerlite-recent-option3-comparison-final.png`): visual geometry passed, but the original light accent measured only approximately `4.02:1` on the canvas. Replaced it with `#D32F2F` for approximately `4.735:1` contrast and recaptured the final full and focused comparisons.
4. Final evidence (`/tmp/playerlite-recent-option3-comparison-final-accessible.png` and `/tmp/playerlite-recent-option3-focused-final-accessible.png`): no actionable P0/P1/P2 visual, interaction, or accessibility mismatch remains.

## Implementation checklist

- [x] Implement the selected flat, dense recent-play queue.
- [x] Preserve existing playback, detail, refresh, category, and read-only generic behavior.
- [x] Add selected-tab semantics and accessible touch targets/contrast.
- [x] Run focused tests, repository regression tests, Debug assembly, emulator install, interaction checks, and same-viewport visual comparison.

final result: passed

---

# 最近播放与搜索结果页一致性修正 Design QA

## Comparison target

- Source visual truth: `/tmp/playerlite-consistency-audit-01-search-results.png`（当前 Debug APK 的搜索结果页，查询词 `jay`，选中“单曲”）。
- Before-fix implementation: `/tmp/playerlite-consistency-audit-02-recent-playback.png`。
- Revised implementation: `/tmp/playerlite-consistency-audit-03-recent-after.png`（当前 Debug APK 的最近播放页，选中“歌曲”）。
- Full-view comparison: `/tmp/playerlite-consistency-comparison-final.png`。
- Focused top/list comparison: `/tmp/playerlite-consistency-comparison-focused.png`。

## Viewport and state

- 两个页面均来自同一台 Android 模拟器、同一构建，物理视口为 `1280 × 2856 px`、密度为 `480 dpi`（约 `426.7 × 952 dp`），未做密度归一化。
- 两侧使用各自的实时接口数据，因此歌曲、封面和时间不要求逐项相同；对照目标是共享的导航、列表、排版、图标和分隔线语言。
- 搜索页保留搜索框与结果分类；最近播放保留页面标题、刷新、序号与时长，这些属于功能差异，不属于视觉漂移。

## Comparison history

1. 初始审计发现跨页不一致：最近播放使用描边胶囊 Tab、竖向更多图标、不同的封面尺寸/圆角、不同的标题层级，以及贯穿整行的细分隔线。
2. 修正后统一为短下划线 Tab、`56 dp` 封面与 `12 dp` 圆角、`15 sp / 13 sp` 标题/说明层级、横向更多图标，以及从文本列起始的 `1 dp` 分隔线。
3. 将修正版安装到模拟器，依次进入“我的 → 最近播放 → 歌曲”，再与同一设备上的搜索结果页做全页和聚焦区域并排复核。

## Required fidelity surfaces

- Typography: 两页列表均使用 `15 sp` 主标题和 `13 sp` 次要信息，字重、截断与行距一致；长标题不会与时长或更多按钮碰撞。
- Spacing and layout: Tab 高度、下划线宽高、`56 dp` 封面、`12 dp` 圆角、行内垂直节奏和文本列分隔线一致。最近播放的序号列会将封面整体右移，这是保留业务信息后的预期布局。
- Colors and tokens: 画布、正文、次要文字与分隔线沿用 PlayerLite 共享 token；最近播放选中红色为可访问性更高的既有 `#D32F2F`，与搜索页红色存在轻微明度差，列为 P3 可接受差异。
- Images and icons: 两页均使用真实远端封面、方形裁切和 Material 横向更多图标；未引入占位绘图或伪造资源。
- Copy and content: 固定页面文案符合各自信息架构；动态歌曲数据不纳入逐字视觉一致性判断。
- Accessibility and interaction: Tab 保持至少 `48 dp` 点击区域并暴露 `Role.Tab` 与选中语义；更多按钮为 `48 dp`；返回、刷新、Tab 切换、歌曲点击与更多操作均保留。

## Findings

- Final P0: none.
- Final P1: none.
- Final P2: none.
- Accepted variation: 最近播放必须保留序号和时长，搜索结果页没有这两列。
- Accepted variation: 最近播放使用标题 + 刷新，搜索结果页使用搜索框；两者是不同页面任务的顶部操作区。
- P3 follow-up only: 最近播放的选中红色略深于搜索结果页，以保留当前页面已有的可访问性对比度。

## Implementation checklist

- [x] 用搜索结果页的下划线 Tab 语言替换描边胶囊 Tab。
- [x] 对齐封面、文字、更多图标和分隔线规格。
- [x] 保留最近播放特有的标题、刷新、序号、时长及原有交互。
- [x] 通过定向 Compose/Robolectric 测试、模拟器安装和同视口并排视觉复核。

final result: passed

---

# 本地歌曲页 Design QA

## Comparison target

- Source visual truth path: `/Users/wxy/.codex/generated_images/01a05100-cdfa-7a42-ad63-77d1da568c46/exec-f8f9eab9-41a5-49d8-9ba1-61e55d62e775.png`
- Implementation screenshot path: `/tmp/playerlite-local-393x852.png`
- Full-view comparison evidence: `/tmp/playerlite-local-reference-vs-implementation-pass1.png`
- State: 已授权、已扫描到 4 首真实本地歌曲、未刷新、更多菜单关闭。

## Viewport and normalization

- Source image: `852 × 1846 px`; it has no embedded density metadata and its aspect ratio maps to the approved `393 × 852 dp` mobile viewport.
- Implementation: Pixel 9 Pro emulator temporarily set to `1179 × 2556 px` at `480 dpi` (`3×`), giving an exact `393 × 852 dp` Compose viewport. The emulator was restored to its physical resolution after capture.
- Comparison: the source was Lanczos-resampled to `1179 × 2556 px`; the implementation stayed at its native capture size. Both were placed side by side without cropping on a `2382 × 2556 px` canvas. The Android status and navigation chrome remain visible only in the implementation and were excluded from product-content findings.

## Full-view comparison

- The approved information hierarchy is preserved: quiet back/scan app bar, `歌曲数 + 播放全部` utility row, continuous flat song rows, pale-red artwork placeholders, title/artist/album, duration, overflow action, and inset dividers.
- Major-region proportions, above-the-fold density, horizontal alignment, row rhythm, and empty-canvas balance remain materially consistent with the source.
- Real MediaStore durations intentionally replace the generated mock durations. The third title truncates slightly earlier because the implementation preserves a full `48 dp` overflow target and real duration width.

## Focused-region evidence

A separate crop was not required. The original combined evidence is `2382 × 2556 px`; app-bar typography, action-row alignment, placeholder masks, titles, metadata, durations, dividers, and overflow icons are all readable at native comparison resolution.

## Required fidelity surfaces

- Fonts and typography: PlayerLite's shared Material typography is retained. Title, song-name, metadata, count, action, and duration weights follow the approved hierarchy; long content uses single-line ellipsis without collision.
- Spacing and layout rhythm: `20 dp` page margins, `64 dp` utility row, `84 dp` minimum song rows, `52 dp` artwork, `14 dp` artwork radius, and `48 dp` overflow targets match the source's compact continuous-list rhythm. No clipping or hidden persistent control was observed.
- Colors and visual tokens: canvas, secondary text, divider, highlight surface, and red accent all use shared PlayerLite tokens. The implementation's canvas is marginally cooler than the generated image by design, keeping this page consistent with Home and Search.
- Image quality and asset fidelity: local files do not expose usable cover art in the current model, so the approved placeholder treatment uses the closest Material rounded music-note icon on the shared pale-red surface. It renders sharply and introduces no handcrafted SVG, emoji, text glyph, or raster artifact.
- Copy and content: fixed copy matches (`本地歌曲`, `4 首歌曲`, `播放全部`). Song metadata and durations come from the device rather than fabricated mock values.
- Interaction and accessibility: back, scan, play-all, song-row, and overflow actions remain wired. The overflow target is `48 dp`; automated tests cover scan, pull-to-refresh, play-all, row playback, insert-next, detail opening, empty state, and duration formatting.

## Findings

- P0: none.
- P1: none.
- P2: none.
- Accepted variation: platform status/navigation chrome appears in the implementation capture but is not app content.
- Accepted variation: real durations use the app-wide `m:ss` convention rather than the generated source's zero-padded mock values.
- P3 follow-up only: the source's music-note glyph has a slight generated tonal glow, while the implementation deliberately uses the product's crisp Material icon and design tokens.

## Comparison history

1. Iteration 1: installed the current Debug APK, forced an exact `393 × 852 dp` viewport, captured the populated list, and compared it side by side with the approved option 1 reference.
2. No actionable P0/P1/P2 mismatch was found in the first comparison, so no visual correction iteration was required.

## Implementation checklist

- [x] Replace full-width CTA and per-row cards with the approved utility row and continuous list.
- [x] Add visible song count and MediaStore duration.
- [x] Preserve playback, scan, pull-to-refresh, overflow, and detail behavior.
- [x] Run focused tests, repository regression tests, Debug assembly, emulator install, and same-viewport visual comparison.

final result: passed

---

# 搜索页三态 Design QA

## Comparison target

- Source visual truth paths:
  - 入口 / 热搜态：`/Users/wxy/.codex/generated_images/01a02e8b-dc3f-7f11-827c-b05710690a61/exec-d6419cde-256c-4556-b376-d5ab791cd06b.png`
  - 输入 / 联想态：`/Users/wxy/.codex/generated_images/01a02e8b-dc3f-7f11-827c-b05710690a61/exec-05f2a6ac-de25-4164-8640-61fc6a40a757.png`
  - 搜索结果态：`/Users/wxy/.codex/generated_images/01a02e8b-dc3f-7f11-827c-b05710690a61/exec-e0e30eeb-3a92-4339-b805-00c9f62e242a.png`
- Implementation screenshot paths:
  - 入口 / 热搜态：`/tmp/playerlite-search-hot-new.png`
  - 输入 / 联想态：`/tmp/playerlite-search-suggest-new.png`
  - 搜索结果态（修正后）：`/tmp/playerlite-search-result-final.png`
  - MV 非方形封面裁切态：`/tmp/playerlite-search-mv-crop-final.png`
- Full-view comparison evidence:
  - `/tmp/playerlite-search-qa-hot.png`
  - `/tmp/playerlite-search-qa-suggest.png`
  - `/tmp/playerlite-search-qa-result-final.png`

## Viewport and normalization

- Implementation device: Android emulator `emulator-5554`, physical viewport `1280 × 2856 px`, density `480 dpi` (`3×`), approximately `426.7 × 952 dp`.
- Entry and suggestion source images: `852 × 1846 px`. Result source image: `853 × 1844 px`.
- Source images have no embedded CSS viewport or density metadata. For comparison they were treated as approximately `426 × 923` logical units at `2×`; this is a normalization assumption, not source metadata.
- Implementation captures were width-normalized to each source while preserving aspect ratio, then center-cropped vertically to the source height. Entry and suggestion normalized from `1280 × 2856` to `852 × 1901`, then cropped to `852 × 1846`; result normalized to `853 × 1903`, then cropped to `853 × 1844`.
- The approximately `29 dp` device-height difference is therefore excluded from layout findings. Status bar and home indicator remain visible in both implementation evidence and were not treated as product-content drift.

## States and interactions checked

- Entry / hot state with persisted history and live hot-search data.
- Typing state using `jay`, including the visible clear action, direct “搜索当前关键词” row, live suggestion loading, and returned suggestions.
- Result state with live song results, horizontal result-type navigation, artwork, metadata, and overflow actions.
- Primary interactions verified on the emulator: open Search from Home, type a query, submit from the first suggestion row, switch from 单曲 to 专辑 and MV, and clear the query back to the hot state.
- Android accessibility hierarchy verified the selected MV tab as `selected="true"` with a `156 × 144 px` bound, equal to `52 × 48 dp` at `3×` density.
- Automated coverage also verifies hot keyword click, suggestion click, query clear, result-type selection semantics/callback, history removal, result pager/type synchronization, visible-song click data, overflow routing, overflow target height, and selected-page loading behavior.

## Full-view comparison

- Information architecture matches all three visual targets: quiet back action + search field, section-led flat content, continuous rows, thin inset dividers, and underline result tabs.
- Entry state preserves the source grouping of recent searches and hot ranking without reintroducing a large board card.
- Suggestion state preserves the source’s direct-search first row and red query emphasis. Returned keywords differ because the implementation uses the live API response.
- Result state preserves the source’s scan pattern and artwork/title/metadata/overflow structure. The implementation intentionally uses Home’s approximately `72–76 dp` song-row rhythm so Search reads as part of the same app rather than a separate skin.
- Query/content limitation: the generated references use `甲乙` / `甲乙丙丁`, while the emulator comparison uses the typeable live query `jay`. The comparison therefore validates structure, hierarchy, spacing, controls, and generic truncation behavior; it does not claim character-for-character copy fidelity or identical line breaks.

## Focused-region evidence

A separate crop was not required. Each side-by-side comparison is approximately `1720 × 1894 px`; the top bar, section titles, icons, dividers, result tabs, artwork masks, typography weights, truncation, and row spacing remain legible in the combined evidence. The album-tab implementation capture at `/tmp/playerlite-search-album-tab-check.png` verifies the selected-tab indicator and non-song metadata hierarchy; `/tmp/playerlite-search-mv-crop-final.png` verifies square center-cropping with live 16:9 MV imagery.

## Required fidelity surfaces

- Fonts and typography: shared PlayerLite / Material typography is used throughout. Search hierarchy is consistent with Home: strong section titles, medium result titles, and muted metadata. Long live-result text truncates without colliding with the overflow action.
- Spacing and layout rhythm: page margins are `20 dp`, the search field is `52 dp` high with an `18 dp` radius, and result rows align with Home’s list density. No clipping, overlap, or hidden persistent controls were observed.
- Colors and visual tokens: canvas, raised surface, dividers, text roles, and accent are sourced from PlayerLite visual tokens. The implementation is slightly cooler/greyer than the generated reference because it intentionally uses the app-wide canvas token.
- Image quality and asset fidelity: live remote covers use explicit `ContentScale.Crop` and render sharply at a consistent square crop with `12 dp` masks, including live 16:9 MV imagery. Standard Material rounded icons are used; no handcrafted SVG, text glyph, placeholder illustration, or CSS-style fake asset was introduced.
- Copy and content: fixed copy matches the approved Chinese information architecture (`最近搜索`, `热搜榜`, `搜索建议`). Live hot terms, suggestions, and results are intentionally not forced to match mock data.
- Accessibility and interaction: back and clear actions have content descriptions; the main input, result tabs, and song overflow controls are at least `48 dp` high. Tabs expose selected semantics with `Role.Tab`; result switching and query clearing work on the emulator. No decorative animation was added to this high-frequency flow.

## Findings

- No actionable P0, P1, or P2 visual, interaction, or accessibility differences remain after iteration 2.
- Accepted variation: source mock suggestions include category subtitles that the current suggestion API model does not expose. Fabricating those labels would be misleading, so the implementation retains a single-line live keyword row.
- Accepted variation: the implementation may show more live hot/result rows above the fold than the static mock because item count and text length are server-driven; row sizing remains aligned with the app-wide list system.
- Accepted variation: source and implementation queries differ, so exact copy wrapping is excluded from the fidelity claim. Both short Latin and long live-result strings were checked for collision-free truncation.
- P3 follow-up only: the generated reference uses a slightly lighter section-title weight and warmer canvas. The implementation deliberately follows PlayerLite’s shared typography and canvas tokens to satisfy the product-wide consistency goal.

## Comparison history

- Iteration 1: compared all three confirmed source states with fresh emulator captures on the same side-by-side canvases. The visual comparison passed, but the following implementation-level P2 gaps were found during independent review: result tabs and song overflow controls were only `40 dp` high, tabs lacked selected/Tab semantics, and remote artwork did not explicitly request center crop.
- Fixes applied: result tabs now use `52 × 48 dp` selectable targets with `Role.Tab`; song overflow uses a `48 dp` `IconButton`; remote artwork uses `ContentScale.Crop`; Compose tests now exercise hot/suggestion/type callbacks, clear behavior, selected semantics, and overflow target height.
- Iteration 2: installed the revised APK, recaptured `/tmp/playerlite-search-result-final.png`, compared it with the result reference in `/tmp/playerlite-search-qa-result-final.png`, switched to MV, and inspected `/tmp/playerlite-search-mv-crop-final.png`. The Android hierarchy confirms the selected tab is `52 × 48 dp` with selected semantics, and live 16:9 artwork has no implementation-introduced letterboxing. No P0/P1/P2 finding remains.

## Implementation checklist

- [x] Match approved three-state structure.
- [x] Reuse PlayerLite visual tokens and Home spacing/list language.
- [x] Preserve search state, navigation, pager, playback, and overflow behavior.
- [x] Verify focused Search tests and required repository regression tasks.
- [x] Install the current Debug APK and exercise the primary flow on the emulator.

final result: passed
