<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { Icon } from "@iconify/vue";
import mapIcon from "@iconify-icons/mdi/map-outline";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import compassIcon from "@iconify-icons/mdi/compass-outline";
import nearIcon from "@iconify-icons/mdi/crosshairs-gps";
import refreshIcon from "@iconify-icons/mdi/refresh";
import searchIcon from "@iconify-icons/mdi/magnify";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import filmIcon from "@iconify-icons/mdi/movie-open-outline";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "leaflet.markercluster/dist/MarkerCluster.css";
import "leaflet.markercluster/dist/MarkerCluster.Default.css";
import http from "@/plugins/axios";
import { COUNTY_LABELS, countyLabel } from "@/constants/county";
import BenefitDialog from "@/components/ui/BenefitDialog.vue";
import AppButton from "@/components/ui/Button.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";

// Leaflet 座標使用 [緯度, 經度]；涵蓋本島、澎湖、金門與馬祖。
const TAIWAN_BOUNDS = [
  [21.85, 118.1],
  [26.45, 122.1],
];

// 初次進入時讓本島成為視覺焦點；仍可拖曳至澎湖、金門、馬祖。
const INITIAL_VIEW_BOUNDS = [
  [21.9, 119.85],
  [25.45, 122.05],
];
const DEFAULT_RADIUS = 5000;
// 尚未定位時，用臺灣中心與足以涵蓋本島及離島的半徑查詢，仍由後端計算距離。
const TAIWAN_CENTER = { latitude: 23.7, longitude: 121.0 };
const TAIWAN_RADIUS = 450000;
const RADIUS_OPTIONS = [
  { value: 1000, label: "1 公里" },
  { value: 3000, label: "3 公里" },
  { value: 5000, label: "5 公里" },
  { value: 10000, label: "10 公里" },
  { value: 20000, label: "20 公里" },
];

const GEO_OPTIONS = {
  enableHighAccuracy: false,
  timeout: 10000,
  maximumAge: 60000,
};
const filterFields = [
  {
    key: "keyword",
    label: "影城名稱",
    type: "search",
    placeholder: "輸入想找的影城",
    icon: searchIcon,
  },
  {
    key: "movieKeyword",
    label: "電影",
    type: "search",
    placeholder: "輸入電影名稱",
    icon: filmIcon,
  },
  {
    key: "benefitKeyword",
    label: "特典",
    type: "search",
    placeholder: "輸入特典名稱",
    icon: giftIcon,
  },
  {
    key: "county",
    label: "縣市",
    type: "select",
    emptyLabel: "全部縣市",
    icon: pinIcon,
    options: COUNTY_LABELS,
  },
  {
    key: "hasBenefit",
    label: "只看有特典的影城",
    type: "checkbox",
  },
];

// SearchFilter 套用後才更新篩選；附近搜尋另外加入定位和半徑參數。
const appliedFilters = ref({
  keyword: "",
  county: "",
  movieKeyword: "",
  benefitKeyword: "",
  hasBenefit: false,
});

const radius = ref(DEFAULT_RADIUS);
const nearbyActive = ref(false);
const locating = ref(false);
const locateOrigin = ref(null);
const locationNotice = ref("");
const cinemas = ref([]);
const total = ref(0);
const resultRadius = ref(null);
const loading = ref(true);
const hasLoaded = ref(false);
const errorMessage = ref("");
const refreshing = computed(() => loading.value && hasLoaded.value);
const mapEl = ref(null);
const selectedId = ref(null);
const mapError = ref("");

function coordinate(value, min, max) {
  if (value == null || String(value).trim() === "") return null;
  const number = Number(value);
  return Number.isFinite(number) && number >= min && number <= max
    ? number
    : null;
}

function getCinemaCoordinates(cinema) {
  const latitude = coordinate(cinema?.latitude, -90, 90);
  const longitude = coordinate(cinema?.longitude, -180, 180);
  return latitude === null || longitude === null
    ? null
    : { latitude, longitude };
}

function isWithinTaiwanBounds(position) {
  if (!position) return false;
  const { latitude, longitude } = position;
  return (
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= TAIWAN_BOUNDS[0][0] &&
    latitude <= TAIWAN_BOUNDS[1][0] &&
    longitude >= TAIWAN_BOUNDS[0][1] &&
    longitude <= TAIWAN_BOUNDS[1][1]
  );
}

function formatDistance(value) {
  if (value == null || value === "") return "";
  const meters = Number(value);
  if (!Number.isFinite(meters) || meters < 0) return "";
  return meters >= 1000
    ? `${(meters / 1000).toFixed(1)} 公里`
    : `${Math.round(meters)} 公尺`;
}

function formatBenefitCount(count) {
  return count ? `${count} 項特典` : "目前沒有特典";
}

function benefitItems(cinema) {
  return Array.isArray(cinema?.benefits) ? cinema.benefits : [];
}

function benefitTotal(cinema) {
  const count = Number(cinema?.benefitCount);
  return Number.isInteger(count) && count >= 0
    ? count
    : benefitItems(cinema).length;
}

// API 資料統一加工，避免在 Marker / Popup / 清單重複轉座標、檢查範圍、換算距離。
const cinemaItems = computed(() =>
  cinemas.value.map((cinema) => {
    const position = getCinemaCoordinates(cinema);
    const benefits = benefitItems(cinema);
    return {
      ...cinema,
      position,
      canLocate: cinema.cinemaId != null && isWithinTaiwanBounds(position),
      distanceText: formatDistance(cinema.distance),
      benefits,
      benefitCount: benefitTotal(cinema),
    };
  }),
);
const mappableCinemas = computed(() =>
  cinemaItems.value.filter((cinema) => cinema.canLocate),
);
const markerCount = computed(() => mappableCinemas.value.length);
const SEARCH_KEYS = ["keyword", "movieKeyword", "benefitKeyword"];
const activeKeywords = computed(() =>
  Object.fromEntries(
    SEARCH_KEYS.map((key) => [
      key,
      String(appliedFilters.value[key] ?? "").trim(),
    ]),
  ),
);
const hasNarrowScope = computed(() =>
  Boolean(
    SEARCH_KEYS.some((key) => activeKeywords.value[key]) ||
    appliedFilters.value.county ||
    appliedFilters.value.hasBenefit,
  ),
);
const resultHint = computed(() => {
  if (nearbyActive.value && resultRadius.value != null) {
    return `你附近 ${formatDistance(resultRadius.value)} 內的影城，依距離排序`;
  }
  return hasNarrowScope.value
    ? "找到這些影城，依直線距離排序"
    : "一起看看全台有哪些影城";
});

