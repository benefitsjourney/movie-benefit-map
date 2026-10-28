# Benefits Journey｜後端

電影特典地圖的 Spring Boot 後端服務，提供電影、影城、特典與庫存查詢 API，處理會員驗證、資料匯入與審核、收藏追蹤、交換刊登及站內訊息。

## 技術

| 項目 | 技術 |
| --- | --- |
| 語言／框架 | Java 17、Spring Boot 4 |
| 安全與驗證 | Spring Security、JWT、Google 登入 |
| 資料存取 | Spring Data JPA |
| 資料庫 | Microsoft SQL Server、H2（測試） |
| API 文件 | Swagger / OpenAPI |
| 部署 | Render |

## 核心模組

### 身分驗證與會員管理

- 會員註冊、帳密及 Google 登入、登出，使用 JWT 驗證身分。
- 忘記密碼與重設信、個人資料及密碼修改、帳號停用。
- 登入紀錄查詢，以及管理員的會員狀態管理。

### 電影、影城、特典與庫存

- 電影、影城和特典的查詢、新增及修改，提供探索列表與地圖所需資料。
- 依電影、影城或特典查詢發放資訊與庫存狀態。
- 管理員管理特典上下架、編輯與刪除。
- 會員回報指定影城的特典庫存，管理員可查詢及刪除回報。

### 貼文匯入與資料審核

這是專案主要的資料處理流程：

1. 從 Facebook 影城貼文或 JSON 檔案匯入資料。
2. 依影城規則解析電影、影城、特典類型、日期及優惠內容；包含威秀、秀泰的專屬解析規則。
3. 比對電影、影城及既有貼文，跳過重複資料；支援重新配對與批次調整日期。
4. 透過管理員審核，將確認後的電影與影城資料納入正式資料。
5. 支援排程抓取與執行紀錄，供後續追蹤。

### 會員回報與審核

- 會員提交缺漏特典並上傳圖片。
- 管理員可核准、退回或修改回報；核准後建立特典並關聯影城。
- 統計會員回報貢獻，提供排行榜資料。

### 收藏、追蹤與行事曆

- 管理會員持有的特典與收藏照片，避免同一特典重複收藏。
- 提供電影與特典追蹤、個人行事曆及公開行事曆資料。

### 票券、交換與站內訊息

- 管理會員刊登、編輯、上下架及收藏的交換／票券資訊。
- 自動處理過期刊登與特典下架。
- 提供會員對話、訊息傳送、對話紀錄與已讀狀態管理。

### 後台與維運

- 匯出電影、影城及特典 Excel；爬蟲資料可匯出 SQL。
- 提供健康檢查 API。
- 可依環境變數初始化第一位管理員。
- 使用 CORS 限制允許的前端來源（GitHub Pages、Render、本機開發）。

## API 文件

