#!/usr/bin/env python3
"""Isolated localhost two-client Paper 26.2 match/reuse/restart test.
Refuses existing server directories. Via and fast ticks are test-only tools.
"""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import urllib.request
import zipfile

def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(filename))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result

deployment = module("deployment", "verify-farm-deployment.py")
pack = module("pack", "package-playtest.py")

def download(url, target, checksum):
    target.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "CubeCraftTD-network-regression/1.0"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                raw = response.read()
            if hashlib.sha256(raw).hexdigest() != checksum:
                raise ValueError(f"Checksum mismatch for {target.name}; refusing unverified bytes")
            target.write_bytes(raw)
            return
        except Exception:
            if attempt == 2:
                raise
            time.sleep(2)

def run_clients(server, root, phase, reports):
    environment = dict(os.environ, CTD_TEST_PHASE=phase)
    process = subprocess.Popen(["node", str(Path(__file__).with_name("network-playtest.cjs").resolve())],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, encoding="utf-8", bufsize=1, env=environment)
    complete = False
    journal = root / "plugins/CubeCraftTowerDefence/recovery"
    try:
        with (root / f"network-clients-{phase}.jsonl").open("w") as log:
            for line in process.stdout:
                log.write(line); log.flush()
                print(line.rstrip(), flush=True)
                try:
                    item = json.loads(line)
                except json.JSONDecodeError:
                    continue
                kind = item.get("type")
                if kind in ("console", "checkpoint"):
                    try:
                        if kind == "console":
                            command = item["command"]
                            expected = item.get("expected")
                            if expected:
                                server.command(command, expected, timeout=45)
                            else:
                                start = len(server.lines)
                                token = f"CTD_NETWORK_ACK_{item['id']}"
                                server.process.stdin.write(command + "\nsay " + token + "\n")
                                server.process.stdin.flush()
                                server.wait(token, start, timeout=45)
                                errors = [line for line in server.lines[start:] if any(text in line for text in
                                    ("Unknown or incomplete command", "Incorrect argument", "<--[HERE]"))]
                                if errors:
                                    raise AssertionError("Console command failed: " + "".join(errors))
                        else:
                            stage = item["stage"]
                            count = len(list(journal.glob("*.snapshot")))
                            if stage == "active":
                                assert count == 2, f"Active match lost player journals: {count}"
                            elif stage in ("restored", "restart_restored"):
                                assert count == 0, f"Restored player journal not cleared: {count}"
                            elif stage == "restart_armed":
                                assert count == 1, f"Restart probe consumed in same process: {count}"
                            else:
                                raise ValueError("Unknown checkpoint " + stage)
                            reports.append({"checkpoint": stage, "pending_journals": count})
                        reply = {"id": item["id"]}
                    except Exception as error:
                        reply = {"id": item["id"], "error": str(error)}
                    process.stdin.write(json.dumps(reply) + "\n")
                    process.stdin.flush()
                elif kind in ("round", "assertion"):
                    reports.append(item)
                elif kind == "complete":
                    complete = True
                elif kind == "failure":
                    raise AssertionError(item["error"])
                if any("tick failed:" in line for line in server.lines):
                    raise AssertionError("Arena tick failed; inspect Paper and client logs")
        assert process.wait(timeout=15) == 0 and complete, "Clients exited without completion"
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact-dir", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    parser.add_argument("--source-head", required=True)
    args = parser.parse_args()
    root = args.work_dir.resolve()
    if root.exists() and any(root.iterdir()):
        parser.error("Existing/nonempty server directory refused")
    root.mkdir(parents=True, exist_ok=True)
    plugins = root / "plugins"
    maps = plugins / "CubeCraftTowerDefence/maps"
    maps.mkdir(parents=True)
    jars = list(args.artifact_dir.glob("libs/*.jar"))
    assert len(jars) == 1, "Expected one built plugin"
    shutil.copyfile(jars[0], plugins / "CubeCraftTowerDefence.jar")
    farm = maps / "ImprovedFarm.schem"
    download("https://drive.google.com/uc?export=download&id=1fCAVGVhO4otbywapp_Ah3EjVRX14foNt",
        farm, "28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e")
    pack.audit_farm(farm.read_bytes())
    paper = root / "paper.jar"
    download("https://fill-data.papermc.io/v1/objects/3cce0a4922baf79bd8e1275ee5ff9f073f24675770e09b0d4be9442d513e7a6b/paper-26.2-134.jar",
        paper, "3cce0a4922baf79bd8e1275ee5ff9f073f24675770e09b0d4be9442d513e7a6b")
    download("https://github.com/ViaVersion/ViaVersion/releases/download/5.12.1/ViaVersion-5.12.1.jar",
        plugins / "ViaVersion.jar", "b8033807e7182c35003a45962352dd642c1e91880ca281da3a43cb6f481a95a2")
    download("https://github.com/ViaVersion/ViaBackwards/releases/download/5.12.1/ViaBackwards-5.12.1.jar",
        plugins / "ViaBackwards.jar", "29d94d3a26c85c8a327574b2322db4078992ae457f0173325431a12aac384911")
    (root / "eula.txt").write_text("eula=true\n")
    (root / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25576\nlevel-name=ctd-network-verify\n"
        "level-type=minecraft:flat\ngenerate-structures=false\nonline-mode=false\n"
        "view-distance=2\nsimulation-distance=2\nspawn-protection=0\nmax-tick-time=120000\n")
    report = {"status": "RUNNING", "source_head": args.source_head,
        "plugin_sha256": pack.digest(jars[0].read_bytes()), "paper_version": "26.2",
        "classification": "AUTOMATED_NETWORK_MATCHES_NOT_HUMAN_VISUAL_CERTIFICATION",
        "human_match_certified": False, "full_original_map": False,
        "native_client_protocol": "26.1 via test-only ViaVersion/ViaBackwards",
        "isolated_test_tick_rate": 100, "events": []}
    server = None
    try:
        java = Path(shutil.which("java"))
        server = deployment.PaperProcess(java, paper, root, 1)
        server.command("ctdinstall create 1", "Managed Farm configured for 1v1")
        server.stop(); server = None
        server = deployment.PaperProcess(java, paper, root, 2)
        server.wait("Managed Farm READY:", timeout=240)
        server.command("ctdpreflight", "Composition preflight PASS:")
        run_clients(server, root, "matches", report["events"])
        assert len([x for x in report["events"] if x.get("type") == "round"]) == 2
        server.stop(); server = None
        journal = root / "plugins/CubeCraftTowerDefence/recovery"
        assert len(list(journal.glob("*.snapshot"))) == 1, "Probe not durable after shutdown"
        server = deployment.PaperProcess(java, paper, root, 3)
        run_clients(server, root, "recovery", report["events"])
        server.command("ctdsnapshotcheck restart-status", "passed=true")
        server.command("ctdpreflight", "Composition preflight PASS:")
        server.stop(); server = None
        logs = [p.read_text() for p in sorted(root.glob("deployment-boot-*.log"))]
        assert all("disabled; clean=true" in log for log in logs)
        assert not any("tick failed:" in log or "Player snapshot round-trip FAIL" in log for log in logs)
        assert "Cross-restart recovery PASS" in logs[-1], "Missing lossless server restart comparison"
        assert len(list(journal.glob("*.snapshot"))) == 0
        report.update(status="PASS", natural_matches=2, consecutive_reuse=True,
            cross_restart_lossless=True, clean_shutdowns=3)
        print("NETWORK_MATCH_REUSE_RESTART_PASS", flush=True)
    except BaseException as error:
        report.update(status="FAIL", error=str(error))
        raise
    finally:
        if server is not None:
            server.abort()
        report["logs"] = {p.name: pack.digest(p.read_bytes()) for p in root.glob("*.log")}
        (root / "network-report.json").write_text(json.dumps(report, indent=2))

if __name__ == "__main__":
    main()
