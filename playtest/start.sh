#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
if [[ ! -f paper.jar ]]; then
  echo 'Put Paper 26.2 in this directory as paper.jar. See README.zh-CN.md.' >&2
  exit 1
fi
exec java -Xms1G -Xmx4G -jar paper.jar --nogui
