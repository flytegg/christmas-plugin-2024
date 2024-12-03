# – Flyte Christmas Event Notebook –

## General:

- Add a sled slide in the lobby + carousel for static entertainment while giveaways/game selection is happening.
- Spawn random snowballs for players to throw (keep this minimal)
- eliminated players get titles/nudges to donate while they wait (clickable links)
    - On-Screen Banner ads using bossbars, resource packs and displays.
- Staff Utils Menu:
    - Teleport all players to a “screenie” location. player’s movement is locked and facing forward to facilitate an easy event screenie. (ik stephen
      will love this)

## Game Specific:

### Sled Racing:

- Illuminate blocks with red/green glow to show clear path
- disable two players in one boat
- Fix interacting bug with boats to respawn at spawn point. (have them sneak out)

### Tree Decorating:

- Disable fucking explosives and lava.
    - disable placing lava
    - disable placing TNt
    - disable spawning any type of entity
    - disable placing water
    - disable flint and steel
    - disable fire charges
    - disable firework rockets
    - disable end_crystals
    - disable TNT minecarts
- Autogive players decorations so that their first thought is not to desecrate the tree!

## Donations:

- Christmas tree that grows with each donation
- Donations progress bar somehow.
- "Chaos" donations -> drop TNT in a spleef game, for example.

## Stream:

- Drumroll while rolling givaway?H

# Things to change:

server.properties:
accepts-transfers=true

bukkit.yml:

shutdown-message: |-
§2Thank you for joining us!
§4Merry X-MAS

    §d- Flyte

connection-throttle: -1

paper.yml:
chunk-loading:
autoconfig-send-distance: true
enable-frustum-priority: false
global-max-chunk-load-rate: -1.0
global-max-chunk-send-rate: -1.0
global-max-concurrent-loads: 500.0
max-concurrent-sends: 2
min-load-radius: 2
player-max-chunk-load-rate: -1.0
player-max-concurrent-loads: 20.0
target-player-chunk-send-rate: 100.0
chunk-loading-advanced:
auto-config-send-distance: true
player-max-concurrent-chunk-generates: 0
player-max-concurrent-chunk-loads: 0

# PlayTest Notes:

### Bauble Tag Stuff

- stxphen got eliminated twice in bauble tag (double tag, somehow)
- eliminate players bauble tag properly
- being able to tag twice?
- add remaining players in the sidebar for bauble tag.
- Shrinking border for Bauble Tag possibly. (probably not if using new/better map)

Extra considerations:

- fix bossbar colours
- also rework avalanche roof designs/ideas.
- Remove glowstone from forest map.
- work out scoring ratio (games like paint wars cant add 4000+ points)
- fix the END button to be in winner overview as well.
