### `server.properties`:

```properties
accepts-transfers=true
allow-flight=true
difficulty=peaceful
enforce-secure-profile=false
pvp=true
rate-limit=-1
simulation-distance=100
view-distance=100
```

### `bukkit.yml`:

```yaml
  connection-throttle: -1
```

### `spigot.yml`

```yaml
world-settings:
  default:
    view-distance: 400
    entity-tracking-range:
      display: 600
settings:
  moved-too-quickly-multiplier: 100.0
```

### `paper-global.yml`:

```yaml
chunk-loading-advanced:
  auto-config-send-distance: true
  player-max-concurrent-chunk-generates: 0
  player-max-concurrent-chunk-loads: 0
chunk-loading-basic:
  player-max-chunk-generate-rate: -1.0
  player-max-chunk-load-rate: -1.0
  player-max-chunk-send-rate: -1.0
chunk-system:
  gen-parallelism: default
  io-threads: -1
  worker-threads: -1
```