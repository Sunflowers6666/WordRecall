/** 日期工具：全部使用本地时间，ISO 字符串 yyyy-MM-dd。 */

const CN_WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

function pad2(value) {
  return value < 10 ? '0' + value : '' + value
}

function toIso(date) {
  return date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate())
}

function todayIso() {
  return toIso(new Date())
}

function parseIso(iso) {
  const parts = String(iso || '').split('-')
  return {
    year: Number(parts[0]),
    month: Number(parts[1]),
    day: Number(parts[2])
  }
}

function sameMonth(iso, year, month) {
  if (!iso) {
    return false
  }
  const parsed = parseIso(iso)
  return parsed.year === year && parsed.month === month
}

function isFuture(iso) {
  return !!iso && iso > todayIso()
}

function monthTitle(year, month) {
  return year + '年' + month + '月'
}

function shiftMonth(year, month, delta) {
  const date = new Date(year, month - 1 + delta, 1)
  return { year: date.getFullYear(), month: date.getMonth() + 1 }
}

function daysInMonth(year, month) {
  const table = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  if (month === 2 && ((year % 4 === 0 && year % 100 !== 0) || year % 400 === 0)) {
    return 29
  }
  return table[month - 1]
}

/**
 * 固定 6 行 × 7 列（周一起始）的日历格，空白格 day 为空字符串，
 * 保证切换月份时高度不跳动。
 */
function monthCells(year, month, today) {
  const first = new Date(year, month - 1, 1)
  const offset = (first.getDay() + 6) % 7
  const total = daysInMonth(year, month)
  const cells = []

  for (let i = 0; i < 42; i += 1) {
    const dayNumber = i - offset + 1
    if (dayNumber < 1 || dayNumber > total) {
      cells.push({ key: 'pad-' + i, day: '', iso: '', inMonth: false, isToday: false, isFuture: false })
      continue
    }
    const iso = year + '-' + pad2(month) + '-' + pad2(dayNumber)
    cells.push({
      key: iso,
      day: dayNumber,
      iso,
      inMonth: true,
      isToday: iso === today,
      isFuture: iso > today
    })
  }
  return cells
}

function dateLabel(iso) {
  if (!iso) {
    return ''
  }
  const parsed = parseIso(iso)
  const date = new Date(parsed.year, parsed.month - 1, parsed.day)
  return parsed.month + '月' + parsed.day + '日 周' + CN_WEEKDAYS[date.getDay()]
}

function weekdayNames() {
  return ['一', '二', '三', '四', '五', '六', '日']
}

module.exports = {
  pad2,
  toIso,
  todayIso,
  parseIso,
  sameMonth,
  isFuture,
  monthTitle,
  shiftMonth,
  daysInMonth,
  monthCells,
  dateLabel,
  weekdayNames
}
