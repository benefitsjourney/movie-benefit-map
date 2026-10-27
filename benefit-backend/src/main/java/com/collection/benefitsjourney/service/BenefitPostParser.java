package com.collection.benefitsjourney.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.sourcerules.BenefitPostRules;

/**
 * 把一篇貼文的文字解析成結構化資料（ParsedBenefitPost）。
 * 比對電影、影城、去重、寫入由BenefitCrawlerImportService負責。 支援三種類型：一般週特典、入場贈送型特別場、限量兌換型特別場。
 * 規則類別的splitOffers回傳true時（例如秀泰），一篇貼文依「購買…即送」逐筆拆開，特別場依場次拆開。
 */
@Component
public class BenefitPostParser {

	private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
	private static final int REGULAR_DAYS = 7;

	/** 第一個《電影名》（名字前面可能有 #） */
	private static final Pattern MOVIE = Pattern.compile("《([^》]+)》");
	/** 沒有《》時，退而求其次用【電影名｜特典場】 */
	private static final Pattern MOVIE_BRACKET = Pattern.compile("【#?([^】｜|]+)");
	private static final Pattern BAD_BRACKET = Pattern.compile("須知|資訊|注意|應援|售票");

	/** 「凡購買10/16(五)~10/22(四)期間…」或「凡購買10/16(五)起…」 */
	private static final Pattern DATE_RANGE = Pattern.compile(
			"凡?購買\\s*(?:《[^》]*》)?.{0,20}?(\\d{1,2})/(\\d{1,2})(?:\\s*\\([^)]*\\))?\\s*起?\\s*(?:[~\\-–—]\\s*(\\d{1,2})/(\\d{1,2}))?");
	/** 「10/02(五)~10/09(五)期間」→ 領取期間 */
	private static final Pattern PERIOD_RANGE = Pattern.compile(
			"(\\d{1,2})/(\\d{1,2})(?:\\s*\\([^)]*\\))?\\s*[~\\-–—]\\s*(\\d{1,2})/(\\d{1,2})(?:\\s*\\([^)]*\\))?\\s*期間");
	/** 沒有「凡購買」時，直接找第一個「09/25(五)-10/01(四)」日期區間 */
	private static final Pattern ANY_RANGE = Pattern
			.compile("(\\d{1,2})/(\\d{1,2})(?:\\s*\\([^)]*\\))?\\s*[~\\-–—]\\s*(\\d{1,2})/(\\d{1,2})");
	/** 「即可兌換領取「特典名」款A3海報1張」→ 抓兌換領取到數量之間 */
	private static final Pattern WEEKLY_GIFT = Pattern.compile("(?:兌換領取|開放領取|即可領取)\\s*(.+?)\\s*\\d+\\s*[個張份組套款本]");
	/** 「即可兌換「PAPAGO 限量彩蛋款」A3海報1張」「兌換領取【…冊子】1本」：括號裡是特典名，括號後面的「A3海報」也算 */
	private static final Pattern BRACKET_GIFT = Pattern.compile("(?:兌換領取|兌換|領取|獲得)[^「【\\n]{0,4}[「【]#?([^」】]+)[」】]"
			+ "(?:\\s*([^\\n「【。,，、]{1,12}?)(?=\\s*\\d+\\s*[個張份組套款本]|\\s*\\n))?");
	/** 「特典內容:【聽海款電影工藝海報】」「特典內容:#粽邪系列前三集 A3官方海報 各1張」 */
	private static final Pattern LABEL_GIFT = Pattern.compile("(?:特典內容|贈品內容)\\s*:\\s*([^\\n]+)");
	/** 預購禮的贈品：「預購…即可獲得～「INFINITY VISION D23專屬特典(A3)」1張」 */
	private static final Pattern PREORDER_GIFT = Pattern
			.compile("(?:預購|預售)[^\\n]{0,30}?(?:即可獲得|可獲得|保證獲得)[^「【\\n]{0,4}[「【]([^」】]+)[」】]");
	/** 預售日：「09/18(五)開放預售」「10/07(三) 中午12:00起 開放預售」 */
	private static final Pattern PREORDER_DATE = Pattern
			.compile("(\\d{1,2})/(\\d{1,2})(?:\\s*\\([^)]*\\))?[^\\n]{0,24}?(?:開放)?預[售購]");

	/** 限量兌換型特別場 */
	private static final Pattern MONTH_DAY = Pattern.compile("(\\d{1,2})/(\\d{1,2})");
	private static final Pattern EVENT_DATE_LINE = Pattern
			.compile("(?:放映日期|放映日|活動日期|活動時間|直播時間|首映日期)\\s*[:｜|]\\s*([^\\n]+)");
	private static final Pattern PREMIERE_DATE = Pattern.compile("凡購票觀賞.{0,10}?(\\d{1,2})/(\\d{1,2})");
	/** 「10/16(五)起」「10/07(三)白天營業時間起」；後者要先於「憑09/18(五)起」這種票根日期 */
	private static final Pattern START_FROM = Pattern
			.compile("(\\d{1,2})/(\\d{1,2})(?:\\([^)]*\\))?\\s*(?:白天)?(?:營業時間)?\\s*起");
	private static final Pattern BUNDLE_ITEM = Pattern.compile("(?m)^\\s*[①-⑳]\\s*(.+?)\\s*[xX×*]\\s*\\d+");
	private static final Pattern REDEEM_GIFT = Pattern.compile("(?:兌換|領取|保證獲得|即可獲得)[^「【\\n]{0,4}[「【]([^」】]+)[」】]");
	/** 「兌換：」後面 ❶❷ 條列的特典 */
	private static final Pattern NUMBERED_GIFT = Pattern.compile("(?m)^\\s*[❶-❿]\\s*([^\\n]+)");
	private static final Pattern NOT_GIFT_LINE = Pattern.compile("不可|恕|請|限|每人|數量|實物|須知|適用|無法|注意|款式隨機$");
	/** 「10/12、10/14、10/17｜2D同步上映」→ 活動沒寫期間時，用放映日期當期間 */
	private static final Pattern SCREENING_DATES = Pattern
			.compile("(?m)^[^\\n]*?(\\d{1,2}/\\d{1,2}(?:\\s*[、,，&]\\s*\\d{1,2}/\\d{1,2})+)[^\\n]*上映");
	/** 「【獨家】棋局降臨A3海報+…」「【#獨家】#網力全開A3夜光海報」 */
	private static final Pattern EXCLUSIVE_GIFT = Pattern.compile("【#?獨家】\\s*#?([^\\n]+)");
	/** 分版本的特典：「🟥 4DX版本 ➤「4DX特製 A3海報」１張」 */
	private static final Pattern VERSION_GIFT = Pattern
			.compile("(?m)^[^\\n]*?(4DX|IMAX)[^\\n「]*?[➤→>:][^\\n「]*「([^」]+)」");
	/** 「即可獲得首週相對應版本之入場特典乙份」：不同版本送不同特典，括號抓貼文對特典的稱呼 */
	private static final Pattern CORRESPOND_VERSION = Pattern.compile("對應版本之?\\s*([^\\s，。,、「【0-9]{0,8}?特典)?");
	/** 購買條件列出的版本：「2D、IMAX場次電影票」 */
	private static final Pattern FORMAT_WORD = Pattern.compile("2D|IMAX|4DX", Pattern.CASE_INSENSITIVE);
	private static final Pattern GIFT_AFTER_KEY =Pattern.compile("(?:專屬特典\\s*:|即可獲得|保證獲得)[^「\\n]{0,4}「([^」]+)」");
	private static final Pattern LIMITED_LINE = Pattern.compile("本場限定\\s*[:｜|]\\s*([^\\n]+)");
	private static final Pattern CINEMA_LABEL = Pattern.compile(
			"(?:放映影城|首映影城|活動影城|直播影城|影城據點|預售影城|領取影城|限定場影城)(?:(?:\\s*[｜|]|[^\\n:：－—｜|]*?[:：－—-])\\s*(.*)|\\s*$)");
	/** 有寫「活動影城：」之類的標題（卻沒抓到影城）→ 不能當全連鎖 */
	private static final Pattern CINEMA_LABEL_ANY = Pattern
			.compile("(?:放映影城|首映影城|活動影城|直播影城|影城據點|預售影城|領取影城|限定場影城)\\s*[:：｜|]");
	private static final Pattern LIST_PREFIX = Pattern.compile("^#[^—－\\-]*[—－-]\\s*");
	private static final Pattern LEADING_DECOR = Pattern.compile("^[┆\\s✦•·▪◆✧◼▸➤📍]+");
	private static final Pattern TOKEN_SPLIT = Pattern.compile("[、,，及｜|/・]");

