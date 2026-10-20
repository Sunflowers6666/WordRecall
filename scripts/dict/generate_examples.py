"""用大模型按词库难度批量生成例句与中文翻译，写入 backend/data/examples/*.jsonl。

用法（仓库根目录）：
    python scripts/dict/generate_examples.py --limit 20          # 先试 20 个
    python scripts/dict/generate_examples.py                    # 全量
    python scripts/dict/generate_examples.py --wordbook 2       # 只跑 CET-6

- 可断点续跑：已在 jsonl 里的单词会跳过
- 每批 15 词，失败重试 3 次；写文件用 UTF-8
- 不打印 key
"""
import argparse
import io
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
SECRET = os.path.join(ROOT, "backend", "llm-secret.yml")
OUT_DIR = os.path.join(ROOT, "backend", "data", "examples")
MYSQL = os.environ.get("WORDAPP_MYSQL", r"E:\Develop\MySQL\mysql-8.0.46-winx64\bin\mysql.exe")
CNF = os.environ.get("WORDAPP_CNF", r"E:\Develop\MySQL\.secrets\client-app.cnf")

BATCH = 15
LEVELS = {
    "CET-4": "beginner Chinese learners (CET-4). Use very common words and simple grammar; at most 8 words.",
    "CET-6": "intermediate Chinese learners (CET-6). Natural everyday English; at most 12 words.",
    "IELTS": "upper-intermediate Chinese learners (IELTS). Rich but clear English; at most 16 words.",
}
MAX_WORDS = {"CET-4": 12, "CET-6": 16, "IELTS": 20}


