-- 由資料庫已確認的特典自動產生：中介表
-- 需要cinema、movie、benefit表已有資料，找不到電影或影城時，那幾行不會寫入任何資料
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'哥吉拉-1.0'
  AND b.benefit_name = N'A3 海報特典兌換券（2D）' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02AZE1Kr6KZoxHy1dUA2kmsVSyG8GigatdqQBDsqHLWkhxdXXXFDT6XPLE5vTAh81Pl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'哥吉拉-1.0'
  AND b.benefit_name = N'A3 海報特典兌換券（SCREENX）' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02AZE1Kr6KZoxHy1dUA2kmsVSyG8GigatdqQBDsqHLWkhxdXXXFDT6XPLE5vTAh81Pl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'哥吉拉-1.0'
  AND b.benefit_name = N'A3 海報特典兌換券（ULTRA 4DX）' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02AZE1Kr6KZoxHy1dUA2kmsVSyG8GigatdqQBDsqHLWkhxdXXXFDT6XPLE5vTAh81Pl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中站前秀泰影城')
  AND m.movie_name = N'哥吉拉-1.0'
  AND b.benefit_name = N'A3 海報特典兌換券（Dolby Vision+Atmos）' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02AZE1Kr6KZoxHy1dUA2kmsVSyG8GigatdqQBDsqHLWkhxdXXXFDT6XPLE5vTAh81Pl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'泥面人'
  AND b.benefit_name = N'SCREENX 透卡' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02jtTmjUQmZkwvpv4mnkvVSxgxWHUQ2FQ8gHY8QqUg5PpyjBxyi5uZQS9s87mCLzw7l' AND b.start_date = N'2026-10-22 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園站前in89豪華影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中豐原in89豪華影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'in89駁二電影院', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'大立in89豪華影城', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'嘉義in89豪華影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城', N'in89澎湖影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'原作者 nagano 全新繪製透卡' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02eigRw9fRC61TrwpdUrT5BcaPT3cNFxjqAGKz3V6n4viUex6dV4m2sy9eeZR4sd2al' AND b.start_date = N'2026-10-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'大巨蛋秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'新店裕隆城威秀影城', N'台中老虎城威秀', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'沙丘：第三部'
  AND b.benefit_name = N'沙丘凝視 A3 特典海報' AND b.source_url = N'https://www.facebook.com/reel/1133893392657716/' AND b.start_date = N'2026-10-20 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'逃出鯨吞'
  AND b.benefit_name = N'特別場套組：4DX「屏息倒數」A3 海報、【獨家】「墜入鯨口」A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid07PRdXymnYwpujTWZJdymMKduNZmoabxbn2m4X1JwviEtpg4tyJsx13Giyy1M7UsBl' AND b.start_date = N'2026-10-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園站前in89豪華影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中豐原in89豪華影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'in89駁二電影院', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'大立in89豪華影城', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'嘉義in89豪華影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城', N'in89澎湖影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'摺頁劇照' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0HCC5BUS3VvWaSb2mveVQ4NQ6ap2Bw42kVK7gbvTFU4aowyi8k27UrWbv9R2mANd9l' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'國賓戲院', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'國賓長春影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'國賓林口昕境廣場', N'國賓新莊晶冠廣場影城', N'樹林秀泰影城', N'土城秀泰影城', N'國賓影城淡水禮萊廣場', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'國賓桃園八德', N'桃園統領威秀影城', N'桃園站前in89豪華影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中豐原in89豪華影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'in89駁二電影院', N'國賓義大世界影城', N'高雄夢時代秀泰影城', N'國賓大魯閣草衙道影城', N'高雄岡山秀泰', N'大立in89豪華影城', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'嘉義in89豪華影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城', N'國賓屏東環球影城', N'in89澎湖影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'電玩概念款 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02YbCeuU11M1kExkXYXK7pXUmbXYPYnETMtBdYdwHKKbs4AoQps9vjeoXprfkeaURhl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中站前秀泰影城')
  AND m.movie_name = N'進擊的鼓手'
  AND b.benefit_name = N'特別場套組：Dolby Vision+Atmos 海報、奮力版原文海報、【獨家】主視覺海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid024kK4orNhPRfDLTP4YUpsYWYLPPb8qN1qHCBbZYfWH97jwR5gow3uTrV9WQJJ7A4Zl' AND b.start_date = N'2026-10-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas'
  AND b.benefit_name = N'獨照小卡（限特殊版本）' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0kusetBXRsTduqM4Y7PE71AgwcfCgEZLCxSvtcETZSFsLSDBVT5K6us5USpfeP3Wfl' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas'
  AND b.benefit_name = N'團體小卡' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0kusetBXRsTduqM4Y7PE71AgwcfCgEZLCxSvtcETZSFsLSDBVT5K6us5USpfeP3Wfl' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'不存在的罪人'
  AND b.benefit_name = N'特別場套組：Dolby Vision+Atmos 燙紅海報、三角謎題 A3 海報、【獨家】危險情劫燙金 A3 海報、【獨家】赤色欲望局部雷射 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02FLqWR2dRUvXWtqhc2WZX9zTszaM9Fa8mpbxkDjzXh3ETfKt1Xp2S4xNkeukbUqKGl' AND b.start_date = N'2026-10-21 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城')
  AND m.movie_name = N'哥吉拉-0.0'
  AND b.benefit_name = N'特別場套組：首映限定特典、特殊工藝日本原版 A3 海報、哥吉拉游泳爆桶' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02jRbZZckDBpdCUfskrLKMjbPwjkqv6hrVbLdvy4nFinvh2PQdQ5Zq3H4H3ec2C8gsl' AND b.start_date = N'2026-11-05 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'泥面人'
  AND b.benefit_name = N'特別場套組：4DX 海報、【獨家】SCREENX 海報、【特別場限定】面對恐懼款 A3 海報、【特別場限定】原文漫畫' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0MSnfZ6d9m1vhpD4pACpyT4WT6o7sbQW53cKwku9PoEhnjfjZBgxxQqUbJHhWY2Srl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城')
  AND m.movie_name = N'哥吉拉-0.0'
  AND b.benefit_name = N'特別場套組：特殊工藝日本原版 A3 海報、首映限定特典' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02aiwHd67fBt88TyeMsF5WKTsRHofU74bVpm8TBAsiTBUvec9HM2PvwFdyJrNSNAXkl' AND b.start_date = N'2026-11-05 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'迷你公仔吊飾' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0hJ5f5d3GdW8F1tWyRQJdwMG5v52fRgSfunJ8v2ZCwVy2rEaHnFReZSickYpKo4qYl' AND b.start_date = N'2026-10-17 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：【獨家】《號角日報》光柵海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid033L5LenX944jnxvenXFfdMKGD7fm2qV4PebhuDg4po2eXZSdyZeRBqUqncqbSvLiEl' AND b.start_date = N'2026-10-19 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'破曉前行款 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02nGHWqEtMAgxBFyQwAG7pUcDVh8qgs48mehVmM8rBhTmTdzgqBciAj5HAK6xyoVuKl' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'進擊的鼓手'
  AND b.benefit_name = N'特別場套組：影評版原文海報、Dolby Vision+Atmos 海報、【獨家】主視覺海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0uCHjqic1mQKKzk26EeAHTim2FdscjeGjh3cPYZKtJKt34ESNEYs4kUptgFaPgKjSl' AND b.start_date = N'2026-10-17 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'厄夜之鼠款 A3 夜光海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid036sHGwbffTW6A2NjKizoFUNRiiXbwqfsxEumpVBR3Gycnkh9F9mkn5ogfb56Ca8yAl' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'巴西駐台漫畫家盧卡斯獨步全球親自設計・電影主題手繪款特典-秀泰影城獨家款（限特殊版本）' AND b.source_url = N'https://www.facebook.com/reel/2286333035478304/' AND b.start_date = N'2026-10-09 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'A3 海報特典' AND b.source_url = N'https://www.facebook.com/reel/2286333035478304/' AND b.start_date = N'2026-10-09 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中站前秀泰影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'特別場套組：隆 杜比版・亮銀龍海報、肯 杜比版・杜比海報、【獨家】隆、肯、春麗角色海報組(全套 3 款)' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0KCAyihxhRpqNwGJer3rYp2NErnMrBamr7kcrFgMxpHqqWRsx37Q1kBxfbLQBg71Fl' AND b.start_date = N'2026-10-21 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'特別場套組：4DX 海報、巴西駐台漫畫家盧卡斯設計手繪款海報、【獨家】隆、肯、春麗角色海報組(全套 3 款)' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid08k4Gb8pcpJoT2vUprvTxcBKYa47F8zi5r9YSBxXeDDRT6GkQbU1cJpEp121Vwb4ql' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'特別場套組：【獨家】SCREENX 海報、巴西駐台漫畫家盧卡斯設計手繪款海報、【獨家】隆、肯、春麗角色海報組(全套 3 款)' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid08k4Gb8pcpJoT2vUprvTxcBKYa47F8zi5r9YSBxXeDDRT6GkQbU1cJpEp121Vwb4ql' AND b.start_date = N'2026-10-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'另一個媽咪'
  AND b.benefit_name = N'特別場套組：DVA 透片海報 A3、【獨家】致命母愛款透片海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02Y81Sb158BhVtvmd1hrRcCbwBaeTUTyb35hNHMCjttFqXs1gV73VCw4niEUdJ8f9zl' AND b.start_date = N'2026-10-14 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台中站前秀泰影城', N'高雄夢時代秀泰影城')
  AND m.movie_name = N'高年級實習生'
  AND b.benefit_name = N'特別場套組：SCREENX 海報、【獨家】基浩/崔岷植海報、【獨家】宣羽/韓韶禧海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid022tCDqDBpJNaWLzvmRjK5hfsjyP5DmtxZE4yc5UA6eX8s3bGiHpDHWNCDGYy8HbJql' AND b.start_date = N'2026-10-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'高年級實習生'
  AND b.benefit_name = N'特別場套組：【獨家】基浩/崔岷植海報、【獨家】宣羽/韓韶禧海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid022tCDqDBpJNaWLzvmRjK5hfsjyP5DmtxZE4yc5UA6eX8s3bGiHpDHWNCDGYy8HbJql' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'迷你公仔吊飾' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0mHH8Ufp9zNaE6FBeckXno5xeFE67R3ZQWvgP44QY8S8p26yWjBhrS4JNwmA3tFMgl' AND b.start_date = N'2026-10-10 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas'
  AND b.benefit_name = N'2D 版海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02RiFsX7xJ1p3ie3M6TQk2aGnueUd9atLvtircgPfsrCqeV3NXeM5LgBaxmT8iHpKjl' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas'
  AND b.benefit_name = N'SCREENX 版海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02RiFsX7xJ1p3ie3M6TQk2aGnueUd9atLvtircgPfsrCqeV3NXeM5LgBaxmT8iHpKjl' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'SB19 Wakas at Simula: The Trilogy Concert Finale In Cinemas'
  AND b.benefit_name = N'4DX 版海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02RiFsX7xJ1p3ie3M6TQk2aGnueUd9atLvtircgPfsrCqeV3NXeM5LgBaxmT8iHpKjl' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'我是我 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02e3UTWPiKubHTGC5eioDtL7ZZd4CajzUJvZWFwq5JUbShSXxfZuQKNpTTE8DdFBRKl' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'紅藍鏡片組' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02e3UTWPiKubHTGC5eioDtL7ZZd4CajzUJvZWFwq5JUbShSXxfZuQKNpTTE8DdFBRKl' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'Linkin Park: Unshatter'
  AND b.benefit_name = N'收藏紀念票卡' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02QQFH64f9sDmXNGkUh7mBeDkXMdRvEJooeV1UePP1gMd8MUVkNuATFgLiabh6F4oRl' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'小心惡犬款 A3 夜光海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0qKrxjzz48MQrfS7xxSGxP5VCug9pLfHbpTQqaG2izCWMSWhyvDMweCS54jviA1Bel' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'不存在的罪人'
  AND b.benefit_name = N'特別場套組：【獨家】「禁色真相」A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02WFtd3bXLwJxm3ZuhP6UfHfLLXYr8Kbo2M3qMMiuRD7Pd3BuCtQb5U8dwoAJG4tXAl' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'暗夜之路款 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02kwWaimJnXECU1zU5M7JrNehCaUbVKMGvNcG2RGjrijqkcRkcobPdMKE7egNBXSWpl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'劇場版 魔法少女小圓〈瓦爾普吉斯的迴天〉'
  AND b.benefit_name = N'特別場套組：小圓 A3 海報、台灣限定圖素 A3 海報、小圓 A5 圖卡' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0Car1WaUrh3RjZZd24nubZnPGF6t2n9LP4AAcEfGkZaPoqwUWufTo9buQ47WUq3jel' AND b.start_date = N'2026-10-03 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'大巨蛋秀泰影城', N'台北京站威秀', N'欣欣秀泰影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'新店裕隆城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'頭份尚順威秀影城', N'嘉義秀泰影城')
  AND m.movie_name = N'復仇者聯盟：末日崛起'
  AND b.benefit_name = N'INFINITY VISION D23 專屬特典' AND b.source_url = N'https://www.facebook.com/reel/1993785067979404/' AND b.start_date = N'2026-09-28 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'復仇者聯盟：末日崛起'
  AND b.benefit_name = N'末日登場特典（限INFINITY VISION版本）' AND b.source_url = N'https://www.facebook.com/reel/1993785067979404/' AND b.start_date = N'2026-10-12 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'復仇者聯盟：末日崛起'
  AND b.benefit_name = N'末日崛起 復仇者標誌特典' AND b.source_url = N'https://www.facebook.com/reel/1993785067979404/' AND b.start_date = N'2026-10-12 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'日本進口 自動充氣氣球' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid06swRcVdjjSR6Z2egq6ZY58Jehf2xhTrgAoMfw3DYSkNvQmMuDkZVTxbmuCnHojnpl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'欣欣秀泰影城', N'樹林秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城')
  AND m.movie_name = N'實境誘捕'
  AND b.benefit_name = N'特別場套組：【獨家】A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0opAcMFw2eZxtT64wwT1AXB9VXVZ1W4JkQWnxQ2NJGibVn9Rp4EiRf1u63rM9QyaVl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'百花繚亂款 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02V835n9n9HsTr4V7TXpDUDXx8oZJXXT2MUk3VAAjvdLLycGRdU3sKgy8AzUyLPTYGl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'Linkin Park: Unshatter'
  AND b.benefit_name = N'A3 主視覺海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02QeE461GJhTTH7NnGMB32bgzzwr5abKBL8eDc2bttb6MCjb1dEyyfDSMwrVMK6hFml' AND b.start_date = N'2026-09-30 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'實境誘捕'
  AND b.benefit_name = N'A24 電影紀念票' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid034XeDVtHaRV5Pg7McEiujKZQpctWWvdiGDpAkwrfYmaHY81GQsVpH3vvaouDujcAJl' AND b.start_date = N'2026-09-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'4DX 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02okEYXyW5dkB1ssoqcRp6KfEizV2aknsx5tkNe4bGtpdaasEAGXzzyqMcrYUmPHnAl' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'日本進口 秘密豪華冊子' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02vgTKyAYvyniBWGCKoEUDQfneQova9nG7Aqy2jYtMDVRCdWwurKT7BrqSp1NwZvoBl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'光影入戲款 A3 海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02KkcAwgDbiyhdp86FzgbY9RPvbdxuNxWzHsBPSpDauShWGHrDqj4g9Jm879WrJ3sZl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'高雄夢時代秀泰影城', N'嘉義秀泰影城')
  AND m.movie_name = N'航海王喬巴身世之謎'
  AND b.benefit_name = N'特別場套組：《航海王劇場版:喬巴身世之謎~冬季綻放、奇跡的櫻花》A3 海報、影迷必收限定片名徽章' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0uULqgGunQ1KMxCrXCgpFQZjGZjQM1iik6xA6W9gcJrGLn983mPpbu6i22FiL5WMxl' AND b.start_date = N'2026-09-27 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'限量 A3 電影海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0382SoNEq4QVFjKX92QQYEFDKxfj6ttrvTFCaCnQ4oLci7UTvc71opPN3rtokFRLdNl' AND b.start_date = N'2026-10-01 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'誤闖遺忘島'
  AND b.benefit_name = N'A3 正式款海報特典' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02ZkasXkK3zTRmak8ojE21gH7qFhkUtf4KxnfK4kpkEdnvxAru9Eiqk9DjeiYp6QvPl' AND b.start_date = N'2026-09-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'驀然回首'
  AND b.benefit_name = N'主視覺冬日款電影工藝海報 A3' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02sLiUsdek3yHT52cMhhHDz3GDSeTvcLA4gmjugg8H6L6XbLBBkY6PWHc18eNFFUdHl' AND b.start_date = N'2026-09-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'欣欣秀泰影城', N'台中站前秀泰影城', N'高雄夢時代秀泰影城')
  AND m.movie_name = N'夢想出擊'
  AND b.benefit_name = N'特別場套組：【獨家】夢的守備款海報、【獨家】宇鎮/Jeno 款海報、【獨家】泰熙/Jaemin 款海報、【獨家】青春並肩款海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02qYK2M7mJNKwNzKEjofcWEGKE3T48fB2dmSftXp3Q2XcPqNtytg976ymmJeCAVs8Gl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城')
  AND m.movie_name = N'藍色監獄'
  AND b.benefit_name = N'特別場套組：「才能的原石收藏版海報」A3(全套 2 款)、【獨家】天才 × 怪物頂尖對決海報 A3、【獨家工藝】雙面手幅(42 × 21.4 cm)' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02Fvao8tUtBq6R7b8epTNm97APuG3tTo8XiRRQomAAkfjQAAHeHASEuH3tE2rprtMGl' AND b.start_date = N'2026-09-21 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'特別場套組：4DX海報、SCREENX海報、「暗夜之路款」海報A3、【特別場限定】「EMERGENCYCALL款」海報A3、【獨家】運送包海報A3、【獨家】夜光海報A3、【獨家．空運】惡靈古堡運送包' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02P6nMa28tN5JV6u4PfL1SNHDdoporNoZ2XkN7akS2vNzctWxtCQP2xtWdtLgf2FnAl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'樹林秀泰影城', N'台中站前秀泰影城', N'高雄岡山秀泰')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'特別場套組：SCREENX 海報、「暗夜之路款」海報 A3、【特別場限定】「EMERGENCY CALL款」海報 A3、【獨家】運送包海報 A3、【獨家】夜光海報 A3' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02P6nMa28tN5JV6u4PfL1SNHDdoporNoZ2XkN7akS2vNzctWxtCQP2xtWdtLgf2FnAl' AND b.start_date = N'2026-09-27 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中站前秀泰影城')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'特別場套組：Dolby Vision+Atmos 海報、「暗夜之路款」海報 A3、【特別場限定】「EMERGENCY CALL款」海報 A3、【獨家】運送包海報 A3、【獨家】夜光海報 A3' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02P6nMa28tN5JV6u4PfL1SNHDdoporNoZ2XkN7akS2vNzctWxtCQP2xtWdtLgf2FnAl' AND b.start_date = N'2026-09-27 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'特別場套組：造型紀念卡、A4 資料夾' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid029UkrMvPEYGX7uhcK8FEGGw461rRG1PC9uPPAXPYmGf7RVW6Kwb6q5Du7hNT6oE35l' AND b.start_date = N'2026-09-19 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'林口三井OUTLET威秀影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'泥面人'
  AND b.benefit_name = N'特別場套組：面對恐懼A3工藝海報、原文漫畫、4DX限定A3海報、主視覺A3海報' AND b.source_url = N'https://www.facebook.com/reel/1505818601359701/' AND b.start_date = N'2026-10-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'林口三井OUTLET威秀影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'遊戲人生ZERO'
  AND b.benefit_name = N'特別場套組：【獨家】《遊戲人生 ZERO》主視覺A3海報、【獨家】人物設定A5場刊' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02RJT24wRRdLZSwExSsEwRGKyojcKVCFEB5rP8Fwhog626oC9N9fttt3BqkauV6bXXl' AND b.start_date = N'2026-11-06 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'YORUSHIKA LIVE 「盜作」'
  AND b.benefit_name = N'電影主視覺軟膠杯墊' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02eDWJVUpu9jfBue8Dw9obeSqjubkKmAuNSSkGcq2JwV2EJ35USZQmaKN17Fb3aYGl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'劇場版 魔法少女小圓〈瓦爾普吉斯的迴天〉'
  AND b.benefit_name = N'角色色紙風圖卡' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid022mXp3cWTpznzxY5obn4DJE1boXBA17G5YHWgGacHuf1iZLKjZhQ4zAQDJHahWU2Dl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'劇場版 BANG DREAM! AVE MUJICA PRIMA AURORA'
  AND b.benefit_name = N'插畫卡' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid037fNJV8AZEGKBHWy6RYehAnAoFW1oJ2hZbAn91EndeDuBvJXA73S8a18EP1gfbkXyl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'國賓戲院', N'台北京站威秀', N'台北西門威秀影城', N'國賓長春影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'國賓林口昕境廣場', N'國賓新莊晶冠廣場影城', N'國賓影城淡水禮萊廣場', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'國賓桃園八德', N'桃園統領威秀影城', N'桃園站前in89豪華影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中豐原in89豪華影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'in89駁二電影院', N'國賓義大世界影城', N'國賓大魯閣草衙道影城', N'大立in89豪華影城', N'頭份尚順威秀影城', N'嘉義in89豪華影城', N'花蓮新天堂樂園威秀影城', N'國賓屏東環球影城', N'in89澎湖影城')
  AND m.movie_name = N'泥面人'
  AND b.benefit_name = N'A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid032oiSEShnxthgLwai3bVA5YyvyhWcbaj8RNNEUdVDNiroiZ2e7qf3Evyhyc8sd2ntl' AND b.start_date = N'2026-10-22 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園站前in89豪華影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中豐原in89豪華影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'in89駁二電影院', N'大立in89豪華影城', N'頭份尚順威秀影城', N'嘉義in89豪華影城', N'花蓮新天堂樂園威秀影城', N'in89澎湖影城')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'生化人類款A3夜光海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0y3x9pnJT4uN2iNgR9DtZRwgEEfATYAaTTXtr7iLAAq3AevyCQofYtNbHcY7UkjjDl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'MUVIE CINEMAS台北松仁威秀', N'台北京站威秀', N'台北西門威秀影城', N'桃園統領威秀影城', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：【威秀獨家】淬鍊戰衣A3厚磅海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid022BTYcP2nBLsVJX219G6iQaYHssb9k3auVdxeotqK5PqemvtfgQBdAifQgsf6LUCWl' AND b.start_date = N'2026-10-26 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'台北京站威秀', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'劇場版 BANG DREAM! AVE MUJICA PRIMA AURORA'
  AND b.benefit_name = N'角色海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0atYkn9RzKUwwy93kDL4zj1uiUvmeiUbkqGJvcw999qvkhm4uMyqNFaBjYF4h7jBKl' AND b.start_date = N'2026-10-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'YORUSHIKA LIVE 「盜作」'
  AND b.benefit_name = N'盜作主題壓克力吊飾' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0DQMBYrg1pLYKhJ2Xepk1Z82k5R8JABBnE2RoRpbAfEKVnMSaSnjardsPHCCdCqqQl' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'劇場版 BANG DREAM! AVE MUJICA PRIMA AURORA'
  AND b.benefit_name = N'色紙風圖卡五款隨機' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02XufWHTJtQ8n3nKXA8wZCDeJexZ9i8FESYTqNF4eErgux5ihyvLVTSqudS5Dabs5Dl' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北南港LaLaport威秀影城', N'台北京站威秀')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'STREET FIGHTER 6 BLANKA 娃娃購物袋' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02z1kfVExKKEjyjXKuTyP5caD95gz4pih7yTjZrc6mabGqnSvRc7uxD6Geb55wE4iql' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'哥吉拉-0.0'
  AND b.benefit_name = N'首映限定特典—特殊工藝日本原版A3海報+神秘驚喜禮' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid022Y6Xv3CUmM3bRxm572tvVEkZ3uYqpsqKZ539JhcHDAEsTmqGrto97aQcFgJPXMdNl' AND b.start_date = N'2026-11-05 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'ALWAYS LALISA'
  AND b.benefit_name = N'電影限定明信片' AND b.source_url = N'https://www.facebook.com/reel/28585291634462760/' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'YORUSHIKA LIVE 「盜作」'
  AND b.benefit_name = N'A3電影主視覺海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0eqLhi688NoHWRSy2efmq6xnnW1pYMbZTor9pErticFuNwBjH1Jt34GUoYcqHNPy2l' AND b.start_date = N'2026-10-09 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'粽邪4：坤蒂拉娜'
  AND b.benefit_name = N'角色明信片' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0BQawn2euk7Sp8zevKUd1oexiqpNkiKfzKEocWnDe3uDx7HL1xAwwsjiYTo922YrXl' AND b.start_date = N'2026-10-09 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'林口三井OUTLET威秀影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'4DX限定｜A3主視覺海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02sWKjx4EJiAwy9qJqU2uYvEozesYLAfTxgKhvbTZHkG65z8GbLQHpYKWXtnPScszol' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'另一個媽咪'
  AND b.benefit_name = N'限量主視覺款A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0UZAQQxfYFBkY9ajbXzdwf43U2Sp6ck5KwW9j3GpuwrDiRj9o8YLA3ZvBYmk4jvXYl' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'MUVIE CINEMAS台北松仁威秀', N'台北京站威秀', N'板橋大遠百威秀影城', N'新店裕隆城威秀影城', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南南紡威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'野獸之心'
  AND b.benefit_name = N'I GOT YOU A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0HAzDvbepQxG7h5TXwvRXkwvsvepZXivf1CtTUxRPNp9VBZLJK2uMpJRJZurDUMJrl' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'角落小夥伴電影版：從天而降的雲之冒險'
  AND b.benefit_name = N'電影造型貼紙' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0261v7LZwzPiAY1T1m8PR6e5J4RTWeb8W7zTQxxpydGqp1A9ByGiTGRznTbSDVwM5Ql' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'板橋大遠百威秀影城', N'新店裕隆城威秀影城', N'新竹巨城威秀影城', N'台中大遠百威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'IMAX A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02EUhy6bNGKgHsJTqJheSuVcT9F3Jt8UfTF1jojWSdwE2ZxBVWvoXhTzJFG1uRuNr4l' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'另一個媽咪'
  AND b.benefit_name = N'獨家｜電影A3 特寫款海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02E5zTUSP9DwbTCcXsLNCvp8Y7j1xqT4W31WpBDZvzn4ydg1dwGBbQd7NTqkZ2PJcGl' AND b.start_date = N'2026-10-08 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'劇場版 魔法少女小圓〈瓦爾普吉斯的迴天〉'
  AND b.benefit_name = N'電影紀念海報(第三彈)A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02imNRY37PNwkSSfcuWs6gVjAPNqdYC3cHV9AGqCBK7oTrpE7KoUnxmKiPposfG7WEl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'鋼之鍊金術師：香巴拉的征服者'
  AND b.benefit_name = N'電影限定海報［愛力克兄弟款］' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0aFrJ7QN1WVrHyq2FJUEQPChjX99TgaW4m8ST4vD2etMRgFZiKUWfD7seRvsK8wN2l' AND b.start_date = N'2026-10-01 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'板橋大遠百威秀影城', N'新店裕隆城威秀影城', N'新竹巨城威秀影城', N'台中大遠百威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'IMAX限定A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02AEL2493c5oFvqZDzwBHHg3vv4ZM8bzHwgUYqX5JFfV9ZoRhiRqEsfNaLAp4cWnH3l' AND b.start_date = N'2026-10-01 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'野獸之心'
  AND b.benefit_name = N'12x17cm UV亮光特典卡' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0EhV6vHvxXus3fRjYDxD3mT5UTASKrZWQ8TLEAcecVpDBgke6Dr1KkZLBRJgaDq4Hl' AND b.start_date = N'2026-10-01 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'林口三井OUTLET威秀影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'復仇者聯盟：終局之戰'
  AND b.benefit_name = N'4DX特製 A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0vUsnpFyH8FhSP99McsXEghUUodFDnT8ZHgSJEe3qkHXqqzjPM7i4b7psMxKj3iWvl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'板橋大遠百威秀影城', N'新店裕隆城威秀影城', N'新竹巨城威秀影城', N'台中大遠百威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'復仇者聯盟：終局之戰'
  AND b.benefit_name = N'IMAX特製 A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0vUsnpFyH8FhSP99McsXEghUUodFDnT8ZHgSJEe3qkHXqqzjPM7i4b7psMxKj3iWvl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'林口三井OUTLET威秀影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'藍色監獄'
  AND b.benefit_name = N'雙面手繪小卡盲包' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02ytDdWVWy6hXZKV2RsHLAsFb4Rw7xkQQxBBHFfr8UVTkVMJfNQA1SDGuo4zDnm1ael' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'MUVIE CINEMAS台北松仁威秀', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南南紡威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'野獸之心'
  AND b.benefit_name = N'特別場套組：威秀獨家 橫款海報(A3)、明信片組(共3款)' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid028MEAdg4keCcPKBf7YjaRwSmgAo3ZTVUu2CvWJCj7GW2ZeYBaBsCvgtWcJcP3RUmol' AND b.start_date = N'2026-10-03 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'板橋大遠百威秀影城', N'新店裕隆城威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'小心惡犬|A5透卡' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02iKhv1ePT3zo5DjAonZ2vVmDY3AfUfje2E5a4BnSkSsgZFPu6fEBSBmo6ajzumFnvl' AND b.start_date = N'2026-09-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'粽邪4：坤蒂拉娜'
  AND b.benefit_name = N'粽邪系列前三集 A3官方海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0Tg3ypsouvv115tqCvVZtaxiwwYW7HUnRMpqoUnDMV2zdkJqsFhJBw4mWn1DDGF3dl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'驀然回首'
  AND b.benefit_name = N'聽海款電影工藝海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0Z2D4zTFrVuiijQKcaiDfnc8NMfZJsCftrZw1tXZXyMfQkJf6KcUUpNkrVNY1Ypezl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'網紅老爸'
  AND b.benefit_name = N'PAPAGO 限量彩蛋款 A3 海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid07jqx5ERwHGJwPUgykT7B4UihDNTYfoYuhRJC3R7LbWmvcDMKUmYE5LGr4AUR4TAal' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'劇場版 魔法少女小圓〈瓦爾普吉斯的迴天〉'
  AND b.benefit_name = N'電影紀念海報(第二彈)' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid022x1kBf8DvAR9vYDGZXjsZnaY97uSQGrX48MJd6JAZw2vTUxnUCWSaBsjCk48Ktvcl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'希望：末日血戰'
  AND b.benefit_name = N'黃晸珉希望亮銀角色A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02P7erazhNux6kxZ8cJxuPVsTXPK7K3zaueryGWaDDp5YbuYkZ7NwpzcxvM3Cn29aDl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'鋼之鍊金術師：香巴拉的征服者'
  AND b.benefit_name = N'愛德華 愛力克&戴拉 迷你色紙圖卡' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid023U9yqbS29MNR4eo4QPMLpHq9oxRtCUoJRf9Ny66yqoobDbdCZVobudQQjvo2nGCol' AND b.start_date = N'2026-09-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'MUVIE CINEMAS台北松仁威秀', N'台北京站威秀', N'台中老虎城威秀', N'高雄大遠百威秀影城')
  AND m.movie_name = N'誤闖遺忘島'
  AND b.benefit_name = N'電影主題錄音帶造型鞋帶收藏盒' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid025HDSfKr7Q7zbgD5fod7oFaSX1qBMNhiHjRMLFr9Qh1MME2mMJsyxW1RQodL4rHhJl' AND b.start_date = N'2026-09-22 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'航海王喬巴身世之謎'
  AND b.benefit_name = N'特殊工藝A3海報、角色小立牌盲包' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0nyZixRbceRZLCE6HbY5t3E1erbmEyTHHH9yKnuQw92Rq7HUHZByULVi53RZzHodFl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'誤闖遺忘島'
  AND b.benefit_name = N'預售專屬A3特典' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0dCYesQerPWNsZ42PGPS1zZJqHXfJrVCraaKS1buKLfhANb8nj2sMUDPJPUHB922bl' AND b.start_date = N'2026-09-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'希望：末日血戰'
  AND b.benefit_name = N'鄭好娟角色A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02r4ZRLcMAepExr6w7DmDBugP374YdZh618yxmzRyXqNt5yzFVAqwSCRHoSGPnodc2l' AND b.start_date = N'2026-09-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'夢想出擊'
  AND b.benefit_name = N'劇照明信片' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0m8DDfChA4P42sLJfu4cUVGBPG8efwLxdH68tP4M5DYWYeSozDCPQo6gwv9yXJv2pl' AND b.start_date = N'2026-09-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'欣欣秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台南仁德秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：「我是我」韓版雙色海報、《號角日報》光柵海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02ww5ayB6ABk21gK5dMY67BqR5Lnabj3se3rBpzu2bJ4RAfVHnEPAbkUGbFnktxmEVl' AND b.start_date = N'2026-10-26 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：BRAND NEW DAY透明海報組、電影紀念票卡、《號角日報》光柵海報' AND b.source_url = N'https://www.facebook.com/share/p/18owhMqC7n/' AND b.start_date = N'2026-10-26 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：英雄突圍橫幅海報、FOR A MOMENT對視款海報A3、《號角日報》光柵海報A3、彼得帕克手寫信+愛心卡片、FRIENDS明信片、手繪風杯墊' AND b.source_url = N'https://www.facebook.com/share/p/18owhMqC7n/' AND b.start_date = N'2026-10-28 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台中文心秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：4DX海報、SCREENX海報、【獨家】摩天仰望工藝海報、【獨家】一躍突圍工藝海報、【獨家】蜘蛛人夜光金屬徽章、【獨家】蜘蛛人泡泡貼紙、【獨家·空運】一網打盡折傘' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02oeEAUpzUa9EzywwwSaA7aBDaZHWXkdirexVqkvVjbsPmA7jW7TwJXKdPVAWJR2Jrl' AND b.start_date = N'2026-10-10 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'欣欣秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台南仁德秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：【獨家】摩天仰望工藝海報、【獨家】一躍突圍工藝海報、【獨家】蜘蛛人旋轉LED雙層杯' AND b.source_url = N'https://www.facebook.com/share/p/1842tTPpJ7/' AND b.start_date = N'2026-10-09 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：【獨家】摩天仰望工藝海報、【獨家】一躍突圍工藝海報、【獨家】蜘蛛人追擊網搖搖吊飾' AND b.source_url = N'https://www.facebook.com/share/p/1842tTPpJ7/' AND b.start_date = N'2026-10-09 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'IU CONCERT THE GOLDEN HOUR'
  AND b.benefit_name = N'A3海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0cWU9zSWAiYvs6PvzrtVbuJPsTVddVj7Bp1aiJvMRSVX6ftXSxztAEPkvMfMtEeTzl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'土城秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南仁德秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'台東秀泰影城')
  AND m.movie_name = N'IU CONCERT THE WINNING'
  AND b.benefit_name = N'A3海報' AND b.source_url = N'https://www.facebook.com/share/p/19T6egq6ZZ/' AND b.start_date = N'2026-10-30 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'復仇者聯盟：終局之戰'
  AND b.benefit_name = N'INFINITY VISION專屬海報（限特殊版本）' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid0rkeGud5DqAvZs8oUwU3HBPKMmXhx73h9kS9mgTppBqk32VWCkzvsVHD95fqxKfvVl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中文心秀泰影城')
  AND m.movie_name = N'復仇者聯盟：終局之戰'
  AND b.benefit_name = N'ULTRA 4DX版電影海報' AND b.source_url = N'https://www.facebook.com/share/p/1CXeuzHNqD/' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'欣欣秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'嘉義秀泰影城')
  AND m.movie_name = N'復仇者聯盟：終局之戰'
  AND b.benefit_name = N'SCREENX版電影海報' AND b.source_url = N'https://www.facebook.com/share/p/1CXeuzHNqD/' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中站前秀泰影城')
  AND m.movie_name = N'復仇者聯盟：終局之戰'
  AND b.benefit_name = N'Dolby Vision+Atmos版電影海報' AND b.source_url = N'https://www.facebook.com/share/p/1CXeuzHNqD/' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'台中站前秀泰影城')
  AND m.movie_name = N'鋼之鍊金術師：香巴拉的征服者'
  AND b.benefit_name = N'特別場套組：「愛德華・愛力克＆尤爾」迷你色紙圖卡、《香巴拉的征服者》主視覺款A3海報、《嘆息之丘的聖星》主視覺款A3海報' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid022seXUxmqpUGhSyhmZFpx4vG4dxUxD2ojXmgEVptrZtM22HJFK8mTfrotbAnX7Ev8l' AND b.start_date = N'2026-10-03 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'欣欣秀泰影城')
  AND m.movie_name = N'我在學校旅行中被分到了一個糟糕的小組'
  AND b.benefit_name = N'特別場套組：紀念透明小卡' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid02wH5GKQoVGirx7RWhoeW7B9AA22LtkG1TbVNDTU5FWZHsN9DvAw6Uh8Y93Ci8FkDbl' AND b.start_date = N'2026-10-11 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'MUVIE CINEMAS台北松仁威秀', N'台北京站威秀', N'台中老虎城威秀', N'高雄大遠百威秀影城')
  AND m.movie_name = N'YORUSHIKA LIVE 「盜作」'
  AND b.benefit_name = N'特別場套組：「前世」A3主視覺海報、「盜作」A3主視覺海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02Jhn4A2UETmCDXvj2KyVJ22623f66siny65zphS6jY3EHRX1GnB8Vbk4H6ApviUqJl' AND b.start_date = N'2026-11-01 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台中老虎城威秀', N'高雄大遠百威秀影城')
  AND m.movie_name = N'我們不是什麼'
  AND b.benefit_name = N'特別場套組：台灣專屬 全球唯一的素描版海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02jm9EUV1GL1SNAAi7MeZWp4cKm9rydt3fys68rpgP8QjTkCcy4V6U2mVCoEbBWwk4l' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'林口三井OUTLET威秀影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'特別場套組（4DX）：【獨家】角色A3海報組(共3款)' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0VfGYgh49Mpyhn2EPe19m9qGpKQttzCx9Sp35Ex23s5LUXHbSkHWtzof9C6x6Gp43l' AND b.start_date = N'2026-10-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'板橋大遠百威秀影城', N'新店裕隆城威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'特別場套組（IMAX）：【獨家】角色A3海報組(共3款)' AND b.source_url = N'https://www.facebook.com/share/p/1DmPTC193p/' AND b.start_date = N'2026-10-17 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'主視覺A3海報（限2D數位版本）' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0pKyfowVoDN7NmH5xRejzbgaTm5bsH3ne28q2WvGU4dhLbb9m4hZNJsoAEJLZurGml' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'新店裕隆城威秀影城', N'新竹巨城威秀影城', N'台中大遠百威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'【獨家】漫畫家盧卡斯主題手繪款A3特典（限IMAX/4DX版本）' AND b.source_url = N'https://www.facebook.com/share/p/1CATLXETCr/' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'【獨家】RYU&KEN A5貼紙' AND b.source_url = N'https://www.facebook.com/share/p/1CATLXETCr/' AND b.start_date = N'2026-10-07 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'MUVIE CINEMAS台北松仁威秀', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：【獨家】網力全開A3夜光海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0BHAFBUyLTGRL9sw3KkshvUR6TAWbq9x91CXJC2BdkJ6Bv5VCpyT4jnU5uJ28sskGl' AND b.start_date = N'2026-10-09 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'MUVIE CINEMAS台北松仁威秀', N'台北京站威秀', N'台北西門威秀影城', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：【獨家】蛛絲天際海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0EeY5apZ3UAmD9vPaPZUaTo3c4nn7R3KYNX1CRp9MJJVE67Em1dj8mRvpBx1Ft8Xtl' AND b.start_date = N'2026-10-04 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'大巨蛋秀泰影城', N'台北京站威秀', N'台北西門威秀影城', N'欣欣秀泰影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'樹林秀泰影城', N'土城秀泰影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台中站前秀泰影城', N'台中文心秀泰影城', N'台中麗寶秀泰', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'台南仁德秀泰影城', N'高雄大遠百威秀影城', N'高雄夢時代秀泰影城', N'高雄岡山秀泰', N'基隆秀泰影城', N'頭份尚順威秀影城', N'雲林北港秀泰', N'嘉義秀泰影城', N'花蓮秀泰影城', N'花蓮新天堂樂園威秀影城', N'台東秀泰影城')
  AND m.movie_name = N'實境誘捕'
  AND b.benefit_name = N'主視覺原文A3海報' AND b.source_url = N'https://www.facebook.com/share/p/1cXF72o9aS/' AND b.start_date = N'2026-09-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'台北京站威秀', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'特別場套組：【獨家】高空鏡界A3工藝海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid038G6QAvhBaHLLdKTm4iwT5hac6dEnAXSsu2sJJPdgw9FiFfqm6XLcQYFqTrXRiMNvl' AND b.start_date = N'2026-09-27 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北京站威秀', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南南紡威秀影城')
  AND m.movie_name = N'怎麼可能我家的祖先是你家的鬼'
  AND b.benefit_name = N'特別場套組：《祖先鬼》A3紅版厚磅海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02UWYKksenXrpbt9ptjootopHdMmApa3yoaRpJiNY53D737xC1EVxXNGs21gXjcrvXl' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'野獸之心'
  AND b.benefit_name = N'口碑場限定 主視覺A3海報' AND b.source_url = N'https://www.facebook.com/reel/2332401947590297/' AND b.start_date = N'2026-09-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'野獸之心'
  AND b.benefit_name = N'首週預售限定 生死與共款明信片' AND b.source_url = N'https://www.facebook.com/share/r/1dEfPPghWL/' AND b.start_date = N'2026-09-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'台北南港LaLaport威秀影城', N'桃園統領威秀影城', N'台中老虎城威秀', N'高雄大遠百威秀影城')
  AND m.movie_name = N'遊戲人生（電視動畫 前篇）'
  AND b.benefit_name = N'特別場套組：【獨家】棋局降臨A3海報、【獨家】第一週劇照明信片套組' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid09u7HavkXxjEwdaaYAMRSqFrLE5AXnhVEZXEz3XPAx1gd2a3AFR6yEJJceswyhWz2l' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'台北南港LaLaport威秀影城', N'桃園統領威秀影城', N'台中老虎城威秀', N'高雄大遠百威秀影城')
  AND m.movie_name = N'遊戲人生（電視動畫 後篇）'
  AND b.benefit_name = N'特別場套組：【獨家】神之試煉A3海報、【獨家】第二週劇照明信片套組' AND b.source_url = N'https://www.facebook.com/share/p/18mtcjrUpz/' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'台北南港LaLaport威秀影城', N'林口三井OUTLET威秀影城', N'桃園統領威秀影城', N'台中老虎城威秀', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'遊戲人生ZERO'
  AND b.benefit_name = N'特別場套組（10/30～11/01場）：【獨家】主視覺A3海報、【獨家】人物設定A5場刊' AND b.source_url = N'https://www.facebook.com/share/p/18mtcjrUpz/' AND b.start_date = N'2026-10-30 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'大巨蛋秀泰影城', N'樹林秀泰影城', N'台中站前秀泰影城', N'高雄夢時代秀泰影城', N'嘉義秀泰影城')
  AND m.movie_name = N'航海王劇場版：黃金島大冒險'
  AND b.benefit_name = N'特別場套組：四部劇場版A3海報、四款片名徽章、【航海王映画祭】徽章收藏盒' AND b.source_url = N'https://www.facebook.com/showtimecinemas/posts/pfbid022uqEaFWSE4ZbuHTGNanpFm8N7KMH9VirVeZmtRLt5YxHEmdEtmJmDYC4e4seq3ial' AND b.start_date = N'2026-10-04 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'林口三井OUTLET威秀影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'4DX A3海報' AND b.source_url = N'https://www.facebook.com/share/p/1A7yfPgwwK/' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'板橋大遠百威秀影城', N'新店裕隆城威秀影城', N'新竹巨城威秀影城', N'台中大遠百威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'劇場版 魔法少女小圓〈瓦爾普吉斯的迴天〉'
  AND b.benefit_name = N'IMAX視覺海報' AND b.source_url = N'https://www.facebook.com/share/p/1DeLZiCoxL/' AND b.start_date = N'2026-09-04 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'台北京站威秀', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'新竹大遠百威秀影城', N'台中老虎城威秀', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'劇場版 BANG DREAM! AVE MUJICA PRIMA AURORA'
  AND b.benefit_name = N'角色海報（10/31加場）' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid029xc4ybqmGibvmXs7g9d55aYqguvDZmqHGe6eEyBcYyCc7kqZvuGMGVCaK5CxSc21l' AND b.start_date = N'2026-10-31 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'國賓長春影城', N'國賓新莊晶冠廣場影城', N'國賓桃園八德', N'桃園站前in89豪華影城', N'台中豐原in89豪華影城', N'in89駁二電影院', N'國賓大魯閣草衙道影城', N'大立in89豪華影城', N'嘉義in89豪華影城', N'國賓屏東環球影城', N'in89澎湖影城')
  AND m.movie_name = N'加速世界 劇場版'
  AND b.benefit_name = N'黑雪公主A3海報' AND b.source_url = N'https://www.facebook.com/AmbassadorFans/posts/pfbid0KS3Ve9jNrLsLxSXRmPU1nfPPi6i2BMFmHvFvaksSdhN7q6SBGXZ6n8WWB7GFPscNl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'國賓新莊晶冠廣場影城', N'國賓大魯閣草衙道影城')
  AND m.movie_name = N'麵包超人電影版：潘坦與約定之星'
  AND b.benefit_name = N'搖搖沙鈴＋可愛貼紙' AND b.source_url = N'https://www.facebook.com/AmbassadorFans/posts/pfbid029rVRqxVgoeBYuHJWBsP3sYQiX56XDcbVji1DDgFyK5oR7mHD8s8xHdKMaqanapUUl' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'國賓長春影城')
  AND m.movie_name = N'Jim Queen：肌情四射'
  AND b.benefit_name = N'特別場套組：主視覺A3海報、濕身露肌特殊工藝海報、魅力角色盲卡' AND b.source_url = N'https://www.facebook.com/AmbassadorFans/posts/pfbid0jgzpK7yu3AgfzntfQdKQWRMKS16ZYB5XcyV7YYTddeGGBExdLqnjWwPJwmTt9sFKl' AND b.start_date = N'2026-10-30 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'國賓戲院')
  AND m.movie_name = N'黑球'
  AND b.benefit_name = N'特別場套組（DOLBY ATMOS）：粉色獨奏特殊工藝A3海報、水中綻放特殊工藝A3海報' AND b.source_url = N'https://www.facebook.com/AmbassadorFans/posts/pfbid02kDMfGEu5h3Q3TsKgpaprECUWd9K4UPcBBwU4m3nUNFtHUnVxTyAJiwzogkhgHmYYl' AND b.start_date = N'2026-11-01 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'桃園站前in89豪華影城', N'台中豐原in89豪華影城', N'in89駁二電影院', N'大立in89豪華影城', N'嘉義in89豪華影城', N'in89澎湖影城')
  AND m.movie_name = N'Jim Queen：肌情四射'
  AND b.benefit_name = N'原文主視覺A3海報' AND b.source_url = N'https://www.facebook.com/in89in89/posts/pfbid025s5HgWDFb3fQ68eqtzqYWE7CHfXAYjFSvbDsDdstx1Y6of3H17K1kw4zUxkcv5Hwl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'桃園站前in89豪華影城', N'台中豐原in89豪華影城', N'in89駁二電影院', N'大立in89豪華影城', N'嘉義in89豪華影城', N'in89澎湖影城')
  AND m.movie_name = N'進擊的鼓手'
  AND b.benefit_name = N'奮力款原文A3海報' AND b.source_url = N'https://www.facebook.com/in89in89/posts/pfbid02Dq9CtkrbgKsnfn5krbBnAPvNQo1T3PnBrhXxhpLRGCvuZsrN44HAFs2WHeJ7yh59l' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'桃園站前in89豪華影城', N'台中豐原in89豪華影城', N'in89駁二電影院', N'大立in89豪華影城', N'嘉義in89豪華影城', N'in89澎湖影城')
  AND m.movie_name = N'宛如星辰的你'
  AND b.benefit_name = N'花火款電影工藝明信片' AND b.source_url = N'https://www.facebook.com/in89in89/posts/pfbid022ysecDiJFomg9FxNUZtwmX69YZ6fuWS2AaaRB5weW53mprjqzpmmTti367P27TSVl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'桃園站前in89豪華影城', N'台中豐原in89豪華影城', N'in89駁二電影院', N'大立in89豪華影城', N'嘉義in89豪華影城', N'in89澎湖影城')
  AND m.movie_name = N'驀然回首'
  AND b.benefit_name = N'雪地同行款電影工藝海報' AND b.source_url = N'https://www.facebook.com/in89in89/posts/pfbid027oTAb3qLt6mA1aD1tvgkbS8TWHwyUzKjQvNwTao36T2njoL6dWHx1SnsG4amquzml' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'桃園站前in89豪華影城', N'台中豐原in89豪華影城', N'in89駁二電影院', N'大立in89豪華影城', N'嘉義in89豪華影城', N'in89澎湖影城')
  AND m.movie_name = N'煙囪小鎮的普佩2：約定的鐘樓'
  AND b.benefit_name = N'主視覺原文海報' AND b.source_url = N'https://www.facebook.com/in89in89/posts/pfbid02wM7jDrHYynoETTXdYW1DcyfiFYDsamhRcRRwPhUZNeygJcWZaU7NtvRbo4QrEETml' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'桃園站前in89豪華影城', N'台中豐原in89豪華影城', N'in89駁二電影院', N'大立in89豪華影城', N'嘉義in89豪華影城', N'in89澎湖影城')
  AND m.movie_name = N'活命水特弓隊'
  AND b.benefit_name = N'A3 國際版電影海報' AND b.source_url = N'https://www.facebook.com/in89in89/posts/pfbid0mSEdcEG5UPVkovjgRp5tgWcedURWTkVtxRn78zNkNrnRkuqHtjKzM2u4KfHJpjKcl' AND b.start_date = N'2026-10-23 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'in89駁二電影院')
  AND m.movie_name = N'青春電幻物語'
  AND b.benefit_name = N'台灣版A3海報' AND b.source_url = N'https://www.facebook.com/in89in89/posts/pfbid032FcFzZjMYWaq9JJR8wVG3ZXTPm2EhbbvswFbdEfhouZZb2uAjk6t6qQf5NUTPov9l' AND b.start_date = N'2026-10-22 00:00:00';
