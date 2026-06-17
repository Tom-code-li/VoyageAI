export const statusMap = {
  0: { label: '计划中', type: 'warning' },
  1: { label: '进行中', type: 'success' },
  2: { label: '已完成', type: 'info' },
}

export function getStatusMeta(status) {
  return statusMap[status] || { label: '未知', type: 'info' }
}