const searchOrigin = computed(() =>
  nearbyActive.value && locateOrigin.value ? locateOrigin.value : TAIWAN_CENTER,
);
const searchRadius = computed(() =>
  nearbyActive.value ? Number(radius.value) : TAIWAN_RADIUS,
);

// 距離、總筆數與特典篩選都由 GET /cinemas/map 計算。
const params = computed(() => {
  const query = {
    latitude: searchOrigin.value.latitude,
    longitude: searchOrigin.value.longitude,
    radius: searchRadius.value,
  };
  for (const key of SEARCH_KEYS) {
    if (activeKeywords.value[key]) query[key] = activeKeywords.value[key];
  }
  if (appliedFilters.value.county) query.county = appliedFilters.value.county;
  if (appliedFilters.value.hasBenefit === true) query.hasBenefit = true;
  return query;
});
const queryKey = computed(() => JSON.stringify(params.value));
const detailCinemaId = ref(null);
const detailCinema = computed(
  () =>
    cinemaItems.value.find(
      (cinema) => cinema.cinemaId === detailCinemaId.value,
    ) ?? null,
);

function dialogDetail(cinema) {
  const place = countyLabel(cinema?.county) || "影城";
  return `${place} · ${benefitTotal(cinema)} 項特典`;
}

let requestController = null;
let locateVersion = 0;
let lastSuccessKey = null;

function abortRequest() {
  requestController?.abort();
  requestController = null;
}

async function loadCinemas() {
  abortRequest();
  const current = new AbortController();
  requestController = current;
  const { signal } = current;
  const requestKey = queryKey.value;
  loading.value = true;
  errorMessage.value = "";
  try {
    const { data } = await http.get("/cinemas/map", {
      params: { ...params.value },
      signal,
    });
    if (signal.aborted) return;
    if (
      !Array.isArray(data?.cinemas) ||
      !Number.isInteger(data?.total) ||
      data.total < 0
    ) {
      throw new Error("影城地圖 API 回傳格式有誤");
    }
    cinemas.value = data.cinemas;
    total.value = data.total;
    resultRadius.value = data.radius == null ? null : Number(data.radius);
    hasLoaded.value = true;
    // 只有查詢條件改變才重新調整視角，手動重新整理保留目前縮放。
    const shouldFit = lastSuccessKey !== requestKey;
    lastSuccessKey = requestKey;
    if (shouldFit) {
      await nextTick(); // 等待標記同步後再調整視角。
      if (signal.aborted) return;
      fitView();
    }
  } catch (error) {
    if (signal.aborted) return;
    console.error("影城地圖資料載入失敗", error);
    errorMessage.value = hasLoaded.value
      ? "更新沒有成功，先為你保留上次的搜尋結果。"
      : "影城資料暫時載入失敗，請稍後再試。";
  } finally {
    if (requestController === current) {
      requestController = null;
      loading.value = false;
    }
  }
}

// Vue 會批次處理同一輪的條件變更，不需要額外計時器或防抖。
watch(
  queryKey,
  () => {
    void loadCinemas();
  },
  { flush: "post" },
);

function closeBenefitDetail() {
  detailCinemaId.value = null;
}

function openBenefitDetail(cinema) {
  if (cinema?.cinemaId == null || !cinema.benefitCount) return;
  detailCinemaId.value = cinema.cinemaId;
}

function finishLocating(version, message = "") {
  if (version !== locateVersion || disposed) return false;
  locating.value = false;
  locationNotice.value = message;
  return true;
}

function disableNearby() {
  locateVersion += 1; // 使尚未回傳的定位結果失效。
  locating.value = false;
  nearbyActive.value = false;
  locateOrigin.value = null;
  locationNotice.value = "";
}

function enableNearby() {
  const version = ++locateVersion;
  locating.value = true;
  locationNotice.value = "";
  const geo = globalThis.navigator?.geolocation;
  if (!geo?.getCurrentPosition) {
    finishLocating(version, "這個瀏覽器似乎不支援定位，可以試試其他瀏覽器。");
    return;
  }
  try {
    geo.getCurrentPosition(
      ({ coords }) => {
        if (!finishLocating(version)) return;
        const position = {
          latitude: coords?.latitude,
          longitude: coords?.longitude,
        };
        if (!isWithinTaiwanBounds(position)) {
          locationNotice.value = "附近搜尋目前支援臺灣本島與離島地區。";
          return;
        }
        locateOrigin.value = position;
        nearbyActive.value = true;
      },
      (error) => {
        finishLocating(
          version,
          error?.code === 1
            ? "開啟位置權限後，就能幫你找附近的影城囉。"
            : "還無法確認你的位置，試著開啟裝置定位後再搜尋。",
        );
      },
      GEO_OPTIONS,
    );
  } catch {
    finishLocating(version, "定位沒有成功，請確認瀏覽器已開啟位置權限。");
  }
}

function toggleNearby() {
  if (nearbyActive.value || locating.value) disableNearby();
  else enableNearby();
}

let map = null;
let markerLayer = null;
let userDot = null;
let radiusCircle = null;
let resizeObserver = null;
let disposed = false;
let creating = false;
const markers = new Map();
const taiwanLatLngBounds = L.latLngBounds(TAIWAN_BOUNDS);

function reduceMotion() {
  return (
    window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches ?? false
  );
}

