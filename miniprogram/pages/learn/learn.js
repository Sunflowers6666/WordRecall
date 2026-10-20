const { request } = require('../../utils/request')
const { ensureLogin } = require('../../utils/auth')
const { errorKind } = require('../../utils/feedback')
const config = require('../../config/index')
const dateUtil = require('../../utils/date')

Page({
  data: {
    /** loading | ready | finished | error */
    state: 'loading',
    errorKind: '',
    words: [],
    index: 0,
    current: null,
    batchSize: 0,
    todayWordCount: 0,
    remaining: 0,
    progressPercent: 0,
    revealHint: config.revealMeaning,
    revealed: false,
    finishedTitle: '',
    finishedSub: ''
  },

  onLoad() {
    this.start()
  },

  async start() {
    this.setData({ state: 'loading' })
    try {
      await ensureLogin()
      const data = await request({
        url: '/api/study/sessions',
        method: 'POST',
        // 不传 size：由服务端按用户设置的每批词数返回
        data: {}
      })
      const words = data.words || []

      if (!words.length) {
        this.setData({
          state: 'finished',
          words: [],
          batchSize: 0,
          todayWordCount: data.todayWordCount || 0,
          remaining: 0,
          finishedTitle: '这个词库学完了',
          finishedSub: '去复习一下学过的词吧'
        })
        return
      }

      this.setData({
        state: 'ready',
        words,
        index: 0,
        current: words[0],
        batchSize: words.length,
        todayWordCount: data.todayWordCount || 0,
        remaining: data.remaining || 0,
        progressPercent: 0,
        revealed: !config.revealMeaning
      })
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  onReveal() {
    if (!this.data.revealed) {
      this.setData({ revealed: true })
    }
  },

  onPrev() {
    if (this.data.index === 0) {
      return
    }
    const index = this.data.index - 1
    this.setData({
      index,
      current: this.data.words[index],
      revealed: !config.revealMeaning
    })
    this.updateProgress()
  },

  async onNext() {
    if (this.saving) {
      return
    }
    const next = this.data.index + 1
    const saved = await this.saveWord(this.data.current.id)
    if (!saved) {
      return
    }
    if (next >= this.data.words.length) {
      this.setData({
        state: 'finished',
        progressPercent: 100,
        finishedTitle: '本批 ' + this.data.batchSize + ' 词已学完',
        finishedSub: '今天已学 ' + this.data.todayWordCount + ' 个单词'
      })
      return
    }
    this.setData({
      index: next,
      current: this.data.words[next],
      revealed: !config.revealMeaning
    })
    this.updateProgress()
  },

  /** 只有用户真的翻过当前词，才写入当天学习记录。 */
  async saveWord(wordId) {
    if (this.saving) {
      return false
    }
    this.saving = true
    try {
      const result = await request({
        url: '/api/study/records',
        method: 'POST',
        data: { wordIds: [wordId] }
      })
      this.setData({ todayWordCount: result.todayWordCount })
      return true
    } catch (error) {
      wx.showToast({ title: (error && error.message) || '记录失败，请重试', icon: 'none' })
      return false
    } finally {
      this.saving = false
    }
  },

  updateProgress() {
    const total = this.data.batchSize || 1
    this.setData({ progressPercent: Math.round((this.data.index / total) * 100) })
  },

  onReviewToday() {
    wx.redirectTo({ url: '/pages/review/review?date=' + dateUtil.todayIso() })
  },

  onBackToCalendar() {
    wx.switchTab({ url: '/pages/index/index' })
  },

  onRetry() {
    this.start()
  }
})
