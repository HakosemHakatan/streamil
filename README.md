# StreamIL TV — Android + Android TV

Native Kotlin Android project with Media3 ExoPlayer. Hebrew RTL interface, Android TV Leanback launcher, D-pad focusable buttons, TV/Radio/VOD tabs, searchable channels, favorites, HTTP(S) M3U imports, persistent local library.

## Build
1. Open this directory in Android Studio (JDK 17, Android SDK 35).
2. Allow Gradle sync to download Android Gradle Plugin 8.7.3, Kotlin 2.0.21 and Media3 1.5.1.
3. Build > Build APK(s). APK is generated under app/build/outputs/apk/debug/.
4. Sideload the APK onto Android TV / NVIDIA Shield or an Android phone.

## Limitations
- Source code delivered, **not compiled or device-tested** in this environment.
- Starter live-TV playlist: iptv-org Israel public index. Radio: Radio Browser country IL directory. These are third-party public indexes; individual streams may fail, be geo-blocked, or have rights restrictions. No stream availability or redistribution rights are guaranteed.
- VOD shows items tagged as VOD/movie/series in imported M3U. It does not include a licensed movie catalog or DRM authentication.
- Supports URL-based M3U; not local file picker, Xtream Codes, DRM, EPG, cast, subtitles management, profiles, or background playback service.
- For a production release: add accessible TV-specific UI, stream error handling, EPG, playlist management, secure playlist storage, content rights review, and privacy policy. Only import playlists you are authorized to use.
- HTTP cleartext enabled for older IPTV playlists. Prefer HTTPS for security.

## Build an APK with GitHub Actions (no Android Studio required)
1. Create a new GitHub repository and upload the **contents** of the `StreamIL-TV` folder to its root, including the hidden `.github/workflows/build-apk.yml` file. Alternatively upload the project using GitHub's web interface with the workflow file at that exact path.
2. Open the repository's **Actions** tab, select **Build Android APK**, and click **Run workflow** (or push to `main` to trigger automatically).
3. When the run succeeds, open its **Artifacts** section and download `StreamIL-TV-debug-APK`.
4. Extract the downloaded ZIP to obtain `app-debug.apk`, then install it on your Android phone or Android TV device.

This workflow builds a debug APK (not a signed Play Store release). It has **not** been run or verified in this environment. If GitHub blocks a workflow upload from the browser, create `.github/workflows/build-apk.yml` using GitHub's **Add file → Create new file** editor.
