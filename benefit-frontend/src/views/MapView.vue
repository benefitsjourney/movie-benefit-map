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
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "leaflet.markercluster/dist/MarkerCluster.css";
import "leaflet.markercluster/dist/MarkerCluster.Default.css";
import http from "@/plugins/axios";
import { COUNTY_LABELS, countyLabel } from "@/constants/county";
import { CINEMA_SORT_DEFAULT, compareCinemas } from "@/constants/sort";
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
    label: "關鍵字",
    type: "search",
    placeholder: "搜尋影城名稱",
    icon: searchIcon,
  },
  {
    key: "county",
    label: "縣市",
    type: "select",
    emptyLabel: "全部縣市",
    icon: pinIcon,
    options: COUNTY_LABELS,
  },
];

// SearchFilter 套用後才更新篩選；附近搜尋另外加入定位和半徑參數。
const appliedFilters = ref({ keyword: "", county: "" });

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

// API 資料統一加工，避免在 Marker / Popup / 清單重複轉座標、檢查範圍、換算距離。
const cinemaItems = computed(() => {
  const items = cinemas.value.map((cinema) => {
    const position = getCinemaCoordinates(cinema);
    return {
      ...cinema,
      position,
      canLocate: cinema.cinemaId != null && isWithinTaiwanBounds(position),
      distanceText: formatDistance(cinema.distance),
    };
  });
  // 附近模式沿用 API：距離由近到遠，相同距離再依影城 ID。
  if (nearbyActive.value) return items;
  return [...items].sort((left, right) =>
    compareCinemas(left, right, CINEMA_SORT_DEFAULT),
  );
});
const mappableCinemas = computed(() =>
  cinemaItems.value.filter((cinema) => cinema.canLocate),
);
const markerCount = computed(() => mappableCinemas.value.length);
const activeKeyword = computed(() =>
  String(appliedFilters.value.keyword ?? "").trim(),
);
const resultHint = computed(() => {
  if (nearbyActive.value && resultRadius.value != null) {
    return `距離你 ${formatDistance(resultRadius.value)}內，由近到遠`;
  }
  const scope = appliedFilters.value.county || activeKeyword.value
    ? "搜尋結果"
    : "全台影城";
  return `${scope}，由北到南`;
});

// 唯一資料來源為 GET /cinemas/map，不在前端自行做 API 搜尋結果的距離篩選。
const params = computed(() => {
  const query = {};
  if (activeKeyword.value) query.keyword = activeKeyword.value;
  if (appliedFilters.value.county) query.county = appliedFilters.value.county;
  if (nearbyActive.value && locateOrigin.value) {
    query.latitude = locateOrigin.value.latitude;
    query.longitude = locateOrigin.value.longitude;
    query.radius = Number(radius.value);
  }
  return query;
});

const queryKey = computed(() => JSON.stringify(params.value));
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
    // 相同條件按「重新整理」不打斷使用者目前縮放的視角。
    const shouldFit = lastSuccessKey !== requestKey;
    lastSuccessKey = requestKey;
    if (shouldFit) {
      await nextTick(); // 先讓 Vue 更新清單與 Leaflet Marker。
      if (signal.aborted) return;
      fitView();
    }
  } catch (error) {
    if (signal.aborted) return;
    console.error("影城地圖資料載入失敗", error);
    errorMessage.value = hasLoaded.value
      ? "暫時無法更新，目前仍顯示上一次的搜尋結果。"
      : "暫時無法取得影城資訊，請稍後再試。";
  } finally {
    if (requestController === current) {
      requestController = null;
      loading.value = false;
    }
  }
}

// 同一輪同步修改 keyword、county、半徑及定位時，Vue 會批次執行一次監聽。
// 無須額外的 setTimeout(0) / timer / 手動防抖。

