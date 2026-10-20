const { request } = require('../../utils/request')
const { errorKind, toastError } = require('../../utils/feedback')
const dateUtil = require('../../utils/date')

Page({
  data: {
    state: 'loading',
    errorKind: '',
    total: 0,
    words: [],
    mode: 'list',
    index: 0,
    current: null,
    remembered: [],
    progressPercent: 0
  },

  onShow() {
    if (this.data.mode === 'list') {
      this.load()
    }
  },

  async load() {
    try {
      const data = await request({ url: '/api/wrongbook?limit=100' })
      this.setData({
        state: 'ready',
        total: data.total || 0,
        words: data.words || []
      })
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  onStart() {
    if (!this.data.words.length) {
      return
    }
    this.setData({
      mode: 'review',
      index: 0,
      current: this.data.words[0],
      remembered: [],
      progressPercent: 0
    })
  },

  onRemember() {
    const remembered = this.data.remembered.concat([this.data.current.id])
    this.advance(remembered)
  },

  onForgot() {
    this.advance(this.data.remembered)
  },

  advance(remembered) {
    const next = this.data.index + 1
    if (next >= this.data.words.length) {
      this.setData({ remembered, progressPercent: 100 })
      this.submit(remembered)
      return
    }
    this.setData({
      remembered,
      index: next,
      current: this.data.words[next],
      progressPercent: Math.round((next / this.data.words.length) * 100)
    })
  },

  async submit(remembered) {
    try {
      if (remembered.length) {
        await request({
          url: '/api/wrongbook/submit',
          method: 'POST',
          data: { rememberedWordIds: remembered }
        })
      }
      wx.showToast({
        title: remembered.length ? '清除 ' + remembered.length + ' 个错词' : '复习完成',
        icon: 'none'
      })
    } catch (error) {
      toastError(error)
    }
    this.setData({ mode: 'list', current: null, progressPercent: 0 })
    this.load()
  },

  onFinishEarly() {
    if (!this.data.remembered.length) {
      this.setData({ mode: 'list', current: null, progressPercent: 0 })
      return
    }
    this.submit(this.data.remembered)
  },

  onRetry() {
    this.setData({ state: 'loading' })
    this.load()
  }
})
