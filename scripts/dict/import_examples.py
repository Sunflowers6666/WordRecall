"""把 backend/data/examples/*.jsonl 导入 word.example_sentence / example_translation。

用法（仓库根目录）：
    python scripts/dict/import_examples.py            # 写入数据库
    python scripts/dict/import_examples.py --dry-run  # 只统计，不写库

只更新 example_sentence 为空的行，不覆盖已有内容。
"""
import argparse
import io
import json
import os
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
DATA_DIR = os.path.join(ROOT, "backend", "data", "examples")
SQL_FILE = os.path.join(ROOT, "backend", "target", "import-examples.sql")
MYSQL = os.environ.get("WORDAPP_MYSQL", r"E:\Develop\MySQL\mysql-8.0.46-winx64\bin\mysql.exe")
CNF = os.environ.get("WORDAPP_CNF", r"E:\Develop\MySQL\.secrets\client-app.cnf")


def sql_escape(value):
    return value.replace("\\", "\\\\").replace("'", "''")


def load_entries():
    entries = []
    if not os.path.isdir(DATA_DIR):
        return entries
    for name in sorted(os.listdir(DATA_DIR)):
        if not name.endswith(".jsonl"):
            continue
        path = os.path.join(DATA_DIR, name)
        with io.open(path, encoding="utf-8") as handle:
            for line in handle:
                line = line.strip()
                if not line:
                    continue
                try:
                    entries.append(json.loads(line))
                except ValueError:
                    continue
    return entries


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    entries = load_entries()
    print("entries loaded: %d" % len(entries))
    if not entries:
        print("nothing to import (run generate_examples.py first)")
        return 1

    os.makedirs(os.path.dirname(SQL_FILE), exist_ok=True)
    with io.open(SQL_FILE, "w", encoding="utf-8", newline="\n") as sql:
        for entry in entries:
            word_id = entry.get("id")
            sentence = (entry.get("sentence") or "").strip()
            translation = (entry.get("translation") or "").strip()
            if not word_id or not sentence:
                continue
            sql.write(
                "UPDATE word SET example_sentence='%s', example_translation='%s' "
                "WHERE id=%d AND (example_sentence IS NULL OR example_sentence='');\n"
                % (sql_escape(sentence), sql_escape(translation), int(word_id)))

    if args.dry_run:
        print("dry run: SQL written to %s" % SQL_FILE)
        return 0

    result = subprocess.run(
        [MYSQL, "--defaults-extra-file=" + CNF, "-D", "wordapp",
         "--default-character-set=utf8mb4"],
        stdin=io.open(SQL_FILE, "r", encoding="utf-8"),
        capture_output=True)
    if result.returncode != 0:
        print("mysql failed: " + result.stderr.decode("utf-8", "ignore")[:400])
        return 1

    coverage = subprocess.run(
        [MYSQL, "--defaults-extra-file=" + CNF, "-D", "wordapp", "-N", "-e",
         "SELECT wordbook_id, COUNT(*) AS total, SUM(example_sentence IS NOT NULL) AS with_example "
         "FROM word GROUP BY wordbook_id ORDER BY wordbook_id"],
        capture_output=True).stdout.decode("utf-8", "ignore")
    print("imported. coverage (wordbook_id, total, with_example):")
    print(coverage.strip())
    return 0


if __name__ == "__main__":
    sys.exit(main())
