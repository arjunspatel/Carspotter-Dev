# CarSpotter: Direct Android Device Installation & CameraX Capture Flow

A comprehensive plan to configure the Android build system for debuggable APK distribution, verify the CameraX optical capture pipeline, and provide clear step-by-step guidance for installing and testing CarSpotter directly on Android devices.

## User Review & Critical Decisions

> [!IMPORTANT]
> The target installation preference confirmed in Phase 1 is **direct APK download from AI Studio to an Android phone/tablet**. In addition, the package name and `applicationId` (`com.aistudio.carspotter.qzvkm`) are strictly preserved as required for Play Console compatibility.

- **Confirmed Installation Channel**: Direct download of the built debug APK (`app-debug.apk`) via AI Studio UI settings menu and browser file transfer.
- **Confirmed Target Platform**: Android phone/tablet running Android 7.0 (API 24) or higher.
- **CameraX Pipeline**: Real-time camera preview, HUD targeting brackets, hardware shutter capture, and AI analysis loading overlay are fully integrated and wired to `CarSpotterViewModel`.

---

## 1. Overview & Core Concept

- **What It Does**: CarSpotter empowers automotive enthusiasts to spot exotic and rare vehicles in the wild. By pointing the phone camera at a car and tapping the shutter, the app runs optical vehicle identification with anti-cheat detection, scores rarity points, unlocks garage trophies, logs sightings to a tactical radar map, and syncs spots to cloud Firestore.
- **Testing on Physical Hardware**: While the browser emulator allows UI simulation, running the APK directly on a physical Android smartphone enables full hardware CameraX preview, autofocus, optical sensor feeds, device vibration, and real-world outdoor vehicle spotting.
- **Key Value**: Instant, zero-friction sideloading of the debug build onto physical hardware without requiring Google Play Console publishing approval.

---

## 2. User Experience & Visual Design

### 2.1 Installation & Sideloading Journey
```
┌─────────────────────────────────┐
│       AI Studio Project         │
│  Top Menu > Export / Download   │
└───────────────┬─────────────────┘
                │ Downloads app-debug.apk
                ▼
┌─────────────────────────────────┐
│     Android Phone / Tablet      │
│  Enable "Install Unknown Apps"  │
└───────────────┬─────────────────┘
                │ Tap to Install
                ▼
┌─────────────────────────────────┐
│       Launch CarSpotter         │
│  Grant Camera & Location Perms  │
└───────────────┬─────────────────┘
                │ Real Hardware Active
                ▼
┌─────────────────────────────────┐
│   Optical Real-Time Spotting    │
└─────────────────────────────────┘
```

### 2.2 CameraCaptureScreen Visual & Interaction Layout
- **Camera Viewfinder**: Full-bleed CameraX `PreviewView` with real-time autofocus and exposure.
- **Tactical HUD Overlay**: Cyberpunk automotive HUD featuring cyan corner brackets, crosshairs, horizon leveling guide, and anti-cheat shield indicator.
- **Top Bar Controls**:
  - *Anti-Cheat Shield*: Visual status pill displaying `ANTI-CHEAT: ARMED` (Green) with debug override menu for testing screen and toy model cheat detection.
  - *Torch Toggle*: Hardware flashlight switch (`Icons.Default.FlashOn` / `Icons.Default.FlashOff`) for nighttime car spotting.
  - *Lens Switcher*: Seamlessly switches between back camera and front camera.
- **Bottom Control Deck**:
  - *Photo Gallery Button*: Quick access to Android Photo Picker (`ActivityResultContracts.PickVisualMedia`).
  - *Primary Shutter Button*: High-tactile amber ring shutter with inner target pulse.
  - *Preset Quick Spot Pills*: Quick-test shortcuts (Porsche GT3 RS, Ferrari 296 GTB, Random Spot) for testing without physical cars nearby.
