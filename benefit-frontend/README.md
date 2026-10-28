# Benefits Journey｜前端

電影特典地圖的前端介面，提供電影與影城探索、特典及庫存查詢、會員收藏追蹤、交換刊登與站內訊息功能。

## 技術與環境

- 前端框架／語言：Vue 3、JavaScript、Vite、Vue Router
- UI 與地圖套件：Bootstrap 5、Bootstrap Icons、Iconify、SweetAlert2、Leaflet（含 MarkerCluster）、FullCalendar
- HTTP：Axios
- 部署：GitHub Pages
- 線上體驗：[https://benefitsjourney.github.io/movie-benefit-map/map](https://benefitsjourney.github.io/movie-benefit-map/map)
- 後端 API：由獨立的 Spring Boot 服務提供，正式環境為 [https://movie-benefit-map.onrender.com](https://movie-benefit-map.onrender.com)

## 主要頁面與功能

### 探索與地圖

- 瀏覽、搜尋電影、影城及特典，查看特典詳細資訊。
- 在地圖上查看影城位置，依電影、特典與影城查詢庫存。
- 查看特典類型與發放日期，包含一般週特典、入場贈與限量兌換。

### 會員中心

- 註冊、登入、登出及 Google 登入。
- 重設密碼、編輯個人資料、修改密碼與停用帳號。
- 查看自己的登入紀錄及其他會員的公開頁面。

### 收藏與行事曆

- 記錄持有的特典，管理收藏照片。
- 追蹤電影或特典，在個人行事曆查看檔期。
- 提供不需登入即可瀏覽的公開行事曆。

### 回報與交流

- 回報缺漏特典、上傳圖片與回報影城庫存。
- 查看公開貢獻排行榜。
- 刊登、編輯、上下架特典交換或票券轉讓資訊，並收藏刊登。
- 開啟會員對話、傳送訊息及標記已讀。

### 管理介面

- 管理會員狀態與登入紀錄。
- 管理電影、影城、特典及上下架狀態。
- 審核匯入資料與會員回報，並操作資料匯出。

## 本機執行

### 環境需求

- Node.js `^22.18.0` 或 `>=24.12.0`（見 `package.json` 的 `engines`）
- 後端 API 需先在本機 `http://localhost:8080` 啟動。開發伺服器會把 `/api` 轉送到該位址，並去掉 `/api` 前綴。

### 安裝與啟動

```bash
npm install
npm run dev
```

啟動後開啟 [http://localhost:5173/](http://localhost:5173/)。

### 環境變數

| 變數 | 說明 |
| --- | --- |
| `VITE_API_URL` | API 基底路徑。本機 `.env` 設為 `/api`，由 Vite 代理到後端。正式建置讀取 `.env.production`，指向已部署的後端。 |

### 建置與預覽

```bash
npm run build
npm run preview
```

`npm run build` 的靜態資源前綴為 `/movie-benefit-map/`，供 GitHub Pages 使用。`npm run dev` 仍以網站根路徑 `/` 提供頁面。

完整專案介紹與測試入口請見 [專案首頁](../README.md)。
