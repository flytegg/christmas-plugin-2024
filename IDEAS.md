# – Flyte Christmas Event Notebook –

## General:

- Add a sled slide in the lobby + carousel for static entertainment while giveaways/game selection is happening.
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
    - disable spawning any type of entity
    - disable firework rockets
    - disable TNT minecarts
- Autogive players decorations so that their first thought is not to desecrate the tree!

## Donations:

- Christmas tree that grows with each donation
- Donations progress bar somehow.
- "Chaos" donations -> drop TNT in a spleef game, for example.

## Donation Events:

### Spleef:
 - Low-tier
    - A few seconds of unlimited double jump
    - +1 double jump for everyone
    - Powerful snowballs for 10 seconds
- Medium-tier
    - Spawn a fast-shooting snow golem riding a flying mob (Bee maybe? Invisible maybe?)
    - Spawn a fast-shooting snow golem on the layer 1 sweats
- High-tier
    - Remove the bottom layer with a 5-second warning, can only be used once
    - Start snowing snowballs from the sky

### Bauble Tag:
 - Low-tier
    - Make everyone glow for 10 seconds
    - Double everyone's speed for 10 seconds
 - Medium-tier
    - Randomly swap a tagged player
    - Teleport everyone to the middle
 - High-tier
    - Explode one random player, killing nearby players too. Disabled in the final round.
    - Explode specifically stephen

### Paintball:
  - Low-tier
    - Make everyone glow for 10 seconds
    - Give everyone nausea for 10 seconds
  - Medium-tier
    - Give everyone a sniper
    - Spawn a snow golem in the middle of the map
  - High-tier
    - Placeholder
    - Placeholder

### King of the Hill:
  - Low-tier
    - Add 3-5 double jumps
    - 5 seconds of slow falling
    - King's blindness
    - 5 seconds of jump boost
  - Medium-tier
    - 5 seconds of stacked kb
    - 8 seconds of invisibility
  - High-tier
    - Swap locations of everyone
    - Inversed knockback for 8 seconds  

## Stream:

- Drumroll while rolling giveaway?

# PlayTest Notes:

### Bugs which I don't want to spam on the discord
- Terminating a game in its camera phase

### PLAYTEST BUGS:
- Spectators can hit eachother in avalanche 
- Spectate point on bauble tag is not mapped properly (after being eliminated)
- bauble tag message about players being regrouped continues in red.
- game summary shows 2st in bauble tag for second place.
- random self-boost has a bracket block party 
  - random opening bracket '{' in block party powerup.
- chat validation error
- visual indication of double jump in block party.
- double NPCs at lobby podium.
- YOU null in new dataSupplier scoreboards.
- donation nudge: "so $10 means etc... ()" <- check that 'so' is in small text 
- non-eliminating games seem to have broken leaderboards:
  - KOTH leaderboard summary is broken
  - paintball leaderboard not working 
  - paintwars leaderboard is broken
  - sled racing leaderboard is broken
- spleef non opped players cant break blocks.
- 'Instructions:' in startGameOverview is not in small text
- musical minecarts too much y velocity and they float way too high 
- minimessage for minecrats not formatting properly
- reduce the minecart item win animation (halve it)
- for API donations, truncate the finalAmount to two decimal places.
- $10 translated into $10.001010 smt
- Donation events in paintball are not announced properly
- Streamline game overviews to be the same durationg
  - paintwars overview a bit too long
  - BaubleTag too short
- sled racing broken, can leave boats
  - add two passengers to sled racing?
- sled racing only do one lap

Lag related:
- lag spike with donation events when submitted.
- see issues of server lagging
