# Contributing

Contributions are welcome, especially fixes that improve Paper 26.2 compatibility, deterministic runtime behavior, tests, and historically supported fidelity.

## Ground rules

- Do not commit proprietary CubeCraft assets.
- Do not present guessed original values as verified facts.
- Keep unresolved original values behind explicit fallback/truth boundaries.
- Add or update fixtures for gameplay changes.
- Keep combat/map queries arena-local; do not introduce global world scans into hot paths.
- Preserve player-state recovery and idempotent teardown behavior.

Before opening a pull request, run:

```bash
gradle clean test shadowJar
```
