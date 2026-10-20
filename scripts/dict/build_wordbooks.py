#!/usr/bin/env python3
"""Build CET-4 / CET-6 / IELTS wordbook JSON files from ECDICT.

Data source: https://github.com/skywind3000/ECDICT (MIT License,
Copyright (c) 2025 Linwei). ECDICT does not ship example sentences yet,
so exampleSentence / exampleTranslation are null in the generated files.

Usage:
  python build_wordbooks.py --input E:\\Develop\\_downloads\\ecdict.csv \
      --output-dir backend\\src\\main\\resources\\dict
"""

import argparse
import csv
import json
import os
from collections import Counter

BOOKS = [
    ("cet4", "CET-4", "大学英语四级核心词汇"),
    ("cet6", "CET-6", "大学英语六级核心词汇"),
    ("ielts", "IELTS", "雅思核心词汇"),
]

ATTRIBUTION = (
    "ECDICT (https://github.com/skywind3000/ECDICT), "
    "MIT License, Copyright (c) 2025 Linwei"
)


def normalize_text(value):
    if not value:
        return ""
    text = value.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n")
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    lines = [line.strip() for line in text.split("\n")]
    lines = [line for line in lines if line]
    return "\n".join(lines)


def to_int(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="Path to ecdict.csv")
    parser.add_argument("--output-dir", required=True, help="Directory for cet4.json etc.")
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)
    rows = {key: [] for key, _, _ in BOOKS}
    seen = {key: set() for key, _, _ in BOOKS}
    tag_counter = Counter()
    total = 0

    with open(args.input, "r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            total += 1
            tags = (row.get("tag") or "").split()
            for tag in tags:
                tag_counter[tag] += 1

            for key, _, _ in BOOKS:
                if key not in tags:
                    continue
                spelling = (row.get("word") or "").strip()
                if not spelling or spelling.lower() in seen[key]:
                    continue
                meaning = normalize_text(row.get("translation") or row.get("definition") or "")
                if not meaning:
                    continue
                seen[key].add(spelling.lower())
                rows[key].append({
                    "spelling": spelling,
                    "phonetic": (row.get("phonetic") or "").strip(),
                    "meaning": meaning,
                    "exampleSentence": None,
                    "exampleTranslation": None,
                    "sortOrder": 0,
                    "_frq": to_int(row.get("frq")),
                    "_bnc": to_int(row.get("bnc")),
                })

    for key, name, description in BOOKS:
        words = rows[key]
        words.sort(key=lambda item: (
            item["_frq"] if item["_frq"] > 0 else 10 ** 9,
            item["_bnc"] if item["_bnc"] > 0 else 10 ** 9,
            item["spelling"].lower(),
        ))
        for index, word in enumerate(words, start=1):
            word["sortOrder"] = index
            word.pop("_frq", None)
            word.pop("_bnc", None)

        payload = {
            "wordbook": name,
            "description": description,
            "source": "BUILTIN",
            "license": "MIT",
            "attribution": ATTRIBUTION,
            "words": words,
        }
        path = os.path.join(args.output_dir, f"{key}.json")
        with open(path, "w", encoding="utf-8") as out:
            json.dump(payload, out, ensure_ascii=False, separators=(",", ":"))
        print(f"{name}: {len(words)} words -> {path}")

    print(f"total CSV rows: {total}")
    print("top tags:", ", ".join(f"{tag}={count}" for tag, count in tag_counter.most_common(15)))


if __name__ == "__main__":
    main()