function markerIcon(selected = false, count = 0) {
  const safeCount = Number.isInteger(count) && count > 0 ? count : 0;
  const badge = safeCount
    ? `<span class="benefit-marker-count">${safeCount > 99 ? "99+" : safeCount}</span>`
    : "";
  return L.divIcon({
    className: "benefit-marker-wrap",
    html: `<span class="benefit-marker${selected ? " is-selected" : ""}"></span>${badge}`,
    iconSize: [36, 42],
    iconAnchor: [15, 36],
    popupAnchor: [0, -32],
  });
}

const POPUP_PIN_PATH =
  "M12 6.5A2.5 2.5 0 0 1 14.5 9a2.5 2.5 0 0 1-2.5 2.5A2.5 2.5 0 0 1 9.5 9A2.5 2.5 0 0 1 12 6.5M12 2a7 7 0 0 1 7 7c0 5.25-7 13-7 13S5 14.25 5 9a7 7 0 0 1 7-7m0 2a5 5 0 0 0-5 5c0 1 0 3 5 9.71C17 12 17 10 17 9a5 5 0 0 0-5-5";
const POPUP_GIFT_PATH =
  "M22 12v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-8a1 1 0 0 1-1-1V8a2 2 0 0 1 2-2h3.17A3 3 0 0 1 6 5a3 3 0 0 1 3-3c1 0 1.88.5 2.43 1.24v-.01L12 4l.57-.77v.01C13.12 2.5 14 2 15 2a3 3 0 0 1 3 3a3 3 0 0 1-.17 1H21a2 2 0 0 1 2 2v3a1 1 0 0 1-1 1M4 20h7v-8H4zm16 0v-8h-7v8zM9 4a1 1 0 0 0-1 1a1 1 0 0 0 1 1a1 1 0 0 0 1-1a1 1 0 0 0-1-1m6 0a1 1 0 0 0-1 1a1 1 0 0 0 1 1a1 1 0 0 0 1-1a1 1 0 0 0-1-1M3 8v2h8V8zm10 0v2h8V8z";

function popupIcon(path) {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("aria-hidden", "true");
  svg.classList.add("popup-icon");
  const shape = document.createElementNS("http://www.w3.org/2000/svg", "path");
  shape.setAttribute("fill", "currentColor");
  shape.setAttribute("d", path);
  svg.append(shape);
  return svg;
}

// Popup 文字一律以 textContent 寫入，避免將 API 欄位當成 HTML。
function popupText(parent, tag, className, content) {
  const element = L.DomUtil.create(tag, className, parent);
  element.textContent = content;
  return element;
}

function popupInfo(parent, className, iconPath, content) {
  const line = L.DomUtil.create("p", className, parent);
  line.append(popupIcon(iconPath));
  popupText(line, "span", "", content);
  return line;
}

function makePopup(cinema) {
  const root = L.DomUtil.create("div", "benefit-popup");
  const meta = L.DomUtil.create("div", "popup-meta", root);
  popupText(meta, "span", "popup-county", countyLabel(cinema.county) || "影城");
  if (cinema.distanceText) {
    popupText(meta, "span", "popup-distance", `距離 ${cinema.distanceText}`);
  }

  popupText(root, "strong", "popup-title", cinema.cinemaName || "影城");

  const details = L.DomUtil.create("div", "popup-details", root);
  popupInfo(
    details,
    "popup-address",
    POPUP_PIN_PATH,
    cinema.cinemaAddress || "影城尚未提供地址",
  );
  popupInfo(
    details,
    cinema.benefitCount ? "popup-stock" : "popup-stock is-empty",
    POPUP_GIFT_PATH,
    formatBenefitCount(cinema.benefitCount),
  );

  const actions = L.DomUtil.create("div", "popup-actions", root);
  if (cinema.benefitCount) {
    const detail = L.DomUtil.create("button", "popup-action", actions);
    detail.type = "button";
    detail.textContent = "特典庫存查詢";
    L.DomEvent.disableClickPropagation(detail);
    L.DomEvent.on(detail, "click", (event) => {
      L.DomEvent.stop(event);
      void openBenefitDetail(cinema);
    });
  }
  if (cinema.position) {
    const link = L.DomUtil.create("a", "popup-route", actions);
    const { latitude, longitude } = cinema.position;
    link.href = `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(`${latitude},${longitude}`)}`;
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    link.textContent = "規劃路線";
  }
  if (!actions.childElementCount) actions.remove();
  return root;
}

function syncMarkers() {
  if (!map || !markerLayer) return;
  markerLayer.clearLayers();
  markers.clear();
  for (const cinema of mappableCinemas.value) {
    const { latitude, longitude } = cinema.position;
    const marker = L.marker([latitude, longitude], {
      title: cinema.cinemaName || "影城",
      icon: markerIcon(
        cinema.cinemaId === selectedId.value,
        cinema.benefitCount,
      ),
    });
    marker.bindPopup(makePopup(cinema), {
      className: "benefit-map-popup",
      minWidth: 300,
      maxWidth: 300,
      autoPanPadding: [24, 24],
    });
    marker.on("click", () => {
      selectedId.value = cinema.cinemaId;
    });
    markerLayer.addLayer(marker);
    markers.set(cinema.cinemaId, marker);
  }
}

function syncLocationCircle() {
  if (!map) return;
  if (userDot) map.removeLayer(userDot);
  if (radiusCircle) map.removeLayer(radiusCircle);
  userDot = null;
  radiusCircle = null;
  const position = nearbyActive.value ? locateOrigin.value : null;
  if (!isWithinTaiwanBounds(position)) return;
  const center = [position.latitude, position.longitude];
  radiusCircle = L.circle(center, {
    radius: Number(radius.value),
    color: "#257f80",
    weight: 2,
    fillColor: "#69c5c0",
    fillOpacity: 0.1,
    interactive: false,
  }).addTo(map);
  userDot = L.circleMarker(center, {
    radius: 7,
    color: "#ffffff",
    weight: 3,
    fillColor: "#2769bb",
    fillOpacity: 1,
  })
    .addTo(map)
    .bindPopup("你目前的位置");
}

