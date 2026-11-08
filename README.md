# Timeline

A tiny Android app that works as my second brain. One timeline for everything I need to remember, shown right on my home screen.

> **This is a personal app, made for personal use.**
> I built it for myself because my head was too full and I needed one simple place to put everything. It was built with the help of AI (Claude). It is not a product, it is not polished for the public, and I do not plan to support or maintain it for anyone else. You are welcome to read the code or fork it, but expect no guarantees.

## Why

My mind was carrying too many things at once: deadlines, habits, and things that matter but have no date. This app takes them out of my head and puts them in one list that I can see without opening anything.

## What it does

- **One combined timeline** for every event.
- **Three event types:**
  - **Daily**: a habit that resets by itself every day.
  - **Deadline**: has a due date, with a countdown like "3 days left" or "Late 1 day".
  - **Important**: no due date, but too important to forget.
- **Fixed order:** Daily, then Deadline (nearest due date first), then Important. Finished items sink to the bottom.
- **Filter** by type.
- **Home screen widget** that shows the top 5 unfinished items from the same list.
- **Export and Import** your data as JSON, in case you need a backup.
- **Offline and local.** No account, no server, no internet. Data stays on the phone.

## How it is built

- Kotlin
- Jetpack Compose for the app
- Jetpack Glance for the widget
- Data saved in a single `items.json` file in the app's private storage
- No backend, no database library, no extra dependencies

The app and the widget read the same file and use the same sorting function, so they always agree.

## Run it yourself

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Plug in an Android phone with USB debugging on, then press Run.
4. Long press the home screen, open Widgets, and add Timeline.

Requires Android 12 (API 31) or newer. I have only tested it on my own phone.

## Known limits

- The widget refreshes right after any change in the app, but otherwise only about every 30 minutes. Labels like "Tomorrow" can lag a little after midnight.
- Some Android versions, especially Xiaomi HyperOS, may stop the app in the background. Set battery usage to "No restrictions" and enable Autostart if the widget stops updating.
- Import overwrites all current data.
- You cannot tick items directly from the widget yet.

## Note

Made for one person. If it helps you too, great. If not, that is fine.