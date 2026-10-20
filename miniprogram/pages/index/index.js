const { request } = require('../../utils/request')
const dateUtil = require('../../utils/date')
const { errorKind } = require('../../utils/feedback')

Page({
  data: {
    weekdays: dateUtil.weekdayNames(),
    year: 0,
    month: 0,
    monthTitle: '',
    isCurrentMonth: true,
    cells: [],
    selected: '',
    daySummary: null,
    monthEmpty: false,
    state: 'loading',
    errorKind: '',
    wordbookName: ''
  },

  onLoad() {
    this.refreshToday()
    const parsed = dateUtil.parseIso(this.todayIso)
    this.skipNextShow = true
    this.setData({
      year: parsed.year,
      month: parsed.month,
      monthTitle: dateUtil.monthTitle(parsed.year, parsed.month),
      isCurrentMonth: true
    })
    this.loadMonth(parsed.year, parsed.month, false)
    this.loadWordbook()
  },

  onShow() {
    if (!this.data.year) {
      return
    }
    if (this.skipNextShow) {
      this.skipNextShow = false
      return
    }
    // 从学习/复习/词库页返回时刷新，保证日历与当天记录一致
    this.refreshToday()
    this.loadMonth(this.data.year, this.data.month, true)
    this.loadWordbook()
  },

  onPullDownRefresh() {
    this.refreshToday()
    this.loadMonth(this.data.year, this.data.month, true).then(() => {
      wx.stopPullDownRefresh()
    })
  },

  refreshToday() {
    this.todayIso = dateUtil.todayIso()
  },

  async loadMonth(year, month, keepSelection) {
    // 刷新（返回本页、下拉）时保留旧数据，避免骨架屏闪一下
    if (!keepSelection) {
      this.setData({ state: 'loading' })
    }
    try {
      const data = await request({ url: '/api/calendar?year=' + year + '&month=' + month })
      const days = data.days || []
      const dayMap = {}
      days.forEach((day) => {
        dayMap[day.date] = day
      })

      const cells = dateUtil.monthCells(year, month, this.todayIso).map((cell) => {
        if (!cell.inMonth) {
          return cell
        }
        const record = dayMap[cell.iso]
        return Object.assign({}, cell, {
          learned: !!record,
          wordCount: record ? record.wordCount : 0,
          wrongCount: record ? record.wrongCount : 0
        })
      })

      let selected = keepSelection ? this.data.selected : ''
      if (!selected || !dateUtil.sameMonth(selected, year, month)) {
        if (dateUtil.sameMonth(this.todayIso, year, month)) {
          selected = this.todayIso
        } else {
          selected = days.length ? days[0].date : ''
        }
      }

      const now = dateUtil.parseIso(this.todayIso)
      this.setData({
        year,
        month,
        monthTitle: dateUtil.monthTitle(year, month),
        isCurrentMonth: now.year === year && now.month === month,
        cells,
        monthEmpty: days.length === 0,
        state: 'ready'
      })
      this.applySelection(selected)
    } catch (error) {
      this.setData({ state: 'error', errorKind: errorKind(error) })
    }
  },

  applySelection(iso) {
    const cells = this.data.cells.map((cell) => Object.assign({}, cell, {
      isSelected: !!iso && cell.iso === iso
    }))
    const target = cells.find((cell) => cell.inMonth && cell.iso === iso)
    const daySummary = target && target.learned ? {
      label: dateUtil.dateLabel(iso),
      wordCount: target.wordCount,
      wrongCount: target.wrongCount
    } : null
    this.setData({ selected: iso || '', cells, daySummary })
  },

  onDayTap(event) {
    const index = Number(event.currentTarget.dataset.index)
    const cell = this.data.cells[index]
    if (!cell || !cell.inMonth || cell.isFuture) {
      return
    }
    this.applySelection(cell.iso)
  },

  onPrevMonth() {
    const shifted = dateUtil.shiftMonth(this.data.year, this.data.month, -1)
    this.changeMonth(shifted.year, shifted.month)
  },

  onNextMonth() {
    const shifted = dateUtil.shiftMonth(this.data.year, this.data.month, 1)
    const now = dateUtil.parseIso(this.todayIso)
    if (shifted.year > now.year || (shifted.year === now.year && shifted.month > now.month)) {
      wx.showToast({ title: '已经是本月了', icon: 'none' })
      return
    }
    this.changeMonth(shifted.year, shifted.month)
  },

  changeMonth(year, month) {
    this.setData({ selected: '' })
    this.loadMonth(year, month, false)
  },

  onToday() {
    const now = dateUtil.parseIso(this.todayIso)
    if (now.year === this.data.year && now.month === this.data.month) {
      this.applySelection(this.todayIso)
      return
    }
    this.loadMonth(now.year, now.month, false)
  },

  onReviewDay() {
    if (!this.data.selected) {
      return
    }
    wx.navigateTo({ url: '/pages/review/review?date=' + this.data.selected })
  },

  onStart() {
    wx.navigateTo({ url: '/pages/learn/learn' })
  },

  onWordbook() {
    wx.navigateTo({ url: '/pages/wordbook/wordbook' })
  },

  onRetry() {
    this.loadMonth(this.data.year, this.data.month, true)
  },

  async loadWordbook() {
    try {
      const list = await request({ url: '/api/wordbooks' })
      const current = list.find((item) => item.selected) || list[0]
      this.setData({ wordbookName: current ? current.name : '' })
    } catch (error) {
      // 词库名不是关键信息，失败时保留上一次的值
    }
  }
})
