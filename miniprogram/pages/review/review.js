const { request } = require('../../utils/request')
const { ensureLogin } = require('../../utils/auth')
const { errorKind } = require('../../utils/feedback')
const dateUtil = require('../../utils/date')

Page({
  data: {
    /** loading | submitting | ready | empty | error */
    state: 'loading',
    errorKind: '',
    date: '',
    dateLabel: '',
    words: [],
    index: 0,
    current: null,
    batchSize: 0,
    progressPercent: 0,
    wrongIds: [],
    wrongOnly: false,
    /** 复习也先遮挡释义，点卡片才显示 */
    revealed: false
  },

  onLoad(query) {
    const date = (query && query.date) || dateUtil.todayIso()
    this.setData({
      date,
      dateLabel: dateUtil.dateLabel(date),
      wrongOnly: !!(query && query.wrongOnly === '1')
    })
    this.load()
  },

  async load() {
    this.setData({ state: 'loading' })
    this.pendingWrongIds = null
    try {
      await ensureLogin()
      const data = await request({ url: '/api/review?date=' + this.data.date })
      let words = data.words || []
      if (this.data.wrongOnly) {
        words = words.filter((word) => word.isWrong)
      }
      if (!words.length) {
        this.setData({ state: 'empty', words: [], batchSize: 0 })
        return
      }
      this.setData({
        state: 'ready',
        words,
        index: 0,
        current: words[0],
        batchSize: words.length,
        progressPercent: 0,
        wrongIds: [],
        revealed: false
      })
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  onRemember() {
    this.advance(false)
  },

  onForgot() {
    this.advance(true)
  },

  onReveal() {
    this.setData({ revealed: true })
  },

  advance(isWrong) {
    const words = this.data.words
    const current = words[this.data.index]
    const wrongIds = this.data.wrongIds.slice()
    if (isWrong && wrongIds.indexOf(current.id) === -1) {
      wrongIds.push(current.id)
    }

    const next = this.data.index + 1
    if (next >= words.length) {
      this.setData({ wrongIds, progressPercent: 100 })
      this.submit(wrongIds)
      return
    }

    this.setData({
      wrongIds,
      index: next,
      current: words[next],
      progressPercent: Math.round((next / words.length) * 100),
      revealed: false
    })
  },

  async submit(wrongIds) {
    // 提交失败重试时只重发提交，不重头复习
    this.pendingWrongIds = wrongIds
    this.setData({ state: 'submitting' })
    try {
      const result = await request({
        url: '/api/review/submit',
        method: 'POST',
        data: { date: this.data.date, wrongWordIds: wrongIds }
      })
      const app = getApp()
      app.globalData.reviewResult = {
        date: this.data.date,
        total: result.total,
        wrong: result.wrong,
        wrongWords: this.data.words.filter((word) => wrongIds.indexOf(word.id) !== -1)
      }
      wx.redirectTo({ url: '/pages/result/result' })
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  onRetry() {
    if (this.pendingWrongIds) {
      this.submit(this.pendingWrongIds)
      return
    }
    this.load()
  }
})
