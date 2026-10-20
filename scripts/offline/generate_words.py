"""把内置词库导出成小程序可直接加载的压缩数据。

用法（仓库根目录）：
    python scripts/offline/generate_words.py

输出：miniprogram/data/wb1.js / wb2.js / wb3.js
每个文件导出一个 base64(gzip(JSON)) 字符串，运行时用 pako 解压。
"""
import base64
import gzip
import io
import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MYSQL = r"E:\Develop\MySQL\mysql-8.0.46-winx64\bin\mysql.exe"
CNF = r"E:\Develop\MySQL\.secrets\client-app.cnf"
OUT_DIR = os.path.join(ROOT, "miniprogram", "data")

BOOKS = [(1, "wb1"), (2, "wb2"), (3, "wb3")]

QUERY = (
    "SELECT spelling, IFNULL(phonetic,''), meaning, "
    "IFNULL(example_sentence,''), IFNULL(example_translation,'') "
    "FROM word WHERE wordbook_id={book_id} ORDER BY sort_order"
)


def fetch(book_id):
    result = subprocess.run(
        [MYSQL, "--defaults-extra-file=" + CNF, "-D", "wordapp", "-N", "-B", "-e",
         QUERY.format(book_id=book_id)],
        capture_output=True, check=True)
    text = result.stdout.decode("utf-8")
    rows = []
    for line in text.splitlines():
        if not line:
            continue
        cells = line.split("\t")
        while len(cells) < 5:
            cells.append("")
        rows.append(cells[:5])
    return rows


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    total = 0
    for book_id, name in BOOKS:
        rows = fetch(book_id)
        raw = json.dumps(rows, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        packed = base64.b64encode(gzip.compress(raw, 9)).decode("ascii")
        content = (
            "// 由 scripts/offline/generate_words.py 生成，请勿手改。\n"
            "// 格式：base64(gzip(JSON[[spelling, phonetic, meaning, example, translation]]))\n"
            "module.exports = '" + packed + "'\n"
        )
        path = os.path.join(OUT_DIR, name + ".js")
        io.open(path, "w", encoding="utf-8", newline="\n").write(content)
        total += len(content)
        print("%s: %d words, raw %.2f MB, module %.2f MB"
              % (name, len(rows), len(raw) / 1048576, len(content) / 1048576))
    print("total %.2f MB" % (total / 1048576))


if __name__ == "__main__":
    sys.exit(main())
