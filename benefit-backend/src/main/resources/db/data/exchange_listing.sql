-- exchange_listing.sql
-- 必須在持有特典紀錄（use_benefit）資料建立完成後執行。
-- 依照指定的 use_benefit_id 查詢持有紀錄。
-- 不使用 NOT EXISTS，重複執行可能產生重複資料或違反唯一鍵限制。


-- 01. 交換《快打旋風》A3 海報特典
-- use_benefit_id = 1

INSERT INTO dbo.[exchange_listing]
    ([title], [description], [status], [use_benefit_id], [created_at], [updated_at])
SELECT
    N'交換《快打旋風》A3 海報特典',
    N'手上有《快打旋風》的 A3 海報特典，希望交換其他電影的特典海報，歡迎提出想交換的特典款式。',
    N'ACTIVE',
    ub.[use_benefit_id],
    CONVERT(DATETIME2, '2026-10-28 09:15:00'),
    CONVERT(DATETIME2, '2026-10-28 09:15:00')
FROM dbo.[use_benefit] ub
WHERE ub.[use_benefit_id] = 1;


-- 02. 交換《惡靈古堡：爆發夜》暗夜之路款 A3 海報
-- use_benefit_id = 2

INSERT INTO dbo.[exchange_listing]
    ([title], [description], [status], [use_benefit_id], [created_at], [updated_at])
SELECT
    N'交換《惡靈古堡：爆發夜》暗夜之路款 A3 海報',
    N'持有《惡靈古堡：爆發夜》的暗夜之路款 A3 海報，希望交換其他電影的 A3 海報特典，交換內容可再討論。',
    N'ACTIVE',
    ub.[use_benefit_id],
    CONVERT(DATETIME2, '2026-10-27 14:20:00'),
    CONVERT(DATETIME2, '2026-10-27 14:20:00')
FROM dbo.[use_benefit] ub
WHERE ub.[use_benefit_id] = 2;


-- 03. 我有《吉伊卡哇》4DX 限定 A3 主視覺海報
-- use_benefit_id = 3

INSERT INTO dbo.[exchange_listing]
    ([title], [description], [status], [use_benefit_id], [created_at], [updated_at])
SELECT
    N'我有《吉伊卡哇》4DX 限定 A3 主視覺海報',
    N'手上有《劇場版 吉伊卡哇 人魚島的秘密》的 4DX 限定 A3 主視覺海報，希望交換其他電影的限定海報或特殊版本特典，歡迎交流。',
    N'ACTIVE',
    ub.[use_benefit_id],
    CONVERT(DATETIME2, '2026-10-25 10:35:00'),
    CONVERT(DATETIME2, '2026-10-25 16:45:00')
FROM dbo.[use_benefit] ub
WHERE ub.[use_benefit_id] = 3;


-- 04. 交換限量 A3 電影海報
-- use_benefit_id = 4

INSERT INTO dbo.[exchange_listing]
    ([title], [description], [status], [use_benefit_id], [created_at], [updated_at])
SELECT
    N'交換 限量 A3 電影海報',
    N'因收藏需求改變，希望將《挖掘者》的限量 A3 電影海報交換成其他電影的特典海報，歡迎提出交換條件。',
    N'ACTIVE',
    ub.[use_benefit_id],
    CONVERT(DATETIME2, '2026-10-01 13:40:00'),
    CONVERT(DATETIME2, '2026-10-01 13:40:00')
FROM dbo.[use_benefit] ub
WHERE ub.[use_benefit_id] = 4;


-- 05. 有人要交換《誤闖遺忘島》A3 正式款海報特典嗎?
-- use_benefit_id = 5

INSERT INTO dbo.[exchange_listing]
    ([title], [description], [status], [use_benefit_id], [created_at], [updated_at])
SELECT
    N'有人要交換《誤闖遺忘島》A3 正式款海報特典嗎?',
    N'持有《誤闖遺忘島》的 A3 正式款海報特典，希望交換其他電影的正式款或限定款海報，歡迎有興趣的會員聯絡。',
    N'ACTIVE',
    ub.[use_benefit_id],
    CONVERT(DATETIME2, '2026-09-30 11:25:00'),
    CONVERT(DATETIME2, '2026-09-30 15:10:00')
FROM dbo.[use_benefit] ub
WHERE ub.[use_benefit_id] = 5;