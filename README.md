# Heartly 💙💗

Native Android couple app MVP. No Flutter required.

### Stack
- Native Android + Kotlin
- Supabase Auth
- Supabase Postgres + RLS
- Supabase Storage
- Android home-screen AppWidget
- GitHub Actions APK build

### Features in this MVP
- Email/password account
- Boy / girl theme selection
- Create a 6-character couple code
- Join with a couple code
- Shared notes
- Send text and/or image
- Home-screen widget showing the latest shared note
- Blue "cute bat-cat" inspired boy theme and pink "cute cat" inspired girl theme

### Setup
1. Create a Supabase project.
2. In Authentication settings, for the easiest first test, disable "Confirm email".
3. Open SQL Editor and run `supabase_schema.sql`.
4. Copy your Supabase Project URL and anon/publishable key.
5. Put them in `app/src/main/java/com/ourspace/app/SupabaseConfig.kt`.
6. Push the repo to GitHub.
7. Open Actions -> Build APK.
8. Download the `our-space-debug-apk` artifact.

### Important
The `media` bucket is public in this MVP so the Android widget can load image URLs easily. Do not use this exact storage policy for highly private production data. A production version should use a private bucket + signed URLs.

Never put a Supabase `service_role` key in the Android app. Only the public anon/publishable key belongs in the client.
