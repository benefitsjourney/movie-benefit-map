
import { COUNTY_LABELS } from "@/constants/county";

// 排序設定
export const CINEMA_SORT_DEFAULT = {
  orderBy: "COUNTY",
  sort: "ASC",
  label: "縣市（北 → 南）",
};

export const MOVIE_SORT_DEFAULT = {
  orderBy: "RELEASE_DATE",
  sort: "DESC",
  label: "上映日期（新 → 舊）",
};

const RELEASE_DATE_DESC = {
  value: "RELEASE_DATE:DESC",
  label: MOVIE_SORT_DEFAULT.label,
};
const RELEASE_DATE_ASC = {
  value: "RELEASE_DATE:ASC",
  label: "上映日期（舊 → 新）",
};
const COUNTY_ASC = {
  value: "COUNTY:ASC",
  label: CINEMA_SORT_DEFAULT.label,
};
const UPDATED_AT_OPTIONS = [
  { value: "UPDATED_AT:DESC", label: "更新時間（新 → 舊）" },
  { value: "UPDATED_AT:ASC", label: "更新時間（舊 → 新）" },
];
const CREATED_AT_OPTIONS = [
  { value: "CREATED_AT:DESC", label: "建立時間（新 → 舊）" },
  { value: "CREATED_AT:ASC", label: "建立時間（舊 → 新）" },
];

export const MOVIE_SORT_OPTIONS = [RELEASE_DATE_ASC];

export const ADMIN_MOVIE_SORT_DEFAULT = {
  orderBy: "MOVIE_ID",
  sort: "ASC",
  label: "編號（小 → 大）",
};

export const ADMIN_MOVIE_SORT_OPTIONS = [
  { value: "MOVIE_ID:DESC", label: "編號（大 → 小）" },
  RELEASE_DATE_DESC,
  RELEASE_DATE_ASC,
  ...UPDATED_AT_OPTIONS,
];

export const ADMIN_CINEMA_SORT_DEFAULT = {
  orderBy: "CINEMA_ID",
  sort: "ASC",
  label: "編號（小 → 大）",
};

export const ADMIN_CINEMA_SORT_OPTIONS = [
  { value: "CINEMA_ID:DESC", label: "編號（大 → 小）" },
  COUNTY_ASC,
  { value: "COUNTY:DESC", label: "縣市（南 → 北）" },
  ...UPDATED_AT_OPTIONS,
];

export const ADMIN_CRAWLER_REVIEW_SORT_DEFAULT = {
  orderBy: "CRAWLED_AT",
  sort: "DESC",
  label: "爬取時間（新 → 舊）",
};

export const ADMIN_CRAWLER_REVIEW_SORT_OPTIONS = [
  { value: "CRAWLED_AT:ASC", label: "爬取時間（舊 → 新）" },
  { value: "REVIEWED_AT:DESC", label: "審核時間（新 → 舊）" },
  { value: "REVIEWED_AT:ASC", label: "審核時間（舊 → 新）" },
  { value: "REVIEW_STATUS:ASC", label: "審查狀態" },
  { value: "CRAWLER_TYPE:ASC", label: "爬蟲類型" },
  { value: "ENTITY_ID:ASC", label: "資料編號（小 → 大）" },
  { value: "ENTITY_ID:DESC", label: "資料編號（大 → 小）" },
  ...UPDATED_AT_OPTIONS,
  ...CREATED_AT_OPTIONS,
  { value: "UNLOCKED_AT:DESC", label: "解鎖時間（新 → 舊）" },
  { value: "UNLOCKED_AT:ASC", label: "解鎖時間（舊 → 新）" },
  { value: "CRAWLER_REVIEW_ID:ASC", label: "編號（小 → 大）" },
  { value: "CRAWLER_REVIEW_ID:DESC", label: "編號（大 → 小）" },
];

/** 僅接受畫面提供的排序值，避免將任意欄位名稱傳給 API。 */
export function parseSort(sortKey, defaultSort, options) {
  const [orderBy, sort] = String(sortKey ?? "").split(":");
  const validKeys = [
    `${defaultSort.orderBy}:${defaultSort.sort}`,
    ...options.map((option) => option.value),
  ];

  return validKeys.includes(`${orderBy}:${sort}`)
    ? { orderBy, sort }
    : { orderBy: defaultSort.orderBy, sort: defaultSort.sort };
}

export const parseMovieSort = (key) =>
  parseSort(key, MOVIE_SORT_DEFAULT, MOVIE_SORT_OPTIONS);

// 地圖影城排序
const countyOrder = Object.keys(COUNTY_LABELS);

function getCountyRank(county) {
  const index = countyOrder.indexOf(county);
  return index === -1 ? countyOrder.length : index;
}

function compareCinemaId(a, b) {
  const aId = Number(a?.cinemaId);
  const bId = Number(b?.cinemaId);

  if (!Number.isFinite(aId)) return Number.isFinite(bId) ? 1 : 0;
  if (!Number.isFinite(bId)) return -1;

  return aId - bId;
}

export function compareCinemas(a, b, { orderBy, sort }) {
  const direction = sort === "DESC" ? -1 : 1;

  if (orderBy === "COUNTY") {
    const result = getCountyRank(a?.county) - getCountyRank(b?.county);
    return result * direction || compareCinemaId(a, b);
  }

  return compareCinemaId(a, b) * direction;
}
