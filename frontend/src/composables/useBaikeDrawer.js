import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { buildBaikeKeyword, buildBaikeUrl } from '@/utils/baike'

export function useBaikeDrawer() {
  const baikeVisible = ref(false)
  const baikeTitle = ref('')
  const baikeUrl = ref('')

  function openBaikeDetail(name, city = '') {
    const keyword = buildBaikeKeyword(name, city)
    const url = buildBaikeUrl(name, city)
    if (!keyword || !url) {
      ElMessage.warning('当前景点暂时无法打开百科详情')
      return false
    }
    baikeTitle.value = keyword
    baikeUrl.value = url
    baikeVisible.value = true
    return true
  }

  return {
    baikeVisible,
    baikeTitle,
    baikeUrl,
    openBaikeDetail,
  }
}
