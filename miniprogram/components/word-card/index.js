/** 单词卡：学习/复习页共用，见 UI 规范 §4.5；带发音按钮。 */
const config = require('../../config/index')

Component({
  options: {
    styleIsolation: 'apply-shared'
  },

  properties: {
    word: { type: Object, value: {} },
    /** 新学默认 false（遮罩），复习默认 true */
    revealed: { type: Boolean, value: true },
    wrong: { type: Boolean, value: false },
    /** 为 true 且未 revealed 时显示「点击查看释义」 */
    showReveal: { type: Boolean, value: false }
  },

  data: {
    playing: false,
    /** 单机版没有音频资源 */
    canPlay: !config.offline
  },

  lifetimes: {
    detached() {
      if (this.audio) {
        this.audio.destroy()
        this.audio = null
      }
    }
  },

  methods: {
    onReveal() {
      this.triggerEvent('reveal')
    },

    onPlay() {
      const word = this.data.word
      if (!word || !word.id) {
        return
      }
      if (!this.audio) {
        this.audio = wx.createInnerAudioContext()
        this.audio.onEnded(() => this.setData({ playing: false }))
        this.audio.onError(() => {
          this.setData({ playing: false })
          wx.showToast({ title: '发音加载失败', icon: 'none' })
        })
      }
      this.audio.stop()
      this.audio.src = config.baseUrl + '/api/words/' + word.id + '/audio?accent=us'
      this.audio.play()
      this.setData({ playing: true })
    }
  }
})
