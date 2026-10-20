/**
 * 功能开关。
 * - 单机版（config.offline = true）：自建词库与文件导入关闭（没有后端）
 * - 在线版：按需要打开
 */
const config = require('./index')

module.exports = {
  enableUserWordbook: !config.offline,
  enableFileImport: !config.offline
}
