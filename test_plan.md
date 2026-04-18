1. **LocaleHelper Creation**:
   - Create `com.dailywork.attedance.utils.LocaleHelper` to handle applying locales properly across SDK versions.
2. **strings.xml extraction**:
   - Extract hardcoded strings related to language selection, settings, and other relevant areas to `strings.xml` and `values-hi/strings.xml`.
3. **MainActivity update**:
   - Apply the current locale immediately in `MainActivity.onCreate` before setting content by reading from DataStore synchronously or applying what we have in process context. Or apply it properly via LocaleHelper inside MainActivity or Application class.
   - However, since we use `AppCompatDelegate` / `LocaleManager`, setting the app locale handles persistence natively from Android 13+. We just need to make sure we also persist to `DataStore` and apply it for older versions.
4. **LanguageSelectorActivity**:
   - According to requirements, create a dedicated `LanguageSelectorActivity` (or update existing `LanguageSelectionScreen` to trigger full app restart logic). Wait, the requirement says "Create a dedicated screen/activity/fragment named: LanguageSelectorActivity". I will create a new Activity `LanguageSelectorActivity.kt` extending `ComponentActivity`.
   - Update `SettingsScreen.kt` so that when user clicks on "Language", it launches `LanguageSelectorActivity`.
5. **Applying Language and Restarting**:
   - In `LanguageSelectorActivity`, when a language is selected, update `DataStore` and `AppCompatDelegate.setApplicationLocales`. Then, restart the app by launching `MainActivity` with `Intent.FLAG_ACTIVITY_NEW_TASK` and `Intent.FLAG_ACTIVITY_CLEAR_TASK`.
   - Update `LanguageSelectionScreen.kt` (used in onboarding) to only show "en" and "hi". Also apply the locale when chosen.
6. **Pre-commit checks**:
   - Verify code compiles and no strings are hardcoded in the new UI.
