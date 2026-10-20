"""Static checks for the mini program sources (no WeChat toolchain required)."""
import json
import os
import re
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
problems = []


def walk(ext):
    for base, _, files in os.walk(ROOT):
        for name in files:
            if name.endswith(ext):
                yield os.path.join(base, name)


# 1. JSON parses
for path in walk(".json"):
    try:
        with open(path, encoding="utf-8") as handle:
            json.load(handle)
    except Exception as error:  # noqa: BLE001
        problems.append("JSON: %s -> %s" % (os.path.relpath(path, ROOT), error))

# 2. JS syntax via node --check
for path in walk(".js"):
    result = subprocess.run(
        ["node", "--check", path],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        problems.append("JS: %s -> %s" % (os.path.relpath(path, ROOT), result.stderr.strip().splitlines()[:3]))

# 3. WXML tag balance
TAG = re.compile(r"<(/?)([a-zA-Z][\w-]*)([^>]*?)(/?)>")
VOID = set()
for path in walk(".wxml"):
    stack = []
    with open(path, encoding="utf-8") as handle:
        source = handle.read()
    source = re.sub(r"<!--.*?-->", "", source, flags=re.S)
    ok = True
    for match in TAG.finditer(source):
        closing, name, _attrs, self_close = match.groups()
        if self_close:
            continue
        if name in VOID:
            continue
        if closing:
            if not stack or stack[-1] != name:
                problems.append("WXML: %s -> unexpected </%s> (stack=%s)"
                                % (os.path.relpath(path, ROOT), name, stack[-3:]))
                ok = False
                break
            stack.pop()
        else:
            stack.append(name)
    if ok and stack:
        problems.append("WXML: %s -> unclosed %s" % (os.path.relpath(path, ROOT), stack))

# 4. every page in app.json has its 4 files, components resolve
with open(os.path.join(ROOT, "app.json"), encoding="utf-8") as handle:
    app = json.load(handle)

for page in app["pages"]:
    for ext in (".js", ".json", ".wxml", ".wxss"):
        target = os.path.join(ROOT, page + ext)
        if not os.path.exists(target):
            problems.append("PAGE: missing %s" % page + ext)

for name, target in app.get("usingComponents", {}).items():
    base = target.lstrip("/")
    for ext in (".js", ".json", ".wxml", ".wxss"):
        full = os.path.join(ROOT, base + ext)
        if not os.path.exists(full):
            problems.append("COMPONENT: %s missing %s" % (name, base + ext))

# 5. tabBar pages must be registered pages
for item in app.get("tabBar", {}).get("list", []):
    if item["pagePath"] not in app["pages"]:
        problems.append("TABBAR: %s not in pages" % item["pagePath"])

print("checked: %d json, %d js, %d wxml" % (
    len(list(walk(".json"))), len(list(walk(".js"))), len(list(walk(".wxml")))))
if problems:
    print("\n".join("FAIL " + item for item in problems))
    sys.exit(1)
print("MINIPROGRAM STATIC CHECKS OK")
