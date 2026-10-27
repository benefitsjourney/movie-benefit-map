-- 庫存回報，5位會員共21筆，地圖會有綠、橘、紅三種顏色
-- 時間用相對現在，哪天建資料庫都算本週
-- 會員用帳號找，特典用名稱＋貼文網址＋開始日找，不寫死ID
-- status：1有庫存、2即將售罄、3已售罄（BenefitInventoryStatus）
-- 最近一筆在25分鐘前，超過回報的15分鐘冷卻時間，現場可以直接再回報

-- churro：挖掘者「限量 A3 電影海報」板橋大遠百威秀影城 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -360, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'churro'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'板橋大遠百威秀影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- hotdog：挖掘者「限量 A3 電影海報」台北信義威秀 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -300, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'hotdog'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台北信義威秀' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- cola：劇場版 吉伊卡哇 人魚島的秘密「4DX限定｜A3主視覺海報」台北信義威秀 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -280, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'cola'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台北信義威秀' AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'4DX限定｜A3主視覺海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02sWKjx4EJiAwy9qJqU2uYvEozesYLAfTxgKhvbTZHkG65z8GbLQHpYKWXtnPScszol' AND b.start_date = N'2026-10-07 00:00:00';

-- hotdog：挖掘者「限量 A3 電影海報」新店裕隆城威秀影城 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -240, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'hotdog'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'新店裕隆城威秀影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- churro：快打旋風「A3 海報特典」高雄夢時代秀泰影城 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -220, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'churro'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'高雄夢時代秀泰影城' AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'A3 海報特典' AND b.source_url = N'https://www.facebook.com/reel/2286333035478304/' AND b.start_date = N'2026-10-09 00:00:00';

-- nacho：挖掘者「限量 A3 電影海報」中和環球威秀影城 即將售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 2, DATEADD(MINUTE, -200, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'nacho'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'中和環球威秀影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- cola：挖掘者「限量 A3 電影海報」台中老虎城威秀 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -180, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'cola'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台中老虎城威秀' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- popcorn：挖掘者「限量 A3 電影海報」欣欣秀泰影城 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -150, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'popcorn'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'欣欣秀泰影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- hotdog：快打旋風「A3 海報特典」台中文心秀泰影城 即將售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 2, DATEADD(MINUTE, -130, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'hotdog'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台中文心秀泰影城' AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'A3 海報特典' AND b.source_url = N'https://www.facebook.com/reel/2286333035478304/' AND b.start_date = N'2026-10-09 00:00:00';

-- nacho：挖掘者「限量 A3 電影海報」大巨蛋秀泰影城 即將售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 2, DATEADD(MINUTE, -120, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'nacho'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'大巨蛋秀泰影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- popcorn：快打旋風「A3 海報特典」大巨蛋秀泰影城 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -110, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'popcorn'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'大巨蛋秀泰影城' AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'A3 海報特典' AND b.source_url = N'https://www.facebook.com/reel/2286333035478304/' AND b.start_date = N'2026-10-09 00:00:00';

-- popcorn：挖掘者「限量 A3 電影海報」台北京站威秀 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -95, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'popcorn'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台北京站威秀' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- nacho：挖掘者「限量 A3 電影海報」高雄夢時代秀泰影城 即將售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 2, DATEADD(MINUTE, -90, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'nacho'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'高雄夢時代秀泰影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- cola：挖掘者「限量 A3 電影海報」板橋大遠百威秀影城 已售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 3, DATEADD(MINUTE, -75, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'cola'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'板橋大遠百威秀影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- popcorn：劇場版 吉伊卡哇 人魚島的秘密「4DX限定｜A3主視覺海報」台北信義威秀 即將售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 2, DATEADD(MINUTE, -70, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'popcorn'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台北信義威秀' AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'4DX限定｜A3主視覺海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02sWKjx4EJiAwy9qJqU2uYvEozesYLAfTxgKhvbTZHkG65z8GbLQHpYKWXtnPScszol' AND b.start_date = N'2026-10-07 00:00:00';

-- cola：挖掘者「限量 A3 電影海報」台北西門威秀影城 已售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 3, DATEADD(MINUTE, -60, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'cola'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台北西門威秀影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- cola：快打旋風「A3 海報特典」欣欣秀泰影城 已售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 3, DATEADD(MINUTE, -50, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'cola'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'欣欣秀泰影城' AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'A3 海報特典' AND b.source_url = N'https://www.facebook.com/reel/2286333035478304/' AND b.start_date = N'2026-10-09 00:00:00';

-- nacho：快打旋風「A3 海報特典」台中文心秀泰影城 即將售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 2, DATEADD(MINUTE, -45, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'nacho'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台中文心秀泰影城' AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'A3 海報特典' AND b.source_url = N'https://www.facebook.com/reel/2286333035478304/' AND b.start_date = N'2026-10-09 00:00:00';

-- cola：挖掘者「限量 A3 電影海報」台北信義威秀 即將售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 2, DATEADD(MINUTE, -40, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'cola'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'台北信義威秀' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- cola：挖掘者「限量 A3 電影海報」新店裕隆城威秀影城 即將售罄
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 2, DATEADD(MINUTE, -30, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'cola'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'新店裕隆城威秀影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- popcorn：挖掘者「限量 A3 電影海報」中和環球威秀影城 有庫存
INSERT INTO dbo.inventory_report (user_id, cinema_movie_benefit_id, status, reported_at)
SELECT u.users_id, cmb.cinema_movie_benefit_id, 1, DATEADD(MINUTE, -25, SYSDATETIME())
FROM dbo.[users] u, dbo.cinema_movie_benefit cmb, dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE u.account = N'popcorn'
  AND cmb.cinema_id = c.cinema_id AND cmb.movie_id = m.movie_id AND cmb.benefit_id = b.benefit_id
  AND c.cinema_name = N'中和環球威秀影城' AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';

-- 地圖顏色看中介表的狀態，改成每間影城最新一筆回報的狀態（跟InventoryReportService的做法一樣）
UPDATE dbo.cinema_movie_benefit
SET benefit_inventory_status = (
        SELECT TOP (1) r.status FROM dbo.inventory_report r
        WHERE r.cinema_movie_benefit_id = cinema_movie_benefit.cinema_movie_benefit_id
        ORDER BY r.reported_at DESC),
    updated_at = SYSDATETIME()
WHERE EXISTS (
        SELECT 1 FROM dbo.inventory_report r
        WHERE r.cinema_movie_benefit_id = cinema_movie_benefit.cinema_movie_benefit_id);
