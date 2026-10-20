# 环境与安装记录（阶段 0）

> 更新时间：2026-10-20  
> 本文件**不包含**数据库密码、JWT 密钥、AppSecret、上传私钥或任何 Token。  
> 本机路径中的用户名统一用 `%USERPROFILE%` 表示，避免把个人目录写进公开仓库。

## 1. 机器

| 项目 | 实际值 |
|---|---|
| 系统 | Windows 11 家庭中文版，build 26200 |
| CPU | AMD Ryzen 7 7840H，8 核 16 线程 |
| 内存 | 15.2 GB |
| C: | 200.0 GB，安装后约 67.7 GB 可用 |
| D: | 274.7 GB，约 131.5 GB 可用 |
| E: | 931.5 GB，约 582.1 GB 可用 |

## 2. 软件清单

| 软件 | 版本 | 路径 | 说明 |
|---|---|---|---|
| JDK | Oracle JDK 17.0.0（17+35-LTS-2724） | `E:\Develop\JDK` | 满足 Spring Boot 3.5 的 Java 17 要求；`JAVA_HOME` 已指向此处 |
| Git | 2.54.0 | `E:\Develop\git\Git` | 复用 |
| MySQL | 8.0.46 | `E:\Develop\MySQL\mysql-8.0.46-winx64` | 本次安装，ZIP 免安装版 |
| GitHub CLI | 2.102.0 | `E:\Develop\gh` | 本次安装，便携版；已加入用户级 PATH |
| 微信开发者工具 | 2.02.2608080 | `E:\Develop\wxDevelop\微信web开发者工具` | 复用，`cli.bat` 存在 |
| Node.js | 24.15.0 | `E:\Develop\node` | 复用；本项目小程序端不用 npm |
| IntelliJ IDEA | 2026.2.2 | `E:\Develop\JavaStudy\IDEA2026\IntelliJ IDEA 2026.2.2` | 复用 |
| VS Code | 1.141.2 | `E:\Develop\Microsoft VS Code` | 复用 |
| Maven | 不安装系统级 | 项目内 `mvnw` | 阶段 1 由 Spring Initializr 生成 wrapper |
| VC++ 运行库 | 已有 | `C:\Windows\System32` | MySQL 运行前提满足 |

> 备注：用户级 PATH 里有 6 条指向旧位置的失效路径（`E:\Python\...`、`E:\JavaStudy\...`、`E:\Microsoft VS Code\bin`），实际安装目录在 `E:\Develop\...`。这不影响本项目，暂不修改；需要清理时单独确认。

## 3. MySQL 8.0.46

### 3.1 安装信息

| 项目 | 值 |
|---|---|
| 来源 | MySQL 官方 CDN：`mysql-8.0.46-winx64.zip` |
| 大小 | 248,009,349 bytes |
| MD5 | `003f527d5df61b663ff191038cd676bd`（已校验） |
| 安装目录 | `E:\Develop\MySQL\mysql-8.0.46-winx64` |
| 数据目录 | `E:\Develop\MySQL\data` |
| 临时目录 | `E:\Develop\MySQL\tmp` |
| 日志 | `E:\Develop\MySQL\logs\mysql-error.log` |
| 配置 | `E:\Develop\MySQL\my.ini` |
| 端口 / 绑定 | `3306` / `127.0.0.1`（不对公网开放） |
| 字符集 | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| 时区 | `+08:00` |
| 启动方式 | 方式 B：不注册 Windows 服务，用脚本按需启动 |

### 3.2 数据库与账号

| 账号 | 主机 | 权限 |
|---|---|---|
| `root` | `localhost` | 管理员（仅本地） |
| `wordapp` | `localhost` | `wordapp.*` 的 `SELECT/INSERT/UPDATE/DELETE` |
| `wordapp` | `127.0.0.1` | 同上 |

- 数据库：`wordapp`，`utf8mb4` / `utf8mb4_0900_ai_ci`。
- 应用账号**没有** DDL 权限；建表/改表由阶段 1 的 SQL 脚本以 root 执行。
- 已实测：root 空密码连接被拒绝（`ERROR 1045`）。

### 3.3 密码与本地密钥文件

密码由本机随机生成，**没有打印到聊天，也没有写入仓库**。文件位置：

```text
E:\Develop\MySQL\.secrets\root.txt
E:\Develop\MySQL\.secrets\app.txt
E:\Develop\MySQL\.secrets\client-root.cnf
E:\Develop\MySQL\.secrets\client-app.cnf
```

- `.secrets` 目录已用 `icacls` 移除继承权限，只保留当前 Windows 用户可访问。
- `client-root.cnf` / `client-app.cnf` 供 `mysql` / `mysqladmin` 读取，避免把密码放到命令行。
- 该目录在项目仓库之外，`.gitignore` 仍会排除同类文件作为第二道防线。

### 3.4 启停命令

在仓库根目录执行：

```bat
scripts\setup\windows\start-mysql.cmd
scripts\setup\windows\status-mysql.cmd
scripts\setup\windows\stop-mysql.cmd
```

