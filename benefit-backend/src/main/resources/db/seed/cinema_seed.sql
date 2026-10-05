USE BenefitsJourney;

DELETE FROM cinema;
DBCC CHECKIDENT ('cinema', RESEED, 0);

INSERT INTO cinema (
    cinema_name,
    cinema_address,
    latitude,
    longitude,
    official_website_url,
    county,
    is_visible,
    is_active
)
VALUES
(N'星河影城 台北信義店', N'臺北市信義區松仁路100號', 25.033100, 121.568300, N'https://example.com/cinema/1', N'TAIPEI', 1, 1),
(N'光影影城 台北西門店', N'臺北市萬華區漢中街120號', 25.043500, 121.507900, N'https://example.com/cinema/2', N'TAIPEI', 1, 1),
(N'映月影城 台北中山店', N'臺北市中山區南京東路一段88號', 25.052200, 121.525800, N'https://example.com/cinema/3', N'TAIPEI', 1, 1),

(N'晨星影城 板橋店', N'新北市板橋區新站路28號', 25.012200, 121.462500, N'https://example.com/cinema/4', N'NEW_TAIPEI', 1, 1),
(N'銀幕影城 新莊店', N'新北市新莊區中平路300號', 25.049700, 121.444200, N'https://example.com/cinema/5', N'NEW_TAIPEI', 1, 1),
(N'夢境影城 淡水店', N'新北市淡水區中山路150號', 25.167600, 121.445300, N'https://example.com/cinema/6', N'NEW_TAIPEI', 1, 1),

(N'曙光影城 桃園店', N'桃園市桃園區中正路900號', 25.017000, 121.300000, N'https://example.com/cinema/7', N'TAOYUAN', 1, 1),
(N'極光影城 中壢店', N'桃園市中壢區中央西路一段80號', 24.953700, 121.225800, N'https://example.com/cinema/8', N'TAOYUAN', 1, 1),

(N'城市影城 新竹店', N'新竹市東區中央路200號', 24.809700, 120.975700, N'https://example.com/cinema/9', N'HSINCHU', 1, 1),
(N'幻境影城 竹北店', N'新竹縣竹北市光明六路東一段180號', 24.826800, 121.012400, N'https://example.com/cinema/10', N'HSINCHU_COUNTY', 1, 1),

(N'星空影城 台中站前店', N'臺中市中區臺灣大道一段100號', 24.137100, 120.684200, N'https://example.com/cinema/11', N'TAICHUNG', 1, 1),
(N'銀河影城 台中西屯店', N'臺中市西屯區臺灣大道三段500號', 24.165500, 120.646600, N'https://example.com/cinema/12', N'TAICHUNG', 1, 1),
(N'映畫影城 豐原店', N'臺中市豐原區中正路300號', 24.252100, 120.721200, N'https://example.com/cinema/13', N'TAICHUNG', 1, 1),

(N'樂光影城 彰化店', N'彰化縣彰化市中山路二段500號', 24.075600, 120.544000, N'https://example.com/cinema/14', N'CHANGHUA', 1, 1),

(N'海風影城 嘉義店', N'嘉義市西區中山路400號', 23.479000, 120.448800, N'https://example.com/cinema/15', N'CHIAYI', 1, 1),

(N'南星影城 台南中西店', N'臺南市中西區西門路一段600號', 22.990800, 120.198500, N'https://example.com/cinema/16', N'TAINAN', 1, 1),
(N'赤嵌影城 台南東區店', N'臺南市東區中華東路二段200號', 22.985300, 120.233000, N'https://example.com/cinema/17', N'TAINAN', 1, 1),

(N'港都影城 高雄左營店', N'高雄市左營區博愛二路700號', 22.669700, 120.303000, N'https://example.com/cinema/18', N'KAOHSIUNG', 1, 1),
(N'海港影城 高雄前鎮店', N'高雄市前鎮區中華五路800號', 22.595900, 120.307000, N'https://example.com/cinema/19', N'KAOHSIUNG', 1, 1),

(N'東岸影城 花蓮店', N'花蓮縣花蓮市中山路300號', 23.991100, 121.611200, N'https://example.com/cinema/20', N'HUALIEN', 1, 0);