function enforceTaiwanBounds() {
  if (!map) return;
  const size = map.getSize();
  if (size.x < 1 || size.y < 1) return;
  map.setMinZoom(0);
  const fitZoom = map.getBoundsZoom(taiwanLatLngBounds);
  if (Number.isFinite(fitZoom)) map.setMinZoom(fitZoom);
  map.panInsideBounds(taiwanLatLngBounds, { animate: false });
}

function fitView() {
  if (!map) return;
  const options = { animate: !reduceMotion(), padding: [26, 26] };
  if (nearbyActive.value && radiusCircle) {
    map.fitBounds(radiusCircle.getBounds(), { ...options, maxZoom: 15 });
    return;
  }
  const fitInitialView = () =>
    map.fitBounds(INITIAL_VIEW_BOUNDS, { ...options, maxZoom: 9 });
  if (!hasNarrowScope.value) {
    fitInitialView();
    return;
  }
  const positions = mappableCinemas.value.map((cinema) => [
    cinema.position.latitude,
    cinema.position.longitude,
  ]);
  if (positions.length === 1) {
    map.setView(positions[0], Math.max(map.getMinZoom(), 14), {
      animate: options.animate,
    });
  } else if (positions.length > 1) {
    map.fitBounds(L.latLngBounds(positions), { ...options, maxZoom: 14 });
  } else {
    fitInitialView();
  }
}

function focusCinema(cinema) {
  selectedId.value = cinema.cinemaId;
  const marker = markers.get(cinema.cinemaId);
  if (!map || !marker) return;
  const open = () => {
    if (!map || disposed) return;
    map.setView(marker.getLatLng(), Math.max(map.getZoom(), 15), {
      animate: !reduceMotion(),
    });
    marker.openPopup();
  };
  if (typeof markerLayer?.zoomToShowLayer === "function")
    markerLayer.zoomToShowLayer(marker, open);
  else open();
}

function scrollToSelected(id) {
  if (id == null) return;
  nextTick(() => {
    const selected = document.getElementById(`benefit-map-cinema-${id}`);
    const container = document.getElementById("benefit-map-results");
    if (
      !selected ||
      !container ||
      container.scrollHeight <= container.clientHeight
    )
      return;
    const itemTop =
      selected.getBoundingClientRect().top -
      container.getBoundingClientRect().top +
      container.scrollTop;
    if (
      itemTop < container.scrollTop ||
      itemTop + selected.offsetHeight >
        container.scrollTop + container.clientHeight
    ) {
      container.scrollTo({
        top: Math.max(0, itemTop - 12),
        behavior: reduceMotion() ? "auto" : "smooth",
      });
    }
  });
}

function onResize() {
  if (!map) return;
  map.invalidateSize({ pan: false });
  enforceTaiwanBounds();
}

async function createMap() {
  if (creating || disposed || !mapEl.value || map) return;
  creating = true;
  try {
    // 既有專案使用 leaflet.markercluster；套件載入失敗時仍可顯示普通 Marker。
    try {
      window.L = L;
      await import("leaflet.markercluster");
    } catch (error) {
      console.warn("Marker 群聚未啟用，改用一般標記", error);
    }
    if (disposed || !mapEl.value) return;
    map = L.map(mapEl.value, {
      center: [23.8, 120.9],
      zoom: 7,
      minZoom: 4,
      maxZoom: 18,
      zoomControl: false,
      maxBounds: taiwanLatLngBounds,
      maxBoundsViscosity: 1,
      attributionControl: false,
    });
    L.control.zoom({ position: "bottomright" }).addTo(map);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors',
    }).addTo(map);
    markerLayer =
      typeof L.markerClusterGroup === "function"
        ? L.markerClusterGroup({
            showCoverageOnHover: false,
            maxClusterRadius: 48,
            spiderfyOnMaxZoom: true,
            disableClusteringAtZoom: 16,
          })
        : L.layerGroup();
    markerLayer.addTo(map);
    // ResizeObserver 已能監聽容器變動，不再同時註冊 window resize。
    if (typeof ResizeObserver !== "undefined") {
      resizeObserver = new ResizeObserver(onResize);
      resizeObserver.observe(mapEl.value);
    } else {
      window.addEventListener("resize", onResize);
    }
    await nextTick();
    if (disposed || !map) return;
    onResize();
    syncMarkers();
    syncLocationCircle();
    fitView();
  } catch (error) {
    console.error("特典據點地圖初始化失敗", error);
    mapError.value = "地圖暫時打不開，還是可以先逛逛右邊的影城清單。";
    cleanupMap();
  } finally {
    creating = false;
  }
}

function cleanupMap() {
  resizeObserver?.disconnect();
  resizeObserver = null;
  window.removeEventListener("resize", onResize);
  markerLayer?.clearLayers();
  markers.clear();
  map?.off();
  map?.remove();
  map = null;
  markerLayer = null;
  userDot = null;
  radiusCircle = null;
}

watch(cinemaItems, (items) => {
  if (
    selectedId.value != null &&
    !items.some((item) => item.cinemaId === selectedId.value)
  ) {
    selectedId.value = null;
  }
  if (
    detailCinemaId.value != null &&
    !items.some((item) => item.cinemaId === detailCinemaId.value)
  ) {
    closeBenefitDetail();
  }
  syncMarkers();
});

watch(selectedId, (id) => {
  const counts = new Map(
    cinemaItems.value.map(({ cinemaId, benefitCount }) => [
      cinemaId,
      benefitCount,
    ]),
  );
  for (const [markerId, marker] of markers) {
    marker.setIcon(markerIcon(markerId === id, counts.get(markerId) ?? 0));
  }
  scrollToSelected(id);
});
watch([nearbyActive, locateOrigin, radius], syncLocationCircle);

