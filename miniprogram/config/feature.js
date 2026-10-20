/**
 * 1.0 功能开关：自建词库与文件导入入口默认关闭（阶段 4/5 再打开）。
 * 页面必须读这里的开关，不要直接写死入口可见性。
 *
 * 2026-10-20：阶段 4/5 本地开发中，先打开；发布前再评估是否保留。
 */
module.exports = {
  enableUserWordbook: true,
  enableFileImport: true
}
