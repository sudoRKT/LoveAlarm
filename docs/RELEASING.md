# Releasing Love Alarm

How a new version gets from the code to both phones. Everything is built on GitHub Actions; nothing needs to be installed on the PC.

## One-time setup: the five GitHub secrets

Go to **github.com/sudoRKT/LoveAlarm → Settings → Secrets and variables → Actions → New repository secret** and add each of these. The name must match exactly (capitals and underscores).

| Name | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | The full contents of `google-services.json` downloaded from Firebase. Open it in Notepad, select all, copy, paste. |
| `KEYSTORE_BASE64` | The full contents of `F:\CODING PROJECTS\_secrets\lovealarm-release.jks.base64.txt` (one long line). |
| `KEYSTORE_PASSWORD` | The value after `KEYSTORE_PASSWORD=` in `_secrets\keystore.properties`. |
| `KEY_ALIAS` | `lovealarm` |
| `KEY_PASSWORD` | The value after `KEY_PASSWORD=` in `_secrets\keystore.properties` (it's the same as the store password; that's normal). |

Paste values only, with no quotes or spaces around them. GitHub never shows a secret again after you save it. To change one, use **Update**.

## Every release

### 1. Bump the version

Open `app/build.gradle.kts` and find:

```kotlin
versionCode = 1
versionName = "1.0.0"
```

- `versionCode` is a whole number that **must go up by at least 1 every release**. Phones refuse to install an update whose versionCode is the same as or lower than what's installed.
- `versionName` is the label people see. Keep it in step with the tag, e.g. `"1.0.1"` for tag `v1.0.1`.

Commit and push that change to `main`. Wait for the **Build check** workflow to go green.

### 2. Push a tag

The tag is `v` followed by the versionName, e.g. `v1.0.1`.

From a terminal in the project folder:

```
git tag v1.0.1
git push origin v1.0.1
```

Don't create the release by hand on GitHub's Releases page. The workflow creates it, and it fails if a release with that tag already exists.

### 3. Let the workflow run

The **Release** workflow (`.github/workflows/release.yml`) starts automatically. It:

1. writes `google-services.json` from the secret,
2. unpacks the signing key from `KEYSTORE_BASE64`,
3. builds and signs the release APK,
4. stops with an error if the APK came out unsigned,
5. renames it `LoveAlarm-v1.0.1.apk` and makes a matching `.sha256` checksum file,
6. creates a GitHub Release called **Love Alarm v1.0.1** with both files attached.

The download page for the phones is always https://github.com/sudoRKT/LoveAlarm/releases/latest.

### Test build without releasing

**Actions → Release → Run workflow** builds a signed APK you can download from the run's **Artifacts** section, without creating a public release. Useful for trying something on your own phone first.

## If the workflow fails

Open the failed run and read the red error line. The workflow names the problem in plain words:

- "GOOGLE_SERVICES_JSON secret is empty" / "KEYSTORE_BASE64 secret is empty": add or re-paste that secret.
- "produced app-release-unsigned.apk": one of the four signing secrets is missing or blank.
- A Gradle error mentioning the keystore password or "Given final block not properly padded": the password secret doesn't match the keystore. Re-copy it from `keystore.properties`.
- "release already exists": that tag was used before. Bump the version and use a new tag.

## The rule that never changes

The keystore (`lovealarm-release.jks`) and its password are permanent. Every release, forever, must be signed with that exact file. If it's lost or replaced, phones will refuse to install updates over the existing app and both of you would have to uninstall and start again. Never generate a new one, and keep a backup of the whole `_secrets` folder off this PC. See `_secrets\README-SECRETS.md`.
