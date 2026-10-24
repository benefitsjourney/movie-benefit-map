-- crawler_log 初始資料
-- A 欄主鍵由 SQL Server 自動產生

INSERT INTO dbo.[crawler_log] ([crawler_type], [crawler_status], [fetched_count], [created_count], [updated_count], [skipped_count], [pending_review_count], [message], [executed_at], [finished_at]) VALUES (N'MOVIE', N'SUCCESS', N'174', N'1', N'0', N'0', N'0', N'本次抓到 174 筆、新增 1 筆、略過 0 筆、更新 0 筆、納入審核 0 筆，耗時 41 秒。', N'2026-10-21 09:11:01', N'2026-10-21 09:11:42');
INSERT INTO dbo.[crawler_log] ([crawler_type], [crawler_status], [fetched_count], [created_count], [updated_count], [skipped_count], [pending_review_count], [message], [executed_at], [finished_at]) VALUES (N'CINEMA', N'SUCCESS', N'108', N'0', N'0', N'0', N'0', N'本次抓到 108 筆、新增 0 筆、略過 0 筆、更新 0 筆、納入審核 0 筆，耗時 6 秒。', N'2026-10-21 09:12:47', N'2026-10-21 09:12:54');
INSERT INTO dbo.[crawler_log] ([crawler_type], [crawler_status], [fetched_count], [created_count], [updated_count], [skipped_count], [pending_review_count], [message], [executed_at], [finished_at]) VALUES (N'MOVIE', N'SUCCESS', N'176', N'18', N'1', N'0', N'1', N'本次抓到 176 筆、新增 18 筆、略過 0 筆、更新 1 筆、納入審核 1 筆，耗時 36 秒。', N'2026-10-24 23:29:48', N'2026-10-24 23:30:25');
