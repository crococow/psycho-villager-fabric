# Psycho Villagers (Fabric 26.3)

An original Fabric mod for Minecraft 26.3, inspired by the idea of hidden psychos among villagers. Written from scratch, it contains no code from other mods.

## What it does

- Each villager has a 10% chance (configurable) to be a hidden psycho. The roll happens once and is saved with the villager.
- During the day a psycho behaves like a normal villager.
- At night a psycho looks for a vulnerable victim within 20 blocks:
  - a player who is asleep, wounded (≤50% HP), or alone in the dark (no golem or other players nearby);
  - a baby, sleeping or lonely villager, but only when there are no witnesses (players or iron golems).
- While hunting, the psycho pulls out an iron sword, sneaks up and strikes. If you are the target, you hear a heartbeat.
- **Detective's Notebook** (crafted from book + ink sac + spider eye, shapeless): right-click a villager to record an observation.
  - A psycho shows a suspicious clue 75% of the time, an innocent villager 10% of the time.
  - A psycho who has already killed always has blood on their sleeves.
  - The observation is tied to the villager, so asking again won't help.

## Commands (OP)

- `/psychovillagers spawn` spawns a psycho at your position.
- `/psychovillagers reveal` makes psychos within 64 blocks glow for 10 seconds.
- `/psychovillagers reload` reloads the config.

## Config

`config/psychovillagers.json` is created on first launch and lets you set:

- psycho chance;
- damage;
- hunt radius;
- night-only mode;
- attacks on players and/or villagers;
- sword visibility;
- horror sounds;
- clue probabilities.

## Build

You need **JDK 25**.

```bash
./gradlew build
```

The finished file is at `build/libs/psychovillagers-fabric-26.3-1.0.0.jar`. Put it in `mods/` together with **Fabric API 0.165.0+26.3** (Fabric Loader 0.20.1+).