- `start-mysql.cmd`：用隐藏窗口启动 `mysqld`，不注册服务、不需要管理员。
- `status-mysql.cmd`：`mysqladmin ping`，输出 `mysqld is alive` 即正常。
- `stop-mysql.cmd`：`mysqladmin shutdown`。
- 如需开机自启：把 `scripts\setup\windows\start-mysql-hidden.vbs` 的快捷方式放到 `shell:startup`（可选，需你确认后做）。

### 3.5 验证输出（实际）

```text
version   datadir                      charset  collation              tz
8.0.46    E:\\Develop\\MySQL\\data\\    utf8mb4  utf8mb4_0900_ai_ci    +08:00

SCHEMA_NAME  DEFAULT_CHARACTER_SET_NAME  DEFAULT_COLLATION_NAME
wordapp      utf8mb4                     utf8mb4_0900_ai_ci

user      host        plugin
root      localhost   caching_sha2_password
wordapp   127.0.0.1   caching_sha2_password
wordapp   localhost   caching_sha2_password

-- wordapp 连接
cur_user              db
wordapp@127.0.0.1     wordapp

GRANT USAGE ON *.* TO `wordapp`@`localhost`
GRANT SELECT, INSERT, UPDATE, DELETE ON `wordapp`.* TO `wordapp`@`localhost`
GRANT USAGE ON *.* TO `wordapp`@`127.0.0.1`
GRANT SELECT, INSERT, UPDATE, DELETE ON `wordapp`.* TO `wordapp`@`127.0.0.1`
```

可重复执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\setup\windows\verify-mysql.ps1
```

## 4. Maven 本地仓库

- 文件：`%USERPROFILE%\.m2\settings.xml`（新建，当前无旧配置）
- 内容：`<localRepository>E:/Develop/Maven/repository</localRepository>`
- 已创建目录：`E:\Develop\Maven\repository`
- `mvnw` 首次运行下载的 Maven 分发包仍在 `%USERPROFILE%\.m2\wrapper`（约 10 MB），可接受。

## 5. GitHub CLI

| 项目 | 值 |
|---|---|
| 版本 | 2.102.0 |
| 路径 | `E:\Develop\gh\bin\gh.exe` |
| 安装包 | `gh_2.102.0_windows_amd64.zip`，SHA256 `ae64e556ecc240b200f7eba60d550e4bb60d78e860e69dd88c449405b86067f4`（已校验） |
| PATH | 已加入**用户级** PATH：`E:\Develop\gh\bin` |
| 登录状态 | **未登录**，需要执行 `gh auth login` 并由你在浏览器授权 |

安装脚本：`scripts\setup\windows\install-gh.ps1`（幂等，重复执行会跳过已安装）。

## 6. 微信开发者工具与缓存

- 工具版本：2.02.2608080，安装目录 `E:\Develop\wxDevelop\微信web开发者工具`。
- 用户数据/缓存默认在：`%LOCALAPPDATA%\微信开发者工具\User Data`，当前约 **248.8 MB**。
- 工具本身没有提供修改缓存目录的官方设置项；直接搬走目录可能导致工具异常。
- 可选的非官方方案（**未执行**）：关闭工具 → 把 `User Data` 移到 E 盘 → 在原位置建立目录联接（junction），工具仍按原路径访问。脚本：`scripts\setup\windows\move-wechat-devtools-data.ps1`（默认只打印计划；`-Execute` 执行，`-Rollback` 回滚；执行前必须关闭工具）。
- 项目代码、编译产物都在 `E:\Develop\Projects\WordRecall`，不在 C 盘。

## 7. C 盘占用风险清单（安装后）

| 路径 | 当前占用 | 建议 | 处理方式 |
|---|---:|---|---|
| `%USERPROFILE%\.m2\repository` | 0（已指向 E） | 已处理 | `settings.xml` 指向 `E:\Develop\Maven\repository` |
| `%LOCALAPPDATA%\JetBrains` | 约 895 MB | 暂不迁移 | 需要时用 `idea.properties` 指向 E，迁移前关闭 IDEA 并备份 |
| `%APPDATA%\JetBrains` | 约 34 MB | 暂不迁移 | 随 config path 一起迁移 |
| `%LOCALAPPDATA%\微信开发者工具` | 约 249 MB | 可选 | 需要时用 junction 方案；当前不执行 |
| `%LOCALAPPDATA%\Tencent` | 约 632 MB | 不动 | 微信聊天数据，与本项目无关 |
| `%LOCALAPPDATA%\Temp` | 约 4.4 GB | 可选清理 | 用 Windows 磁盘清理；不改系统 TEMP/TMP |
| `%USERPROFILE%\.m2\wrapper` | 首次运行后约 10 MB | 保留 | 体积小，可接受 |
| MySQL data | 约 193 MB（在 E） | 已处理 | 数据在 `E:\Develop\MySQL\data` |

## 8. 待办

- [x] 配置 Git 提交身份：`Sunflowers6666 <307085798+Sunflowers6666@users.noreply.github.com>`
- [ ] `gh auth login` 登录 GitHub。
- [ ] 创建 GitHub 仓库 `WordRecall`（public）。
- [ ] 阶段 1：Spring Boot 骨架、MySQL 表结构、词库导入、WxJava + JWT 登录。
- [ ] 小程序 AppID 填入 `project.config.json`（阶段 1）。