watch(
  queryKey,
  () => {
    void loadCinemas();
  },
  { flush: "post" },
);

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
    locating.value = false;
    locationNotice.value = "目前無法使用位置功能，請換個瀏覽器試試。";
    return;
  }
  try {
    geo.getCurrentPosition(
      ({ coords }) => {
        if (version !== locateVersion || disposed) return;
        locating.value = false;
        const position = {
          latitude: coords?.latitude,
          longitude: coords?.longitude,
        };
        if (!isWithinTaiwanBounds(position)) {
          locationNotice.value = "目前只能搜尋臺灣及離島附近的影城。";
          return;
        }
        locateOrigin.value = position;
        nearbyActive.value = true;
      },
      (error) => {
        if (version !== locateVersion || disposed) return;
        locating.value = false;
        locationNotice.value =
          error?.code === 1
            ? "請允許網站使用你的位置，再試一次。"
            : "找不到你目前的位置，請確認裝置的位置服務已開啟。";
      },
      GEO_OPTIONS,
    );
  } catch {
    if (version !== locateVersion || disposed) return;
    locating.value = false;
    locationNotice.value = "暫時無法取得你的位置，請檢查瀏覽器的位置權限。";
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

function markerIcon(selected = false) {
  return L.divIcon({
    className: "benefit-marker-wrap",
    html: `<span class="benefit-marker${selected ? " is-selected" : ""}"></span>`,
    iconSize: [30, 38],
    iconAnchor: [15, 33],
    popupAnchor: [0, -29],
  });
}

// API 欄位用 textContent 輸出，不拼入 HTML；僅經緯度用於 Google Maps 網址。

function makePopup(cinema) {
  const root = L.DomUtil.create("div", "benefit-popup");
  const eyebrow = L.DomUtil.create("span", "popup-eyebrow", root);
  eyebrow.textContent = countyLabel(cinema.county) || "影城";
  const title = L.DomUtil.create("strong", "popup-title", root);
  title.textContent = cinema.cinemaName || "影城";
  const address = L.DomUtil.create("p", "popup-address", root);
  address.textContent = cinema.cinemaAddress || "地址尚未提供";
  if (cinema.distanceText) {
    const distance = L.DomUtil.create("p", "popup-distance", root);
    distance.textContent = `離你約 ${cinema.distanceText}（直線距離）`;
  }
  if (cinema.position) {
    const link = L.DomUtil.create("a", "popup-link", root);
    const { latitude, longitude } = cinema.position;
    link.href = `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(`${latitude},${longitude}`)}`;
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    link.textContent = "查看路線 ↗";
  }
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
      icon: markerIcon(cinema.cinemaId === selectedId.value),
    });
    marker.bindPopup(makePopup(cinema), {
      className: "benefit-map-popup",
      maxWidth: 290,
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
    .bindPopup("你的位置");
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
  if (!activeKeyword.value && !appliedFilters.value.county) {
    map.fitBounds(INITIAL_VIEW_BOUNDS, { ...options, maxZoom: 9 });
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
    map.fitBounds(INITIAL_VIEW_BOUNDS, { ...options, maxZoom: 9 });
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
    mapError.value = "地圖暫時無法顯示，你仍可以查看影城清單。";
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
  syncMarkers();
});

watch(selectedId, (id) => {
  for (const [markerId, marker] of markers)
    marker.setIcon(markerIcon(markerId === id));
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
    <!-- 保留共用頁面介紹、篩選模組與 API 接線 -->
    <PageIntro kicker="✦ BENEFIT MAP" lead="特典" mark="地圖" description="從地圖出發，尋找你的下一站影城。" :icon-left="giftIcon"
      :icon-right="ticketIcon" :icon-top="pinIcon" />

    <SearchFilter v-model="appliedFilters" title="尋找下一站" :title-icon="compassIcon" submit-label="探索影城"
      :fields="filterFields" />

    <div class="map-layout">
      <section class="map-frame" aria-label="臺灣影城地圖">
        <div class="map-toolbar">
          <div class="toolbar-title">
            <span class="toolbar-icon">
              <Icon :icon="mapIcon" aria-hidden="true" />
            </span>
            <div class="toolbar-copy">
              <h2>探索地圖</h2>
              <p>點選地圖上的圖釘，查看影城資訊</p>
            </div>
          </div>

          <div class="toolbar-actions">
            <label v-if="nearbyActive" class="radius-control" for="benefit-map-radius">
              <span>搜尋範圍</span>
              <select id="benefit-map-radius" v-model.number="radius">
                <option v-for="option in RADIUS_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <AppButton type="button" :icon="nearIcon" :disabled="!!mapError" :aria-pressed="nearbyActive"
              @click="toggleNearby">
              {{
                locating
                  ? "取消搜尋"
                  : nearbyActive
                    ? "取消附近搜尋"
                    : "找附近影城"
              }}
            </AppButton>
          </div>
        </div>

        <p v-if="locationNotice" class="location-notice" role="status">
          {{ locationNotice }}
        </p>

        <div class="map-body">
          <div ref="mapEl" class="map-canvas" role="region" aria-label="臺灣影城互動地圖"></div>

          <div v-if="loading && !hasLoaded" class="map-overlay" role="status">
            <span class="loading-spinner" aria-hidden="true"></span>
            正在尋找影城…
          </div>
          <div v-if="mapError" class="map-overlay" role="alert">
            {{ mapError }}
          </div>
          <div v-if="hasLoaded && !!cinemas.length && !markerCount" class="map-empty-tip">
            目前沒有可在地圖上顯示的影城
          </div>

          <div v-if="hasLoaded && !mapError" class="map-info-chip" aria-live="polite">
            <Icon :icon="pinIcon" aria-hidden="true" />
            <span>地圖上有 {{ markerCount }} 間影城</span>
          </div>
        </div>
      </section>

      <aside class="results-panel" aria-label="影城搜尋結果">
        <div class="results-heading">
          <div class="results-heading-main">
            <div class="results-title-row">
              <h2>影城探索</h2>
              <span class="count" aria-label="影城數量">
                {{ hasLoaded ? total : "—" }} <small>間</small>
              </span>
            </div>
            <p class="results-hint">{{ resultHint }}</p>
          </div>
          <button class="refresh-button" type="button" aria-label="更新影城清單" title="更新影城清單" :disabled="loading"
            @click="loadCinemas">
            <Icon :icon="refreshIcon" aria-hidden="true" />
          </button>
        </div>

        <p v-if="errorMessage" class="error-notice" role="alert">
          {{ errorMessage }}
        </p>
        <p v-if="refreshing" class="refresh-notice" role="status">
          正在更新影城清單…
        </p>

        <div v-if="loading && !hasLoaded" class="panel-state" role="status">
          正在尋找影城…
        </div>
        <div v-else-if="!hasLoaded" class="panel-state">
          <p>目前無法顯示影城，請再試一次。</p>
          <AppButton type="button" :icon="refreshIcon" @click="loadCinemas">再試一次</AppButton>
        </div>
        <div v-else-if="cinemas.length === 0" class="panel-state">
          <Icon :icon="giftIcon" aria-hidden="true" />
          <p>
            {{
              nearbyActive
                ? "這附近沒有找到符合條件的影城，試試擴大範圍或調整搜尋條件。"
                : "沒有找到符合條件的影城，換個名稱或縣市試試吧。"
            }}
          </p>
        </div>
        <div v-else id="benefit-map-results" class="cinema-list" aria-label="符合搜尋條件的影城">
          <button v-for="(cinema, index) in cinemaItems" :id="`benefit-map-cinema-${cinema.cinemaId}`"
            :key="cinema.cinemaId" type="button" class="location-ticket"
            :class="{ 'is-selected': selectedId === cinema.cinemaId }" :aria-pressed="selectedId === cinema.cinemaId"
            :disabled="!cinema.canLocate || !!mapError" @click="focusCinema(cinema)">
            <span class="ticket-stub" aria-hidden="true">
              <span class="ticket-index">{{
                String(index + 1).padStart(2, "0")
              }}</span>
            </span>
            <span class="ticket-content">
              <span class="ticket-topline">
                <span class="ticket-county">{{
                  countyLabel(cinema.county) || "其他地區"
                }}</span>
                <span v-if="cinema.distanceText" class="ticket-distance">直線距離 {{ cinema.distanceText }}</span>
                <span v-else-if="!cinema.canLocate" class="ticket-muted">尚無地圖位置</span>
              </span>
              <strong class="ticket-name">{{
                cinema.cinemaName || "影城"
              }}</strong>
              <span class="ticket-address">
                <Icon :icon="pinIcon" aria-hidden="true" />
                {{ cinema.cinemaAddress || "地址尚未提供" }}
              </span>
            </span>
            <span class="ticket-action" aria-hidden="true">
              <Icon :icon="compassIcon" />
            </span>
          </button>
        </div>
      </aside>
    </div>
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
  font-size: var(--text-body, 1.0625rem);
}

.benefit-map-page button,
.benefit-map-page select {
  font-family: inherit;
  font-size: var(--text-control, 1rem);
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
  gap: 1.25rem;
  align-items: stretch;
}

.map-frame,
.results-panel {
  min-width: 0;
  height: min(72vh, 740px);
  min-height: 520px;
  border: 1px solid #f0e3d6;
  border-radius: 24px;
  background: var(--map-surface);
  box-shadow: 0 14px 35px rgb(65 43 29 / 6%);
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
  min-height: 86px;
  padding: 1rem 1.25rem;
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
  font-size: 1.55rem;
}

.toolbar-copy {
  min-width: 0;
}

.toolbar-copy h2 {
  margin: 0;
  font-size: var(--text-feature-title, 1.25rem);
  line-height: 1.35;
  font-weight: 900;
  letter-spacing: 0.015em;
}

.toolbar-copy p {
  margin: 0.12rem 0 0;
  color: var(--map-muted);
  font-size: var(--text-control, 1rem);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.toolbar-actions :deep(.app-button) {
  min-height: 42px;
  padding: 0.48rem 0.9rem;
  border-radius: 14px;
  box-shadow: none;
}

.radius-control {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--map-muted);
  font-weight: 700;
  font-size: 1rem;
}

.radius-control select {
  max-width: 135px;
  min-height: 42px;
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
  font-size: 1rem;
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
}

.results-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.7rem;
  padding: 0 0.2rem 0.6rem;
}

.results-heading-main {
  min-width: 0;
}

.results-title-row {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.results-title-row h2 {
  margin: 0;
  color: var(--map-navy);
  font-size: var(--text-section-title, 1.875rem);
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
  font-size: 1.2rem;
  line-height: 1.4;
  font-weight: 850;
  font-variant-numeric: tabular-nums;
}

.count small {
  font-size: 1rem;
  font-weight: 700;
}

.results-hint {
  margin: 0.4rem 0 0;
  color: var(--map-muted);
  font-size: 1rem;
}

.refresh-button {
  display: grid;
  place-items: center;
  flex: none;
  width: 43px;
  height: 43px;
  border: 1px solid #efe6dc;
  border-radius: 14px;
  background: #fff;
  color: #656b74;
  transition:
    background 0.2s,
    color 0.2s,
    transform 0.2s;
}

.refresh-button :deep(svg) {
  font-size: 1.3rem;
}

.refresh-button:hover:not(:disabled) {
  color: var(--map-brand);
  background: var(--map-soft);
  transform: rotate(20deg);
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
  font-size: 2rem;
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

/* 車票卡片：左側是站點編號，右側名稱與地址；不使用厚重外框。 */
.location-ticket {
  display: grid;
  grid-template-columns: 63px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 0.85rem;
  flex-shrink: 0;
  width: 100%;
  min-height: 112px;
  padding: 0 0.85rem 0 0;
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

.ticket-stub {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  align-self: stretch;
  margin: -1px 0 -1px -1px;
  border-right: 1px dashed #efccbb;
  border-radius: 17px 0 0 17px;
  background: #fff0e6;
  color: #b75c3e;
}

.ticket-index {
  font-size: 1.45rem;
  line-height: 1.2;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.ticket-content {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 0.17rem;
  padding-block: 0.7rem;
}

.ticket-topline {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.3rem 0.6rem;
  min-width: 0;
}

.ticket-county {
  color: #a4654d;
  font-size: 1rem;
  font-weight: 800;
}

.ticket-distance,
.ticket-muted {
  color: #81766f;
  font-size: 1rem;
  font-weight: 600;
}

.ticket-name {
  display: block;
  color: var(--map-navy);
  font-size: 1.2rem;
  line-height: 1.35;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.ticket-address {
  display: flex;
  align-items: flex-start;
  gap: 0.27rem;
  color: #726e6b;
  font-size: 1rem;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.ticket-address :deep(svg) {
  flex: none;
  margin-top: 0.15rem;
  font-size: 1.07rem;
  color: #aa8272;
}

.ticket-action {
  color: #b3aaa2;
  font-size: 1.25rem;
  transition:
    transform 0.18s ease,
    color 0.18s ease;
}

.location-ticket:hover:not(:disabled) {
  transform: translateX(3px);
  border-color: #efc2ad;
  background: #fff5ee;
  box-shadow: 0 5px 15px rgb(46 34 26 / 7%);
}

.location-ticket:hover:not(:disabled) .ticket-action {
  color: var(--map-brand);
  transform: rotate(20deg);
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

.location-ticket.is-selected .ticket-action {
  color: var(--map-brand);
}

.location-ticket:disabled {
  opacity: 0.63;
}

/* Leaflet 的標記和群聚使用珊瑚橘／深藍，避免和其他縣市色混雜。 */
.map-canvas :deep(.benefit-marker-wrap) {
  border: none;
  background: transparent;
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
  background: var(--map-navy);
  box-shadow:
    0 0 0 4px rgb(255 255 255 / 60%),
    0 4px 13px rgb(29 53 76 / 35%);
}

.map-canvas :deep(.marker-cluster-small),
.map-canvas :deep(.marker-cluster-medium),
.map-canvas :deep(.marker-cluster-large) {
  background: #ffd3bcb8;
}

.map-canvas :deep(.marker-cluster-small div),
.map-canvas :deep(.marker-cluster-medium div),
.map-canvas :deep(.marker-cluster-large div) {
  background: var(--map-brand);
  color: #fff;
  font-size: 1rem;
  font-weight: 900;
}

.map-canvas :deep(.leaflet-control-attribution) {
  font-size: 1rem;
  line-height: 1.4;
}

.map-canvas :deep(.benefit-map-popup .leaflet-popup-content-wrapper) {
  border-radius: 14px;
}

.map-canvas :deep(.benefit-popup) {
  min-width: 190px;
  font-size: 1rem;
  line-height: 1.5;
}

.map-canvas :deep(.popup-eyebrow) {
  color: #a85f46;
  font-weight: 800;
}

.map-canvas :deep(.popup-title) {
  display: block;
  margin: 0.3rem 0;
  font-size: 1.25rem;
}

.map-canvas :deep(.popup-address),
.map-canvas :deep(.popup-distance) {
  margin: 0;
  color: #516273;
}

.map-canvas :deep(.popup-distance) {
  margin-top: 0.35rem;
  font-weight: 800;
}

.map-canvas :deep(.popup-link) {
  display: inline-block;
  margin-top: 0.55rem;
  color: #a9553e;
  font-weight: 800;
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

  .ticket-name {
    font-size: 1.125rem;
  }
}

@media (max-width: 800px) {
  .map-layout {
    grid-template-columns: 1fr;
  }

  .map-frame {
    height: 56vh;
    min-height: 410px;
  }

  .results-panel {
    height: auto;
    min-height: 260px;
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
    gap: 0.85rem;
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
    font-size: 1.625rem;
  }

  .location-ticket {
    grid-template-columns: 55px minmax(0, 1fr) 20px;
    gap: 0.55rem;
    padding-right: 0.55rem;
  }

  .ticket-index {
    font-size: 1.3rem;
  }

  .ticket-name {
    font-size: 1.125rem;
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

  .location-ticket,
  .ticket-action,
  .refresh-button {
    transition: none;
  }

  .location-ticket:hover:not(:disabled),
  .refresh-button:hover:not(:disabled) {
    transform: none;
  }
}
</style>
