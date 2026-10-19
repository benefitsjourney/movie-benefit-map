-- 由資料庫已確認的特典自動產生：特典 ↔ 電影 ↔ 影城
-- 需要 cinema、movie、benefit 表已有資料；找不到電影或影城時，那幾行不會寫入任何資料
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'林口三井OUTLET威秀影城', N'台南大遠百威秀影城', N'高雄大遠百威秀影城')
  AND m.movie_name = N'泥面人'
  AND b.benefit_name = N'特別場套組：面對恐懼A3工藝海報、原文漫畫、4DX限定A3海報、主視覺A3海報' AND b.source_url = N'https://www.facebook.com/reel/2349394229154426/' AND b.start_date = N'2026-10-24 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'YORUSHIKA LIVE 「盜作」'
  AND b.benefit_name = N'盜作主題壓克力吊飾' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0DQMBYrg1pLYKhJ2Xepk1Z82k5R8JABBnE2RoRpbAfEKVnMSaSnjardsPHCCdCqqQl' AND b.start_date = N'2026-10-16 00:00:00';
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
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'厄夜之鼠款A3夜光海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02E1WjLtoHKbCunS21thXpDVZhupr7GfnTrknNf8eoM2zRsx1ax3fzxHrcCqDpYfXMl' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'破曉前行款A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid035DcNzegVAqMx3YSuLU27eRAvb7CaG2TK911KrUJpApt1VB257siv5soSS7taAEdkl' AND b.start_date = N'2026-10-16 00:00:00';
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
  AND m.movie_name = N'粽邪4'
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
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'日本進口｜迷你公仔吊飾' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02cSyEE5yWhVfPiXsJKprEBJoiCbFPBRFFtnyWcu6GRy45jC6ofptjSbTUK7Ardwjul' AND b.start_date = N'2026-10-10 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'惡靈古堡：爆發夜'
  AND b.benefit_name = N'【小心惡犬】款A3夜光海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0Q8xNvGRAT6FD6ouQeQzyNzGQFNd561CMkn6BhJT8QzhmHfruF5WhBmjZbaRo1RyQl' AND b.start_date = N'2026-10-08 00:00:00';
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
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'快打旋風'
  AND b.benefit_name = N'對應版本A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02EUhy6bNGKgHsJTqJheSuVcT9F3Jt8UfTF1jojWSdwE2ZxBVWvoXhTzJFG1uRuNr4l' AND b.start_date = N'2026-10-16 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'我是我A3海報(另附紅藍鏡片組)' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid03773EZivfPtRkQ5mMux3o71u5oVBppfP9jiVfjZSjoKPHCBsykzKaqHbUsXTQjzYtl' AND b.start_date = N'2026-10-08 00:00:00';
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
WHERE c.cinema_name IN (N'板橋大遠百威秀影城', N'新店裕隆城威秀影城', N'新竹巨城威秀影城', N'台中大遠百威秀影城', N'台南南紡威秀影城', N'高雄大遠百威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'IMAX限定A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02AEL2493c5oFvqZDzwBHHg3vv4ZM8bzHwgUYqX5JFfV9ZoRhiRqEsfNaLAp4cWnH3l' AND b.start_date = N'2026-10-01 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'野獸之心'
  AND b.benefit_name = N'第二週觀影特典｜12x17cm UV亮光特典卡' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0EhV6vHvxXus3fRjYDxD3mT5UTASKrZWQ8TLEAcecVpDBgke6Dr1KkZLBRJgaDq4Hl' AND b.start_date = N'2026-10-01 00:00:00';
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
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'【百花繚亂】款A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0ptrG2wfxf1fWcS9rPAbdd3XsHTyw2S1ALqkxMgJPp9dqgHK7mZhmiNdNQmPeLaN1l' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'自動充氣氣球' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid08cC1YfN3Ftsp4w1Rij5NpB4wnsFbU4Ar4nH1qodZzRFKGEPxQMzGhN88TbS7tV1Gl' AND b.start_date = N'2026-10-02 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'ALWAYS LALISA'
  AND b.benefit_name = N'電影限定明信片' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid034ZiRAKnh2MVEL9Mb1GeUwhym94TNxfT9LfQKqVUQk5UJsgAr3N8oMYi9bUupb7Gul' AND b.start_date = N'2026-10-12 00:00:00';
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
  AND m.movie_name = N'粽邪4'
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
  AND m.movie_name = N'挖掘者'
  AND b.benefit_name = N'預售款A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02s1vrRMzuLSWTqmzzxrUjJVHUsFwivpxXJj6m867K65obW4hgNhhNoWbCr4TMHQsYl' AND b.start_date = N'2026-10-01 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'鋼之鍊金術師'
  AND b.benefit_name = N'愛德華 愛力克&戴拉 迷你色紙圖卡' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid023U9yqbS29MNR4eo4QPMLpHq9oxRtCUoJRf9Ny66yqoobDbdCZVobudQQjvo2nGCol' AND b.start_date = N'2026-09-24 00:00:00';
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
  AND m.movie_name = N'蜘蛛人：重生日'
  AND b.benefit_name = N'光影入戲A3海報' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02ecTEWBofEgPdFtbvvfCAAdCtEoYsy9waksV9r4NuQkujHgf8uvvnGGbLVJ39TQD9l' AND b.start_date = N'2026-09-25 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'劇場版 吉伊卡哇 人魚島的秘密'
  AND b.benefit_name = N'【日本進口 原作者nagano繪製秘密豪華冊子】' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid02pV3M7YEvqYYrbYJER9DtgmKoX9vVZfJbrg23EN26VryLEGqtsAPEZEENVoogSveGl' AND b.start_date = N'2026-09-25 00:00:00';
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
  AND m.movie_name = N'驀然回首'
  AND b.benefit_name = N'【主視覺冬日款電影A3海報】' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0HHYGtkwJ7fcagGXhm2LqowFx3XEYwoFLes8JHUud3b8G8Y2MoahEhX29Ecu5XiH1l' AND b.start_date = N'2026-09-18 00:00:00';
INSERT INTO dbo.cinema_movie_benefit (cinema_id, movie_id, benefit_id, benefit_inventory_status, created_at, updated_at)
SELECT c.cinema_id, m.movie_id, b.benefit_id, 0, SYSDATETIME(), SYSDATETIME()
FROM dbo.cinema c, dbo.movie m, dbo.benefit b
WHERE c.cinema_name IN (N'台北信義威秀', N'MUVIE CINEMAS台北松仁威秀', N'台北南港LaLaport威秀影城', N'台北京站威秀', N'台北西門威秀影城', N'板橋大遠百威秀影城', N'林口三井OUTLET威秀影城', N'中和環球威秀影城', N'新店裕隆城威秀影城', N'桃園統領威秀影城', N'桃園桃知道威秀影城', N'新竹大遠百威秀影城', N'新竹巨城威秀影城', N'台中老虎城威秀', N'台中大遠百威秀影城', N'台中iFG遠雄廣場威秀影城', N'台南大遠百威秀影城', N'台南南紡威秀影城', N'台南FOCUS威秀影城', N'高雄大遠百威秀影城', N'頭份尚順威秀影城', N'花蓮新天堂樂園威秀影城')
  AND m.movie_name = N'夢想出擊'
  AND b.benefit_name = N'劇照明信片' AND b.source_url = N'https://www.facebook.com/vieshow/posts/pfbid0m8DDfChA4P42sLJfu4cUVGBPG8efwLxdH68tP4M5DYWYeSozDCPQo6gwv9yXJv2pl' AND b.start_date = N'2026-09-18 00:00:00';
