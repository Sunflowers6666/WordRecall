/** 单机版本地状态：选中的词库、每批词数、每个词的学习记录。 */
const KEY = 'wr_offline_v1'

function defaults() {
  return { wordbookId: 1, batchSize: 20, records: {} }
}

function load() {
  try {
    const saved = wx.getStorageSync(KEY)
    if (!saved || typeof saved !== 'object') {
      return defaults()
    }
    return {
      wordbookId: saved.wordbookId || 1,
      batchSize: saved.batchSize || 20,
      records: saved.records || {}
    }
  } catch (error) {
    return defaults()
  }
}

function save(state) {
  try {
    wx.setStorageSync(KEY, state)
  } catch (error) {
    // 缓存写失败不影响当前会话
  }
}

function reset() {
  try {
    wx.removeStorageSync(KEY)
  } catch (error) {
    // ignore
  }
}

module.exports = { load, save, reset }
