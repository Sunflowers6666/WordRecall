/**
 * 统一的加载 / 空 / 错误状态块，见 UI 规范 §4.4。
 * 用法：<state-view state="error" kind="network" bind:action="onRetry" />
 */
Component({
  options: {
    styleIsolation: 'apply-shared'
  },

  properties: {
    /** loading | empty | success | error */
    state: { type: String, value: 'empty' },
    /** 仅 state=error 时使用：network | auth | server */
    kind: { type: String, value: '' },
    title: { type: String, value: '' },
    desc: { type: String, value: '' },
    actionText: { type: String, value: '' }
  },

  data: {
    loading: false,
    resolvedTitle: '',
    resolvedDesc: '',
    iconType: 'info',
    resolvedAction: ''
  },

  observers: {
    'state, kind, title, desc, actionText': function (state, kind, title, desc, actionText) {
      this.render(state, kind, title, desc, actionText)
    }
  },

  lifetimes: {
    attached() {
      this.render(this.data.state, this.data.kind, this.data.title, this.data.desc, this.data.actionText)
    }
  },

  methods: {
    render(state, kind, title, desc, actionText) {
      const presets = {
        empty: { title: '这里还是空的', desc: '', icon: 'info', action: '' },
        success: { title: '没有错词', desc: '这一轮全对，继续保持', icon: 'success', action: '' },
        'error-network': { title: '网络开小差了', desc: '检查网络后重试', icon: 'warn', action: '重试' },
        'error-auth': { title: '登录已失效', desc: '重新进入小程序即可恢复', icon: 'warn', action: '重新登录' },
        'error-server': { title: '服务暂时不可用', desc: '请稍后再试', icon: 'warn', action: '重试' }
      }
      const key = state === 'error' ? 'error-' + (kind || 'server') : state
      const preset = presets[key] || presets.empty

      this.setData({
        loading: state === 'loading',
        resolvedTitle: title || preset.title,
        resolvedDesc: desc || preset.desc,
        iconType: preset.icon,
        resolvedAction: actionText || preset.action
      })
    },

    onAction() {
      this.triggerEvent('action')
    }
  }
})
