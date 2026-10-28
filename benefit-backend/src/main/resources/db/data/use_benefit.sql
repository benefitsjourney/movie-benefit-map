-- 收藏庫，爆米花收藏7款，最後一款是已結束發放的
-- 時間用相對現在，哪天建資料庫都算本週
-- 會員用帳號找，特典用名稱＋貼文網址＋開始日找，不寫死ID
INSERT INTO dbo.use_benefit (user_id, benefit_id, created_at, updated_at)
SELECT u.users_id, b.benefit_id, DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, -1, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'A3 海報特典' AND b.source_url = N'https://www.facebook.com/reel/2286333035478304/' AND b.start_date = N'2026-10-09 00:00:00';
INSERT INTO dbo.use_benefit (user_id, benefit_id, created_at, updated_at)
SELECT u.users_id, b.benefit_id, DATEADD(DAY, -2, SYSDATETIME()), DATEADD(DAY, -2, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'暗夜之路款 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02kwWaimJnXECU1zU5M7JrNehCaUbVKMGvNcG2RGjrijqkcRkcobPdMKE7egNBXSWpl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.use_benefit (user_id, benefit_id, created_at, updated_at)
SELECT u.users_id, b.benefit_id, DATEADD(DAY, -3, SYSDATETIME()), DATEADD(DAY, -3, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'4DX限定｜A3主視覺海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02sWKjx4EJiAwy9qJqU2uYvEozesYLAfTxgKhvbTZHkG65z8GbLQHpYKWXtnPScszol' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.use_benefit (user_id, benefit_id, created_at, updated_at)
SELECT u.users_id, b.benefit_id, DATEADD(DAY, -4, SYSDATETIME()), DATEADD(DAY, -4, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';
INSERT INTO dbo.use_benefit (user_id, benefit_id, created_at, updated_at)
SELECT u.users_id, b.benefit_id, DATEADD(DAY, -6, SYSDATETIME()), DATEADD(DAY, -6, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'A3 正式款海報特典' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02ZkasXkK3zTRmak8ojE21gH7qFhkUtf4KxnfK4kpkEdnvxAru9Eiqk9DjeiYp6QvPl' AND b.start_date = N'2026-09-18 00:00:00';
INSERT INTO dbo.use_benefit (user_id, benefit_id, created_at, updated_at)
SELECT u.users_id, b.benefit_id, DATEADD(DAY, -8, SYSDATETIME()), DATEADD(DAY, -8, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'電影造型貼紙' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0261v7LZwzPiAY1T1m8PR6e5J4RTWeb8W7zTQxxpydGqp1A9ByGiTGRznTbSDVwM5Ql' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.use_benefit (user_id, benefit_id, created_at, updated_at)
SELECT u.users_id, b.benefit_id, DATEADD(DAY, -10, SYSDATETIME()), DATEADD(DAY, -10, SYSDATETIME())
FROM dbo.[users] u, dbo.benefit b
WHERE u.account = N'popcorn' AND b.benefit_name = N'破曉前行款 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02nGHWqEtMAgxBFyQwAG7pUcDVh8qgs48mehVmM8rBhTmTdzgqBciAj5HAK6xyoVuKl' AND b.start_date = N'2026-10-16 00:00:00';