- **AI Analysis Loading State**:
  - Semi-transparent carbon glass scrim.
  - High-precision amber `CircularProgressIndicator`.
  - Dynamic status text cycling through stages: *"Analyzing body lines & aero elements..."* -> *"Auditing reflections & depth map..."* -> *"Querying vehicle registry..."*.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Debug APK vs. Play Console Publishing**
  - *Chosen Approach*: Deliver `app-debug.apk` directly for on-device sideloading.
  - *Why*: The user previously encountered `Http response at 400 or 500 level` due to the package name not yet being registered in Google Play Console. Sideloading the debug APK works immediately on any Android phone without Play Console setup delays.
  - *Alternatives Considered*: Internal App Sharing or Closed Testing track on Play Console—postponed until user registers their developer account.
- **Decision 2: CameraX Architecture (`ImageCapture` + `PreviewView`)**
  - *Chosen Approach*: Jetpack CameraX using lifecycle-bound `PreviewView` and latency-optimized `ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY`.
  - *Why*: Ensures maximum compatibility across heterogeneous Android devices (Samsung, Pixel, Motorola, OnePlus) with hardware-level camera lifecycle safety.
- **Decision 3: Fallback Simulation Presets in Viewfinder**
  - *Chosen Approach*: Keep subtle preset pills at the bottom of `CameraCaptureScreen`.
  - *Why*: Allows the user to test the AI identification and trophy reveal logic even indoors or when no physical sports cars are immediately visible from their window.

---

## 4. Technical Architecture & Data Strategy

### 4.1 System Component Hierarchy
```
┌─────────────────────────────────────────────────────────────┐
│                       MainActivity                          │
│               (App Check & Firestore Init)                  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
   ┌──────────────────────────┐  ┌──────────────────────────┐
   │   CarSpotterBottomBar    │  │    CarSpotterTopBar      │
   └────────────┬─────────────┘  └──────────────────────────┘
                │
                ▼ TabNavigation (BackHandler protected)
   ┌─────────────────────────────────────────────────────────┐
   │                   Tab Destinations                      │
   │  ┌────────────────────┐   ┌──────────────────────────┐  │
   │  │ CameraCaptureScreen│   │ CollectionScreen (Garage)│  │
   │  │   • CameraX View   │   │ MapSpotScreen (Radar)    │  │
   │  │   • Shutter Button │   │ CommunityScreen (Battles)│  │
   │  │   • AI Loading HUD │   └──────────────────────────┘  │
   │  └─────────┬──────────┘                                 │
   └────────────┼────────────────────────────────────────────┘
                │ Captured Image Uri / Preset
                ▼
   ┌─────────────────────────────────────────────────────────┐
   │                CarSpotterViewModel                      │
   │  • snapCar(photoUri)                                    │
   │  • isAnalyzing StateFlow                                │
   │  • antiCheatMode StateFlow                              │
   └────────────┬─────────────────────────────┬──────────────┘
                │                             │
                ▼                             ▼
   ┌──────────────────────────┐  ┌──────────────────────────┐
   │  CarRecognitionEngine    │  │  Firestore & Room Sync   │
   │  • Vehicle Specs & Tier  │  │  • Cloud `car_spots`     │
   │  • Anti-Cheat Detection  │  │  • Local SQLite Garage   │
   └────────────┬─────────────┘  └──────────────────────────┘
                │
                ▼
   ┌──────────────────────────┐
   │   TrophyRevealDialog     │
   │  • Specs & Rarity Points │
   │  • Social Share Sheets   │
   └──────────────────────────┘
```

### 4.2 Build Configuration Verification
- **Application ID**: `com.aistudio.carspotter.qzvkm` (strictly preserved)
- **Namespace**: `com.example`
- **Build Type**: `debug` with debug keystore signing
- **Artifact Path**: `app/build/outputs/apk/debug/app-debug.apk`
- **Permissions Declared**:
  - `android.permission.CAMERA`
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`
  - `android.permission.ACCESS_FINE_LOCATION`