onMounted(() => {
  void createMap();
  void loadCinemas();
});

onBeforeUnmount(() => {
  disposed = true;
  locateVersion += 1;
  abortRequest();
  cleanupMap();
});
</script>

<template>
  <section class="benefit-map-page">
    <!-- 共用頁首與搜尋條件 -->
    <PageIntro
      kicker="✦ BENEFIT MAP"
      lead="特典"
      mark="地圖"
      description="想找電影特典？從地圖看看哪間影城有驚喜。"
      :icon-left="giftIcon"
      :icon-right="ticketIcon"
      :icon-top="pinIcon"
    />

    <SearchFilter
      v-model="appliedFilters"
      title="找找你想去的影城"
      :title-icon="compassIcon"
      submit-label="搜尋影城"
      :fields="filterFields"
    />

    <div class="map-layout">
      <section class="map-frame" aria-label="臺灣影城地圖">
        <div class="map-toolbar">
          <div class="toolbar-title">
            <span class="toolbar-icon">
              <Icon :icon="mapIcon" aria-hidden="true" />
            </span>
            <div class="toolbar-copy">
              <h2>影城地圖</h2>
              <p>點一下地圖上的標記，就能看看影城和特典資訊。</p>
            </div>
          </div>

          <div class="toolbar-actions">
            <label
              v-if="nearbyActive"
              class="radius-control"
              for="benefit-map-radius"
            >
              <span>附近範圍</span>
              <select id="benefit-map-radius" v-model.number="radius">
                <option
                  v-for="option in RADIUS_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </label>
            <AppButton
              type="button"
              :icon="nearIcon"
              :disabled="!!mapError"
              :aria-pressed="nearbyActive"
              @click="toggleNearby"
            >
              {{
                locating
                  ? "停止定位"
                  : nearbyActive
                    ? "查看全部影城"
                    : "找我附近的影城"
              }}
            </AppButton>
          </div>
        </div>

        <p v-if="locationNotice" class="location-notice" role="status">
          {{ locationNotice }}
        </p>

        <div class="map-body">
          <div
            ref="mapEl"
            class="map-canvas"
            role="region"
            aria-label="臺灣影城互動地圖"
          ></div>

          <div v-if="loading && !hasLoaded" class="map-overlay" role="status">
            <span class="loading-spinner" aria-hidden="true"></span>
            正在幫你找影城…
          </div>
          <div v-if="mapError" class="map-overlay" role="alert">
            {{ mapError }}
          </div>
          <div
            v-if="hasLoaded && !!cinemas.length && !markerCount"
            class="map-empty-tip"
          >
            這些影城目前還沒有地圖位置
          </div>

          <div
            v-if="hasLoaded && !mapError"
            class="map-info-chip"
            aria-live="polite"
          >
            <Icon :icon="pinIcon" aria-hidden="true" />
            <span>地圖上找到 {{ markerCount }} 間影城</span>
          </div>
        </div>
      </section>

      <aside class="results-panel" aria-label="影城搜尋結果">
        <div class="results-heading">
          <div class="results-heading-main">
            <div class="results-title-row">
              <h2>找到的影城</h2>
              <span class="count" aria-label="影城數量">
                {{ hasLoaded ? total : "—" }} <small>間</small>
              </span>
            </div>
            <p class="results-hint">{{ resultHint }}</p>
          </div>
        </div>

        <p v-if="errorMessage" class="error-notice" role="alert">
          {{ errorMessage }}
        </p>
        <p v-if="refreshing" class="refresh-notice" role="status">
          正在更新搜尋結果…
        </p>

        <div v-if="loading && !hasLoaded" class="panel-state" role="status">
          正在幫你找影城…
        </div>
        <div v-else-if="!hasLoaded" class="panel-state">
          <p>影城清單暫時載入失敗，再試一次吧。</p>
          <AppButton type="button" :icon="refreshIcon" @click="loadCinemas"
            >再試一次</AppButton
          >
        </div>
        <div v-else-if="cinemas.length === 0" class="panel-state">
          <Icon :icon="giftIcon" aria-hidden="true" />
          <p>
            {{
              nearbyActive
                ? "這附近暫時沒有符合條件的影城，試試拉大搜尋範圍，或換個條件找找看。"
                : "這次沒有找到符合條件的影城，換個關鍵字再找找看吧！"
            }}
          </p>
        </div>
        <div
          v-else
          id="benefit-map-results"
          class="cinema-list"
          aria-label="符合搜尋條件的影城"
        >
          <article
            v-for="(cinema, index) in cinemaItems"
            :id="`benefit-map-cinema-${cinema.cinemaId}`"
            :key="cinema.cinemaId"
            class="location-ticket"
            :class="{
              'is-selected': selectedId === cinema.cinemaId,
              'is-disabled': !cinema.canLocate || !!mapError,
            }"
          >
            <button
              type="button"
              class="ticket-hit"
              :aria-pressed="selectedId === cinema.cinemaId"
              :disabled="!cinema.canLocate || !!mapError"
              @click="focusCinema(cinema)"
            >
              <span class="ticket-stub" aria-hidden="true">
                <span class="ticket-index">{{
                  String(index + 1).padStart(2, "0")
                }}</span>
              </span>
              <span class="ticket-content">
                <strong class="ticket-name">{{
                  cinema.cinemaName || "影城"
                }}</strong>
                <span class="ticket-benefit-row">
                  <span class="ticket-benefits">
                    <Icon :icon="giftIcon" aria-hidden="true" />
                    <span>{{ formatBenefitCount(cinema.benefitCount) }}</span>
                  </span>
                </span>
              </span>
            </button>
          </article>
        </div>
      </aside>
    </div>

    <BenefitDialog
      v-if="detailCinema"
      :title="detailCinema.cinemaName || '影城'"
      :detail="dialogDetail(detailCinema)"
      :benefits="detailCinema.benefits"
      headline-key="movieName"
      headline-fallback="尚未提供電影名稱"
      close-label="返回地圖"
      @close="closeBenefitDetail"
    />
  </section>
