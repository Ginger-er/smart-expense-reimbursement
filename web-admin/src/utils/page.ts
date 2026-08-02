/**
 * 循环翻页取回全量数据。
 * 后端分页插件全局 maxLimit=100，pageSize 传超过 100 会被静默截断导致漏数据，
 * 这里按页循环拉取直到取到不足一页为止。
 */
export async function fetchAll(
  fetcher: (pageNum: number) => Promise<any>,
  pageSize = 100
): Promise<any[]> {
  const all: any[] = []
  let pageNum = 1
  while (true) {
    const res: any = await fetcher(pageNum)
    const list = (res?.data || []) as any[]
    all.push(...list)
    if (list.length < pageSize) {
      break
    }
    pageNum++
  }
  return all
}
