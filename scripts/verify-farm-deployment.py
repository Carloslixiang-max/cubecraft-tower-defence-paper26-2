#!/usr/bin/env python3
"""Opt-in Paper deployment smoke in a NEW localhost-only server directory.

Uses an externally obtained verified Farm. No player/match/reset gate is certified.
Java 25, Paper 26.2, and a green CI plugin are supplied explicitly by the operator.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import shutil
import subprocess
import threading
import time
import zipfile

spec = importlib.util.spec_from_file_location("pack", Path(__file__).with_name("package-playtest.py"))
pack = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pack)
saved_spec = importlib.util.spec_from_file_location("saved", Path(__file__).with_name("verify-saved-farm.py"))
saved = importlib.util.module_from_spec(saved_spec)
saved_spec.loader.exec_module(saved)


class PaperProcess:
    def __init__(self, java, paper, directory, boot):
        self.lines = []
        self.log_path = directory / f"deployment-boot-{boot}.log"
        self.log = self.log_path.open("w", encoding="utf-8")
        self.process = subprocess.Popen(
            [str(java), "-Xms512M", "-Xmx2G", "-Dterminal.jline=false", "-Dterminal.ansi=false",
             "-jar", str(paper), "--nogui"], cwd=directory,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, encoding="utf-8", errors="replace", bufsize=1)

        def read():
            for line in self.process.stdout:
                self.log.write(line)
                self.log.flush()
                self.lines.append(line)
        self.reader = threading.Thread(target=read, daemon=True)
        self.reader.start()
        try:
            self.wait("Done (", timeout=180)
            self.wait("paperAdapters=LIVE_ADAPTER_SMOKE_PASSED")
        except BaseException:
            self.abort()
            raise
        print(f"Boot {boot}: ready", flush=True)

    def wait(self, expected, start=0, timeout=45):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if any(expected in line for line in self.lines[start:]):
                return
            if self.process.poll() is not None:
                raise RuntimeError(f"Paper exited while waiting for {expected}; see {self.log_path}")
            time.sleep(0.1)
        raise TimeoutError(f"Missing {expected}; see {self.log_path}")

    def command(self, command, expected, timeout=45):
        start = len(self.lines)
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()
        self.wait(expected, start, timeout)
        print(f"PASS: {command}", flush=True)
        return start

    def sentinel(self, x, y, z, state, token):
        # No players are present. Forced chunks can still be loading just after boot.
        for attempt in range(9):
            try:
                self.command(f"execute if block {x} {y} {z} {state} run say {token}", token, timeout=5)
                return
            except TimeoutError:
                if attempt == 8:
                    raise

    def stop(self):
        if self.process.poll() is None:
            self.command("stop", "disabled; clean=true")
            self.process.wait(timeout=60)
        self.reader.join(timeout=5)
        self.log.close()
        if self.process.returncode != 0:
            raise RuntimeError(f"Unclean Paper exit: {self.process.returncode}")

    def abort(self):
        if self.process.poll() is None:
            self.process.stdin.write("stop\n")
            self.process.stdin.flush()
            try:
                self.process.wait(timeout=60)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=10)
        self.reader.join(timeout=5)
        self.log.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("java", "paper", "plugin", "farm", "work-dir"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--source-head", required=True)
    parser.add_argument("--mojang-cache", type=Path, help="Optional already-downloaded vanilla server cache file")
    parser.add_argument("--accept-eula", action="store_true", help="Operator confirms Minecraft EULA acceptance")
    args = parser.parse_args()
    if not args.accept_eula:
        parser.error("Read/accept Minecraft EULA before using --accept-eula")
    if not re.fullmatch("[0-9a-f]{40}", args.source_head):
        parser.error("--source-head must be the plugin's full source commit SHA")
    root = args.work_dir.resolve()
    if root.exists() and (not root.is_dir() or any(root.iterdir())):
        parser.error("--work-dir must be absent or an empty directory; existing servers are refused")
    farm = args.farm.read_bytes()
    audit, _ = pack.audit_farm(farm)
    with zipfile.ZipFile(args.paper) as jar:
        cache_sha, _, cache_name = jar.read("META-INF/download-context").decode().strip().split("\t")
    if args.mojang_cache and pack.digest(args.mojang_cache.read_bytes()) != cache_sha:
        parser.error("Mojang cache checksum does not match this Paper download context")
    root.mkdir(parents=True, exist_ok=True)
    target = root / "plugins/CubeCraftTowerDefence/maps"
    target.mkdir(parents=True)
    shutil.copyfile(args.plugin, root / "plugins/CubeCraftTowerDefence.jar")
    (target / "ImprovedFarm.schem").write_bytes(farm)
    (root / "eula.txt").write_text("eula=true\n")
    (root / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25576\nlevel-name=ctd-deploy-verify\n"
        "level-type=minecraft:flat\ngenerate-structures=false\nonline-mode=true\n"
        "view-distance=4\nsimulation-distance=4\nspawn-protection=0\n")
    if args.mojang_cache:
        (root / "cache").mkdir()
        shutil.copyfile(args.mojang_cache, root / "cache" / cache_name)
    config = root / "plugins/CubeCraftTowerDefence/config.yml"
    backup = config.with_name("config.before-engineering-playtest.yml")
    server = None
    try:
        server = PaperProcess(args.java.resolve(), args.paper.resolve(), root, 1)
        before = config.read_bytes()
        server.command("ctdplaytestsetup apply", "Console setup requires an explicit world")
        server.command("ctdplaytestsetup apply __missing_farm_world__ 0 80 0", "Requested map world is missing/not loaded")
        server.command("ctdplaytestsetup apply ctd-deploy-verify 0.5 80 0", "coordinates must be whole")
        server.command("ctdplaytestsetup apply ctd-deploy-verify 0 319 0", "entire schematic must fit inside world Y range")
        assert config.read_bytes() == before and not backup.exists(), "Rejected setup mutated configuration"
        server.command("ctdplaytestsetup apply ctd-deploy-verify 0 80 0", "Engineering playtest profile saved")
        assert backup.read_bytes() == before, "Strict configuration backup differs"
        configured = config.read_bytes()
        server.stop()
        server = None

        # Direct source sentinels; colours in the preview are never used as block evidence.
        nbt = pack.read_nbt(__import__("gzip").decompress(farm))
        w, length = nbt["Width"], nbt["Length"]
        assert all(byte < 128 for byte in nbt["BlockData"]), "Sentinel scan requires one-byte palette IDs"
        sentinels = []
        for state in ("red_stained_glass", "blue_stained_glass", "beacon", "red_wool", "blue_wool", "oak_planks"):
            state = "minecraft:" + state
            index = nbt["BlockData"].index(nbt["Palette"][state])
            y, plane = divmod(index, w * length)
            z, x = divmod(plane, w)
            sentinels.append((x, y + 80, z, state))

        server = PaperProcess(args.java.resolve(), args.paper.resolve(), root, 2)
        server.command("ctdmapcheck", "Farm raw-schematic fixtures: 45/45 PASS")
        server.command("ctdmapplan", "Farm plan PASS:")
        server.command("forceload add 0 0 124 190", "to be force loaded")
        paste_start = server.command("ctdpastefarm 28d24136afe8", "Farm safe paste started.")
        server.command("ctdplaytestsetup apply ctd-deploy-verify 10 80 10", "Wait for the active Farm paste/reset")
        assert config.read_bytes() == configured, "Active paste setup changed configuration"
        server.wait("Farm safe paste complete", paste_start, timeout=180)
        server.command("ctdpreflight", "Composition preflight PASS:")
        for i, (x, y, z, state) in enumerate(sentinels):
            token = f"CTD_DEPLOYMENT_SENTINEL_{i}_PASS"
            server.sentinel(x, y, z, state, token)
        server.command("ctdpastefarm 28d24136afe8", "Destination is not empty", timeout=180)
        server.command("ctdlivegate", "full real-server certification: certified=false")
        server.stop()
        server = None

        server = PaperProcess(args.java.resolve(), args.paper.resolve(), root, 3)
        server.command("ctdpreflight", "Composition preflight PASS:")
        for i, (x, y, z, state) in enumerate(sentinels):
            token = f"CTD_RESTART_SENTINEL_{i}_PASS"
            server.sentinel(x, y, z, state, token)
        server.command("ctdlivegate", "full real-server certification: certified=false")
        server.stop()
        server = None
        comparison = saved.compare_saved_volume(farm, root / "ctd-deploy-verify")
        assert comparison["status"] == "PASS", json.dumps(comparison)
        report = {
            "status": "PASS", "classification": "AUTOMATED_CONSOLE_DEPLOYMENT_SMOKE_ONLY",
            "source_head": args.source_head, "plugin_sha256": pack.digest(args.plugin.read_bytes()),
            "paper_sha256": pack.digest(args.paper.read_bytes()), "farm_sha256": audit["sha256"],
            "boots": 3, "raw_farm_fixtures": "45/45", "paste_non_air_blocks": audit["non_air_blocks"],
            "sentinel_blocks": sentinels, "strict_backup_preserved": True,
            "invalid_setup_and_active_paste_refused": True, "occupied_paste_refused": True,
            "test_chunks_force_loaded": 96,
            "preflight_after_paste_and_restart": True,
            "human_match_certified": False, "full_original_map": False,
            "player_recovery_verified": False, "saved_block_state_comparison": comparison,
            "logs": {p.name: pack.digest(p.read_bytes()) for p in sorted(root.glob("deployment-boot-*.log"))},
        }
        (root / "deployment-report.json").write_text(json.dumps(report, indent=2))
        print("Deployment smoke PASS; human match and full original scene remain pending", flush=True)
    finally:
        if server is not None:
            server.abort()


if __name__ == "__main__":
    main()
