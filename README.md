# Love Alarm

## What this is

A tiny app for two people. Each of you has a board of tiles. Tap a tile and the other person's phone gets a notification straight away. That's it.

## Install

1. On your Android phone, open **https://github.com/sudoRKT/LoveAlarm/releases/latest**
2. Under **Assets**, tap the file ending in **.apk** (for example `LoveAlarm-v1.0.0.apk`). Ignore the other files.
3. When it finishes, open it from the **Downloads** notification, or from the **Files** app → Downloads.

**"Install unknown apps" prompt.** Your phone will warn you because the app isn't from the Play Store. That's expected.

- Most phones: a box says your phone isn't allowed to install apps from this source. Tap **Settings**, turn on **Allow from this source**, then go back and tap **Install**.
- Xiaomi / Redmi / POCO: you may see a warning screen with a countdown. Wait for it, tick the box if there is one, and tap **Continue** or **Install**. If asked, allow your browser or Files app to install apps.

**Play Protect prompt.** If Google shows "Unsafe app blocked" or offers to scan the app, tap **More details**, then **Install anyway**. If it asks to send the app for scanning, either answer is fine.

## First open

1. Type your name.
2. Pair with the other person:
   - If they've already started, they'll give you a **6-character code**. Type it in.
   - If you're first, create a code and send it to them.
3. When asked to **allow notifications**, tap **Allow**. Without this, alerts won't show.
4. When asked to **allow running in the background**, tap **Allow**. This stops the phone from putting the app to sleep.

## Xiaomi / Redmi / POCO extra steps

These phones are strict about apps running in the background. Do all of these once, or alerts may arrive late or not at all.

1. Open **Settings** → **Apps** → **Manage apps** → **Love Alarm**.
2. Turn **Autostart** on.
   (On some versions this lives in the **Security** app → **Permissions** → **Autostart** instead.)
3. On the same Love Alarm screen, tap **Battery saver** and choose **No restrictions**.
4. Go to **Settings** → **Notifications** → **Love Alarm**. Make sure notifications are allowed, and turn on **Lock screen notifications**.

If you switch on **Battery saver** or **Ultra battery saver** for the whole phone, alerts can be delayed until you turn it off.

## Updating

When there's a new version, go to the same page (**https://github.com/sudoRKT/LoveAlarm/releases/latest**), download the newer .apk and install it over the top. Don't uninstall first. Your tiles and pairing stay as they are.

## If notifications don't arrive

Go through this list:

- Internet is on (Wi-Fi or mobile data).
- Notifications are allowed for Love Alarm (in the app: **Settings** → Notification permission).
- The background and battery steps above are done, especially on Xiaomi / Redmi / POCO.
- You've opened Love Alarm at least once since installing or updating it.
- Ask the other person to open Love Alarm → **Settings** and tap **Send a test alert to myself**. If their test arrives on their own phone, the server is working and the problem is on your phone's settings.

## Privacy

- Only the two of you are connected. Nobody else can join your pair.
- No accounts, no email, no password.
- No ads.
- Nothing is collected or sold. The app only stores what it needs to deliver your alerts: your names, your tiles, and the alerts you send each other.
