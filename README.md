# TierSMP (Free Version)
A lightweight Minecraft Paper plugin (1.21.4+) that adds a competitive tier system (S/A/B/C/UNRANKED) to your SMP server. Players gain score through PvP kills and climb the tier ladder, unlocking health boosts, XP multipliers, potion bonuses, and extra inventory slots.

## Features (Free Version)
- S/A/B/C/UNRANKED tier system with per-tier player caps
- PvP-based score progression with kill cooldown anti-abuse
- Combat log penalty system
- Score decay (all players including offline)
- Extra inventory (/einv) for A and S tier
- Tier nametag colors and particle effects
- Server-wide tier change broadcasts
- Kill streak scoreboard for S tier
- PlaceholderAPI support
- Per-world enable/disable
- Full config and messages customization

## Premium Version
Premium version available on BuiltByBit (link coming soon).

Premium includes:
- Mixed progression (PvP + achievement milestones)
- Season system with auto-reset and winner announcements
- High-stakes S tier demotion system
- Score transaction between players (/tierscore give)
- Web dashboard (self-hosted, like BlueMap)
- Full admin web panel
- Specialty skill system (coming in v2)

## Installation
1. Download the latest release from GitHub Releases
2. Drop TierSMP-x.x.x-reobf.jar into your plugins/ folder
3. Restart your server
4. Configure plugins/TierSMP/config.yml to your liking

## Compatibility
- Paper 1.21.4+
- Java 21+
- Optional: PlaceholderAPI

## Commands
| Command | Description | Permission |
|---------|-------------|------------|
| /tier [player] | View tier info | none |
| /tiertop | View leaderboard | none |
| /einv | Open extra inventory | none (A/S tier only) |
| /tieradmin set <player> <tier> | Force set tier | tiersmp.admin |
| /tieradmin reset <player> | Reset player data | tiersmp.admin |
| /tieradmin resetall | Reset all data | tiersmp.admin |
| /tieradmin reload | Reload config | tiersmp.admin |
| /tieradmin setscore <player> <amount> | Set score | tiersmp.admin |
| /tieradmin givescore <player> <amount> | Give/take score | tiersmp.admin |
| /einvsee <player> | Inspect player einv | tiersmp.admin |

## License
GPL-3.0

<!--
# Development workflow:
# - All free version fixes and features: work on main branch
# - Merge main into premium regularly: git checkout premium && git merge main
# - Premium-only features: develop directly on premium branch
# - Never merge premium into main
-->