</template>

<style scoped>
/* 視覺規則：沿用網站既有變數，減少邊框，卡片採「旅行站點／票根」形式。 */
.benefit-map-page {
  --map-brand: #ff7049;
  --map-ink: var(--c-ink, #263349);
  --map-muted: var(--c-muted, #6c7380);
  --map-surface: var(--c-card, #fff);
  --map-border: var(--c-border, #eae1d9);
  --map-warm: #fffaf5;
  --map-soft: #fff1e8;
  --map-navy: #202c45;
  color: var(--map-ink);
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

.benefit-map-page button,
.benefit-map-page select {
  font-family: inherit;
  font-size: var(--text-control);
  line-height: 1.5;
}

.benefit-map-page button {
  cursor: pointer;
}

.benefit-map-page button:disabled {
  cursor: not-allowed;
}

.benefit-map-page button:focus-visible,
.benefit-map-page select:focus-visible {
  outline: 2px solid var(--map-brand);
  outline-offset: 3px;
}

.map-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(365px, 0.83fr);
  align-items: stretch;
  height: min(72vh, 740px);
  min-height: 520px;
  overflow: hidden;
  border: 1px solid #f0e3d6;
  border-radius: 24px;
  background: var(--map-surface);
  box-shadow: 0 14px 35px rgb(65 43 29 / 6%);
}

.map-frame,
.results-panel {
  min-width: 0;
  min-height: 0;
}

.map-frame {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.map-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
  min-height: 6rem;
  padding: 1.05rem 1.25rem;
  background: #fffdf9;
}

.toolbar-title {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 0.75rem;
}

.toolbar-icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 45px;
  height: 45px;
  border-radius: 15px;
  background: var(--map-soft);
  color: var(--map-brand);
}

.toolbar-icon :deep(svg) {
  font-size: var(--text-section-title);
}

.toolbar-copy {
  min-width: 0;
}

.toolbar-copy h2 {
  margin: 0;
  font-size: var(--text-feature-title);
  line-height: 1.35;
  font-weight: 900;
  letter-spacing: 0.015em;
}

.toolbar-copy p {
  margin: 0.12rem 0 0;
  color: var(--map-muted);
  font-size: var(--text-control);
  line-height: 1.5;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.toolbar-actions :deep(.app-button) {
  min-height: 2.75rem;
  padding: 0.5rem 0.95rem;
  border-radius: 14px;
  box-shadow: none;
}

.radius-control {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--map-muted);
  font-weight: 700;
  font-size: var(--text-control);
}

.radius-control select {
  max-width: 9rem;
  min-height: 2.75rem;
  padding: 0.4rem 0.65rem;
  border: 1px solid var(--map-border);
  border-radius: 12px;
  background: #fff;
  color: var(--map-ink);
}

.location-notice {
  margin: 0;
  padding: 0.6rem 1rem;
  background: #fff6e8;
  color: #785521;
  font-weight: 700;
}

.map-body {
  position: relative;
  flex: 1;
  min-height: 260px;
  isolation: isolate;
}

.map-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  background: #e6eef0;
}

.map-info-chip,
.map-empty-tip {
  position: absolute;
  bottom: 1.1rem;
  left: 1.1rem;
  z-index: 400;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  max-width: calc(100% - 6.5rem);
  padding: 0.55rem 0.85rem;
  border: 1px solid rgb(255 255 255 / 75%);
  border-radius: 999px;
  background: rgb(255 253 249 / 96%);
  color: var(--map-navy);
  box-shadow: 0 5px 17px rgb(19 40 64 / 12%);
  font-size: var(--text-control);
  line-height: 1.4;
  font-weight: 750;
  pointer-events: none;
}

.map-info-chip :deep(svg) {
  color: var(--map-brand);
}

.map-empty-tip {
  bottom: 4.3rem;
  border-radius: 12px;
}

.map-overlay {
  position: absolute;
  inset: 0;
  z-index: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.8rem;
  padding: 1rem;
  background: rgb(255 253 249 / 92%);
  text-align: center;
  font-weight: 800;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 4px solid #f6d5cb;
  border-top-color: var(--map-brand);
  border-radius: 50%;
  animation: map-spin 0.8s linear infinite;
}

.results-panel {
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
  padding: 1.15rem 1.05rem 1rem;
  border-left: 1px solid #f0e3d6;
}

.results-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.7rem;
  padding: 0 0.2rem 0.6rem;
}

.results-heading-main {
  flex: 1;
  min-width: 0;
}

.results-title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.55rem;
}

.results-title-row h2 {
  margin: 0;
  color: var(--map-navy);
  font-size: var(--text-section-title);
  line-height: 1.25;
  font-weight: 900;
  letter-spacing: -0.03em;
}

.count {
  display: inline-flex;
  align-items: baseline;
  gap: 0.23rem;
  padding: 0.1rem 0.55rem;
  border-radius: 11px;
  background: var(--map-soft);
  color: #b85539;
  font-size: var(--text-card-title);
  line-height: 1.4;
  font-weight: 850;
  font-variant-numeric: tabular-nums;
}

.count small {
  font-size: var(--text-min);
  font-weight: 700;
}

.results-hint {
  margin: 0.45rem 0 0;
  color: var(--map-muted);
  font-size: var(--text-body);
  line-height: 1.55;
}

.error-notice,
.refresh-notice {
  margin: 0 0.2rem 0.65rem;
  padding: 0.65rem 0.8rem;
  border-radius: 12px;
  font-weight: 700;
}

.error-notice {
  color: #a74735;
  background: #fff0ed;
}

.refresh-notice {
  color: #246960;
  background: #edf9f6;
}

.panel-state {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.8rem;
  padding: 1rem;
  color: var(--map-muted);
  text-align: center;
}

.panel-state p {
  margin: 0;
}

.panel-state :deep(svg) {
  font-size: var(--text-page-title);
  color: #d89a80;
}

