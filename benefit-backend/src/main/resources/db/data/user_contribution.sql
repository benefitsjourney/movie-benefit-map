-- 展示用資料：貢獻，照庫存回報一筆一筆產生，跟InventoryReportService的做法一樣
-- 時間用相對現在，哪天建資料庫都算本週
-- 會員用帳號找，特典用名稱＋貼文網址＋開始日找，不寫死ID
-- 排行榜：可樂7、爆米花5、玉米片4、熱狗堡3、吉拿棒2
INSERT INTO dbo.user_contribution (user_id, contribution_type, reference_id, created_at)
SELECT r.user_id, N'INVENTORY_REPORT', r.report_id, r.reported_at
FROM dbo.inventory_report r;
