export function buildBaikeKeyword(name, city = '') {
  const normalizedName = `${name || ''}`.trim()
  return normalizedName
}

export function buildBaikeUrl(name, city = '') {
  const keyword = buildBaikeKeyword(name, city)
  return keyword
    ? `https://baike.baidu.com/item/${encodeURIComponent(keyword)}`
    : ''
}

export function openBaikePage(name, city = '') {
  const url = buildBaikeUrl(name, city)
  if (!url) {
    return false
  }
  window.open(url, '_blank', 'noopener,noreferrer')
  return true
}
