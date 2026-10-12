<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
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
import AppButton from "@/components/AppButton.vue";
import PageIntro from "@/components/PageIntro.vue";
import SearchFilter from "@/components/SearchFilter.vue";

// Leaflet 座標使用 [緯度, 經度]；涵蓋本島、澎湖、金門與馬祖。
const TAIWAN_BOUNDS = [[21.85, 118.1], [26.45, 122.1]];
const DEFAULT_RADIUS = 5000;
const RADIUS_OPTIONS = [
  { value: 1000, label: "1 公里" },
  { value: 3000, label: "3 公里" },
  { value: 5000, label: "5 公里" },
  { value: 10000, label: "10 公里" },
  { value: 20000, label: "20 公里" },
];
const GEO_OPTIONS = { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 };

const filterFields = [
  {
    key: "keyword",
    label: "影城名稱",
    type: "search",
    placeholder: "例如：威秀、國賓",
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
  return Number.isFinite(number) && number >= min && number <= max ? number : null;
}

function getCinemaCoordinates(cinema) {
  const latitude = coordinate(cinema?.latitude, -90, 90);
  const longitude = coordinate(cinema?.longitude, -180, 180);
  return latitude === null || longitude === null ? null : { latitude, longitude };
}

function isWithinTaiwanBounds(position) {
  if (!position) return false;
  const { latitude, longitude } = position;
  return Number.isFinite(latitude) && Number.isFinite(longitude)
    && latitude >= TAIWAN_BOUNDS[0][0] && latitude <= TAIWAN_BOUNDS[1][0]
    && longitude >= TAIWAN_BOUNDS[0][1] && longitude <= TAIWAN_BOUNDS[1][1];
}

function formatDistance(value) {
  if (value == null || value === "") return "";
  const meters = Number(value);
  if (!Number.isFinite(meters) || meters < 0) return "";
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)} 公里` : `${Math.round(meters)} 公尺`;
}

// API 資料統一加工，避免在 Marker / Popup / 清單重複轉座標、檢查範圍、換算距離。
const cinemaItems = computed(() => cinemas.value.map((cinema) => {
  const position = getCinemaCoordinates(cinema);
  return {
    ...cinema,
    position,
    canLocate: cinema.cinemaId != null && isWithinTaiwanBounds(position),
    distanceText: formatDistance(cinema.distance),
  };
}));
const mappableCinemas = computed(() => cinemaItems.value.filter((cinema) => cinema.canLocate));
const markerCount = computed(() => mappableCinemas.value.length);
const activeKeyword = computed(() => String(appliedFilters.value.keyword ?? "").trim());
const resultHint = computed(() => {
  if (nearbyActive.value && resultRadius.value != null) {
    return `附近 ${formatDistance(resultRadius.value)} 內的影城`;
  }
  return appliedFilters.value.county || activeKeyword.value
    ? "符合條件的影城"
    : "全台影城";
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
let requestVersion = 0;
let locateVersion = 0;
let lastSuccessKey = null;

function abortRequest() {
  requestVersion += 1;
  requestController?.abort();
  requestController = null;
}

async function loadCinemas() {
  abortRequest();
  const version = requestVersion;
  const current = new AbortController();
  requestController = current;
  const requestKey = queryKey.value;
  loading.value = true;
  errorMessage.value = "";

  try {
    const { data } = await http.get("/cinemas/map", {
      params: { ...params.value },
      signal: current.signal,
    });
    if (current.signal.aborted || version !== requestVersion) return;
    if (!Array.isArray(data?.cinemas) || !Number.isInteger(data?.total) || data.total < 0) {
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
      if (!current.signal.aborted && version === requestVersion) fitView();
    }
  } catch (error) {
    if (current.signal.aborted || version !== requestVersion) return;
    console.error("影城地圖資料載入失敗", error);
    errorMessage.value = hasLoaded.value
      ? "更新失敗，畫面仍顯示上次的搜尋結果。"
      : "無法載入影城，請稍後再試。";
  } finally {
    if (version === requestVersion) {
      requestController = null;
      loading.value = false;
    }
  }
}

// 同一輪同步修改 keyword、county、半徑及定位時，Vue 會批次執行一次監聽。
// 無須額外的 setTimeout(0) / timer / 手動防抖。
watch(queryKey, () => { void loadCinemas(); }, { flush: "post" });

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
    locationNotice.value = "此瀏覽器不支援定位。";
    return;
  }

  try {
    geo.getCurrentPosition(
      ({ coords }) => {
        if (version !== locateVersion || disposed) return;
        locating.value = false;
        const position = { latitude: coords?.latitude, longitude: coords?.longitude };
        if (!isWithinTaiwanBounds(position)) {
          locationNotice.value = "目前位置不在地圖範圍內。";
          return;
        }
        locateOrigin.value = position;
        nearbyActive.value = true;
      },
      (error) => {
        if (version !== locateVersion || disposed) return;
        locating.value = false;
        locationNotice.value = error?.code === 1
          ? "請開啟瀏覽器定位權限後重試。"
          : "無法取得位置，請確認定位功能已開啟。";
      },
      GEO_OPTIONS,
    );
  } catch {
    if (version !== locateVersion || disposed) return;
    locating.value = false;
    locationNotice.value = "定位無法使用，請檢查瀏覽器設定。";
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
  return window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches ?? false;
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
  title.textContent = cinema.cinemaName || "未命名影城";
  const address = L.DomUtil.create("p", "popup-address", root);
  address.textContent = cinema.cinemaAddress || "暫無地址";

  if (cinema.distanceText) {
    const distance = L.DomUtil.create("p", "popup-distance", root);
    distance.textContent = `直線距離 ${cinema.distanceText}`;
  }
  const note = L.DomUtil.create("p", "popup-caveat", root);
  note.textContent = "特典資訊尚未開放查詢。";
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
    marker.bindPopup(makePopup(cinema), { className: "benefit-map-popup", maxWidth: 290 });
    marker.on("click", () => { selectedId.value = cinema.cinemaId; });
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
    fillOpacity: 0.10,
    interactive: false,
  }).addTo(map);
  userDot = L.circleMarker(center, {
    radius: 7,
    color: "#ffffff",
    weight: 3,
    fillColor: "#2769bb",
    fillOpacity: 1,
  }).addTo(map).bindPopup("你的位置");
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
    map.fitBounds(taiwanLatLngBounds, { ...options, maxZoom: 9 });
    return;
  }

  const positions = mappableCinemas.value.map((cinema) => [
    cinema.position.latitude,
    cinema.position.longitude,
  ]);
  if (positions.length === 1) {
    map.setView(positions[0], Math.max(map.getMinZoom(), 14), { animate: options.animate });
  } else if (positions.length > 1) {
    map.fitBounds(L.latLngBounds(positions), { ...options, maxZoom: 14 });
  } else {
    map.fitBounds(taiwanLatLngBounds, { ...options, maxZoom: 9 });
  }
}

function focusCinema(cinema) {
  selectedId.value = cinema.cinemaId;
  const marker = markers.get(cinema.cinemaId);
  if (!map || !marker) return;
  const open = () => {
    if (!map || disposed) return;
    map.setView(marker.getLatLng(), Math.max(map.getZoom(), 15), { animate: !reduceMotion() });
    marker.openPopup();
  };
  if (typeof markerLayer?.zoomToShowLayer === "function") markerLayer.zoomToShowLayer(marker, open);
  else open();
}

function scrollToSelected(id) {
  if (id == null) return;
  nextTick(() => {
    const selected = document.getElementById(`benefit-map-cinema-${id}`);
    const container = document.getElementById("benefit-map-results");
    if (!selected || !container || container.scrollHeight <= container.clientHeight) return;
    const itemTop = selected.getBoundingClientRect().top
      - container.getBoundingClientRect().top + container.scrollTop;
    if (itemTop < container.scrollTop
      || itemTop + selected.offsetHeight > container.scrollTop + container.clientHeight) {
      container.scrollTo({ top: Math.max(0, itemTop - 12), behavior: reduceMotion() ? "auto" : "smooth" });
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
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors',
    }).addTo(map);

    markerLayer = typeof L.markerClusterGroup === "function"
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
    mapError.value = "地圖載入失敗，仍可查看影城清單。";
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
  if (selectedId.value != null && !items.some((item) => item.cinemaId === selectedId.value)) {
    selectedId.value = null;
  }
  syncMarkers();
});
watch(selectedId, (id) => {
  for (const [markerId, marker] of markers) marker.setIcon(markerIcon(markerId === id));
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
    <PageIntro kicker="✦ BENEFIT MAP" lead="特典" mark="地圖" description="找影城、看位置，規劃你的下一站。" :icon-left="giftIcon"
      :icon-right="ticketIcon" :icon-top="pinIcon" />

    <SearchFilter v-model="appliedFilters" title="搜尋影城" :title-icon="compassIcon" submit-label="搜尋"
      :submit-icon="searchIcon" reset-label="清除" :fields="filterFields" />

    <div class="map-layout">
      <section class="map-frame" aria-label="臺灣影城地圖">
        <div class="map-toolbar">
          <div class="toolbar-title">
            <Icon :icon="mapIcon" aria-hidden="true" />
            <div><strong>影城地圖</strong><small>點選標記查看資訊</small></div>
          </div>
          <div class="toolbar-actions">
            <AppButton type="button" :icon="nearIcon" :disabled="!!mapError" :aria-pressed="nearbyActive"
              @click="toggleNearby">
              {{ locating ? "取消定位" : nearbyActive ? "取消附近搜尋" : "附近影城" }}
            </AppButton>
            <label v-if="nearbyActive" class="radius-control" for="benefit-map-radius">
              <span>範圍</span>
              <select id="benefit-map-radius" v-model.number="radius">
                <option v-for="option in RADIUS_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>
        </div>

        <p v-if="locationNotice" class="location-notice" role="status">{{ locationNotice }}</p>

        <div class="map-body">
          <div ref="mapEl" class="map-canvas" role="region" aria-label="臺灣影城互動地圖"></div>
          <div v-if="loading && !hasLoaded" class="map-overlay" role="status">
            <span class="loading-spinner" aria-hidden="true"></span>
            影城載入中…
          </div>
          <div v-if="mapError" class="map-overlay" role="alert">{{ mapError }}</div>
          <div v-if="hasLoaded && !!cinemas.length && !markerCount" class="map-empty-tip">
            沒有可顯示的位置資訊
          </div>
        </div>
        <div class="map-bottom-bar">
          <span>
            <Icon :icon="pinIcon" aria-hidden="true" /> 地圖範圍：臺灣及離島
          </span>
        </div>
      </section>

      <aside class="results-panel" aria-label="影城搜尋結果">
        <div class="results-heading">
          <div>
            <p class="eyebrow">CINEMAS</p>
            <h2>影城清單 <span class="count">{{ hasLoaded ? total : "—" }}</span></h2>
            <p>{{ resultHint }}</p>
          </div>
          <button class="refresh-button" type="button" aria-label="重新整理影城清單" :disabled="loading || refreshing"
            @click="loadCinemas">
            <Icon :icon="refreshIcon" aria-hidden="true" />
          </button>
        </div>
        <p class="benefit-disclaimer">
          <Icon :icon="giftIcon" aria-hidden="true" />
          目前僅顯示影城位置，特典資訊尚未開放。
        </p>
        <p v-if="errorMessage" class="error-notice" role="alert">{{ errorMessage }}</p>
        <p v-if="refreshing" class="refresh-notice" role="status">影城資料更新中…</p>

        <div v-if="loading && !hasLoaded" class="panel-state" role="status">影城載入中…</div>
        <div v-else-if="!hasLoaded" class="panel-state">
          <p>無法載入影城清單。</p>
          <AppButton type="button" :icon="refreshIcon" @click="loadCinemas">重新載入</AppButton>
        </div>
        <div v-else-if="cinemas.length === 0" class="panel-state">
          <Icon :icon="giftIcon" aria-hidden="true" />
          <p>{{ nearbyActive ? "附近沒有符合條件的影城，試試擴大範圍。" : "找不到符合條件的影城，試試其他名稱或縣市。" }}</p>
        </div>
        <div v-else id="benefit-map-results" class="cinema-list">
          <button v-for="(cinema, index) in cinemaItems" :id="`benefit-map-cinema-${cinema.cinemaId}`"
            :key="cinema.cinemaId" class="location-ticket" :class="{ 'is-selected': selectedId === cinema.cinemaId }"
            type="button" :aria-pressed="selectedId === cinema.cinemaId" :disabled="!cinema.canLocate || !!mapError"
            @click="focusCinema(cinema)">
            <span class="ticket-index">NO. {{ String(index + 1).padStart(2, "0") }}</span>
            <span class="ticket-county">{{ countyLabel(cinema.county) || "其他地區" }}</span>
            <strong class="ticket-name">{{ cinema.cinemaName || "未命名影城" }}</strong>
            <span class="ticket-address">
              <Icon :icon="pinIcon" aria-hidden="true" />{{ cinema.cinemaAddress || "暫無地址" }}
            </span>
            <span v-if="cinema.distanceText" class="ticket-distance">{{ cinema.distanceText }}
              <small>直線距離</small></span>
            <span v-else-if="!cinema.canLocate" class="ticket-muted">暫無位置資訊</span>
            <span v-if="cinema.canLocate && !mapError" class="ticket-link">查看位置</span>
          </button>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.benefit-map-page {
  --map-brand: var(--c-brand, #f36c4e);
  --map-ink: var(--c-ink, #263349);
  --map-muted: var(--c-muted, #6c7380);
  --map-surface: var(--c-card, #fff);
  --map-border: var(--c-border, #eae1d9);
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
  outline-offset: 2px;
}

.map-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(310px, .8fr);
  gap: 1rem;
}

.map-frame,
.results-panel {
  height: min(70vh, 730px);
  min-height: 510px;
  min-width: 0;
  border: 1.5px solid var(--map-border);
  border-radius: 22px;
  background: var(--map-surface);
  box-shadow: var(--shadow-soft);
}

.map-frame {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.map-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: .75rem;
  padding: .8rem 1rem;
  border-bottom: 1px solid var(--map-border);
  background: #fffdf9;
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: .55rem;
  min-width: 0;
}

.toolbar-title> :deep(svg) {
  color: var(--map-brand);
  font-size: 1.55rem;
}

.toolbar-title strong {
  display: block;
  font-size: var(--text-feature-title, 1.25rem);
  font-weight: 900;
}

.toolbar-title small {
  display: block;
  color: var(--map-muted);
  font-size: var(--text-control, 1rem);
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .6rem;
}

.toolbar-actions :deep(.app-button) {
  min-height: 40px;
  padding: .45rem .9rem;
}

.radius-control {
  display: inline-flex;
  align-items: center;
  gap: .4rem;
  color: var(--map-muted);
  font-weight: 700;
}

.radius-control select {
  max-width: 126px;
  min-height: 40px;
  padding: .35rem .5rem;
  border: 1.5px solid var(--map-border);
  border-radius: 12px;
  background: #fff;
  color: var(--map-ink);
}

.location-notice {
  margin: 0;
  padding: .55rem 1rem;
  background: #fff8e8;
  color: #795a21;
  font-weight: 700;
}

.map-body {
  position: relative;
  flex: 1;
  min-height: 230px;
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

.map-overlay {
  position: absolute;
  inset: 0;
  z-index: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: .8rem;
  padding: 1rem;
  background: #fffdf0e8;
  text-align: center;
  font-weight: 800;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 4px solid #f6d5cb;
  border-top-color: var(--map-brand);
  border-radius: 50%;
  animation: map-spin .8s linear infinite;
}

.map-empty-tip {
  position: absolute;
  z-index: 400;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  width: max-content;
  max-width: 90%;
  padding: .55rem .8rem;
  border-radius: 14px;
  background: #fffffff2;
  box-shadow: 0 2px 10px #0002;
  text-align: center;
}

.map-bottom-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: .5rem;
  flex-wrap: wrap;
  padding: .55rem .85rem;
  background: #fffdf9;
  color: var(--map-muted);
  font-size: var(--text-control, 1rem);
}

.map-bottom-bar span {
  display: inline-flex;
  align-items: center;
  gap: .25rem;
}

.results-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 1rem .95rem;
}

.results-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: .65rem;
  padding: 0 .2rem .85rem;
}

.results-heading h2 {
  margin: .2rem 0;
  font-size: var(--text-section-title, 1.875rem);
  line-height: 1.3;
  font-weight: 900;
}

.results-heading p {
  margin: 0;
  color: var(--map-muted);
}

.results-heading .eyebrow {
  color: #b36a53;
  font-size: var(--text-control, 1rem);
  overflow-wrap: anywhere;
  font-weight: 800;
  letter-spacing: .08em;
}

.count {
  display: inline-grid;
  place-items: center;
  min-width: 2.1rem;
  padding: 0 .4rem;
  border-radius: 10px;
  background: #ffede2;
  color: #b75537;
}

.refresh-button {
  display: grid;
  place-items: center;
  flex: none;
  width: 42px;
  height: 42px;
  border: 1px solid var(--map-border);
  border-radius: 12px;
  background: #fff;
  color: var(--map-muted);
}

.refresh-button :deep(svg) {
  font-size: 1.3rem;
}

.refresh-button:hover:not(:disabled) {
  color: var(--map-brand);
  background: #fff6ee;
}

.benefit-disclaimer {
  display: flex;
  gap: .5rem;
  margin: 0 0 .85rem;
  padding: .55rem .7rem;
  border: 1px dashed #e9d9c4;
  border-radius: 12px;
  background: #fffbf5;
  color: #786859;
  font-size: var(--text-control, 1rem);
}

.benefit-disclaimer :deep(svg) {
  flex: none;
  margin-top: .1rem;
  color: #c5883d;
  font-size: 1.1rem;
}

.error-notice,
.refresh-notice {
  margin: 0 0 .75rem;
  padding: .7rem;
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
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: .85rem;
  padding: 1rem;
  color: var(--map-muted);
  text-align: center;
}

.panel-state p {
  margin: 0;
}

.panel-state :deep(svg) {
  font-size: 2rem;
  color: #e0a18a;
}

.cinema-list {
  display: flex;
  flex-direction: column;
  gap: .75rem;
  flex: 1;
  min-height: 0;
  padding: .15rem .3rem .35rem .1rem;
  overflow: auto;
  scrollbar-gutter: stable;
}

.location-ticket {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: .4rem .6rem;
  flex-shrink: 0;
  width: 100%;
  padding: .85rem 1rem;
  border: 1.5px solid #eee2d9;
  border-left: 4px solid #ed9e79;
  border-radius: 15px;
  background: #fff;
  color: var(--map-ink);
  text-align: left;
  box-shadow: 0 3px 10px #28354d0b;
  transition: transform .18s, border-color .18s, background .18s;
}

/* 卡片內的地址與操作提示用 17px，輸入與工具列按鈕維持 16px */
.benefit-map-page .location-ticket {
  font-size: var(--text-body, 1.0625rem);
}

.location-ticket:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: #ed9e79;
}

.location-ticket.is-selected {
  border-color: var(--map-brand);
  background: #fff8f2;
}

.location-ticket:disabled {
  opacity: .6;
}

.ticket-index {
  color: #b57356;
  font-size: var(--text-control, 1rem);
  font-weight: 800;
}

.ticket-county {
  color: #397b78;
  font-size: var(--text-control, 1rem);
  font-weight: 800;
}

.ticket-name {
  grid-column: 1/-1;
  font-size: var(--text-card-title, 1.375rem);
  line-height: 1.4;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.ticket-address {
  display: flex;
  align-items: flex-start;
  gap: .2rem;
  grid-column: 1/-1;
  color: var(--map-muted);
  overflow-wrap: anywhere;
}

.ticket-address :deep(svg) {
  flex: none;
  margin-top: .15rem;
}

.ticket-distance {
  color: #ad5e45;
  font-weight: 800;
}

.ticket-distance small {
  color: var(--map-muted);
  font-size: var(--text-control, 1rem);
  font-weight: 600;
}

.ticket-muted {
  color: var(--map-muted);
}

.ticket-link {
  justify-self: end;
  color: #af593d;
  font-weight: 800;
}

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
  background: #273d5b;
}

.map-canvas :deep(.marker-cluster-small),
.map-canvas :deep(.marker-cluster-medium),
.map-canvas :deep(.marker-cluster-large) {
  background: #f1bda0b0;
}

.map-canvas :deep(.marker-cluster-small div),
.map-canvas :deep(.marker-cluster-medium div),
.map-canvas :deep(.marker-cluster-large div) {
  background: var(--map-brand);
  color: #fff;
  font-size: var(--text-control, 1rem);
  font-weight: 900;
}

.map-canvas :deep(.leaflet-control-attribution) {
  font-size: var(--text-control, 1rem);
  line-height: 1.4;
}

.map-canvas :deep(.benefit-map-popup .leaflet-popup-content-wrapper) {
  border-radius: 14px;
}

.map-canvas :deep(.benefit-popup) {
  min-width: 190px;
  font-size: var(--text-control, 1rem);
  line-height: 1.5;
}

.map-canvas :deep(.popup-eyebrow) {
  color: #a85f46;
  font-weight: 800;
}

.map-canvas :deep(.popup-title) {
  display: block;
  margin: .3rem 0;
  font-size: var(--text-feature-title, 1.25rem);
}

.map-canvas :deep(.popup-address),
.map-canvas :deep(.popup-distance) {
  margin: 0;
  color: #516273;
}

.map-canvas :deep(.popup-distance) {
  margin-top: .35rem;
  font-weight: 800;
}

.map-canvas :deep(.popup-caveat) {
  margin: .5rem 0 0;
  color: #837364;
  font-size: var(--text-control, 1rem);
}

.map-canvas :deep(.popup-link) {
  display: inline-block;
  margin-top: .55rem;
  color: #a9553e;
  font-weight: 800;
}

@keyframes map-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1020px) {
  .map-layout {
    grid-template-columns: minmax(0, 1.2fr) minmax(280px, .8fr);
  }
}

@media (max-width: 780px) {
  .map-layout {
    grid-template-columns: 1fr;
  }

  .map-frame {
    height: 58vh;
    min-height: 400px;
  }

  .results-panel {
    height: auto;
    max-height: none;
    min-height: 240px;
  }

  .cinema-list {
    overflow: visible;
  }

  .toolbar-title small {
    display: none;
  }
}

@media (max-width: 520px) {
  .map-toolbar {
    align-items: flex-start;
    padding: .7rem;
  }

  .toolbar-actions {
    width: 100%;
    gap: .4rem;
  }

  .toolbar-actions :deep(.app-button) {
    flex: 1;
    padding: .4rem .5rem;
  }

  .radius-control {
    width: 100%;
    justify-content: space-between;
  }

  .radius-control select {
    max-width: none;
    width: 60%;
  }

  .map-frame {
    height: 61vh;
    min-height: 425px;
  }

  .results-panel {
    padding: .8rem;
  }

  .results-heading h2 {
    font-size: 1.625rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .loading-spinner {
    animation: none;
  }

  .location-ticket {
    transition: none;
  }
}
</style>
