# Heartly

Native Android couple app with a polished romantic/glow UI built around the supplied HEARTLY artwork.

### Stack
- Native Android + Kotlin
- Supabase Auth
- Supabase Postgres + RLS
- Supabase Storage
- Android home-screen AppWidget
- GitHub Actions APK build

### Included
- Email/password login and account creation
- Boy / girl theme selector using the supplied artwork (no emoji character placeholders)
- Supplied HEARTLY artwork used in the login/home UI and launcher icon
- 6-character couple-code create/join flow
- Shared notes
- Text and image sharing
- Home-screen widget
- Animated screen entrance, logo pulse, floating hearts, button feedback, image pop-in and error shake
- Short local UI sound effects for tap, selection, success, send and error events
- Android notification channel + notification permission handling for local Heartly activity notifications

### Setup
1. Create a Supabase project.
2. For the easiest first test, disable email confirmation in Supabase Authentication settings.
3. Open SQL Editor and run `supabase_schema.sql`.
4. Copy your Supabase Project URL and anon/publishable key into `app/src/main/java/com/ourspace/app/SupabaseConfig.kt`.
5. Push the repo to GitHub.
6. Open Actions and run **Build Heartly APK**.
7. Download the `heartly-debug-apk` artifact.

### Important
- The Android app must only contain the public anon/publishable key. Never put a Supabase `service_role` key in the APK.
- The current `media` bucket is public because the widget needs direct image URLs. For production privacy, move to a private bucket and signed URLs.
- These notifications are local app notifications. True remote partner-to-partner push notifications require a push backend/FCM integration in addition to Supabase.