.cinema-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 0.65rem;
  min-height: 0;
  padding: 0.12rem 0.45rem 0.35rem 0.2rem;
  overflow: auto;
  scrollbar-gutter: stable;
  overscroll-behavior: contain;
}

/* 影城卡片左側保留編號票根，內容只顯示名稱與特典。 */
.location-ticket {
  flex-shrink: 0;
  width: 100%;
  border: 1px solid #f1e8df;
  border-radius: 17px;
  background: var(--map-warm);
  color: var(--map-ink);
  text-align: left;
  box-shadow: 0 2px 9px rgb(46 34 26 / 3%);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.ticket-hit {
  display: grid;
  grid-template-columns: 4.25rem minmax(0, 1fr);
  align-items: stretch;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.ticket-stub {
  display: flex;
  align-items: center;
  justify-content: center;
  align-self: stretch;
  margin: -1px 0 -1px -1px;
  border-right: 1px dashed #efccbb;
  border-radius: 17px 0 0 17px;
  background: #fff0e6;
  color: #b75c3e;
}

.ticket-index {
  font-size: var(--text-section-title);
  line-height: 1.2;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.ticket-content {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.75rem 0.9rem;
}

.ticket-name {
  display: block;
  color: var(--map-navy);
  font-size: var(--text-card-title);
  line-height: 1.4;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.ticket-benefits :deep(svg) {
  display: block;
  flex: none;
  width: 1.15em;
  height: 1.15em;
  margin-top: calc((1lh - 1.15em) / 2);
  font-size: inherit;
  line-height: inherit;
  color: #aa8272;
}

.ticket-benefit-row {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: flex-start;
  gap: 0.65rem;
}

.ticket-benefits {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 0.35rem;
  color: #9a4e38;
  font-size: var(--text-control);
  line-height: 1.45;
  font-weight: 800;
}

.location-ticket:hover:not(.is-disabled) {
  transform: translateX(3px);
  border-color: #efc2ad;
  background: #fff5ee;
  box-shadow: 0 5px 15px rgb(46 34 26 / 7%);
}

.location-ticket.is-selected {
  border-color: var(--map-brand);
  background: #fff5ef;
  box-shadow: 0 0 0 1px rgb(243 108 78 / 30%);
}

.location-ticket.is-selected .ticket-stub {
  background: var(--map-brand);
  color: #fff;
  border-right-color: rgb(255 255 255 / 60%);
}

.location-ticket.is-disabled {
  opacity: 0.63;
}

/* Leaflet 的標記和群聚使用珊瑚橘，選中圖釘加大並加光暈。 */
.map-canvas :deep(.benefit-marker-wrap) {
  position: relative;
  border: none;
  background: transparent;
}

.map-canvas :deep(.benefit-marker-count) {
  position: absolute;
  top: -14px;
  left: 24px;
  z-index: 2;
  min-width: 1.55rem;
  padding: 0.14rem 0.42rem;
  border: 2px solid #fff;
  border-radius: 999px;
  background: var(--map-soft);
  color: var(--map-brand);
  font-size: 13px;
  font-weight: 900;
  line-height: 1.15;
  text-align: center;
  box-shadow:
    0 0 0 1.5px var(--map-brand),
    0 2px 8px rgb(255 112 73 / 40%);
}

.map-canvas :deep(.benefit-marker.is-selected + .benefit-marker-count) {
  top: -22px;
  left: 32px;
}

.map-canvas :deep(.benefit-marker) {
  display: block;
  width: 21px;
  height: 21px;
  margin: 6px 0 0 4px;
  transform: rotate(-45deg);
  border: 3px solid #fff;
  border-radius: 50% 50% 50% 0;
  background: #f36c4e;
  box-shadow: 0 3px 10px #1d354c66;
}

.map-canvas :deep(.benefit-marker.is-selected) {
  width: 27px;
  height: 27px;
  margin: 2px 0 0 1px;
  background: var(--map-brand);
  box-shadow:
    0 0 0 4px rgb(255 255 255 / 90%),
    0 0 0 8px rgb(255 112 73 / 38%),
    0 6px 16px rgb(255 112 73 / 55%);
}

.map-canvas :deep(.marker-cluster-small),
.map-canvas :deep(.marker-cluster-medium),
.map-canvas :deep(.marker-cluster-large) {
  background: #ffd3bcb8;
}

.map-canvas.leaflet-container {
  font-family: var(--font-sans);
  font-size: var(--text-body);
  line-height: 1.55;
}

.map-canvas :deep(.marker-cluster-small div),
.map-canvas :deep(.marker-cluster-medium div),
.map-canvas :deep(.marker-cluster-large div) {
  background: var(--map-brand);
  color: #fff;
  font-size: var(--text-control);
  font-weight: 900;
}

.map-canvas :deep(.leaflet-control-attribution),
.map-canvas :deep(.leaflet-tooltip) {
  font-size: var(--text-min);
  line-height: 1.45;
}

.map-canvas :deep(.benefit-map-popup .leaflet-popup-content-wrapper) {
  padding: 0;
  border: 1px solid var(--c-border);
  border-radius: 16px;
  background: var(--c-card);
  color: var(--map-ink);
  box-shadow: 0 8px 18px rgb(32 44 69 / 8%);
}

.map-canvas :deep(.benefit-map-popup) {
  margin-bottom: 14px;
}

.map-canvas :deep(.benefit-map-popup .leaflet-popup-tip-container) {
  width: 28px;
  height: 14px;
  margin-top: -1px;
  margin-left: -14px;
}

.map-canvas :deep(.benefit-map-popup .leaflet-popup-tip) {
  width: 14px;
  height: 14px;
  margin: -8px auto 0;
  padding: 1px;
  border: 1px solid var(--c-border);
  background: var(--c-card);
  box-shadow: none;
}

.map-canvas :deep(.benefit-map-popup .leaflet-popup-content) {
  width: 300px !important;
  margin: 0;
  font-family: var(--font-sans);
  font-size: var(--text-min);
  line-height: 1.45;
}

.map-canvas :deep(.benefit-map-popup .leaflet-popup-content p) {
  margin: 0;
}

.map-canvas :deep(.benefit-map-popup a.leaflet-popup-close-button) {
  top: 0.4rem;
  right: 0.4rem;
  display: grid;
  place-items: center;
  width: 1.5rem;
  height: 1.5rem;
  padding: 0;
  border-radius: 999px;
  background: transparent;
  color: #8a837c;
  font: 600 1rem/1 var(--font-sans);
  text-decoration: none;
}

.map-canvas :deep(.benefit-map-popup a.leaflet-popup-close-button:hover),
.map-canvas
  :deep(.benefit-map-popup a.leaflet-popup-close-button:focus-visible) {
  background: #ffd3bc;
  color: #9a4e38;
}

.map-canvas :deep(.benefit-popup) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.45rem;
  box-sizing: border-box;
  width: 300px;
  padding: 0.75rem 0.85rem 0.7rem;
  text-align: left;
}

.map-canvas :deep(.popup-meta) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem 0.55rem;
  box-sizing: border-box;
  width: 100%;
  min-height: 1.5rem;
  padding-right: 1.35rem;
}

.map-canvas :deep(.popup-county) {
  display: inline-flex;
  align-items: center;
  margin: 0;
  padding: 0.1rem 0.55rem;
  border-radius: 999px;
  background: var(--map-soft);
  color: #b85539;
  font-size: var(--text-min);
  font-weight: 800;
  line-height: 1.4;
  text-align: left;
}

.map-canvas :deep(.popup-distance) {
  margin: 0;
  padding: 0;
  border-radius: 0;
  background: transparent;
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 500;
  line-height: 1.4;
  text-align: left;
}

.map-canvas :deep(.popup-title) {
  margin: 0;
  box-sizing: border-box;
  width: 100%;
  padding: 0;
  color: var(--map-navy);
  font-size: var(--text-card-title);
  font-weight: 900;
  line-height: 1.35;
  text-align: left;
  overflow-wrap: anywhere;
}

.map-canvas :deep(.popup-details) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.28rem;
  width: 100%;
}

.map-canvas :deep(.popup-address),
.map-canvas :deep(.popup-stock) {
  display: grid;
  grid-template-columns: 1.15rem minmax(0, 1fr);
  align-items: start;
  column-gap: 0.45rem;
  box-sizing: border-box;
  width: 100%;
  margin: 0;
  padding: 0;
  line-height: 1.5;
  text-align: left;
}

.map-canvas :deep(.popup-address) {
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 500;
}

.map-canvas :deep(.popup-icon) {
  width: 1.15rem;
  height: 1.15rem;
  margin-top: 0.12rem;
}

.map-canvas :deep(.popup-stock) {
  color: #e15a3c;
  font-size: var(--text-min);
  font-weight: 800;
}

.map-canvas :deep(.popup-stock.is-empty) {
  color: #8a837c;
  font-weight: 600;
}

.map-canvas :deep(.popup-actions) {
  display: flex;
  align-items: stretch;
  gap: 0.45rem;
  width: 100%;
  margin-top: 0.15rem;
}

.map-canvas :deep(.popup-action),
.map-canvas :deep(.popup-route) {
  display: flex;
  flex: 1 1 0;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-width: 0;
  margin: 0;
  padding: 0.45rem 0.4rem;
  border-radius: 12px;
  font-size: var(--text-min);
  font-weight: 800;
  line-height: 1.35;
  text-align: center;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.map-canvas :deep(.popup-action) {
  border: 0;
  background: var(--map-brand);
  color: #fff;
  font: inherit;
  font-size: var(--text-min);
  font-weight: 800;
}

.map-canvas :deep(.popup-action:hover),
.map-canvas :deep(.popup-action:focus-visible) {
  background: #f36c4e;
  transform: translateY(-1px);
}

.map-canvas :deep(.popup-route) {
  border: 1px solid #ffd3bc;
  background: var(--map-soft);
  color: #b85539;
}

.map-canvas :deep(.popup-route:hover),
.map-canvas :deep(.popup-route:focus-visible) {
  border-color: #f3b89a;
  background: #ffe4d4;
  color: #9a4e38;
}

@keyframes map-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1100px) {
  .map-layout {
    grid-template-columns: minmax(0, 1.25fr) minmax(330px, 0.85fr);
  }
}

@media (max-width: 800px) {
  .map-layout {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 0;
  }

  .map-frame {
    height: 56vh;
    min-height: 410px;
  }

  .results-panel {
    min-height: 260px;
    border-top: 1px solid #f0e3d6;
    border-left: 0;
  }

  .cinema-list {
    flex: initial;
    max-height: 560px;
  }

  .toolbar-copy p {
    display: none;
  }
}

@media (max-width: 520px) {
  .map-layout {
    border-radius: 20px;
  }

  .map-toolbar {
    padding: 0.8rem;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions :deep(.app-button) {
    flex: 1;
  }

  .radius-control {
    width: 100%;
    justify-content: space-between;
  }

  .radius-control select {
    width: 58%;
    max-width: none;
  }

  .map-frame {
    height: 58vh;
    min-height: 425px;
  }

  .results-panel {
    padding: 1rem 0.8rem;
  }

  .results-title-row h2 {
    font-size: var(--text-feature-title);
  }

  .ticket-hit {
    grid-template-columns: 3.6rem minmax(0, 1fr);
  }

  .ticket-content {
    padding: 0.65rem 0.75rem;
  }

  .ticket-index {
    font-size: var(--text-card-title);
  }

  .map-info-chip {
    left: 0.65rem;
    bottom: 0.65rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .loading-spinner {
    animation: none;
  }

  .location-ticket {
    transition: none;
  }

  .location-ticket:hover:not(.is-disabled) {
    transform: none;
  }
}
</style>
