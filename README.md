# seequid

**Your screen is thirsty.** seequid is an Android hydration app that slowly fills your phone screen with water as you fall behind on drinking. The water rises over whatever app you're using, and drinking makes it drain away.

Most hydration apps rely on notifications, which are easy to swipe away and forget. seequid replaces them with a constant, ambient signal that you can see but that never interrupts you. The water is see-through and taps pass through it, so you can keep using your phone normally.

> Built for RevenueCat Shipaton 2026, Next Gen (student) category.

## How it works

- **Pace, not reminders.** You set a daily goal (capped at 4 L) and your waking hours. seequid spreads the goal evenly across your day. The water level shows how far behind that pace you are: when you're on pace the screen is dry, and when you're 500 ml behind it's full.
- **Always on, never in the way.** The water is drawn in an overlay window that's never touchable, so taps reach the app underneath. The window is only as tall as the water, so nothing above it is redrawn.
- **Tap the drop.** Once the water passes a threshold, a small drop button appears. Tap it to log 150–500 ml without leaving the app you're in. You can also log from the notification or from inside seequid.
- **Quiet at night.** No water appears outside your waking hours.

## Monetization (RevenueCat)

| Free | seequid Pro (`pro` entitlement) |
|---|---|
| The water overlay, logging, daily pace | Four premium liquids (Matcha, Cold brew, Boba tea, Night lagoon) that restyle the live overlay |
| Water skin | 7-day drinking history |

- **Paywall:** a remote RevenueCat Paywall (`purchases-ui` `PaywallDialog`). It's shown once, right after the user has seen the water working on their own phone, and again whenever they tap a locked liquid, the history card or "Go Pro".
- **Entitlement-driven UI:** one `StateFlow<Boolean>` fed by `UpdatedCustomerInfoListener`. Buying Pro restyles the overlay that's already running, without a restart.
- **Customer Center** for managing the subscription, plus **Restore purchases**.
- **Products:** monthly, annual and lifetime in the `default` offering. These are RevenueCat Test Store products; see setup below.

## Technical choices

| Choice | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` overlay in a foreground service | Works on every Android 9+ phone with one permission. An AccessibilityService would allow opaque water, but it makes many banking apps refuse to run, is blocked for sideloaded apps on Android 13+ ("restricted settings"), and is revoked under Android 16+ Advanced Protection. |
| Water drawn with **window** alpha ≤ 0.75, never above the device's `maximumObscuringOpacityForTouch` | Since Android 12, touches passing through an untrusted overlay are dropped if the window's alpha is above 0.8. The skin colours are opaque, and all the translucency comes from the window. |
| Level computed from clock + drink log, never stored | Correct after process death, reboots and timezone changes. The logic is a pure function (`HydrationCalculator`) with unit tests. |
| Kotlin, Jetpack Compose, Room, DataStore, manual DI | A small app with fast iteration. |

**Privacy:** seequid never reads screen content. All data stays on the device. The only network traffic is RevenueCat's purchase and entitlement calls.

**Health:** goals are capped at 4 L/day, there's no water during sleeping hours, and the app says clearly that it isn't medical advice.

## Build and run

Requirements: Android Studio (JDK 17+), Android SDK 36.

```bash
cd android
cp local.properties.example local.properties   # set sdk.dir and revenuecat.apiKey
./gradlew :app:installDebug
```

### RevenueCat setup (Test Store, no Play Console needed)

1. Create a RevenueCat project. Test Store is provisioned automatically. Copy the **Test Store API key** (`test_…`) into `local.properties` as `revenuecat.apiKey`.
2. Create the entitlement `pro`.
3. Create Test Store products (for example `seequid_monthly`, `seequid_annual`, `seequid_lifetime`), attach each one to `pro`, and add them to the `default` offering as Monthly, Annual and Lifetime packages.
4. Design a Paywall for the `default` offering in the dashboard.

Test Store keys only work in **debug** builds; the SDK deliberately crashes a release build that uses one. Without a key, the app runs in free mode and explains that purchases aren't configured.

### Trying the water quickly

On the home screen, turn on **Demo mode**. It squeezes a whole day's pace into 10 minutes, so the water starts rising immediately.

## Roadmap

- Immersive mode via an optional AccessibilityService (opaque water, no persistent notification), offered only with clear disclosure of the trade-offs above
- Smart pacing (workouts, hot days), home-screen widget, per-app quiet list
- Paywall A/B experiments and targeted offerings once there's real traffic

## License

[MIT](LICENSE)