	// ---------- 第四版新增 ----------
	/** 「2026/11/5」「2026.10.15」：年份拿掉，只留月日 */
	private static final Pattern YEAR_PREFIX = Pattern.compile("(?:19|20)\\d{2}[/.](\\d{1,2})[/.](\\d{1,2})");
	/** 每週輪替的週特典：第N週、首週、最終特典 */
	private static final Pattern WEEK_MARK = Pattern.compile("第\\s*[0-9一二三四五六七八九十]+\\s*週|首週|最終(?:觀影)?特典|最終回特典");
	/** 追加印製、重映：不是每週輪替的特典 */
	private static final Pattern NOT_WEEKLY = Pattern.compile("追加|加印|重映");
	/** 「購票與領取時間：09/28(一)-10/11(日)」「領取時間：10/7(三)-11/20(五)」 */
	private static final Pattern PICKUP_PERIOD = Pattern.compile("(?:購票與領取時間|領取時間)\\s*[:｜|]\\s*(\\d{1,2})/(\\d{1,2})"
			+ "(?:\\s*\\([^)]*\\))?\\s*[~\\-–—]\\s*(\\d{1,2})/(\\d{1,2})");
	/** 「特典領取時間：10/01(四)起」 */
	private static final Pattern PICKUP_FROM = Pattern.compile("領取時間\\s*[:｜|]\\s*(\\d{1,2})/(\\d{1,2})");
	/** 「僅限10/22當日」 */
	private static final Pattern ONLY_DAY = Pattern.compile("僅限\\s*(\\d{1,2})/(\\d{1,2})\\s*當日");
	/** 括號裡看起來像贈品的名稱（找不到其他寫法時才用） */
	private static final Pattern QUOTED_ITEM = Pattern
			.compile("[「【]([^」】\\n]{2,30}?(?:海報|卡|明信片|吊飾|貼紙|徽章|色紙|書籤|杯墊|冊子|套組|劇照))[」】]");

	/** 連映、聯映 */
	private static final Pattern JOINT_SCREENING = Pattern.compile("連映|聯映");
	/** 多週套票：「第一週(10/16-10/18) 單人套票」 */
	private static final Pattern WEEK_PACKAGE = Pattern.compile("(第\\s*[0-9一二三四五六七八九十]+\\s*週)[^\\n]*套票");
	private static final Pattern LINE_TITLE = Pattern.compile("^[^《\\p{L}\\p{N}]*《([^》]+)》");

	/** 特別場品項：「1 張【獨家】「墜入鯨口」A3 海報」 */
	private static final Pattern ITEM_QTY_FIRST = Pattern.compile("^\\d+\\s*[張組份個本套枚入盒包]\\s*(.+)$");
	/** 特別場品項：「◆ 原文漫畫 x1」「【獨家】人物設定A5場刊*1」「▸ 首映限定特典 ×1」 */
	private static final Pattern ITEM_MARKED = Pattern
			.compile("^[◆✧◼✦▸⭐★•▪]?\\s*(.+?)\\s*[xX×*]\\s*\\d+\\s*(?:[(（][^)）]*[)）])?\\s*$");
	/** 特別場品項：「⭐造型紀念卡 1 組（全套 5 張）」 */
	private static final Pattern ITEM_TAIL_QTY = Pattern
			.compile("^[◆✧◼✦▸⭐★]\\s*(.+?)\\s*\\d+\\s*[張組份個本套枚入盒包]\\s*(?:[(（][^)）]*[)）])?\\s*$");
	/** 不是特典的品項：電影票、飲料、爆米花，還有當週特典（週特典另外一筆，會重複） */
	private static final Pattern NOT_ITEM = Pattern.compile(
			"電影票|飲料|爆米花|可樂|氣泡水|調酒|茶|當週特典|第.{1,3}週特典|購票特典|即可|購買|兌換|獲得|凡|限購|每人|\\$");

	/** 場次表的標題 */
	private static final Pattern SCHEDULE_LABEL = Pattern
			.compile("活動日期|活動時間|放映日期|放映日|首映日期|直播時間|活動影城|放映影城|首映影城|影城據點|直播影城");
	private static final Pattern SCHEDULE_LABEL_TEXT = Pattern.compile(
			"(?:活動日期|活動時間|放映日期|放映日|首映日期|直播時間|活動影城|放映影城|首映影城|影城據點|直播影城)(?:及影城|、影城)?\\s*[:｜|]?");
	/** 售票、開賣這類行不是場次表 */
	private static final Pattern SALE_LINE = Pattern
			.compile("開賣|開售|售票|購票|訂票|限購|啟售|預售|公布|兌換|領取|上映|臨櫃|官網|APP|營業時間");
	private static final Pattern TIME = Pattern.compile("\\d{1,2}:\\d{2}");
	private static final Pattern WEEKDAY = Pattern.compile("\\([^)]*\\)");

	/** 一行裡的特典：「即可獲得限量「X」1 張」「即送【X】乙份」「兌換【百花繚亂款】A3 海報 1 張」 */
	private static final Pattern INLINE_GIFT = Pattern.compile(
			"(?:即可獲得|即可領取|即送|即贈|兌換|領取)(?:限量|相對應版本|對應版本)?\\s*[「【]([^」】\\n]+)[」】]"
					+ "([^\\n「【。，,、]{0,10}?)(?=\\s*(?:\\d+|乙|一)\\s*[張個份組套本枚]|\\s*[！!。，,(]|\\s*$)");
	/** 「即可領取相對應版本「X」」：配合▸版本清單，一個版本一筆 */
	private static final Pattern CORRESPOND_GIFT = Pattern.compile("對應版本\\s*[「【]([^」】\\n]+)[」】]");
	/** 後面接著條列的那一行：「即可獲得：」「即可獲得對應版本特典：」 */
	private static final Pattern LIST_INTRO = Pattern.compile("(?:獲得|領取|兌換|即送|即贈)[^\\n]{0,16}[:：]\\s*$");
	/** 條列的特典：「①「我是我 A3 海報」1 張」「▸ 特殊版本｜「獨照小卡」1 組」「▸ 2D 版本：2D 版海報 1 張」 */
	private static final Pattern LIST_ITEM = Pattern.compile("^[①-⑳▸•◆✦]\\s*(.+)$");
	private static final Pattern QUOTED = Pattern.compile("[「【]([^」】\\n]+)[」】]");
	private static final Pattern TRAILING_QTY = Pattern.compile("\\s*(?:\\d+|乙|一)\\s*[張個份組套本枚](?:\\s*[(（][^)）]*[)）])?\\s*$");
	/** 期間標題：「｜09/28－10/11 期間限定｜」「🎬第一週觀影特典｜9/24－10/1」 */
	private static final Pattern SEGMENT_HEAD = Pattern.compile("(\\d{1,2})/(\\d{1,2})(?:\\s*\\([^)]*\\))?\\s*[~\\-–—]\\s*"
			+ "(\\d{1,2})/(\\d{1,2})(?:\\s*\\([^)]*\\))?\\s*(?:期間限定)?\\s*[｜|]?\\s*$");
	/** 版本名稱，用來在特典名稱後面加「（限SCREENX版本）」 */
	private static final Pattern VERSION_WORD = Pattern.compile(
			"((?:ULTRA\\s*)?4DX|SCREENX|DOLBY\\s*VISION\\s*\\+?\\s*ATMOS|INFINITY\\s*VISION|IMAX|REMMI|特殊)\\s*版本",
			Pattern.CASE_INSENSITIVE);
	/** 名稱裡已經寫了版本 */
	private static final Pattern NAME_HAS_VERSION = Pattern.compile("4DX|SCREENX|DOLBY|杜比|IMAX|INFINITY|REMMI|2D",
			Pattern.CASE_INSENSITIVE);
	/** 「預計海報兌換期間｜11/13－11/30」 */
	private static final Pattern EXCHANGE_PERIOD = Pattern
			.compile("兌換期間\\s*[:｜|]\\s*(\\d{1,2}/\\d{1,2})\\s*[~\\-–—]\\s*(\\d{1,2}/\\d{1,2})");
	/** 秀泰影城清單的標題（後面幾行是影城） */
	private static final Pattern CINEMA_LIST_LABEL = Pattern.compile("領取影城|限定場影城|活動影城|首映影城|影城據點");

	private final List<BenefitPostRules> rulesList;

	public BenefitPostParser(List<BenefitPostRules> rulesList) {
		this.rulesList = rulesList;
	}

	/** 內部暫存：一篇貼文抓到的欄位 */
	private record Draft(String gift, LocalDate start, LocalDate end, String chainKeyword, List<String> cinemas) {
	}

