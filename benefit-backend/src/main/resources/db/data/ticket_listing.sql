-- ticket_listing.sql
-- 必須先有 users、cinema、movie 資料，再執行此檔案。
-- 使用指定的 users_id、cinema_id、movie_id 建立 25 筆票券刊登。
-- 每筆資料使用獨立的 INSERT INTO ... SELECT 指令。
-- ticketShowtime、created_at、updated_at 均使用指定的 LocalDateTime。
-- status 全部為 ACTIVE。

-- 1. 爆米花建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《不存在的罪人》電影票',
    N'手上有一張《不存在的罪人》電影票，希望交換其他電影的票券，歡迎提出想交換的片名。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-12 14:30:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-06 09:15:00'),
    CONVERT(DATETIME2, '2026-10-06 09:15:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 1
  AND c.[cinema_id] = 1
  AND m.[movie_id] = 1;

-- 2. 可樂建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換角落小夥伴電影票',
    N'想交換《角落小夥伴電影版：從天而降的雲之冒險》票券，其他動畫電影也可以討論。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-13 16:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-06 10:20:00'),
    CONVERT(DATETIME2, '2026-10-06 10:20:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 2
  AND c.[cinema_id] = 25
  AND m.[movie_id] = 3;

-- 3. 玉米片建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《鬼入室》電影票',
    N'因行程變更，希望將《鬼入室》電影票換成其他電影票券，歡迎私訊討論。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-14 19:30:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-06 11:35:00'),
    CONVERT(DATETIME2, '2026-10-06 12:00:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 3
  AND c.[cinema_id] = 16
  AND m.[movie_id] = 5;

-- 4. 熱狗堡建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《鋼之鍊金術師》電影票',
    N'想交換《鋼之鍊金術師：嘆息之丘的聖星》電影票，歡迎喜歡動畫電影的朋友聯絡。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-15 13:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-06 13:40:00'),
    CONVERT(DATETIME2, '2026-10-06 13:40:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 4
  AND c.[cinema_id] = 51
  AND m.[movie_id] = 7;

-- 5. 吉拿棒建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《驚天劫機》電影票',
    N'手上有《驚天劫機》電影票，希望交換其他動作或懸疑類電影票券。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-16 18:45:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-06 14:25:00'),
    CONVERT(DATETIME2, '2026-10-06 14:25:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 5
  AND c.[cinema_id] = 54
  AND m.[movie_id] = 8;

-- 6. 爆米花建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換 YORUSHIKA LIVE 電影票',
    N'想交換 YORUSHIKA LIVE「盜作」現場演出電影票，歡迎提出其他演唱會電影票券。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-17 20:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-06 15:30:00'),
    CONVERT(DATETIME2, '2026-10-06 16:10:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 1
  AND c.[cinema_id] = 2
  AND m.[movie_id] = 13;

-- 7. 可樂建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《神鬼劫案2：偷天換日》票券',
    N'因觀看計畫改變，希望交換《神鬼劫案2：偷天換日》電影票，場次可再討論。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-18 15:15:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-06 16:45:00'),
    CONVERT(DATETIME2, '2026-10-06 16:45:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 2
  AND c.[cinema_id] = 44
  AND m.[movie_id] = 16;

-- 8. 玉米片建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《鬼6咒》電影票',
    N'想交換《鬼6咒》恐怖電影票券，有其他恐怖片或懸疑片票券也歡迎交流。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-19 19:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-06 17:20:00'),
    CONVERT(DATETIME2, '2026-10-06 17:20:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 3
  AND c.[cinema_id] = 67
  AND m.[movie_id] = 19;

-- 9. 熱狗堡建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《復活男》電影票',
    N'手上有《復活男》電影票，希望交換其他電影的票券，歡迎私訊確認交換條件。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-20 14:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 09:10:00'),
    CONVERT(DATETIME2, '2026-10-07 09:10:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 4
  AND c.[cinema_id] = 75
  AND m.[movie_id] = 24;

-- 10. 吉拿棒建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《誤闖遺忘島》電影票',
    N'想將《誤闖遺忘島》電影票交換成其他冒險類電影票券，歡迎提出需求。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-21 16:30:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 09:55:00'),
    CONVERT(DATETIME2, '2026-10-07 10:30:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 5
  AND c.[cinema_id] = 3
  AND m.[movie_id] = 26;

-- 11. 爆米花建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《猛鬼大學：4不瞑目》票券',
    N'因時間安排改變，希望交換《猛鬼大學：4不瞑目》電影票，其他恐怖片也可以討論。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-22 20:15:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 10:40:00'),
    CONVERT(DATETIME2, '2026-10-07 10:40:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 1
  AND c.[cinema_id] = 21
  AND m.[movie_id] = 31;

-- 12. 可樂建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《惡靈古堡：爆發夜》電影票',
    N'想交換《惡靈古堡：爆發夜》電影票，歡迎喜歡恐怖或動作電影的朋友聯絡。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-23 18:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 11:25:00'),
    CONVERT(DATETIME2, '2026-10-07 11:25:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 2
  AND c.[cinema_id] = 60
  AND m.[movie_id] = 35;

-- 13. 玉米片建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《驀然回首》電影票',
    N'手上有《驀然回首》電影票，希望交換其他動畫或劇情類電影票券。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-24 13:30:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 12:15:00'),
    CONVERT(DATETIME2, '2026-10-07 12:45:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 3
  AND c.[cinema_id] = 6
  AND m.[movie_id] = 43;

-- 14. 熱狗堡建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《魔法少女小圓》電影票',
    N'想交換《劇場版 魔法少女小圓〈瓦爾普吉斯的迴天〉》電影票，歡迎動畫電影愛好者聯絡。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-25 17:45:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 13:20:00'),
    CONVERT(DATETIME2, '2026-10-07 13:20:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 4
  AND c.[cinema_id] = 13
  AND m.[movie_id] = 48;

-- 15. 吉拿棒建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《粽邪4：坤蒂拉娜》電影票',
    N'因個人行程變動，希望交換《粽邪4：坤蒂拉娜》電影票，其他恐怖片票券也可以討論。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-26 19:15:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 14:05:00'),
    CONVERT(DATETIME2, '2026-10-07 14:05:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 5
  AND c.[cinema_id] = 10
  AND m.[movie_id] = 52;

-- 16. 爆米花建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《藍色監獄》電影票',
    N'想交換《藍色監獄》電影票，歡迎提出其他動畫或運動題材電影的交換需求。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-27 15:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 15:10:00'),
    CONVERT(DATETIME2, '2026-10-07 15:40:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 1
  AND c.[cinema_id] = 50
  AND m.[movie_id] = 59;

-- 17. 可樂建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《蜘蛛人：重生日》電影票',
    N'手上有《蜘蛛人：重生日》電影票，希望交換其他超級英雄或動作電影票券。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-28 20:30:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 16:25:00'),
    CONVERT(DATETIME2, '2026-10-07 16:25:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 2
  AND c.[cinema_id] = 19
  AND m.[movie_id] = 62;

-- 18. 玉米片建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《吉伊卡哇》電影票',
    N'想交換《劇場版 吉伊卡哇 人魚島的秘密》電影票，其他動畫電影票券也歡迎提出。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-29 14:45:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-07 17:00:00'),
    CONVERT(DATETIME2, '2026-10-07 17:00:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 3
  AND c.[cinema_id] = 31
  AND m.[movie_id] = 63;

-- 19. 熱狗堡建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《名偵探柯南》電影票',
    N'因觀影時間改變，希望交換《名偵探柯南 高速公路的墮天使》電影票，歡迎私訊討論。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-11-30 18:30:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-08 09:05:00'),
    CONVERT(DATETIME2, '2026-10-08 09:05:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 4
  AND c.[cinema_id] = 35
  AND m.[movie_id] = 66;

-- 20. 吉拿棒建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《復仇者聯盟：終局之戰》電影票',
    N'想交換《復仇者聯盟：終局之戰》電影票，歡迎提出其他科幻或英雄電影票券。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-12-01 19:45:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-08 09:50:00'),
    CONVERT(DATETIME2, '2026-10-08 10:15:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 5
  AND c.[cinema_id] = 68
  AND m.[movie_id] = 70;

-- 21. 爆米花建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《海洋奇緣》電影票',
    N'手上有《海洋奇緣》真人版電影票，希望交換其他動畫或奇幻類電影票券。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-12-02 13:15:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-08 10:35:00'),
    CONVERT(DATETIME2, '2026-10-08 10:35:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 1
  AND c.[cinema_id] = 40
  AND m.[movie_id] = 75;

-- 22. 可樂建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《揭密日》電影票',
    N'希望將《揭密日》電影票交換成其他電影票券，片種與交換條件可再協調。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-12-03 16:15:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-08 11:20:00'),
    CONVERT(DATETIME2, '2026-10-08 11:20:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 2
  AND c.[cinema_id] = 22
  AND m.[movie_id] = 81;

-- 23. 玉米片建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《極限返航》電影票',
    N'想交換《極限返航》電影票，歡迎喜歡科幻或冒險題材的朋友聯絡。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-12-04 20:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-08 12:10:00'),
    CONVERT(DATETIME2, '2026-10-08 12:40:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 3
  AND c.[cinema_id] = 79
  AND m.[movie_id] = 86;

-- 24. 熱狗堡建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《刺殺小說家2》電影票',
    N'因行程安排改變，希望交換《刺殺小說家2》電影票，其他奇幻或動作電影也可以討論。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-12-05 15:30:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-08 13:00:00'),
    CONVERT(DATETIME2, '2026-10-08 13:00:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 4
  AND c.[cinema_id] = 41
  AND m.[movie_id] = 95;

-- 25. 吉拿棒建立票券交換刊登（ACTIVE）
INSERT INTO dbo.[ticket_listing]
    ([title], [description], [users_id], [cinema_id], [movie_id],
     [ticket_showtime], [status], [created_at], [updated_at])
SELECT
    N'交換《名偵探柯南 第30號殺人事件》電影票',
    N'手上有《名偵探柯南 第30號殺人事件》電影票，希望交換其他電影票券，歡迎提出想交換的片名。',
    u.[users_id], c.[cinema_id], m.[movie_id],
    CONVERT(DATETIME2, '2026-12-06 19:00:00'),
    N'ACTIVE',
    CONVERT(DATETIME2, '2026-10-08 14:15:00'),
    CONVERT(DATETIME2, '2026-10-08 14:15:00')
FROM dbo.[users] u
CROSS JOIN dbo.[cinema] c
CROSS JOIN dbo.[movie] m
WHERE u.[users_id] = 5
  AND c.[cinema_id] = 12
  AND m.[movie_id] = 123;