def load_secret():
    cfg = {}
    with io.open(SECRET, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            key, _, value = line.partition(":")
            cfg[key.strip()] = value.strip()
    return cfg


def db_rows(sql):
    result = subprocess.run(
        [MYSQL, "--defaults-extra-file=" + CNF, "-D", "wordapp", "-N",
         "--default-character-set=utf8mb4", "-e", sql],
        capture_output=True)
    if result.returncode != 0:
        raise SystemExit("mysql failed: " + result.stderr.decode("utf-8", "ignore")[:300])
    return result.stdout.decode("utf-8")


def load_wordbooks():
    books = []
    for line in db_rows("SELECT id, name FROM wordbook ORDER BY id").splitlines():
        if "\t" in line:
            book_id, name = line.split("\t", 1)
            books.append((int(book_id), name.strip()))
    return books


def load_words(wordbook_id, limit):
    sql = ("SELECT id, spelling, meaning FROM word WHERE wordbook_id=%d ORDER BY sort_order"
           % wordbook_id)
    if limit:
        sql += " LIMIT %d" % limit
    words = []
    for line in db_rows(sql).splitlines():
        parts = line.split("\t")
        if len(parts) >= 3:
            words.append({"id": int(parts[0]), "word": parts[1].strip(),
                          "meaning": parts[2].strip()[:80]})
    return words


def done_ids(path):
    ids = set()
    if os.path.exists(path):
        with io.open(path, encoding="utf-8") as handle:
            for line in handle:
                try:
                    ids.add(json.loads(line)["id"])
                except (ValueError, KeyError):
                    continue
    return ids


def request_batch(cfg, batch, level_text):
    listing = "\n".join("%s (%s)" % (item["word"], item["meaning"]) for item in batch)
    prompt = (
        "Wordbook level: " + level_text + "\n"
        "For each word below write ONE natural English sentence using its most common meaning, "
        "plus a faithful Simplified Chinese translation. Use each target word exactly as given.\n"
        "Return strict JSON: {\"items\":[{\"word\":\"...\",\"sentence\":\"...\",\"translation\":\"...\"}]}\n\n"
        + listing
    )
    payload = {
        "model": cfg["LLM_MODEL"],
        "messages": [
            {"role": "system",
             "content": "You write example sentences for a Chinese vocabulary app. Reply with JSON only."},
            {"role": "user", "content": prompt},
        ],
        "response_format": {"type": "json_object"},
        "temperature": 0.7,
    }
    request = urllib.request.Request(
        cfg["LLM_BASE_URL"].rstrip("/") + "/chat/completions",
        data=json.dumps(payload).encode("utf-8"),
        method="POST")
    request.add_header("Content-Type", "application/json")
    request.add_header("Authorization", "Bearer " + cfg["LLM_API_KEY"])
    with urllib.request.urlopen(request, timeout=120) as response:
        body = json.loads(response.read().decode("utf-8"))
    return json.loads(body["choices"][0]["message"]["content"]).get("items", [])


def valid(item, batch, max_words):
    word = (item.get("word") or "").strip().lower()
    sentence = (item.get("sentence") or "").strip()
    translation = (item.get("translation") or "").strip()
    if not sentence or not translation:
        return False
    if word not in {entry["word"].lower() for entry in batch}:
        return False
    if word not in sentence.lower():
        return False
    if len(sentence.split()) > max_words:
        return False
    return True


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=0, help="每个词库最多处理多少个词（测试用）")
    parser.add_argument("--wordbook", type=int, default=0, help="只处理指定词库 id")
    parser.add_argument("--sleep", type=float, default=0.3, help="每批之间的间隔秒数")
    args = parser.parse_args()

    cfg = load_secret()
    os.makedirs(OUT_DIR, exist_ok=True)

    books = load_wordbooks()
    if args.wordbook:
        books = [book for book in books if book[0] == args.wordbook]

    total_written = 0
    total_calls = 0
    started = time.time()

    for book_id, name in books:
        path = os.path.join(OUT_DIR, "%s.jsonl" % name.lower().replace(" ", "-"))
        done = done_ids(path)
        words = [word for word in load_words(book_id, args.limit or 0)
                 if word["id"] not in done]
        if not words:
            print("[%s] nothing to do (%d already done)" % (name, len(done)))
            continue

        level_text = LEVELS.get(name, "intermediate Chinese learners; at most 12 words.")
        max_words = MAX_WORDS.get(name, 16)
        print("[%s] %d words to generate, file=%s" % (name, len(words), os.path.basename(path)))

        written = 0
        with io.open(path, "a", encoding="utf-8", newline="\n") as out:
            for start in range(0, len(words), BATCH):
                batch = words[start:start + BATCH]
                items = None
                for attempt in range(1, 4):
                    try:
                        items = request_batch(cfg, batch, level_text)
                        total_calls += 1
                        break
                    except urllib.error.HTTPError as error:
                        body = error.read().decode("utf-8", "ignore")[:200]
                        print("  HTTP %s (attempt %d): %s" % (error.code, attempt, body))
                        if error.code in (401, 402, 403):
                            raise SystemExit("LLM auth/quota problem, stop.")
                        time.sleep(5 * attempt)
                    except Exception as error:  # noqa: BLE001
                        print("  error (attempt %d): %s" % (attempt, error))
                        time.sleep(5 * attempt)
                if not items:
                    print("  batch at %d skipped" % start)
                    continue

                by_word = {word["word"].lower(): word for word in batch}
                for item in items:
                    entry = by_word.get((item.get("word") or "").strip().lower())
                    if not entry or not valid(item, batch, max_words):
                        continue
                    out.write(json.dumps({
                        "id": entry["id"],
                        "word": entry["word"],
                        "sentence": item["sentence"].strip(),
                        "translation": item["translation"].strip(),
                    }, ensure_ascii=False) + "\n")
                    written += 1
                out.flush()
                print("  %d/%d (%.0f%%)" % (min(start + BATCH, len(words)), len(words),
                                            100.0 * min(start + BATCH, len(words)) / len(words)))
                time.sleep(args.sleep)

        total_written += written
        print("[%s] wrote %d entries" % (name, written))

    elapsed = time.time() - started
    print("")
    print("done: %d entries, %d calls, %.1fs" % (total_written, total_calls, elapsed))
    return 0


if __name__ == "__main__":
    sys.exit(main())
