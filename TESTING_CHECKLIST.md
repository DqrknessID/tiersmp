# TierSMP Server Testing Checklist

This checklist covers all features, commands, perks, and edge cases in the **TierSMP** plugin for testing on your Paper 1.21.4 server.

---

## 🛠️ 1. Build & Server Setup

- [ ] **Build with Gradle**:
  - Run `./gradlew build` (or `.\gradlew.bat build` on Windows) from the `tiersmp/` folder.
  - Verify JAR created at `tiersmp/build/libs/TierSMP-1.0.0.jar`.
- [ ] **Test Server Launch**:
  - Or run `./gradlew runServer` to spin up a local Paper 1.21.4 test server.
- [ ] **Startup Logs**:
  - Verify `[TierSMP] TierSMP enabled!` in console with no startup exceptions.
  - Verify default `config.yml`, `messages.yml`, and `players.yml` are created in `plugins/TierSMP/`.

---

## 🎖️ 2. Tier Progression & Thresholds

| Tier | Min Score | Player Cap (Default) | Extra Inv | Max Health | Speed Effect | XP Boost | Potion Duration Boost |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **S** | `500` (Cap: `505`) | 3 | 18 slots (2 rows) | 32.0 HP (16 ❤) | Speed I | +40% (1.4x) | +30% (1.3x) |
| **A** | `300` | 7 | 9 slots (1 row) | 28.0 HP (14 ❤) | None | +25% (1.25x) | +20% (1.2x) |
| **B** | `150` | 10 | None | 24.0 HP (12 ❤) | None | +10% (1.1x) | None |
| **C** | `50` | 10 | None | 20.0 HP (10 ❤) | None | None | None |
| **UNRANKED** | `0` | Unlimited | None | 20.0 HP (10 ❤) | None | None | None |

### Tests:
- [ ] **Default State**: New players start as `UNRANKED` with `0` score.
- [ ] **Promotion Triggers**:
  - Reach 50 score → Promoted to **C Tier**.
  - Reach 150 score → Promoted to **B Tier**.
  - Reach 300 score → Promoted to **A Tier**.
  - Reach 500 score → Promoted to **S Tier**.
- [ ] **Promotion Broadcasts & Titles**:
  - Verify chat broadcast sent to all players on promotion.
  - Verify on-screen Title & Subtitle displayed to the promoted player.
- [ ] **Demotion Triggers**:
  - Drop below threshold score → Demoted to previous tier.
  - Verify demotion chat broadcast and Title/Subtitle.
- [ ] **Tier Capacity Limits**:
  - Fill tier cap (e.g. 3 players in S tier).
  - Verify another player reaching 500 score cannot join S tier until a slot opens.
- [ ] **S-Tier Cooldown & Cap**:
  - Score capped at `s-score-cap` (`505`).
  - Demoted S-tier player cannot immediately re-enter S-tier until `s-demotion-cooldown-seconds` (`60s`) expires.

---

## ⚔️ 3. PvP & Score System

- [ ] **Kill Score Gains**:
  - Kill Higher Tier: `+60` score.
  - Kill Same Tier: `+30` score.
  - Kill Lower Tier: `+15` score.
- [ ] **Death Score Losses**:
  - Death to Higher Tier: `-9` score.
  - Death to Same Tier: `-15` score.
  - Death to Lower Tier: `-30` score.
- [ ] **Kill Cooldown (Anti-Abuse)**:
  - Kill the same player twice within 5 minutes (`kill-cooldown-minutes`).
  - Verify second kill awards 0 score and shows cooldown notice: *"Kill not counted — cooldown active."*
- [ ] **Kill Streaks (S-Tier)**:
  - Consecutive kills as S-Tier increments kill streak counter.
  - Scoreboard sidebar updates periodically (`streak-scoreboard-update-seconds: 10`).
  - Streak resets to 0 upon death.

---

## 🛡️ 4. Combat Tagging & Combat Logging

- [ ] **Combat Tag Trigger**:
  - Dealing or receiving damage from another player tags player for 15 seconds (`combat-tag-seconds`).
- [ ] **Combat Logging**:
  - Disconnect while combat tagged.
  - Verify player dies immediately.
  - Verify player inventory drops at logout location.
  - Verify server-wide broadcast: `[player] combat logged!`.

---

## ⚡ 5. Tier Perks & Passive Benefits

- [ ] **Max Health Scaling**:
  - `UNRANKED` & `C`: 10 Hearts (20 HP).
  - `B`: 12 Hearts (24 HP).
  - `A`: 14 Hearts (28 HP).
  - `S`: 16 Hearts (32 HP).
- [ ] **Permanent Speed (S-Tier)**:
  - S-tier players receive infinite Speed I with hidden particles.
  - Drinking milk or dying: verify Speed I is reapplied by `SpeedReapplyTask` (every 30s).
