# Automated full-match gate

`Paper 26.2 Full Network Playtest` follows a successful main push build and
checks out that exact commit. It downloads the triggering build's JAR rather
than rebuilding a different version. Logs/reports are retained even on failure.
Only a passing run uploads the separate plugin JAR and data-only existing-server
ZIP. The source community Farm is downloaded from its author's public link and
strictly SHA-256 verified, not committed to this repository.

The test creates a fresh localhost-only server, installs the managed dimension,
and connects two real protocol clients. It exercises ordinary queue/voting,
Summoner payment, Archer builder/path/upgrade/rangefinder, natural outcomes,
two consecutive rounds, hub item/XP/mode/position restoration, durable journals,
and a held recovery probe across a third server boot. All three shutdowns must
be clean. A protected/forced end is not a successful natural outcome.

The isolated test uses tick rate 100 after real UI setup to shorten the wait;
production configuration is unchanged. Mineflayer 26.1 clients use test-only
ViaVersion/ViaBackwards with actual Paper 26.2. Native 26.2 human visual
smoothness, 60+ tower performance, resource/rendering fidelity and compatibility
with the user's other plugins remain separate human-server gates. This workflow
must pass before the automated scenarios may be described as verified.
