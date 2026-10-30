# Benefits Journey｜電影特典地圖

「電影特典地圖」是一個整合電影、影城、電影特典及庫存資訊的網站，目的是解決電影特典資訊分散、查詢不便的問題。

目前電影特典相關資訊大多散落在各影城官網、社群平台及粉絲社團，使用者往往需要花費大量時間搜尋與確認。

因此，本專案希望建立一個集中化的資訊平台，讓使用者能夠快速查詢電影特典、影城位置及庫存狀況，並透過地圖功能查看附近影城的特典資訊，提升資訊查詢的便利性。

## 線上體驗

| 項目 | 連結 |
| --- | --- |
| 網站測試版 | [電影特典地圖](https://benefitsjourney.github.io/movie-benefit-map/map) |
| 前端原始碼 | [benefit-frontend](https://github.com/benefitsjourney/movie-benefit-map/tree/main/benefit-frontend) |
| 後端原始碼 | [benefit-backend](https://github.com/benefitsjourney/movie-benefit-map/tree/main/benefit-backend) |
| API 文件 | [Swagger UI](https://movie-benefit-map.onrender.com/swagger-ui/index.html) |

> 未提供共用測試帳號。可在網站自行註冊，或使用 Google 登入。管理後台需管理員權限，請勿公開管理員帳密。Render 免費服務休眠後，首次開啟 API 可能需要等待。

## 主要功能

- **電影與特典資訊整合**：集中整理電影、影城及特典資訊，讓使用者輕鬆查詢感興趣的電影與特典，不必再跨平台搜尋。
- **影城地圖與庫存查詢**：結合地圖定位與會員庫存回報，快速找到附近有發放特典的影城，掌握各影城的特典庫存狀況。
- **會員特典交換與討論**：提供特典交換討論區與站內私訊功能，讓使用者發布交換需求、尋找交換對象，並與其他收藏者交流特典交換資訊。
- **自動化資訊蒐集與更新**：透過網路爬蟲自動蒐集電影及影城相關資訊，減少人工整理時間，提升平台資訊更新效率。
- **資料審核與品質管理**：結合自動化資料比對與後台人工審核機制，避免重複資料或錯誤更新，維持平台資訊的正確性與完整性。

## 技術概覽

| 範圍 | 技術 |
| --- | --- |
| 前端 | Vue 3、JavaScript、Vite、Vue Router、Bootstrap 5、Leaflet、FullCalendar、Axios |
| 後端 | Java 17、Spring Boot 4、Spring Security、Spring Data JPA |
| 身分驗證 | JWT、Google 登入 |
| 資料庫 | Microsoft SQL Server（本機預設）；H2（本機可選、Render 測試環境） |
| API 文件 | Swagger / OpenAPI |
| 部署 | 前端 GitHub Pages、後端 Render |

## 主要使用流程

1. 瀏覽電影、影城與特典，透過地圖和庫存回報尋找發放地點。
2. 登入後收藏特典、追蹤電影或特典，並在行事曆查看檔期。
3. 回報缺漏資訊或庫存，協助更新平台內容。
4. 刊登交換或票券轉讓資訊，並使用站內訊息交流。

## 專案架構

- **前端**：負責探索、地圖、搜尋、會員操作、行事曆及管理介面。詳見 [前端 README](benefit-frontend/README.md)。
- **後端**：提供 REST API，處理驗證授權、資料存取、貼文解析、審核、排程及會員互動。詳見 [後端 README](benefit-backend/README.md)。

## 專案狀態與注意事項

- 測試版已上線。前端部署於 GitHub Pages，後端部署於 Render，並以 H2 檔案資料庫運行。
- 未登入即可瀏覽電影、影城、特典、地圖庫存、公開行事曆、貢獻排行，以及交換與票券刊登。登入後可收藏、追蹤檔期、回報缺漏與庫存、刊登交換或轉讓，並使用站內訊息。
- 會員管理、貼文匯入、審核與資料匯出只開放給管理員。
- 後端在 Render 免費方案休眠後，第一次呼叫 API 會較慢。庫存與特典內容來自會員回報及影城貼文，經比對、審核後才納入，會隨時間變動，僅供參考。

## 專案畫面

<img width="1905" height="1244" alt="localhost_5173_map" src="https://github.com/user-attachments/assets/8ea0f301-c403-4863-8661-6474ab09f5a4" />
<img width="1920" height="914" alt="localhost_5173_map (2)" src="https://github.com/user-attachments/assets/039139cf-075c-4c32-98d4-8585d8fd0460" />
<img width="1905" height="5215" alt="localhost_5173_benefits" src="https://github.com/user-attachments/assets/77422e08-d0d3-46e1-bb0b-ff2ae339d40a" />
<img width="1905" height="1554" alt="localhost_5173_benefits_75" src="https://github.com/user-attachments/assets/0c6660be-478b-47c4-b755-6c18eba64e13" />
<img width="1905" height="1060" alt="localhost_5173_admin_crawler" src="https://github.com/user-attachments/assets/635f840c-3837-49f7-bdf0-e55a7ffe232c" />
<img width="1920" height="919" alt="localhost_5173_admin_crawler (1)" src="https://github.com/user-attachments/assets/d8526545-671e-4173-9856-aa11855b57bb" />
<img width="1920" height="919" alt="localhost_5173_admin_cinemas-movies_reviews" src="https://github.com/user-attachments/assets/1f87efc2-b8e1-4f70-b2cc-8a8ebb16a7cf" />
