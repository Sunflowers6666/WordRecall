/** 单词卡：学习/复习页共用，见 UI 规范 §4.5。 */
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

  methods: {
    onReveal() {
      this.triggerEvent('reveal')
    }
  }
})
