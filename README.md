# Charger Companion

Android app for a **2020 Dodge Charger / Uconnect 8.4** setup:

1. **Android Auto — Nearby places** (Gas / Food / Parking) using OpenStreetMap Overpass data and the official Car App POI templates.
2. **Media remote** — huge play / pause / skip / volume buttons so Netflix, YouTube, or Spotify on the phone can use the Charger’s Bluetooth speakers without tiny on-screen controls.

This does **not** cast video onto Android Auto (Google doesn’t allow that). It solves the loud-speakers + nearby stops problem the legal way.

## Build

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer).
2. Open this folder.
3. Let Gradle sync.
4. Plug in a phone (Android 8+) → Run `app`.

## Use on the Charger

### Media remote
1. Pair the phone to Uconnect over Bluetooth (or use AA audio).
2. Start Netflix / YouTube / Spotify on the phone.
3. Open **Charger Companion → Open media remote**.
4. Use the big buttons; audio stays on the car speakers.

### Android Auto places
1. Grant **location** when the phone app asks.
2. Connect Android Auto (USB on 2020 Charger).
3. Open **Charger Companion** from the AA launcher.
4. Pick Gas, Food, or Parking — results show on the 8.4" map list.

> POI apps must be installed via Android Studio / sideload for testing. Publishing to Play Store for Android Auto requires Google’s AA review.

## Privacy

- Location is used only to query nearby OSM places.
- No accounts, no analytics in this v1.

## License

MIT — not affiliated with Stellantis, Google, Netflix, or YouTube.
