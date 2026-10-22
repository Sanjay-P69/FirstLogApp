#!/usr/bin/env python3
"""Enforce dependency direction and keep presentation rendering free of platform work."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1] / "app/src/main/java"
errors = []
for path in root.rglob("*.kt"):
    source = path.read_text(encoding="utf-8")
    relative = path.relative_to(root)
    if "data" in relative.parts or path.name.endswith("Service.kt"):
        if re.search(r"^import .*\.(?:ui|compose)\.", source, re.M):
            errors.append(f"{relative}: data/platform layer depends on UI")
    if path.name.endswith("Content.kt"):
        forbidden = r"^import (?:android\.|androidx\.lifecycle\.|.*\.data\.|.*Service\b)|\b(?:DataManager|LocalContext|rememberLauncherForActivityResult|getSystemService|LaunchedEffect|produceState)\b"
        if re.search(forbidden, source, re.M):
            errors.append(f"{relative}: Content must only render state and emit actions")
        if re.search(r"Color\(0x|Brush\.|fontSize\s*=", source):
            errors.append(f"{relative}: use Material theme roles and typography")
activity = (root / "com/Sanjay/firstlogapp/MainActivity.kt").read_text(encoding="utf-8")
if re.search(r"registerReceiver|updateMetricState|NetworkCallback|UsageStatsManager", activity):
    errors.append("MainActivity must not monitor devices or mutate telemetry")
if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Architecture boundaries passed.")