	/**
	 * @param pageUrl粉專網址（JSON的facebookUrl），用來決定套用哪間影城的規則
	 * @param postUrl貼文網址（JSON的url），存進sourceUrl用來去重
	 * @param time   JSON的time（UTC）
	 * @return 一篇貼文解析出的候選特典；沒有符合的規則、或不是特典則回傳空list
	 */
	public List<ParsedBenefitPost> parseAll(String postId, String pageUrl, String postUrl, String username, String text,
			Instant time) {
		Optional<BenefitPostRules> found = rulesList.stream().filter(r -> r.supports(pageUrl)).findFirst();
		if (found.isEmpty() || text == null) {
			return List.of();
		}
		BenefitPostRules rules = found.get();
		text = TextFold.fold(text); // 花體字、全形英數字先還原，避免片名比對失敗
		Classification cls = rules.classifyPost(text);
		if (cls == Classification.NOT_BENEFIT) {
			return List.of();
		}

		LocalDateTime published = time == null ? null : LocalDateTime.ofInstant(time, TAIPEI);
		BenefitType type = rules.classifyBenefitType(text).orElse(null);
		String movie = findMovie(text);

		// 一張票看好幾部電影（連映、聯映、多週套票）：資料結構一張回報單只能對一部電影，送審核
		String multi = multiFilmNote(text);
		if (multi != null) {
			return List.of(build(postId, postUrl, username, text, published, movie, null, type, null, null, multi,
					Classification.INCOMPLETE, null, List.of()));
		}

		if (cls == Classification.INCOMPLETE || type == null) {
			String why = rules.incompleteNote(text);
			return List.of(build(postId, postUrl, username, text, published, movie, null, type, null, null,
					why != null ? why : "資訊不完整（尚未公布特典或未提到特典），請人工確認", Classification.INCOMPLETE, null, List.of()));
		}

		String body = normalize(rules.removeBoilerplate(text));
		LocalDate postDate = published == null ? null : published.toLocalDate();
		if (rules.splitOffers()) {
			Post post = new Post(postId, postUrl, username, text, published, movie, postDate);
			return type == BenefitType.SPECIAL_BY_DATE ? parseSessions(post, body, rules)
					: parseOffers(post, body, type, rules);
		}
		if (type == BenefitType.SPECIAL_UNTIL_OUT) {
			List<ParsedBenefitPost> byVersion = parseByVersion(postId, postUrl, username, text, published, movie, body,
					postDate, rules);
			if (!byVersion.isEmpty()) {
				return byVersion;
			}
		}
		if (type == BenefitType.REGULAR) {
			List<ParsedBenefitPost> perFormat = parseCorrespondingVersions(postId, postUrl, username, text, published,
					movie, body, postDate, rules);
			if (!perFormat.isEmpty()) {
				return perFormat;
			}
		}
		Draft d = switch (type) {
		case REGULAR -> parseRegular(body, postDate, rules);
		case SPECIAL_BY_DATE -> parseSpecialByDate(body, postDate, rules);
		case SPECIAL_UNTIL_OUT -> parseUntilOut(body, postDate, rules);
		};
		if (type == BenefitType.SPECIAL_UNTIL_OUT && d.end() != null) {
			type = BenefitType.REGULAR; // 貼文有指定領取期間，期間結束就下架
		} else if (type == BenefitType.REGULAR && d.start() != null && d.end() == null) {
			type = BenefitType.SPECIAL_UNTIL_OUT; // 版本限定、只寫「A起」沒寫第幾週：送完為止
		}

		boolean formatDefault = false;
		if (type != BenefitType.REGULAR && d.chainKeyword() == null && d.cinemas().isEmpty()) {
			List<String> byFormat = rules.defaultCinemasByFormat(body);
			if (!byFormat.isEmpty()) {
				d = new Draft(d.gift(), d.start(), d.end(), null, byFormat);
				formatDefault = true;
			}
		}

		List<String> missing = new ArrayList<>();
		if (d.gift() == null) {
			missing.add("特典名稱");
		}
		if (d.start() == null) {
			missing.add("活動日期");
		}
		if (!missing.isEmpty()) {
			return List.of(build(postId, postUrl, username, text, published, movie, d.gift(), type, null, null,
					"無法解析：" + String.join("、", missing), Classification.INCOMPLETE, d.chainKeyword(), d.cinemas()));
		}

		String note = "爬蟲自動建立，請確認電影與日期";
		if (d.chainKeyword() == null && d.cinemas().isEmpty()) {
			note += "；找不到活動影城";
		}
		if (formatDefault) {
			note += "；影城按照4DX/IMAX廳別預設";
		}
		LocalDateTime start = d.start().atStartOfDay();
		LocalDateTime end = d.end() == null ? null : d.end().atTime(LocalTime.of(23, 59, 59));
		return List.of(build(postId, postUrl, username, text, published, movie, d.gift(), type, start, end, note,
				Classification.BENEFIT, d.chainKeyword(), d.cinemas()));
	}

	// ---------- 三種類型 ----------

