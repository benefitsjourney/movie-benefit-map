
import { COUNTY_LABELS } from "@/constants/county";

// 排序設定
export const CINEMA_SORT_DEFAULT = {
  orderBy: "COUNTY",
  sort: "ASC",
  label: "縣市（北 → 南）",
};

export const CINEMA_SORT_OPTIONS = [
  { value: "COUNTY:DESC", label: "縣市（南 → 北）" },
  { value: "CINEMA_ID:ASC", label: "編號（小 → 大）" },
  { value: "CINEMA_ID:DESC", label: "編號（大 → 小）" },
];

export const MOVIE_SORT_DEFAULT = {
  orderBy: "RELEASE_DATE",
  sort: "DESC",
  label: "上映日期（新 → 舊）",
};

export const MOVIE_SORT_OPTIONS = [
  { value: "RELEASE_DATE:ASC", label: "上映日期（舊 → 新）" },
];

// 解析排序參數
function parseSort(sortKey, defaultSort, options) {
  const [orderBy, sort] = String(sortKey ?? "").split(":");
  const validKeys = [
    `${defaultSort.orderBy}:${defaultSort.sort}`,
    ...options.map((option) => option.value),
  ];

  return validKeys.includes(`${orderBy}:${sort}`)
    ? { orderBy, sort }
    : { orderBy: defaultSort.orderBy, sort: defaultSort.sort };
}

export const parseCinemaSort = (key) =>
  parseSort(key, CINEMA_SORT_DEFAULT, CINEMA_SORT_OPTIONS);

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
