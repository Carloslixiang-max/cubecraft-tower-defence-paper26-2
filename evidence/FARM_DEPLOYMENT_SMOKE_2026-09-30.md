# Farm console deployment smoke — 2026-09-30

An isolated localhost-only Paper server used the actual hash-verified Farm asset and the v107 plugin produced by successful Actions run `36709309580`, source commit `43b42d9218711808000b47130af537bc04646506`.

- Java: Temurin 25.0.4.1+1
- Paper: 26.2 build 129, SHA-256 `b1d8f6bfa1b6101fa8e947b53041cb3bdf5540e7b83b6547ca19ba7edefeb083`
- Plugin SHA-256: `4916fd4b2ead4e08ca993d589add9d4de030736d1f88da68e505604e64e6442c`
- Farm SHA-256: `28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e`
- Engineering origin: `(0,80,0)` in a fresh `ctd-deploy-verify` world
- Online players: **zero**

## Observed results

| Check | Result |
| --- | --- |
| Console `apply` with no coordinates | Refused with explicit-origin instructions |
| Missing world, fractional coordinate, out-of-height volume | Refused; config unchanged and no backup created |
| Explicit origin apply | PASS; no extra +8 offset; strict backup byte-identical |
| Real source `/ctdmapcheck` | 45/45 PASS |
| `/ctdmapplan` | PASS; 125 × 18 × 191; four compiled branch routes; two Guard anchors per team |
| Safe paste | PASS; 24,420 non-air cells written in 429,750-cell volume |
| Setup during active paste | Refused; configuration unchanged |
| Live composition preflight after paste | PASS |
| Six direct-source block sentinels | PASS after paste and after restart |
| Second paste into occupied volume | Refused before overwrite |
| Composition preflight after restart | PASS |
| Three deployment boots/shutdowns | All clean |
| Independent saved-world block-state comparison | **429,750/429,750 match, zero differences, 96 chunks read** |
| Wrong-origin comparison `(1,80,0)` | FAIL as expected, 10,693 differences |
| `/ctdlivegate` full certification | Remains **false** |

The no-player harness retains 96 test chunks with vanilla `forceload`. Without those tickets, Paper can unload the area and vanilla block-condition commands cannot read it; this was investigated before accepting the sentinel observations. Tickets live only in the dedicated disposable test world. They are not a production performance recommendation.

The saved-world check runs after shutdown, reading Paper 26.2 Anvil region NBT independently of the plugin's Bukkit block reads. It normalizes block-state property order and compares both occupied and air cells. The block palette uses the non-spanning packed-long layout; [Prismarine's primary implementation](https://github.com/PrismarineJS/prismarine-chunk/blob/master/src/pc/common/BitArrayNoSpan.js) was consulted for the packing convention. The source asset decoder/checksum is shared with the packager; world region decoding is separate.

## Scope limits

This proves an automated **map deployment/restart path** for the community-adjusted track. It does not certify any player snapshot round-trip, player restart recovery, a played match, two consecutive arena rounds, 60+ tower performance, full original scene reconstruction, GUI/movement/raytrace visual fidelity or production verified-reset gate. Saved block states are compared; entities and beacon block-entity NBT are outside this comparison. Local Mojang authentication/version-update requests encountered DNS errors; no authenticated client connection was tested.

## Reproduce

Use `scripts/verify-farm-deployment.py --help` to supply Java, Paper, the successful CI plugin, your verified schematic and a **new/empty** work directory. The script binds only localhost, refuses existing servers, requires operator EULA acceptance and creates three logs plus `deployment-report.json`. An optional Mojang cache is checked against the supplied Paper download context.

For a stopped saved world only:

```bash
python3 scripts/verify-saved-farm.py \
  --farm /path/to/ImprovedFarm.schem \
  --world /path/to/stopped-server/ctd-deploy-verify --origin 0 80 0
```

Packaging can include matching deployment evidence with `--deployment-evidence /path/to/deployment-directory`. It refuses a report for different plugin/map bytes or logs with changed checksums. The original deployment source commit stays in the report even when a later, byte-identical CI artifact is packaged. These reports are recorded operator evidence, not independent attestations of caller-provided commit identifiers.
