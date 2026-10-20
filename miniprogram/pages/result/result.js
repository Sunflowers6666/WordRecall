const dateUtil = require('../../utils/date')

Page({
  data: {
    loaded: false,
    date: '',
    dateLabel: '',
    total: 0,
    wrong: 0,
    wrongWords: [],
    allRight: true
  },

  onLoad() {
    const app = getApp()
    const result = app.globalData.reviewResult
    if (!result) {
      // 直接打开结果页（刷新/分享）时没有数据，回到日历
      wx.switchTab({ url: '/pages/index/index' })
      return
    }
    app.globalData.reviewResult = null

    this.setData({
      loaded: true,
      date: result.date,
      dateLabel: dateUtil.dateLabel(result.date),
      total: result.total || 0,
      wrong: result.wrong || 0,
      wrongWords: result.wrongWords || [],
      allRight: !result.wrong
    })
  },

  onAgain() {
    wx.redirectTo({ url: '/pages/review/review?date=' + this.data.date + '&wrongOnly=1' })
  },

  onBack() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