- [ ] **XP Multipliers**:
  - Pick up XP orbs on B (1.1x), A (1.25x), and S (1.4x).
  - Verify higher amount of experience points credited.
- [ ] **Potion Duration Boosts**:
  - Drink / splash a potion (e.g. Strength 3:00):
  - A-tier player gets 1.2x duration (3:36).
  - S-tier player gets 1.3x duration (3:54).

---

## 🎒 6. Extra Inventory (`/einv`)

- [ ] **Access Control**:
  - `UNRANKED`, `C`, and `B` tiers: Running `/einv` displays `no-einv-access` error.
  - `A Tier`: Opens 9-slot (1 row) GUI titled `Extra Inventory [A]`.
  - `S Tier`: Opens 18-slot (2 rows) GUI titled `Extra Inventory [S]`.
- [ ] **Item Persistence**:
  - Place items in `/einv`, close inventory, run `/einv` again. Verify items remain.
- [ ] **Death Drop**:
  - Die with items inside extra inventory.
  - Verify all extra inventory items drop on the ground at death location.
  - Verify extra inventory is empty on respawn.
- [ ] **Downgrade Protection**:
  - Downgrade player from A/S tier to B/C while their extra inventory GUI is open.
  - Verify inventory auto-saves and closes with notification.
- [ ] **Admin Inspection (`/einvsee`)**:
  - Admin runs `/einvsee <player>`.
  - Verify GUI opens with target's items and admin edits persist to the player.

---

## 💬 7. Player Commands

- [ ] **`/tier`**:
  - Displays sender's Tier, Score, Kill Streak, and Server Rank.
- [ ] **`/tier <player>`**:
  - Displays target player's Tier, Score, Streak, and Rank.
- [ ] **`/tiertop`**:
  - Displays Top 10 leaderboard formatted with ranks, player names, scores, and tiers.
- [ ] **`/einv`** (Aliases: `/extrainv`, `/extrainventory`):
  - Opens extra inventory GUI (A and S tier).
- [ ] **`/tierscore give <player> <amount>`**:
  - Transfers score from sender to target player.
  - Blocked when transferring to self (`transaction-self`).
  - Blocked when sender doesn't have enough score (`transaction-insufficient`).
  - Blocked while in combat (`transaction-combat-tagged`).
  - Blocked in disabled worlds (`transaction-disabled-world`).
  - Blocked when disabled via `score-transaction-enabled: false`.

---

## 👑 8. Admin Commands (`tiersmp.admin`)

- [ ] **`/tieradmin set <player> <tier>`**:
  - Overrides player's tier directly (S, A, B, C, UNRANKED) and adjusts score to tier minimum.
- [ ] **`/tieradmin setscore <player> <amount>`**:
  - Sets exact score for target player; adjusts tier accordingly.
- [ ] **`/tieradmin givescore <player> <amount>`**:
  - Adds or subtracts score from target player.
- [ ] **`/tieradmin reset <player>`**:
  - Clears score, kills, streak, and einv for the target player.
- [ ] **`/tieradmin resetall`**:
  - First attempt warns: *"Run again within 10 seconds to confirm"*.
  - Second attempt within 10s clears all player data across the server.
- [ ] **`/tieradmin reload`**:
  - Reloads `config.yml` and `messages.yml` without restarting the server.
- [ ] **`/einvsee <player>`** (Alias: `/extrainventorysee`):
  - Opens and allows editing another player's extra inventory.

---

## 💾 9. Data Persistence & Tasks

- [ ] **Auto-Save Task**:
  - Every 5 minutes (`save-interval-minutes`), cached data saves to `players.yml`.
- [ ] **Server Restart Persistence**:
  - Modify scores/tiers, restart server (`/stop`).
  - Verify scores, tiers, and `/einv` contents restore cleanly after reboot.
- [ ] **Score Decay Task**:
  - Decay task runs every 24 hours (`decay-interval-hours`) and deducts 2% (`decay-percent`) score from all players (online & offline).

---

## 🧩 10. PlaceholderAPI (PAPI) Integration

- [ ] Verify Placeholders parse correctly (e.g. with `/papi parse me <placeholder>`):
  - `%tiersmp_tier%` → `S`, `A`, `B`, `C`, `UNRANKED`
  - `%tiersmp_score%` → Current score integer
  - `%tiersmp_streak%` → Current kill streak integer
  - `%tiersmp_rank%` → Leaderboard position (1, 2, 3...)

---

## 🌐 11. World Restrictions (`disabled-worlds`)

- [ ] Add world to `disabled-worlds: ["world_nether"]` in `config.yml` and `/tieradmin reload`.
- [ ] In disabled world:
  - Extra health & speed benefits are removed while in that world.
  - Score gains/losses on PvP kills are suppressed.
  - `/tierscore give` commands are blocked.
