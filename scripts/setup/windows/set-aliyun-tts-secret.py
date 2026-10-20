"""把阿里云智能语音交互（TTS）的凭据写入本地 backend/tts-secret.yml。

用法（任意目录）：
    python scripts/setup/windows/set-aliyun-tts-secret.py

需要准备三样（都在阿里云控制台拿）：
    1. AccessKey ID      —— RAM 子账号的 AK ID（LTAI 开头）
    2. AccessKey Secret  —— 只在创建时显示一次，隐藏输入
    3. AppKey            —— 智能语音交互「项目管理」里创建项目后得到

- Secret 不回显、不进命令历史
- 只写本地文件（已被 .gitignore 排除），不打印内容
"""
import getpass
import io
import os
import re
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
TARGET = os.path.join(REPO, "backend", "tts-secret.yml")


def ask(label):
    return input(label).strip()


def read_secret():
    if sys.stdin is not None and sys.stdin.isatty():
        return getpass.getpass("粘贴 AccessKey Secret（输入不回显，回车确认）: ").strip()
    return sys.stdin.readline().strip()


def main():
    print("阿里云语音合成凭据录入（内容只写到本地 gitignore 文件）")
    print("")

    ak_id = ask("AccessKey ID（LTAI 开头）: ")
    if not re.fullmatch(r"[A-Za-z0-9]{16,32}", ak_id or ""):
        print("AccessKey ID 看起来不对（长度 %d）" % len(ak_id or ""))
        return 2

    ak_secret = read_secret()
    if not ak_secret or len(ak_secret) < 20:
        print("AccessKey Secret 看起来不对（长度 %d）" % len(ak_secret))
        return 2

    appkey = ask("AppKey（项目管理里复制）: ")
    if not re.fullmatch(r"[A-Za-z0-9_-]{8,40}", appkey or ""):
        print("AppKey 看起来不对（长度 %d）" % len(appkey or ""))
        return 2

    region = ask("区域 [cn-shanghai]: ") or "cn-shanghai"

    content = (
        "# 本地阿里云 TTS 配置，已被 .gitignore 排除，不要提交\n"
        "ALIYUN_AK_ID: %s\n"
        "ALIYUN_AK_SECRET: %s\n"
        "ALIYUN_NLS_APPKEY: %s\n"
        "ALIYUN_NLS_REGION: %s\n"
        "# 音色代码先用占位，试听后填真实值；生成脚本会读取这两个值\n"
        "ALIYUN_TTS_VOICE_US: \n"
        "ALIYUN_TTS_VOICE_UK: \n"
    ) % (ak_id, ak_secret, appkey, region)

    with io.open(TARGET, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(content)

    print("")
    print("已写入 " + TARGET)
    print("AK ID 长度 %d、Secret 长度 %d、AppKey 长度 %d（内容未显示）"
          % (len(ak_id), len(ak_secret), len(appkey)))
    print("下一步：在控制台试听英文音色，把美式/英式音色代码填到文件最后两行。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