- Swagger UI：[https://movie-benefit-map.onrender.com/swagger-ui/index.html](https://movie-benefit-map.onrender.com/swagger-ui/index.html)
- OpenAPI JSON：`/v3/api-docs`
- 測試環境基底網址：`https://movie-benefit-map.onrender.com`（無額外 context path）

未登入即可查詢的主要路徑：

| 方法 | 路徑 | 說明 |
| --- | --- | --- |
| GET | `/health` | 健康檢查 |
| GET | `/movies/explore` | 電影探索 |
| GET | `/cinemas/explore`、`/cinemas/map` | 影城列表與地圖 |
| GET | `/benefits`、`/benefits/{benefitId}` | 特典列表與詳情 |
| GET | `/movies/{movieId}/benefits` | 某電影的特典 |
| GET | `/benefits/calendar` | 公開行事曆 |
| GET | `/benefits/{benefitId}/cinemas/status` | 某特典在各影城的庫存 |
| GET | `/cinemas/{cinemaId}/benefits/status` | 某影城的特典庫存 |
| GET | `/movies/{movieId}/benefits/status` | 某電影的特典庫存 |
| GET | `/exchange-listings`、`/exchange-listings/{id}` | 公開交換刊登 |
| GET | `/ticket-listings`、`/ticket-listings/{id}` | 公開票券刊登 |
| GET | `/users/{id}` | 會員公開頁 |
| GET | `/users/report-ranking` | 回報貢獻排行 |

其餘寫入、個人資料、收藏、訊息與 `/admin` 路徑需登入；管理操作另需管理員角色。完整欄位與錯誤碼以 Swagger 為準。

## 本機執行與測試

### 環境需求

- JDK 17
- 專案內的 Maven Wrapper（Windows 用 `mvnw.cmd`，macOS／Linux 用 `./mvnw`）

### 資料庫

- 未設定 `DB_TYPE` 時使用 `mssql`。連線寫在 `application.yml`：本機 SQL Server `localhost:14330`，資料庫 `BenefitsJourney`。帳密只留在該設定檔，本文件不列出。
- `DB_TYPE=h2` 會改走 `application-h2.yml`，使用檔案型 H2（`./data/h2/BenefitsJourney`）。Render 映像即採此設定；表由 JPA 建立，空資料庫再由 `SqlDataInitializer` 匯入種子 SQL。

### 環境變數

本機從專案根目錄的 `.env` 讀取（`application.yml` 的 `spring.config.import`）。

#### 必填

| 變數 | 沒設會怎樣 |
| --- | --- |
| `MAIL_USERNAME`、`MAIL_PASSWORD` | Gmail SMTP 帳號與應用程式密碼，寫在 `.env`。`application.yml` 沒有預設值，沒設服務起不來。用來寄忘記密碼信（`POST /auth/password/forgot`）。帳密只留在 `.env`，本文件不列出。 |

#### 選填（沒設仍可啟動）

| 變數 | 未設定時 | 沒有它就測不到 |
| --- | --- | --- |
| `PORT` | 使用 `8080`。 | — |
| `DB_TYPE` | 使用 `mssql`，連本機 SQL Server。 | 設成 `h2` 才改走檔案型 H2，不必先裝 SQL Server。 |
| `ADMIN_INIT_ACCOUNT`、`ADMIN_INIT_PASSWORD`、`ADMIN_INIT_EMAIL` | 不建立管理員。三者都要有值才會建立；密碼須 8～16 碼。資料庫裡已有 `ADMIN`，或帳號／信箱已被占用時不會再建立。種子 SQL 只有一般會員。 | `/admin`、貼文匯入、審核、上下架、匯出等管理 API。一般會員註冊後角色是 `USER`，不能代替。 |
| `CLOUDINARY_CLOUD_NAME`、`CLOUDINARY_API_KEY`、`CLOUDINARY_API_SECRET` | 服務照常啟動，上傳會失敗並寫 log。 | 收藏照片上傳、缺漏特典回報附圖、貼文匯入時把圖片轉存到 Cloudinary。JSON 匯入的文字解析仍可測，只是官方圖會是空的。可用 `GET /crawler/benefits/cloudinary-check`（需管理員）確認連線。 |
| `CLOUDINARY_FOLDER` | 圖片放在 `benefits` 資料夾。 | — |
| `APIFY_TOKEN` | 服務照常啟動。 | `POST /crawler/benefits/import-apify` 與排程抓 Facebook 貼文，會回 503。改上傳既有 JSON（`POST /crawler/benefits/import-json`）仍可測解析與審核。 |
| `APIFY_FACEBOOK_ACTOR` | 使用 `apify/facebook-posts-scraper`。 | 只有要換 Actor 時才需要設。 |

電影、影城、特典、地圖、庫存查詢、帳密登入、收藏（不含照片上傳）、追蹤、行事曆、交換與票券刊登、站內訊息，都不依賴上表的選填變數。忘記密碼寄信一定要有上面的郵件帳密。

### 啟動

SQL Server：

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

H2：

```bash
# Windows PowerShell
$env:DB_TYPE="h2"; .\mvnw.cmd spring-boot:run

# macOS / Linux
DB_TYPE=h2 ./mvnw spring-boot:run
```

啟動後可開 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)，並以 `GET /health` 確認服務有起來。

### 測試

```bash
# Windows
mvnw.cmd test

# macOS / Linux
./mvnw test
```

測試以 Mock 的 WebMvc 與 Service 單元測試為主，涵蓋電影、影城、地圖、庫存回報、貼文解析（含威秀、秀泰規則）、日期調整、Excel 匯出、爬蟲匯入與健康檢查；這些不需要 Cloudinary、Apify 或管理員環境變數。`BenefitsjourneyApplicationTests` 與 `BenefitAdminServiceTransactionTest` 會啟動 Spring context，因此一樣要有 `.env` 的 `MAIL_USERNAME`、`MAIL_PASSWORD`，且預設會連本機 SQL Server。不想連 SQL Server 時，執行這兩類測試前先設 `DB_TYPE=h2`。

## 架構補充（建議後續新增）

- 系統模組／分層架構圖。
- 核心資料表與關聯圖（ERD）。
- 貼文匯入及管理員審核的實際流程圖。

完整專案介紹與線上測試入口請見 [專案首頁](../README.md)。
