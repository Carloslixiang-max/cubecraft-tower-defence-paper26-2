# CubeCraft Tower Defence — Paper 26.2 recreation

An **unofficial, open-source reimplementation** of the classic CubeCraft Tower Defence gameplay loop for modern **Paper 26.2 / Java 25** servers.

> This project is not affiliated with, endorsed by, or sponsored by CubeCraft Games. CubeCraft names, branding, maps, textures, and other original assets belong to their respective owners. This repository does **not** bundle original CubeCraft proprietary assets.

## Current status

This repository is an active high-fidelity recreation, not a finished drop-in clone yet.

- Paper target: **26.2**
- Java target: **25**
- Kotlin/JVM plugin
- Current shell lineage: **v32 → GitHub/CI v33**
- Pure-domain baseline before publishing: **315/315 fixtures PASS**
- Real Paper 26.2 live-server certification: **still pending**

The implementation deliberately separates:

1. **direct/recovered gameplay evidence**,
2. **historical/community references**, and
3. **explicit engineering fallbacks** for values that have not been recovered with enough confidence.

Unknown original values are not silently invented.

## What is implemented

The codebase already contains substantial runtime work, including:

- arena-local runtime/indexing and deterministic tick scheduling;
- map/schematic parsing and placement geometry;
- economy, Goldmine, troop sending, progression and rollback;
- route movement, Castle attacks and Castle Guards;
- 11 tower families with upgrade paths and runtime combat abstractions;
- tower placement, management, quick upgrade, sell and rangefinder state;
- Summoner, Bazaar, progression and settings menu projections;
- player snapshot/recovery journal;
- Normal match timing and Armageddon runtime scaffolding;
- Paper-side adapters for entities, LOS, menus, map binding and Stage-4 live tests.

## Build

Paper 26.2 requires Java 25. The project is configured for:

```text
Java toolchain: 25
Kotlin JVM target: 25
Paper API: io.papermc.paper:paper-api:26.2.build.+
```

Because this source snapshot does not currently include a complete Gradle Wrapper distribution, CI installs a pinned Gradle version explicitly.

### GitHub Actions

Every push to `main`, pull request, or manual workflow run executes:

```text
Java 25
Gradle 9.1.0
clean
DomainFixtureSuite via JUnit
shadowJar
artifact upload
```

The generated plugin JAR appears in the workflow run under **Artifacts**.

### Local build

With Java 25 and Gradle 9.1+ installed:

```bash
gradle clean test shadowJar
```

Output:

```text
build/libs/
```

## Paper test commands

The plugin currently exposes engineering/admin commands such as:

- `/ctdstatus`
- `/ctdfixtures`
- `/ctdmapcheck`
- `/ctdready`
- `/ctdlivegate`
- `/ctdpreflight`
- `/ctdfallbacks`
- `/ctdarmageddonfallbacks`
- `/ctdlivetest`
- `/ctdmenu`

These are development/test interfaces and may change before a stable release.

## Maps and assets

The current Farm pipeline expects an external `ImprovedFarm.schem` engineering fixture for validation. It is intentionally not redistributed here unless its redistribution rights are separately verified.

Production-quality tower bodies, particles, projectiles, sounds, and several Mature-era visual details remain active development work.

## Fidelity policy

The project does **not** treat guessed numbers as original CubeCraft truth. Unresolved values stay behind explicit configuration/TruthGate boundaries until they are supported by sufficient evidence or deliberately selected as engineering fallbacks.

See the source under `dev.cubecrafttd.truth` for the runtime fallback model.

## License

Source code in this repository is licensed under **GPL-3.0-only**. See [`LICENSE`](LICENSE).

Third-party names, trademarks, and assets are not granted by this code license. See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
