"""把大模型 API key 写入本地 backend/llm-secret.yml（该文件已被 .gitignore 排除）。

用法（任意目录）：
    python scripts/setup/windows/set-llm-secret.py

- 输入不回显（getpass），不进命令历史
- 默认写 DeepSeek 的 base_url 与模型；其他 OpenAI 兼容服务可改这两个值
- 只打印写入的文件路径与 key 长度，不打印 key
"""
import getpass
import io
import os
import re
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
TARGET = os.path.join(REPO, "backend", "llm-secret.yml")

PROVIDERS = [
    ("deepseek", "https://api.deepseek.com", "deepseek-chat"),
    ("openai", "https://api.openai.com/v1", "gpt-4o-mini"),
    ("other", "", "")
]


def read_secret():
    if sys.stdin is not None and sys.stdin.isatty():
        return getpass.getpass("粘贴 API key（输入不回显，回车确认）: ")
    return sys.stdin.readline()


def main():
    print("选择服务商：")
    for index, (name, base_url, model) in enumerate(PROVIDERS, 1):
        label = "%s（%s / %s）" % (name, base_url or "自定义", model or "自定义")
        print("  %d) %s" % (index, label))

    choice = input("编号 [1]: ").strip() or "1"
    try:
        name, base_url, model = PROVIDERS[int(choice) - 1]
    except (ValueError, IndexError):
        print("编号不对")
        return 1

    if name == "other":
        base_url = input("base_url（例如 https://api.example.com/v1）: ").strip()
        model = input("模型名: ").strip()
        if not base_url or not model:
            print("base_url 与模型名都要填")
            return 1

    key = read_secret().strip()
    if not key or len(key) < 16 or re.search(r"\s", key):
        print("key 看起来不合法（长度 %d）" % len(key))
        print("没有写入任何内容。")
        return 2

    content = (
        "# 本地大模型配置，已被 .gitignore 排除，不要提交\n"
        "LLM_PROVIDER: %s\n"
        "LLM_BASE_URL: %s\n"
        "LLM_MODEL: %s\n"
        "LLM_API_KEY: %s\n"
    ) % (name, base_url, model, key)

    with io.open(TARGET, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(content)

    print("已写入 " + TARGET)
    print("服务商 %s，模型 %s，key 长度 %d（内容未显示）" % (name, model, len(key)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
