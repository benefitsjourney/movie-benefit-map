import { ref, watch } from "vue";
import http from "@/plugins/axios";

/** fetchAll 補齊其餘頁時，同時最多送出的請求數。 */
const FETCH_ALL_CONCURRENCY = 4;

/**
 * 共用列表請求：負責載入、重試、取消舊請求與分頁回應。
 * fetchAll=true 用於需要一次顯示所有結果的頁面。
 */
export function useApiCollection(endpoint, params, { fetchAll = false, page = null } = {}) {
  const items = ref([]);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const loading = ref(true);
  const error = ref(false);
  const reloadKey = ref(0);

  // 依頁碼放入固定位置，完成先後不影響合併順序。
  async function fetchRemainingPages(requestParams, pages, signal) {
    const total = pages - 1;
    const contents = new Array(total);
    let nextIndex = 0;
    let failure = null;

    async function worker() {
      while (nextIndex < total && !signal.aborted && !failure) {
        const index = nextIndex++;
        try {
          const { data } = await http.get(endpoint, {
            params: { ...requestParams, page: index + 2 },
            signal,
          });
          contents[index] = Array.isArray(data?.content) ? data.content : [];
        } catch (error) {
          failure = failure ?? error;
        }
      }
    }

    const workers = Math.min(FETCH_ALL_CONCURRENCY, total);
    await Promise.all(Array.from({ length: workers }, () => worker()));
    if (signal.aborted) return null;
    if (failure) throw failure;
    return contents.flat();
  }

  watch(
    [params, reloadKey],
    async (_, __, onCleanup) => {
      const controller = new AbortController();
      const { signal } = controller;
      onCleanup(() => controller.abort());

      loading.value = true;
      error.value = false;
      let redirecting = false;

      try {
        const requestParams = params.value;
        const { data } = await http.get(endpoint, { params: requestParams, signal });
        if (signal.aborted) return;

        const parsedPages = Number(data?.totalPages);
        const pages =
          Number.isFinite(parsedPages) && parsedPages > 0 ? Math.trunc(parsedPages) : 0;
        // 結果變少時，避免停在不存在的頁碼；維持載入中，等下一輪請求。
        if (page && pages > 0 && page.value > pages) {
          redirecting = true;
          page.value = pages;
          return;
        }

        let nextItems = Array.isArray(data?.content) ? data.content : [];
        if (fetchAll && pages > 1) {
          const rest = await fetchRemainingPages(requestParams, pages, signal);
          if (signal.aborted || rest == null) return;
          nextItems = nextItems.concat(rest);
        }

        items.value = nextItems;
        totalPages.value = pages;
        totalElements.value = Number(data?.totalElements) || nextItems.length;
      } catch {
        if (signal.aborted) return;
        error.value = true;
        items.value = [];
        totalElements.value = 0;
        totalPages.value = 0;
      } finally {
        if (!signal.aborted && !redirecting) loading.value = false;
      }
    },
    { immediate: true },
  );

  function retry() {
    reloadKey.value += 1;
  }

  return { items, totalElements, totalPages, loading, error, retry };
}
