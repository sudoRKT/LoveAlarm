# Love Alarm sender — server setup

Hand this file and the rest of this folder to whoever runs the home server.

## What it is

One small Node container. It watches a Firestore database for new "alerts"
(someone tapped a tile in the Love Alarm app) and sends each one to the other
phone as a push notification through Firebase Cloud Messaging. It only makes
outbound connections to Google. No ports to open, no reverse proxy, nothing
to expose.

Memory use is about 60–80 MB. CPU is idle except for a second or two per alert.

## Files

```
sender/
  index.js             the program
  package.json         dependency list (firebase-admin only)
  package-lock.json    pinned versions
  Dockerfile
  docker-compose.yml   restart: unless-stopped
  secrets/             YOU create this; it holds the Firebase service account key
    serviceAccountKey.json
```

## Steps

1. Copy the whole `sender` folder onto the server, e.g. to `/opt/love-alarm/sender`.
2. Create the folder `secrets` next to `docker-compose.yml` and put the Firebase
   service account key in it, named exactly `serviceAccountKey.json`.
   (The owner has this file in `F:\CODING PROJECTS\_secrets\serviceAccountKey.json`
   on his PC. It is a private key — do not commit it, post it, or put it in a
   shared folder.) Then lock it down:
   ```
   chmod 700 secrets
   chmod 600 secrets/serviceAccountKey.json
   ```
3. From the `sender` folder:
   ```
   docker compose up -d --build
   ```
4. Check it is running:
   ```
   docker compose logs -f
   ```
   A healthy start looks like:
   ```
   2026-10-02T18:00:00.000Z love-alarm-sender starting project=love-alarm-xxxxx maxAge=30min
   2026-10-02T18:00:01.000Z watching pair=AbCdEf123
   ```
   Every push is one line: `sent alert=... from=Name to=... "Drink water 💧"`.
   `skipped ... reason=too-old` means the server was down for more than 30
   minutes when the alert was sent; that is by design, it is not pushed late.
   `skipped ... reason=no-token` means the recipient's phone has not registered
   for push yet (they need to open the app once).

## Updating

If a newer `index.js` arrives, replace the file and run
`docker compose up -d --build` again. The key file stays where it is.

## Settings (optional)

In `docker-compose.yml`:
- `MAX_AGE_MINUTES` — alerts older than this when first seen are skipped. Default 30.
- `TZ` — only affects log timestamps.

## Removing

```
docker compose down
```
