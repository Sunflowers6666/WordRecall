/** 单词卡：学习/复习页共用，见 UI 规范 §4.5；带发音按钮。 */
const config = require('../../config/index')

/** 单机版用微信「同声传译」插件做文本转语音（需要在小程序后台添加插件）。 */
const TTS_PROVIDER = 'WechatSI'

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
    canPlay: true
  },

  lifetimes: {
    attached() {
      if (!config.offline) {
        return
      }
      // 插件没在小程序后台添加时 requirePlugin 会抛错，此时直接隐藏发音按钮
      try {
        requirePlugin(TTS_PROVIDER)
        this.setData({ canPlay: true })
      } catch (error) {
        this.setData({ canPlay: false })
      }
    },

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

    ensureAudio() {
      if (!this.audio) {
        this.audio = wx.createInnerAudioContext()
        this.audio.onEnded(() => this.setData({ playing: false }))
        this.audio.onError(() => {
          this.setData({ playing: false })
          wx.showToast({ title: '发音加载失败', icon: 'none' })
        })
      }
      return this.audio
    },

    onPlay() {
      const word = this.data.word
      if (!word || !word.spelling) {
        return
      }
      if (config.offline) {
        this.playByTts(word.spelling)
        return
      }
      const audio = this.ensureAudio()
      audio.stop()
      audio.src = config.baseUrl + '/api/words/' + word.id + '/audio?accent=us'
      audio.play()
      this.setData({ playing: true })
    },

    playByTts(text) {
      let plugin = null
      try {
        plugin = requirePlugin(TTS_PROVIDER)
      } catch (error) {
        wx.showToast({ title: '发音插件未启用', icon: 'none' })
        return
      }
      if (!plugin || !plugin.textToSpeech) {
        wx.showToast({ title: '发音插件不可用', icon: 'none' })
        return
      }
      this.setData({ playing: true })
      plugin.textToSpeech({
        lang: 'en_US',
        tts: true,
        content: text,
        success: (res) => {
          if (!res || !res.filename) {
            this.setData({ playing: false })
            wx.showToast({ title: '发音生成失败', icon: 'none' })
            return
          }
          const audio = this.ensureAudio()
          audio.stop()
          audio.src = res.filename
          audio.play()
        },
        fail: () => {
          this.setData({ playing: false })
          wx.showToast({ title: '发音生成失败', icon: 'none' })
        }
      })
    }
  }
})
