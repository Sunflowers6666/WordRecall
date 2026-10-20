"""把微信小程序 AppSecret 安全写入本地 backend/application-secret.yml。

用法（任意目录均可）：
    python scripts/setup/windows/set-wx-secret.py

为什么用它：
- 交互式输入不回显（getpass），不会留在命令历史里
- 先校验格式（微信 AppSecret 是 32 位字母数字），格式不对不写文件
- 不打印密钥内容，只打印长度
- 顺手清掉误粘进来的多行内容（例如 BEGIN/END PRIVATE KEY 块）

注意：小程序「代码上传密钥」是 RSA 私钥，不是 AppSecret，不要填到这里。

测试用：第一个参数可传一个临时文件路径，避免动到真实配置：
    python scripts/setup/windows/set-wx-secret.py path/to/tmp.yml
（stdin 不是终端时改为从标准输入读一行，便于自动化测试）
"""
import getpass
import io
import os
import re
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
DEFAULT_TARGET = os.path.join(REPO, "backend", "application-secret.yml")
KEY_LINE = "WX_MINIAPP_SECRET:"

SECRET_PATTERN = re.compile(r"^[0-9A-Za-z]{32}$")


def read_secret():
    if sys.stdin is not None and sys.stdin.isatty():
        return getpass.getpass("粘贴 AppSecret（输入不回显，回车确认）: ")
    # 非交互（管道/测试）时读一行，仍然不打印
    return sys.stdin.readline()


def main(argv):
    target = os.path.abspath(argv[1]) if len(argv) > 1 else DEFAULT_TARGET

    if not os.path.exists(target):
        print("找不到 " + target)
        print("请先从 backend/src/main/resources/application-secret.example.yml 复制一份。")
        return 1

    secret = read_secret().strip().strip('"').strip("'")
    if not SECRET_PATTERN.match(secret):
        print("格式不对：微信 AppSecret 是 32 位字母数字，你输入了 %d 个字符。" % len(secret))
        print("没有写入任何内容（原文件保持不变）。")
        return 2

    with io.open(target, encoding="utf-8", newline="") as handle:
        lines = handle.read().splitlines()

    out = []
    found = False
    in_key_block = False

    for line in lines:
        stripped = line.strip()

        if in_key_block:
            if "END" in stripped and "PRIVATE KEY" in stripped:
                in_key_block = False
            continue

        if stripped.startswith("-----BEGIN") and "PRIVATE KEY" in stripped:
            in_key_block = True
            continue

        if stripped.startswith(KEY_LINE):
            out.append("WX_MINIAPP_SECRET: " + secret)
            found = True
            continue

        # 不属于任何配置项的多行残留（例如被拆开的 base64）
        if stripped and ":" not in stripped:
            continue

        out.append(line)

    if not found:
        out.append("WX_MINIAPP_SECRET: " + secret)

    with io.open(target, "w", encoding="utf-8", newline="") as handle:
        handle.write("\n".join(out) + "\n")

    print("已写入 " + target)
    print("配置项 %d 个，AppSecret 长度 %d（内容未显示）" % (len(out), len(secret)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
