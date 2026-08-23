# Roadmap & TODO (BookStack Companion v1.3.0)

## Completed (v1.3.0)
- [x] Multi-server support with secure credential storage
- [x] Dashboard with quick access, recent pages, book preview
- [x] Books browsing with local search, pull-to-refresh, tree view
- [x] Page reader with themes, font size, TOC, prev/next navigation
- [x] Offline mode with full book download
- [x] Favorites and reading history (per server)
- [x] Global API search with debounce
- [x] Settings: theme, font size, cache management

## New in v1.3.0
- [x] HTTP logging disabled in release builds (security)
- [x] Android 15 support (compileSdk/targetSdk 35) + edge-to-edge UI
- [x] Background book downloads via WorkManager (survive app close)
- [x] Reader options: font family (sans/serif/mono) + line height
- [x] Page export: Markdown / HTML / PDF (system print)
- [x] Offline images: pre-downloaded during book download, served locally in reader
- [x] Last sync timestamp per book (shown in book detail)
- [x] Unit tests for BookStackRepositoryImpl and ServerConnectViewModel
- [x] F-Droid metadata structure (fastlane/metadata/android)

## Future Improvements

### Reading Experience
- [ ] Search filters by category (Books, Shelves, Chapters, Pages)
- [ ] Search history with clickable chips
- [ ] "Continue Reading" quick card on Dashboard
- [ ] Reading statistics (downloaded books, recent pages)

### Advanced Features
- [ ] Deep linking from BookStack URLs
- [ ] App lock (fingerprint / PIN)
- [ ] English localization (values-en)

### F-Droid Publication
- [ ] Add real phone screenshots to fastlane/metadata/android/{en-US,fr-FR}/images/phoneScreenshots/
- [ ] Submit inclusion request (RFP) on fdroiddata

---

## Technical Info
- **Current version**: v1.3.0 (`versionCode = 5`)
- **JDK Required**: JDK 21
- **Build guide**: See [info_build.md](info_build.md)