	/** 一般週特典：全連鎖影城，起訖日期寫在「凡購買…」那句，只寫起始日就是7天 */
	private Draft parseRegular(String body, LocalDate postDate, BenefitPostRules rules) {
		String gift = findWeeklyGift(body);
		LocalDate start = null;
		LocalDate end = null;
		boolean sevenDays = false;
		Matcher m = DATE_RANGE.matcher(body);
		if (m.find() && postDate != null) {
			start = toDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), postDate);
			if (start != null && m.group(3) != null) {
				end = toDate(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), postDate);
				if (end != null && end.isBefore(start)) { // 跨年，例如12/25~1/1
					end = end.plusYears(1);
				}
			} else if (start != null) {
				end = start.plusDays(REGULAR_DAYS - 1);
				sevenDays = true;
			}
		} else if (postDate != null) {
			Matcher r = ANY_RANGE.matcher(body); // 兌換資訊格式：「場次期間：09/25(五)-10/01(四)」
			Matcher f = START_FROM.matcher(body);
			if (r.find()) {
				start = toDate(Integer.parseInt(r.group(1)), Integer.parseInt(r.group(2)), postDate);
				end = toDate(Integer.parseInt(r.group(3)), Integer.parseInt(r.group(4)), postDate);
				if (start != null && end != null && end.isBefore(start)) {
					end = end.plusYears(1);
				}
			} else if (f.find()) {
				start = toDate(Integer.parseInt(f.group(1)), Integer.parseInt(f.group(2)), postDate);
				end = start == null ? null : start.plusDays(REGULAR_DAYS - 1);
				sevenDays = true;
			}
		}
		// 貼文列出影城（例如「活動影城：」）就照列的，不當全連鎖
		List<String> listed = findCinemas(body, rules);
		if (!listed.isEmpty()) {
			return new Draft(gift, start, end, null, listed);
		}
		// 版本限定（例如「IMAX/4DX版本電影票1張」）：只綁有該廳的影城，只寫「A起」沒寫第幾週就算送完為止
		List<String> byVersion = rules.versionCinemas(purchaseLine(body), body);
		if (byVersion != null && !byVersion.isEmpty()) {
			if (sevenDays && !WEEK_MARK.matcher(body).find()) {
				end = null;
			}
			return new Draft(gift, start, end, null, byVersion);
		}
		return new Draft(gift, start, end, rules.chainCinemaKeyword(), List.of());
	}

	/** 購買條件那一行：「凡購買…電影票」 */
	private String purchaseLine(String body) {
		for (String line : body.split("\\R")) {
			if (line.contains("購買") && (line.contains("電影票") || line.contains("場次"))) {
				return line;
			}
		}
		return "";
	}

	/** 週特典的贈品名：先看「特典內容／贈品內容：」，再看「兌換領取…1張」，最後看「兌換「…」」 */
	private String findWeeklyGift(String body) {
		Matcher l = LABEL_GIFT.matcher(body);
		if (l.find()) {
			String raw = l.group(1).replaceAll("\\s*各?\\s*\\d+\\s*[個張份組套款本].*$", "");
			String g = cleanName(raw);
			if (!g.isEmpty()) {
				return g;
			}
		}
		Matcher g = WEEKLY_GIFT.matcher(body);
		if (g.find()) {
			return cleanGift(g.group(1));
		}
		Matcher b = BRACKET_GIFT.matcher(body);
		if (b.find()) {
			String tail = b.group(2) == null ? "" : " " + b.group(2);
			return cleanName(b.group(1) + tail);
		}
		Matcher n = NUMBERED_GIFT.matcher(body); // 「兌換：❶…❷…」條列；規則說明行（不可、請、限…）不算特典
		List<String> items = new ArrayList<>();
		while (n.find()) {
			String line = n.group(1);
			if (NOT_GIFT_LINE.matcher(line).find()) {
				continue;
			}
			String item = cleanName(
					line.replaceAll("[（(][^）)]*[）)]", "").replaceAll("\\s*[０-９0-9]*\\s*[個張份組套款本]\\s*$", ""));
			if (!item.isEmpty() && !items.contains(item)) {
				items.add(item);
			}
		}
		if (!items.isEmpty()) {
			return String.join("、", items);
		}
		Matcher q = QUOTED_ITEM.matcher(body); // 最後才找括號裡看起來像贈品的名稱，例如「沙丘凝視特典A3海報」
		return q.find() ? cleanName(q.group(1)) : null;
	}

	/** 入場贈送型特別場：特典在活動當天入場發，起訖＝放映日期。影城看「放映影城」那一段。 */
	private Draft parseSpecialByDate(String body, LocalDate postDate, BenefitPostRules rules) {
		String gift = findBundle(body);
		if (gift == null) {
			gift = bundleName(findItems(body)); // ◆✧▸開頭、x1結尾或「1 張 …」的品項
		}
		if (gift == null) {
			Matcher r = REDEEM_GIFT.matcher(body);
			if (r.find()) {
				gift = cleanGift(r.group(1));
			}
		}
		if (gift == null) {
			Matcher k = GIFT_AFTER_KEY.matcher(body);
			if (k.find()) {
				gift = cleanGift(k.group(1));
			}
		}
		if (gift == null) {
			Matcher l = LIMITED_LINE.matcher(body);
			if (l.find()) {
				gift = l.group(1).replace("#", "").replaceAll("\\s+", " ").trim();
			}
		}

		if (gift == null) {
			Matcher x = EXCLUSIVE_GIFT.matcher(body);
			List<String> items = new ArrayList<>();
			while (x.find()) {
				String item = x.group(1).replace("#", "").replaceAll("\\s+", " ").trim();
				if (!item.isEmpty() && !items.contains(item)) {
					items.add(item);
				}
			}
			if (!items.isEmpty()) {
				gift = String.join("、", items);
			}
		}

		List<LocalDate> days = new ArrayList<>();
		Matcher line = EVENT_DATE_LINE.matcher(body);
		if (line.find()) {
			addAllDates(line.group(1), postDate, days);
		} else {
			Matcher p = PREMIERE_DATE.matcher(body);
			if (p.find()) {
				addDate(p.group(1), p.group(2), postDate, days);
			} else {
				Matcher dr = DATE_RANGE.matcher(body);
				if (dr.find()) {
					addDate(dr.group(1), dr.group(2), postDate, days);
					if (dr.group(3) != null) {
						addDate(dr.group(3), dr.group(4), postDate, days);
					}
				}
			}
		}
		if (days.isEmpty()) {
			days.addAll(scheduleDates(body, postDate, rules)); //「10/03(六)｜台北松仁、新竹大遠百」這種一行日期加影城
		}
		if (days.isEmpty()) {
			days.addAll(specialLineDates(body, postDate)); //「#角色海報特別場 10/24(六) coming soon」
		}
		if (days.isEmpty() && body.contains("雙十") && postDate != null) { //「雙十特別場」＝10/10
			addDate("10", "10", postDate, days);
		}
		LocalDate start = days.stream().min(LocalDate::compareTo).orElse(null);
		LocalDate end = days.stream().max(LocalDate::compareTo).orElse(null);
		List<String> cinemas = findCinemas(body, rules);
		if (cinemas.isEmpty()) {
			cinemas = scheduleCinemas(body, rules);
		}
		return new Draft(gift, start, end, null, cinemas);
	}

	/** 限量兌換型：只有開始日（「10/16(五)起」），送完為止，沒有結束日 */
	private Draft parseUntilOut(String body, LocalDate postDate, BenefitPostRules rules) {
		boolean preorder = rules.isPreorderBenefit(body);
		String gift = null;
		Matcher r = REDEEM_GIFT.matcher(body);
		if (r.find()) {
			gift = cleanGift(r.group(1));
		}
		if (gift == null) {
			Matcher p = PREORDER_GIFT.matcher(body);
			if (p.find()) {
				gift = cleanName(p.group(1));
			}
		}
		if (gift == null) {
			Matcher k = GIFT_AFTER_KEY.matcher(body);
			if (k.find()) {
				gift = cleanGift(k.group(1));
			}
		}
		if (gift == null) {
			Matcher l = LIMITED_LINE.matcher(body);
			if (l.find()) {
				gift = l.group(1).replace("#", "").replaceAll("\\s+", " ").trim();
			}
		}

		LocalDate start = null;
		LocalDate end = null;
		if (postDate != null) {
			Matcher pd = PREORDER_DATE.matcher(body);
			Matcher s = START_FROM.matcher(body);
			Matcher period = PERIOD_RANGE.matcher(body);
			Matcher pickup = PICKUP_PERIOD.matcher(body);
			if (pickup.find()) { //「購票與領取時間：09/28(一)-10/11(日)」
				start = toDate(Integer.parseInt(pickup.group(1)), Integer.parseInt(pickup.group(2)), postDate);
				end = toDate(Integer.parseInt(pickup.group(3)), Integer.parseInt(pickup.group(4)), postDate);
				if (start != null && end != null && end.isBefore(start)) {
					end = end.plusYears(1);
				}
			} else if (period.find()) { //「凡10/02(五)～10/09(五)期間，購買…」
				start = toDate(Integer.parseInt(period.group(1)), Integer.parseInt(period.group(2)), postDate);
				end = toDate(Integer.parseInt(period.group(3)), Integer.parseInt(period.group(4)), postDate);
				if (start != null && end != null && end.isBefore(start)) {
					end = end.plusYears(1);
				}
			} else if (preorder && pd.find()) {
				start = toDate(Integer.parseInt(pd.group(1)), Integer.parseInt(pd.group(2)), postDate);
			} else if (s.find()) {
				start = toDate(Integer.parseInt(s.group(1)), Integer.parseInt(s.group(2)), postDate);
			} else {
				Matcher dr = DATE_RANGE.matcher(body); //「凡購買09/24(四)~09/27(日)期間…」
				Matcher sd = SCREENING_DATES.matcher(body);
				if (dr.find()) {
					start = toDate(Integer.parseInt(dr.group(1)), Integer.parseInt(dr.group(2)), postDate);
					if (start != null && dr.group(3) != null) {
						end = toDate(Integer.parseInt(dr.group(3)), Integer.parseInt(dr.group(4)), postDate);
					}
				} else if (sd.find()) { //「10/12、10/14、10/17｜2D同步上映」
					List<LocalDate> days = new ArrayList<>();
					addAllDates(sd.group(1), postDate, days);
					start = days.stream().min(LocalDate::compareTo).orElse(null);
					end = days.stream().max(LocalDate::compareTo).orElse(null);
				}
			}
			if (start == null && preorder) {
				start = postDate; //預購禮沒寫日期：從貼文當天算起
			}
		}
		List<String> cinemas = findCinemas(body, rules);
		boolean wholeRun = rules.isWholeRunBenefit(body);
		if (wholeRun && start == null && postDate != null) {
			// 「凡購買上映期間…」沒有明講起始日，貼文開頭通常寫上映日（例如「10/08(四)準備好和…」）
			Matcher first = MONTH_DAY.matcher(body);
			if (first.find()) {
				start = toDate(Integer.parseInt(first.group(1)), Integer.parseInt(first.group(2)), postDate);
			}
		}
		if (start == null && gift != null) {
			start = postDate; //限量兌換沒寫開始日：跟預購禮一樣用發文日
		}
		// 沒列影城、也不是特殊廳／特別場視為全連鎖都能領，特別場沒列影城則留給人工
		boolean cinemaLabelWithoutList = CINEMA_LABEL_ANY.matcher(body).find();
		boolean chainWide = wholeRun || preorder
				|| (!cinemaLabelWithoutList && (!rules.hasSpecialScreeningClue(body) || body.contains("不限版本")));
		if (chainWide && cinemas.isEmpty()) {
			return new Draft(gift, start, end, rules.chainCinemaKeyword(), List.of()); // 全連鎖影城
		}
		return new Draft(gift, start, end, null, cinemas);
	}

	/** 特典依版本分開寫（4DX版本→A海報、IMAX版本→B海報）：每個版本各一筆，影城按照該廳別預設 */
	private List<ParsedBenefitPost> parseByVersion(String postId, String postUrl, String username, String text,
			LocalDateTime published, String movie, String body, LocalDate postDate, BenefitPostRules rules) {
		List<String[]> gifts = new ArrayList<>();
		Matcher m = VERSION_GIFT.matcher(body);
		while (m.find()) {
			gifts.add(new String[] { m.group(1), cleanGift(m.group(2)) });
		}
		Matcher s = START_FROM.matcher(body);
		if (gifts.size() < 2 || !s.find() || postDate == null) {
			return List.of();
		}
		LocalDate startDate = toDate(Integer.parseInt(s.group(1)), Integer.parseInt(s.group(2)), postDate);
		if (startDate == null) {
			return List.of();
		}
		List<ParsedBenefitPost> out = new ArrayList<>();
		for (String[] g : gifts) {
			out.add(build(postId, postUrl, username, text, published, movie, g[1], BenefitType.SPECIAL_UNTIL_OUT,
					startDate.atStartOfDay(), null, "爬蟲自動建立，請確認電影與日期；影城依4DX/IMAX廳別預設", Classification.BENEFIT, null,
					rules.defaultCinemasByFormat(g[0])));
		}
		return out;
	}

	/**
	 * 週特典寫「2D、IMAX場次電影票…即可獲得相對應版本之特典」：一個版本各一筆。
	 * 2D綁全台影城，IMAX、4DX只綁有該廳的影城。
	 * 名稱：貼文有寫特典名稱就用「名稱（版本）」，沒寫就用「週次＋貼文的稱呼（版本）」當暫用名稱並送審核，例如「首週入場特典（IMAX）」。
	 * 每個版本的名稱一定要不同，不然匯入時同貼文同名稱會被當成重複略過。
	 * 日期：2D照一般週特典，IMAX、4DX照版本限定（只寫「A起」又沒寫週次就是送完為止）。
	 */
	private List<ParsedBenefitPost> parseCorrespondingVersions(String postId, String postUrl, String username,
			String text, LocalDateTime published, String movie, String body, LocalDate postDate, BenefitPostRules rules) {
		String line = purchaseLine(body);
		Matcher c = CORRESPOND_VERSION.matcher(line);
		if (!c.find()) {
			return List.of();
		}
		Set<String> formats = new LinkedHashSet<>();
		Matcher f = FORMAT_WORD.matcher(line);
		while (f.find()) {
			formats.add(f.group().toUpperCase());
		}
		if (formats.size() < 2) {
			return List.of(); // 只有一個版本不用拆，照原本的流程
		}
		if (!findCinemas(body, rules).isEmpty()) {
			return List.of(); // 貼文自己列了影城，照原本的流程用列出來的影城
		}
		for (String format : formats) {
			if (!"2D".equals(format) && rules.defaultCinemasByFormat(format).isEmpty()) {
				return List.of(); // 這個品牌沒有特殊廳影城清單，照原本的流程
			}
		}
		Draft d = parseRegular(body, postDate, rules); // 日期和特典名稱沿用週特典的抓法
		if (d.start() == null) {
			return List.of();
		}
		LocalDateTime start = d.start().atStartOfDay();
		// 版本限定又沒寫週次時parseRegular會把結束日清掉，2D照一般週特典補回7天
		LocalDate twoDEnd = d.end() != null ? d.end() : d.start().plusDays(REGULAR_DAYS - 1);

		boolean hasName = d.gift() != null;
		String baseName = d.gift();
		if (!hasName) {
			Matcher w = WEEK_MARK.matcher(line);
			if (!w.find()) {
				w = WEEK_MARK.matcher(body);
			}
			String week = w.find(0) ? w.group() : "";
			String call = c.group(1) == null ? "特典" : c.group(1);
			baseName = week + call;
		}
		String note = hasName ? "爬蟲自動建立，請確認電影與日期，依版本拆開，影城按照廳別預設"
				: "貼文沒寫特典名稱（可能在圖片），先用暫用名稱，請改成正式名稱，依版本拆開，影城按照廳別預設";
		Classification cls = hasName ? Classification.BENEFIT : Classification.INCOMPLETE;

		List<ParsedBenefitPost> out = new ArrayList<>();
		for (String format : formats) {
			// 名稱已經有這個版本的字就不重複加
			String name = baseName.toUpperCase().contains(format) ? baseName : baseName + "（" + format + "）";
			boolean twoD = "2D".equals(format);
			LocalDate endDate = twoD ? twoDEnd : d.end();
			LocalDateTime end = endDate == null ? null : endDate.atTime(LocalTime.of(23, 59, 59));
			BenefitType type = end == null ? BenefitType.SPECIAL_UNTIL_OUT : BenefitType.REGULAR;
			String chain = twoD ? rules.chainCinemaKeyword() : null;
			List<String> cinemas = twoD ? List.of() : rules.defaultCinemasByFormat(format);
			out.add(build(postId, postUrl, username, text, published, movie, name, type, start, end, note, cls, chain,
					cinemas));
		}
		return out;
	}

	// ---------- 小工具 ----------
	/** ①②③開頭的特典清單→「特別場套組：A、B、C」，合成一筆 */
	private String findBundle(String body) {
		Matcher m = BUNDLE_ITEM.matcher(body);
		List<String> items = new ArrayList<>();
		while (m.find()) {
			items.add(m.group(1).trim());
		}
		return items.isEmpty() ? null : "特別場套組：" + String.join("、", items);
	}

	/** 找「放映影城／首映影城／活動影城」那一段，拆成影城簡稱清單（已套用別名） */
	private List<String> findCinemas(String body, BenefitPostRules rules) {
		String[] lines = body.split("\\R");
		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < lines.length; i++) {
			Matcher m = CINEMA_LABEL.matcher(lines[i]);
			if (!m.find()) {
				continue;
			}
			if (m.group(1) != null) {
				addTokens(m.group(1), tokens, rules.cinemaAliases());
			}
			for (int j = i + 1; j < lines.length; j++) {
				String next = LEADING_DECOR.matcher(lines[j]).replaceFirst("").trim();
				if (next.isEmpty() || next.contains(":") || startsWithSymbol(next)) {
					break;
				}
				addTokens(next, tokens, rules.cinemaAliases());
			}
			break;
		}
		return tokens;
	}

	private void addTokens(String line, List<String> out, Map<String, String> aliases) {
		String s = LIST_PREFIX.matcher(LEADING_DECOR.matcher(line).replaceFirst("").trim()).replaceFirst("")
				.replaceAll("\\([^)]*\\)", "").replaceAll("^【[^】]*】\\s*", "") //「【IMAX】板橋大遠百、…」
				.replaceAll("\\d{1,2}/\\d{1,2}", ""); //「10/03(六)｜台北松仁、…」的日期不是影城
		String region = "";
		int bar = s.indexOf('│');
		if (bar > 0) { //「台北 │ 信義威秀、京站威秀影城」→台北信義威秀、台北京站威秀影城
			region = s.substring(0, bar).trim();
			s = s.substring(bar + 1).trim();
		}
		for (Map.Entry<String, String> a : aliases.entrySet()) {
			s = Pattern.compile(Pattern.quote(a.getKey()), Pattern.CASE_INSENSITIVE).matcher(s)
					.replaceAll(Matcher.quoteReplacement(a.getValue()));
		}
		for (String t : TOKEN_SPLIT.split(s)) {
			String name = t.trim();
			if (!name.isEmpty()) {
				out.add(name.startsWith(region) ? name : region + name);
			}
		}
	}

	/** emoji／符號開頭的行代表新的段落（例如🦖首映限定票價） */
	private boolean startsWithSymbol(String line) {
		int cp = line.codePointAt(0);
		return Character.getType(cp) == Character.OTHER_SYMBOL || (cp > 0xFFFF && !Character.isLetterOrDigit(cp));
	}

	private String cleanGift(String raw) {
		String s = raw.replace("「", "").replace("」", "").replaceAll("\\s+", " ").trim();
		return s.replaceAll("\\s*[一1]\\s*[份個張組套]$", "").trim();
	}

	/** 新格式（括號裡的名稱、特典內容：…）專用：額外去掉【】、開頭的#、結尾的emoji */
	private String cleanName(String raw) {
		return cleanGift(raw.replace("【", "").replace("】", "").replaceAll("^[#\\s]+|[\\s🎬]+$", ""));
	}

	private void addAllDates(String line, LocalDate postDate, List<LocalDate> out) {
		Matcher m = MONTH_DAY.matcher(line);
		while (m.find()) {
			addDate(m.group(1), m.group(2), postDate, out);
		}
	}

	private void addDate(String month, String day, LocalDate postDate, List<LocalDate> out) {
		if (postDate == null) {
			return;
		}
		LocalDate d = toDate(Integer.parseInt(month), Integer.parseInt(day), postDate);
		if (d != null) {
			out.add(d);
		}
	}

	private ParsedBenefitPost build(String postId, String postUrl, String username, String text,
			LocalDateTime published, String movie, String gift, BenefitType type, LocalDateTime start,
			LocalDateTime end, String note, Classification cls, String chainKeyword, List<String> cinemas) {
		return new ParsedBenefitPost(postId, postUrl, username, text, published, movie, gift, isMemberOnlyText(text),
				type == null ? null : type.name(), start, end, List.of(), note, cls, chainKeyword,
				cinemas == null ? List.of() : cinemas);
	}

	/** 「iShow會員限定」「僅限收到活動EDM之會員」→會員禮 */
	private static final Pattern MEMBER_ONLY = Pattern.compile("會員限定|僅限收到活動EDM|iShow會員請留意");

	private Boolean isMemberOnlyText(String text) {
		return text != null && MEMBER_ONLY.matcher(text).find();
	}

	private String findMovie(String text) {
		Matcher m = MOVIE.matcher(text);
		String name = null;
		if (m.find()) {
			name = m.group(1);
		} else {
			Matcher b = MOVIE_BRACKET.matcher(text);
			if (b.find() && !BAD_BRACKET.matcher(b.group(1)).find()) {
				name = b.group(1);
			}
		}
		if (name == null) {
			return null;
		}
		name = name.replace("#", "").trim();
		return name.isEmpty() ? null : name;
	}

	/** 貼文日期沒有年份：先用發文年份，若算出來比發文日早超過半年，代表是明年（12月發文講1月的事） */
	private LocalDate toDate(int month, int day, LocalDate postDate) {
		try {
			LocalDate d = LocalDate.of(postDate.getYear(), month, day);
			if (ChronoUnit.DAYS.between(d, postDate) > 180) {
				d = d.plusYears(1);
			}
			return d;
		} catch (RuntimeException e) { // 例如2/30
			return null;
		}
	}

	/** 全形轉半形：數字、符號統一，方便比對 */
	private String normalize(String s) {
		StringBuilder sb = new StringBuilder(s.length());
		for (char c : s.toCharArray()) {
			switch (c) {
			case '～', '〜' -> sb.append('~');
			case '／' -> sb.append('/');
			case '（' -> sb.append('(');
			case '）' -> sb.append(')');
			case '：' -> sb.append(':');
			case '＃' -> sb.append('#');
			case '＊' -> sb.append('*');
			case '－' -> sb.append('-');
			case '＋' -> sb.append('+');
			case '　' -> sb.append(' ');
			default -> sb.append(c >= '０' && c <= '９' ? (char) (c - '０' + '0') : c);
			}
		}
		return YEAR_PREFIX.matcher(sb.toString()).replaceAll("$1/$2"); //「2026/11/5」→「11/5」
	}

	// ---------- 第四版：多部電影 ----------

	/** 連映、聯映、多週套票、標題列了好幾部電影：回傳審核備註，不是就回傳null */
	private String multiFilmNote(String text) {
		String t = normalize(text);
		Set<String> weeks = new LinkedHashSet<>();
		Matcher w = WEEK_PACKAGE.matcher(t);
		while (w.find()) {
			weeks.add(w.group(1).replaceAll("\\s+", ""));
		}
		if (weeks.size() >= 2) {
			return "多週套票，請手動拆";
		}
		List<String> titles = new ArrayList<>();
		Matcher m = MOVIE.matcher(t);
		while (m.find()) {
			String name = m.group(1).replace("#", "").trim();
			if (!name.isEmpty() && !titles.contains(name)) {
				titles.add(name);
			}
		}
		if (JOINT_SCREENING.matcher(t).find()) {
			return titles.size() >= 2 ? "連映場，含多部電影：" + String.join("、", titles.subList(0, Math.min(3, titles.size())))
					: "連映場，含多部電影";
		}
		List<String> lineTitles = new ArrayList<>();
		for (String line : t.split("\\R")) {
			Matcher lt = LINE_TITLE.matcher(line.trim());
			if (lt.find()) {
				String name = lt.group(1).replace("#", "").trim();
				if (!lineTitles.contains(name)) {
					lineTitles.add(name);
				}
			}
		}
		if (lineTitles.size() >= 2) {
			return "含多部電影：" + String.join("、", lineTitles.subList(0, Math.min(3, lineTitles.size()))) + "，請確認";
		}
		return null;
	}

	// ---------- 第四版：特別場品項、場次表（兩家共用） ----------

	/** 一段文字裡的特別場品項，排除電影票、飲料、爆米花、當週特典 */
	private List<String> findItems(String block) {
		List<String> items = new ArrayList<>();
		for (String raw : block.split("\\R")) {
			String line = raw.trim();
			String name = null;
			Matcher q = ITEM_QTY_FIRST.matcher(line);
			Matcher x = ITEM_MARKED.matcher(line);
			Matcher t = ITEM_TAIL_QTY.matcher(line);
			if (q.matches()) {
				name = q.group(1);
			} else if (x.matches()) {
				name = x.group(1);
			} else if (t.matches()) {
				name = t.group(1);
			}
			if (name == null) {
				continue;
			}
			name = cleanItem(name);
			if (name.isEmpty() || NOT_ITEM.matcher(name).find() || items.contains(name)) {
				continue;
			}
			items.add(name);
		}
		return items;
	}

	/** 品項名稱：拿掉#、開頭的符號、多餘空白 */
	private String cleanItem(String raw) {
		String s = raw.replace("#", "").replaceAll("\\s+", " ").trim();
		int i = 0;
		while (i < s.length()) {
			int cp = s.codePointAt(i);
			if (Character.isLetterOrDigit(cp) || "【「《(（".indexOf(cp) >= 0) {
				break;
			}
			i += Character.charCount(cp);
		}
		return s.substring(i).trim();
	}

	/** 「特別場套組：A、B、C」，超過資料庫上限100字時先拿掉空白，還是太長就截斷 */
	private String bundleName(List<String> items) {
		if (items.isEmpty()) {
			return null;
		}
		String name = "特別場套組：" + String.join("、", items);
		if (name.length() > 100) {
			name = name.replace(" ", "");
		}
		return name.length() > 100 ? name.substring(0, 99) + "…" : name;
	}

	/** 場次表的那幾行：「📅活動日期：…」「📍活動影城：…」「10/03(六)｜台北松仁、新竹大遠百」「大巨蛋｜10/25(日)」 */
	private List<String> scheduleLines(String block, BenefitPostRules rules) {
		List<String> out = new ArrayList<>();
		boolean inBlock = false;
		for (String raw : block.split("\\R")) {
			String line = raw.trim();
			if (line.isEmpty() || isSeparator(line)) {
				inBlock = false;
				continue;
			}
			boolean label = SCHEDULE_LABEL.matcher(cleanItem(line)).lookingAt() || line.startsWith("📅")
					|| line.startsWith("📍") || line.startsWith("🗓") || line.startsWith("📆");
			if (!label && (SALE_LINE.matcher(line).find() || isItemLine(line))) {
				inBlock = false;
				continue;
			}
			boolean hasDate = MONTH_DAY.matcher(line).find();
			List<String> tokens = cinemaTokens(stripSchedule(line), rules);
			boolean shortTokens = !tokens.isEmpty() && tokens.stream().allMatch(tok -> tok.length() <= 12);
			if (label || (inBlock && (hasDate || shortTokens)) || (hasDate && shortTokens)) {
				out.add(line);
				inBlock = inBlock || label;
			} else {
				inBlock = false;
			}
		}
		return out;
	}

	private List<LocalDate> scheduleDates(String block, LocalDate postDate, BenefitPostRules rules) {
		List<LocalDate> days = new ArrayList<>();
		for (String line : scheduleLines(block, rules)) {
			addAllDates(TIME.matcher(line).replaceAll(""), postDate, days);
		}
		return days;
	}

	private List<String> scheduleCinemas(String block, BenefitPostRules rules) {
		List<String> out = new ArrayList<>();
		for (String line : scheduleLines(block, rules)) {
			for (String tok : cinemaTokens(stripSchedule(line), rules)) {
				if (!out.contains(tok)) {
					out.add(tok);
				}
			}
		}
		return out;
	}

	/** 有「特別場」又有日期的那一行，例如「#角色海報特別場 10/24(六) coming soon」 */
	private List<LocalDate> specialLineDates(String body, LocalDate postDate) {
		List<LocalDate> days = new ArrayList<>();
		for (String line : body.split("\\R")) {
			if (line.contains("特別場") && MONTH_DAY.matcher(line).find() && !SALE_LINE.matcher(line).find()) {
				addAllDates(TIME.matcher(line).replaceAll(""), postDate, days);
				break;
			}
		}
		return days;
	}

	/** 場次表的一行拿掉標題、日期、星期、時間，剩下的是影城 */
	private String stripSchedule(String line) {
		String s = SCHEDULE_LABEL_TEXT.matcher(line).replaceAll(" ");
		s = WEEKDAY.matcher(s).replaceAll("");
		s = TIME.matcher(s).replaceAll("");
		s = s.replaceAll("\\d{1,2}/\\d{1,2}", "").replaceAll("[~\\-–—]", " ").replace("起", "");
		return s;
	}

	/** 影城簡稱：規則有固定清單就用規則的，沒有就依、｜/切開 */
	private List<String> cinemaTokens(String text, BenefitPostRules rules) {
		if (rules != null) {
			List<String> known = rules.extractCinemaNames(text);
			if (known != null) {
				return known;
			}
		}
		List<String> out = new ArrayList<>();
		addTokens(text, out, rules == null ? Map.of() : rules.cinemaAliases());
		List<String> clean = new ArrayList<>();
		for (String tok : out) {
			String c = cleanItem(tok);
			if (c.codePoints().anyMatch(Character::isLetter) && !clean.contains(c)) {
				clean.add(c);
			}
		}
		return clean;
	}

	private boolean isItemLine(String line) {
		return ITEM_QTY_FIRST.matcher(line).matches() || ITEM_MARKED.matcher(line).matches();
	}

	/** 分隔線：整行沒有任何文字或數字，例如「━━━━━━ 🌊 ━━━━━━」 */
	private boolean isSeparator(String line) {
		return line.codePointCount(0, line.length()) >= 3 && line.codePoints().noneMatch(Character::isLetterOrDigit);
	}

	// ---------- 第四版：逐筆拆開（splitOffers） ----------

	/** 一篇貼文的共用欄位 */
	private record Post(String postId, String postUrl, String username, String text, LocalDateTime published,
			String movie, LocalDate postDate) {
	}

	/** 一樣特典：名稱、購買條件、版本（▸條列的版本，沒有就是null）、所在期間 */
	private record Offer(String gift, String condition, String version, LocalDate[] segment) {
	}

	/** 特別場：依分隔線切成場次，一個場次一筆「特別場套組」 */
	private List<ParsedBenefitPost> parseSessions(Post p, String body, BenefitPostRules rules) {
		List<String> sessions = splitSessions(body);
		long withItems = sessions.stream().filter(s -> !findItems(s).isEmpty()).count();
		List<ParsedBenefitPost> out = new ArrayList<>();
		for (String session : sessions) {
			List<String> items = findItems(session);
			if (items.isEmpty()) {
				continue;
			}
			String scope = withItems == 1 ? body : session; // 只有一個場次時，日期影城可能寫在分隔線外面
			List<LocalDate> days = scheduleDates(scope, p.postDate(), rules);
			if (days.isEmpty()) {
				Matcher line = EVENT_DATE_LINE.matcher(scope);
				if (line.find()) {
					addAllDates(TIME.matcher(line.group(1)).replaceAll(""), p.postDate(), days);
				}
			}
			if (days.isEmpty()) {
				days.addAll(specialLineDates(scope, p.postDate()));
			}
			List<String> cinemas = scheduleCinemas(scope, rules);
			if (cinemas.isEmpty()) {
				cinemas = findCinemas(scope, rules);
			}
			String gift = bundleName(items);
			if (days.isEmpty()) {
				out.add(build(p.postId(), p.postUrl(), p.username(), p.text(), p.published(), p.movie(), gift,
						BenefitType.SPECIAL_BY_DATE, null, null, "無法解析：活動日期", Classification.INCOMPLETE, null,
						cinemas));
				continue;
			}
			LocalDate start = days.stream().min(LocalDate::compareTo).orElseThrow();
			LocalDate end = days.stream().max(LocalDate::compareTo).orElseThrow();
			String note = "爬蟲自動建立，請確認電影與日期" + (cinemas.isEmpty() ? "，找不到活動影城" : "");
			out.add(build(p.postId(), p.postUrl(), p.username(), p.text(), p.published(), p.movie(), gift,
					BenefitType.SPECIAL_BY_DATE, start.atStartOfDay(), end.atTime(LocalTime.of(23, 59, 59)), note,
					Classification.BENEFIT, null, cinemas));
		}
		if (out.isEmpty()) {
			out.add(build(p.postId(), p.postUrl(), p.username(), p.text(), p.published(), p.movie(), null,
					BenefitType.SPECIAL_BY_DATE, null, null, "特典只在圖片（貼文沒有列出特典品項），請人工確認",
					Classification.INCOMPLETE, null, List.of()));
		}
		return out;
	}

	/** 依分隔線切場次，一段裡有兩個以上「$」價格標題時再切開 */
	private List<String> splitSessions(String body) {
		List<String> chunks = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String raw : body.split("\\R")) {
			String line = raw.trim();
			if (isSeparator(line) || line.startsWith("▌")) {
				if (cur.length() > 0) {
					chunks.add(cur.toString());
				}
				cur = new StringBuilder();
				if (isSeparator(line)) {
					continue;
				}
			}
			cur.append(line).append('\n');
		}
		if (cur.length() > 0) {
			chunks.add(cur.toString());
		}
		List<String> out = new ArrayList<>();
		for (String chunk : chunks) {
			out.addAll(splitByPrice(chunk));
		}
		return out;
	}

	private List<String> splitByPrice(String chunk) {
		List<String> parts = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		int prices = 0;
		for (String line : chunk.split("\\R")) {
			if (line.contains("$") && !findItemsInLine(line)) {
				prices++;
				if (prices > 1 && cur.length() > 0) {
					parts.add(cur.toString());
					cur = new StringBuilder();
				}
			}
			cur.append(line).append('\n');
		}
		parts.add(cur.toString());
		return parts;
	}

	private boolean findItemsInLine(String line) {
		return !findItems(line).isEmpty();
	}

	/** 週特典、預售禮：依「購買…即送／即可獲得」逐筆拆開 */
	private List<ParsedBenefitPost> parseOffers(Post p, String body, BenefitType type, BenefitPostRules rules) {
		String[] lines = body.split("\\R");
		List<Offer> offers = new ArrayList<>();
		String condition = "";
		LocalDate[] segment = null;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.isEmpty()) {
				continue;
			}
			LocalDate[] head = segmentHead(line, p.postDate());
			if (head != null) {
				segment = head;
				condition = "";
				continue;
			}
			if (line.contains("購買")) {
				condition = line;
			}
			if (LIST_INTRO.matcher(line).find()) {
				List<Offer> listed = listOffers(lines, i + 1, condition, segment);
				if (!listed.isEmpty()) {
					offers.addAll(listed);
					continue;
				}
			}
			Matcher cv = CORRESPOND_GIFT.matcher(line);
			if (cv.find()) {
				List<String> versions = versionList(lines);
				if (!versions.isEmpty()) {
					for (String v : versions) {
						offers.add(new Offer(cleanName(cv.group(1)) + "（" + v + "）", condition, v, segment));
					}
					continue;
				}
			}
			Matcher g = INLINE_GIFT.matcher(line);
			while (g.find()) {
				String tail = g.group(2) == null ? "" : g.group(2).trim();
				String name = cleanName(g.group(1) + (tail.isEmpty() ? "" : " " + tail)).replace(" 款", "款");
				offers.add(new Offer(name, condition.isEmpty() ? line : condition, null, segment));
			}
		}
		if (offers.isEmpty()) {
			return List.of(build(p.postId(), p.postUrl(), p.username(), p.text(), p.published(), p.movie(), null, type,
					null, null, "無法解析：特典名稱", Classification.INCOMPLETE, null, List.of()));
		}

		List<String> listedCinemas = listedCinemas(body, rules);
		String extraNote = "";
		Matcher ex = EXCHANGE_PERIOD.matcher(body);
		if (ex.find()) {
			extraNote = "，海報兌換期間" + ex.group(1) + "～" + ex.group(2);
		}
		// 同一篇同名的合成一筆，影城合起來（例：4DX加贈的SCREENX版海報和SCREENX版海報）
		Map<String, ParsedBenefitPost> byName = new LinkedHashMap<>();
		for (Offer o : offers) {
			LocalDate[] range = offerDates(o, body, type, rules, p.postDate());
			List<String> cinemas = listedCinemas;
			String chain = null;
			String name = o.gift();
			if (cinemas.isEmpty()) {
				List<String> byVersion = rules.versionCinemas(o.version() != null ? o.version() : o.condition(), body);
				if (byVersion == null || byVersion.isEmpty()) {
					chain = rules.chainCinemaKeyword();
					cinemas = List.of();
				} else {
					cinemas = byVersion;
					name = withVersionSuffix(name, o.version() != null ? o.version() : o.condition());
				}
			}
			BenefitType t = range[1] == null ? BenefitType.SPECIAL_UNTIL_OUT : BenefitType.REGULAR;
			ParsedBenefitPost built;
			if (range[0] == null) {
				built = build(p.postId(), p.postUrl(), p.username(), p.text(), p.published(), p.movie(), name, t, null,
						null, "無法解析：活動日期", Classification.INCOMPLETE, chain, cinemas);
			} else {
				built = build(p.postId(), p.postUrl(), p.username(), p.text(), p.published(), p.movie(), name, t,
						range[0].atStartOfDay(), range[1] == null ? null : range[1].atTime(LocalTime.of(23, 59, 59)),
						"爬蟲自動建立，請確認電影與日期" + extraNote, Classification.BENEFIT, chain, cinemas);
			}
			ParsedBenefitPost old = byName.get(name);
			byName.put(name, old == null ? built : mergeCinemas(old, built));
		}
		return new ArrayList<>(byName.values());
	}

	/** 同一篇同名的兩筆：影城合起來，其中一筆是全連鎖就用全連鎖 */
	private ParsedBenefitPost mergeCinemas(ParsedBenefitPost a, ParsedBenefitPost b) {
		String chain = a.chainKeyword() != null ? a.chainKeyword() : b.chainKeyword();
		Set<String> cinemas = new LinkedHashSet<>(a.cinemaNames());
		cinemas.addAll(b.cinemaNames());
		return new ParsedBenefitPost(a.postId(), a.sourceUrl(), a.username(), a.text(), a.publishedAt(), a.movieName(),
				a.benefitName(), a.memberOnly(), a.benefitType(), a.startDate(), a.endDate(), a.imageUrls(),
				a.reviewNote(), a.classification(), chain, chain != null ? List.of() : new ArrayList<>(cinemas));
	}

	/** 期間標題那一行（沒有「購買」、「領取」，只有日期區間），回傳起訖 */
	private LocalDate[] segmentHead(String line, LocalDate postDate) {
		if (postDate == null || line.contains("購買") || line.contains("領取") || line.contains("開放")) {
			return null;
		}
		Matcher m = SEGMENT_HEAD.matcher(line);
		if (!m.find() || !(line.contains("｜") || line.contains("|") || line.contains("期間限定"))) {
			return null;
		}
		LocalDate start = toDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), postDate);
		LocalDate end = toDate(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), postDate);
		if (start == null || end == null) {
			return null;
		}
		return new LocalDate[] { start, end.isBefore(start) ? end.plusYears(1) : end };
	}

	/** 「即可獲得：」後面的條列：①「X」、▸ 版本｜「X」、▸ 版本：X 1 張＋加贈 Y 1 張 */
	private List<Offer> listOffers(String[] lines, int from, String condition, LocalDate[] segment) {
		List<Offer> out = new ArrayList<>();
		for (int i = from; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.isEmpty()) {
				continue;
			}
			Matcher item = LIST_ITEM.matcher(line);
			if (!item.matches()) {
				break;
			}
			String rest = item.group(1);
			String version = null;
			int cut = indexOfAny(rest, "｜", "|", ":", "：");
			boolean circled = line.charAt(0) >= '①' && line.charAt(0) <= '⑳';
			if (!circled && cut > 0) {
				version = rest.substring(0, cut).trim();
				rest = rest.substring(cut + 1).trim();
			}
			Matcher q = QUOTED.matcher(rest);
			if (q.find()) {
				out.add(new Offer(cleanName(q.group(1)), condition, version, segment));
				continue;
			}
			for (String part : rest.split("[+＋]")) {
				String name = TRAILING_QTY.matcher(part.replace("加贈", "")).replaceFirst("").trim();
				if (!name.isEmpty() && TRAILING_QTY.matcher(part).find()) {
					out.add(new Offer(cleanName(name), condition, version, segment));
				}
			}
		}
		return out;
	}

	/** ▸版本清單（例：▸ 2D｜局部光）的版本名稱，條列裡沒有特典名稱時用 */
	private List<String> versionList(String[] lines) {
		List<String> out = new ArrayList<>();
		for (String raw : lines) {
			String line = raw.trim();
			if (!line.startsWith("▸")) {
				continue;
			}
			String rest = line.substring(1).trim();
			int cut = indexOfAny(rest, "｜", "|");
			if (cut <= 0 || QUOTED.matcher(rest).find() || TRAILING_QTY.matcher(rest).find()) {
				continue;
			}
			out.add(rest.substring(0, cut).trim());
		}
		return out;
	}

	private int indexOfAny(String s, String... keys) {
		int best = -1;
		for (String k : keys) {
			int at = s.indexOf(k);
			if (at >= 0 && (best < 0 || at < best)) {
				best = at;
			}
		}
		return best;
	}

	/** 一樣特典的起訖：領取時間 → 購買條件的期間 → 期間標題 → 僅限當日 → 領取時間起 → 購買條件的「A起」 → 預售日 → 發文日 */
	private LocalDate[] offerDates(Offer o, String body, BenefitType type, BenefitPostRules rules, LocalDate postDate) {
		LocalDate[] none = { null, null };
		if (postDate == null) {
			return none;
		}
		String cond = o.condition() == null ? "" : o.condition();
		Matcher pick = PICKUP_PERIOD.matcher(body);
		if (o.segment() == null && pick.find()) {
			return range(pick, postDate);
		}
		Matcher r = ANY_RANGE.matcher(cond);
		if (r.find()) {
			return range(r, postDate);
		}
		if (o.segment() != null) {
			return o.segment();
		}
		Matcher only = ONLY_DAY.matcher(body);
		if (only.find()) {
			LocalDate d = toDate(Integer.parseInt(only.group(1)), Integer.parseInt(only.group(2)), postDate);
			return new LocalDate[] { d, d };
		}
		boolean preorder = type == BenefitType.SPECIAL_UNTIL_OUT || rules.isPreorderBenefit(body);
		Matcher pf = PICKUP_FROM.matcher(body);
		if (preorder && pf.find()) {
			return new LocalDate[] { toDate(Integer.parseInt(pf.group(1)), Integer.parseInt(pf.group(2)), postDate),
					null };
		}
		Matcher from = START_FROM.matcher(cond);
		Matcher single = MONTH_DAY.matcher(cond);
		LocalDate start = null;
		if (from.find()) {
			start = toDate(Integer.parseInt(from.group(1)), Integer.parseInt(from.group(2)), postDate);
		} else if (!preorder && single.find()) {
			start = toDate(Integer.parseInt(single.group(1)), Integer.parseInt(single.group(2)), postDate);
		}
		if (start != null) {
			boolean weekly = type == BenefitType.REGULAR && WEEK_MARK.matcher(body).find()
					&& !NOT_WEEKLY.matcher(body).find();
			return new LocalDate[] { start, weekly ? start.plusDays(REGULAR_DAYS - 1) : null };
		}
		if (preorder) {
			Matcher pd = PREORDER_DATE.matcher(body);
			LocalDate d = pd.find() ? toDate(Integer.parseInt(pd.group(1)), Integer.parseInt(pd.group(2)), postDate)
					: null;
			return new LocalDate[] { d != null ? d : postDate, null };
		}
		return none;
	}

	private LocalDate[] range(Matcher m, LocalDate postDate) {
		LocalDate start = toDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), postDate);
		LocalDate end = toDate(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), postDate);
		if (start != null && end != null && end.isBefore(start)) {
			end = end.plusYears(1);
		}
		return new LocalDate[] { start, end };
	}

	/** 貼文列出的影城（領取影城、首波限定場影城…），標題那一行和後面幾行 */
	private List<String> listedCinemas(String body, BenefitPostRules rules) {
		String[] lines = body.split("\\R");
		List<String> out = new ArrayList<>();
		for (int i = 0; i < lines.length; i++) {
			if (!CINEMA_LIST_LABEL.matcher(lines[i]).find() || lines[i].contains("購買")) {
				continue;
			}
			addAllNew(out, cinemaTokens(stripSchedule(lines[i]), rules));
			for (int j = i + 1; j < lines.length; j++) {
				List<String> next = cinemaTokens(stripSchedule(lines[j]), rules);
				if (lines[j].isBlank() || next.isEmpty() || isSeparator(lines[j].trim())) {
					break;
				}
				addAllNew(out, next);
			}
			break;
		}
		return out;
	}

	private void addAllNew(List<String> out, List<String> more) {
		for (String s : more) {
			if (!out.contains(s)) {
				out.add(s);
			}
		}
	}

	/** 版本限定的特典，名稱後面加「（限SCREENX版本）」，名稱已經寫了版本就不加 */
	private String withVersionSuffix(String name, String versionText) {
		if (name == null || NAME_HAS_VERSION.matcher(name).find()) {
			return name;
		}
		Matcher v = VERSION_WORD.matcher(versionText == null ? "" : versionText);
		String label = v.find() ? v.group(1).replaceAll("\\s+", " ").trim() : null;
		return label == null ? name : name + "（限" + label + "版本）";
	}
}
