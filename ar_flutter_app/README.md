# Flutter + ARCore Hello AR

This module wraps the **Hello AR (Java)** sample from the ARCore Android SDK inside a
Flutter application. The Flutter UI launches the original native activity that handles
plane detection and anchor placement while providing a simple onboarding screen.

## Project layout

- `lib/main.dart` – Flutter entry point with a `MethodChannel` (`arcore_hello`) used to
  trigger the native AR session.
- `android/app/src/main/java` – Original ARCore sample sources copied from
  `samples/hello_ar_java` along with shared helpers.
- `android/app/src/main/assets` & `res` – Models, shaders, and UI resources required by
  the Hello AR sample.

## Prerequisites

1. Flutter SDK installed (`flutter --version` should work).
2. Android Studio / command-line tools with the Android SDK and NDK versions that match
   your Flutter installation.
3. An **ARCore-supported Android device** connected via USB (emulators do not support
   ARCore plane detection).
4. Ensure Google Play Services for AR is installed and up to date on the device.

## Running the sample

From the repository root:

```bash
cd ar_flutter_app
flutter pub get
flutter run
```

When the Flutter app loads, tap **“Launch AR Experience”**. The native
`HelloArActivity` will open, reusing all ARCore logic from the Java sample (plane
rendering, anchor placement, settings menu, etc.).

## Notes

- Camera permission prompts and ARCore installation dialogs are surfaced by the native
  activity, identical to the original sample.
- If you need to modify the AR experience, edit the Java sources under
  `android/app/src/main/java/com/google/ar/core/examples/java/helloar/`.
- Flutter UI can be extended to pass data to the native layer via the existing
  `MethodChannel`.
