-- 追蹤，爆米花追蹤2部11/6上映的電影和2個特典，行事曆「我的日曆」才有東西
-- 時間用相對現在，哪天建資料庫都算本週
-- 會員用帳號找，特典用名稱＋貼文網址＋開始日找，不寫死ID
INSERT INTO dbo.reminder (reminder_type, user_id, movie_id, created_at)
SELECT N'MOVIE', u.users_id, m.movie_id, DATEADD(DAY, -2, SYSDATETIME())
FROM dbo.[users] u, dbo.movie m
WHERE u.account = N'popcorn' AND m.movie_name = N'哥吉拉-0.0';
INSERT INTO dbo.reminder (reminder_type, user_id, movie_id, created_at)
SELECT N'MOVIE', u.users_id, m.movie_id, DATEADD(DAY, -2, SYSDATETIME())
FROM dbo.[users] u, dbo.movie m
WHERE u.account = N'popcorn' AND m.movie_name = N'名偵探柯南 第30號殺人事件';
INSERT INTO dbo.reminder (reminder_type, user_id, benefit_id, created_at)
SELECT N'BENEFIT', u.users_id, b.benefit_id, DATEADD(DAY, -2, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'典藏票卡（款式一）' AND b.source_url = N'https://www.facebook.com/share/p/19yKWD43NE/' AND b.start_date = N'2026-11-06 00:00:00';
INSERT INTO dbo.reminder (reminder_type, user_id, benefit_id, created_at)
SELECT N'BENEFIT', u.users_id, b.benefit_id, DATEADD(DAY, -2, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'A3海報' AND b.source_url = N'https://www.facebook.com/share/p/19T6egq6ZZ/' AND b.start_date = N'2026-10-30 00:00:00';
