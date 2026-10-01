# Seequid

**Your screen is thirsty.** Seequid is an Android hydration app that slowly covers your phone screen with water when you fall behind on drinking. The water rises over whatever app you're using. Drink, tap the squid, and watch it drain away.

Most hydration apps rely on notifications, which are easy to swipe away and forget. Seequid replaces them with a constant, ambient signal you can see but that never interrupts you. The water is see-through and your taps pass through it, so you keep using your phone normally.

> Built for **RevenueCat Shipaton 2026**, Next Gen (student) category.

## For judges: try it in 2 minutes

1. Download **`Seequid-1.0.0.apk`** from [Releases](https://github.com/iamstrix/seequid/releases) and open it on an Android 9+ phone. Allow "Install unknown apps" if Android asks.
2. Go through the short intro. On the last page, tap **Open phone settings**, find **Seequid** and turn on **Display over other apps**. Seequid brings you back automatically.
3. Open **Settings → For testing → Demo mode**. It squeezes a whole day into 10 minutes, so the water starts rising right away.
4. Go to any other app and watch the water rise. When the squid appears on the edge of the screen, tap it to log a drink and watch the water drain.
5. Tap **Go Pro**. Purchases use the **RevenueCat Test Store**, so nothing is charged: pick a plan, then choose the successful purchase option in the test dialog.

The APK is a debug build on purpose: the RevenueCat SDK refuses to run a release build with a Test Store key.

## How it works

- **Pace, not reminders.** You set a daily goal (capped at 4 L). Seequid spreads it across your waking hours and compares it with what you've logged. On pace, your screen is clear. Behind, the water rises: the screen is full when you're 40% of your daily goal behind, so every drink visibly lowers it.
- **The squid.** The mascot shows how you're doing at a glance: happy, worried, thirsty, asleep at night, or celebrating when you reach your goal. Tap it in the app for a tip ("Drink about 250 ml to clear your screen").
- **The squid on the edge.** Over other apps, a small squid appears once the water is high. Tap it to log a drink in one tap: the sheet tells you exactly how much clears your screen and highlights the matching button. Drag it to either edge; after a few seconds it squishes into the edge with one eye peeking out, so it never covers another app's buttons.
- **Same water everywhere.** The water in the app means the same as the water on your screen: it rises when you're behind and drains when you drink. Progress towards your goal is a separate green bar.
- **Flexible schedule.** Sleep hours (bedtimes up to 4 AM for night owls), or no schedule at all.

## Monetization (RevenueCat)

The core habit is free forever on purpose: a health app shouldn't put health behind a paywall. Pro is about personality and insight.

| Free | Seequid Pro (`seequid_pro` entitlement) |
|---|---|
| The water overlay, logging, squid moods, sleep schedule | 4 extra drinks (Matcha, Cold brew, Boba tea, Night lagoon) that restyle the live overlay |
| Daily streak counter | Squid outfits: crown, sunglasses, party hat, headband |
| 7-day history preview | 30-day history, best streak and average per day |

**Pricing:** $1.99 / month, $11.99 / year (50% off, preselected) or $24.99 once.

- **Paywall:** a remote paywall designed in the RevenueCat dashboard, attached to the `default` offering (`pro_monthly`, `pro_yearly`, `pro_lifetime`), rendered full screen with `purchases-ui`. It appears once after onboarding, and whenever the user taps Go Pro, a locked drink, outfit or the history card. Prices and the discount badge come from the offering, so they can change without an app update.
- **Purchase celebration:** a `PaywallListener` reads which product was bought and plays a matching celebration: bubbles for monthly, confetti for yearly, a crowned squid in gold for lifetime.
- **Entitlement-driven UI:** one `StateFlow<Boolean>` fed by `UpdatedCustomerInfoListener`. Buying Pro restyles the overlay that's already running, without a restart.
- **Customer Center** ("Manage or cancel" in Settings) and **Restore purchases**. With Test Store purchases, RevenueCat hides the Cancel option by design because there's no store subscription to cancel; on Google Play it appears automatically.

## Technical choices

| Choice | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` overlay in a foreground service | Works on every Android 9+ phone with one permission. An AccessibilityService could draw opaque water, but it makes many banking apps refuse to run and is blocked for sideloaded apps on Android 13+. |
| Water drawn with **window** alpha ≤ 0.75, below the device's `maximumObscuringOpacityForTouch` | Since Android 12, touches through an untrusted overlay are dropped above 0.8 window alpha. All the translucency comes from the window, never the colours. |
| Level computed from the clock and the drink log, never stored | Correct after process death, reboots and timezone changes. The pacing is a pure, unit-tested function, including night owls whose day ends after midnight. |
| The overlay steps aside for Seequid's own screens | No double water inside the app, and the quick-log sheet isn't tinted. When it comes back, the water drains from its old level to the new one. |
| Kotlin, Jetpack Compose, Room, DataStore, manual DI | A small app with fast iteration. |

**Privacy:** Seequid never reads screen content. All data stays on the device. The only network traffic is RevenueCat's purchase and entitlement calls.

**Health:** goals are capped at 4 L a day, there's no water during sleep hours by default, and the app says clearly that it isn't medical advice.

## Build from source

Requirements: Android Studio (JDK 17 to 21; Gradle 8.14 doesn't run on JDK 25), Android SDK 36.

```bash
cd android
cp local.properties.example local.properties   # set sdk.dir and revenuecat.apiKey
./gradlew :app:installDebug
```

Without a RevenueCat key the app runs in free mode and shows a short "purchases not configured" message instead of the paywall.

### RevenueCat setup (Test Store, no Play Console needed)

1. Create a RevenueCat project and copy the **Test Store API key** (`test_…`) into `local.properties` as `revenuecat.apiKey`.
2. Create the entitlement `seequid_pro`.
3. Create Test Store products `pro_monthly`, `pro_yearly` and `pro_lifetime`, attach them to `seequid_pro`, and add them to the `default` offering as Monthly, Annual and Lifetime packages.
4. Design a paywall for the `default` offering and publish it.
5. Optionally enable Customer Center.

## Roadmap

- The quick-log sheet floating above the water, so you see it drain while you log
- Outfits on the edge squid, more drinks, smart goals for workouts and hot days
- Night-shift schedules, per-app pause, home screen widget
- Paywall experiments once there's real traffic

## License

[MIT](LICENSE)
