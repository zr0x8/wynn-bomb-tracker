# wynn-bomb-tracker

a fabric mod for tracking bombs in wynncraft. hooks into wynntils for the most accurate data (not guaranteed tho).
features include discord webhook relaying and auto replying to guild chat.

## Features

- **discord relay**: forwards bomb bells to discord. uses a cloudflare worker to deduplicate reports (prevents spam if multiple guildies use the mod).
- **guild chat relay**: automatically announces bombs in `/g` when thrown.
- **auto reply**: automatically checks for active bombs and replies when someone asks things like "any loot?" or "any dxp?".

## Setup

### 1. cloudflare worker (very important)
this mod relies on a free cloudflare worker to handle request deduplication.

```bash
cd worker
npm install
npx wrangler login
npx wrangler deploy
```

or manually deploy it [here](https://dash.cloudflare.com)

save the url after u deploy

### 2. build the Mod
navigate to `mod/src/main/java/com/example/wynnbombtracker/BombTrackerClient.java`.
find `WORKER_URL` and replace it with your worker's url.

```java
// replace this with your actual worker url
private static final String WORKER_URL = "https://wynn-bomb-tracker.your-name.workers.dev";
```

then build:
```bash
cd mod
./gradlew build
```
the jar will be in `mod/build/libs/`.

## usage

in-game config (via mod menu or `/wbt`):
- **webhook url**: your discord channel webhook.
- **aliases**: shortcuts for bomb names (e.g., `dxp` -> `Combat XP`).

Commands:
- `/wbt webhook <url>`: quick set webhook.
- `/wbt alias set <bomb> <alias>`: add query alias.
- `/wbt alias list`: view ur aliases.



## dependencies

- wynntils
- fabric api

## recommended mods

- mod menu
- cloth config api

enjoy
bye.
