"""用 Windows 系统语音（SAPI）离线生成单词发音，输出 mp3 到 backend/data/audio/<accent>/<wordId>.mp3。

用法（仓库根目录）：
    python scripts/dict/generate_audio.py --limit 20     # 先跑 20 个
    python scripts/dict/generate_audio.py                # 全量
    python scripts/dict/generate_audio.py --accent uk    # 英音（需要系统装 en-GB 语音包）

说明：
- 词库里没有英式语音包时，--accent uk 会提示安装方法后退出
- 断点续跑：已存在的 mp3 会跳过
- 单词语速默认放慢一档（--rate -1），发音更清楚
"""
import argparse
import io
import os
import subprocess
import sys
import time
import wave

import lameenc
import win32com.client

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
WAV_DIR = os.path.join(ROOT, "backend", "target", "tts-wav")
MYSQL = os.environ.get("WORDAPP_MYSQL", r"E:\Develop\MySQL\mysql-8.0.46-winx64\bin\mysql.exe")
CNF = os.environ.get("WORDAPP_CNF", r"E:\Develop\MySQL\.secrets\client-app.cnf")

VOICES = {
    "us": "Microsoft Zira Desktop",
    "uk": "Microsoft Hazel Desktop",
}
DEFAULT_OUT = {"us": "us", "uk": "uk"}


def db_rows(sql):
    result = subprocess.run(
        [MYSQL, "--defaults-extra-file=" + CNF, "-D", "wordapp", "-N",
         "--default-character-set=utf8mb4", "-e", sql],
        capture_output=True)
    if result.returncode != 0:
        raise SystemExit("mysql failed: " + result.stderr.decode("utf-8", "ignore")[:300])
    return result.stdout.decode("utf-8")


def load_words(limit):
    sql = "SELECT id, spelling FROM word ORDER BY wordbook_id, sort_order"
    if limit:
        sql += " LIMIT %d" % limit
    words = []
    for line in db_rows(sql).splitlines():
        parts = line.split("\t")
        if len(parts) >= 2:
            words.append((int(parts[0]), parts[1].strip()))
    return words


def pick_voice(voice, accent):
    voices = voice.GetVoices()
    wanted = VOICES[accent]
    for index in range(voices.Count):
        item = voices.Item(index)
        if item.GetDescription().startswith(wanted):
            return item
    available = [voices.Item(i).GetDescription() for i in range(voices.Count)]
    print("找不到音色: " + wanted)
    print("系统可用音色: " + ", ".join(available))
    if accent == "uk":
        print("安装英式语音：Windows 设置 → 时间和语言 → 语言和区域 → 添加 English (United Kingdom) → 勾选语音包")
    return None


def to_mp3(wav_path, mp3_path):
    with wave.open(wav_path, "rb") as handle:
        frames = handle.readframes(handle.getnframes())
        rate = handle.getframerate()
        channels = handle.getnchannels()
    encoder = lameenc.Encoder()
    encoder.set_bit_rate(48)
    encoder.set_in_sample_rate(rate)
    encoder.set_channels(channels)
    encoder.set_quality(2)
    with open(mp3_path, "wb") as out:
        out.write(encoder.encode(frames))
        out.write(encoder.flush())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--accent", choices=["us", "uk"], default="us")
    parser.add_argument("--rate", type=int, default=-1, help="SAPI 语速，-10..10，默认 -1")
    parser.add_argument("--out", default="", help="输出目录，默认 backend/data/audio/<accent>")
    args = parser.parse_args()

    out_dir = args.out or os.path.join(ROOT, "backend", "data", "audio", DEFAULT_OUT[args.accent])
    os.makedirs(out_dir, exist_ok=True)
    os.makedirs(WAV_DIR, exist_ok=True)

    voice = win32com.client.Dispatch("SAPI.SpVoice")
    picked = pick_voice(voice, args.accent)
    if not picked:
        return 2
    voice.Voice = picked
    voice.Rate = args.rate

    words = load_words(args.limit)
    todo = [(word_id, spelling) for word_id, spelling in words
            if not os.path.exists(os.path.join(out_dir, "%d.mp3" % word_id))]
    print("accent=%s voice=%s total=%d todo=%d out=%s"
          % (args.accent, picked.GetDescription(), len(words), len(todo), out_dir))

    started = time.time()
    written = 0
    for index, (word_id, spelling) in enumerate(todo, 1):
        wav_path = os.path.join(WAV_DIR, "%d.wav" % word_id)
        mp3_path = os.path.join(out_dir, "%d.mp3" % word_id)
        try:
            stream = win32com.client.Dispatch("SAPI.SpFileStream")
            stream.Open(wav_path, 3, False)
            voice.AudioOutputStream = stream
            voice.Speak(spelling, 0)
            stream.Close()
            to_mp3(wav_path, mp3_path)
            written += 1
        except Exception as error:  # noqa: BLE001
            print("  failed %d/%s: %s" % (word_id, spelling, error))
        finally:
            if os.path.exists(wav_path):
                os.remove(wav_path)
            voice.AudioOutputStream = None

        if index % 500 == 0 or index == len(todo):
            elapsed = time.time() - started
            print("  %d/%d (%.0f%%) %.0fs" % (index, len(todo), 100.0 * index / max(len(todo), 1), elapsed))

    total_bytes = 0
    for name in os.listdir(out_dir):
        total_bytes += os.path.getsize(os.path.join(out_dir, name))
    print("done: wrote %d files, dir size %.1f MB, %.0fs"
          % (written, total_bytes / 1024.0 / 1024.0, time.time() - started))
    return 0


if __name__ == "__main__":
    sys.exit(main())
