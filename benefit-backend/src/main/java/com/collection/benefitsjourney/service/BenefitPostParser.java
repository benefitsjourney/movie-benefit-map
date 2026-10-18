package com.collection.benefitsjourney.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.collection.benefitsjourney.enums.BenefitType;
import com.collection.benefitsjourney.service.ParsedBenefitPost.Classification;
import com.collection.benefitsjourney.service.sourcerules.BenefitPostRules;

/**
 * 把一篇貼文的文字解析成結構化資料（ParsedBenefitPost）。
 * 比對電影、影城、去重、寫入由BenefitCrawlerImportService負責。 支援三種類型：一般週特典、入場贈送型特別場、限量兌換型特別場。
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
			"凡?購買.{0,20}?(\\d{1,2})/(\\d{1,2})(?:\\s*\\([^)]*\\))?\\s*起?\\s*(?:[~\\-–—]\\s*(\\d{1,2})/(\\d{1,2}))?");
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
	private static final Pattern EVENT_DATE_LINE = Pattern.compile("(?:放映日期|放映日|活動日期|直播時間)\\s*:\\s*([^\\n]+)");
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
	private static final Pattern GIFT_AFTER_KEY = Pattern.compile("(?:專屬特典\\s*:|即可獲得|保證獲得)[^「\\n]{0,4}「([^」]+)」");
	private static final Pattern LIMITED_LINE = Pattern.compile("本場限定\\s*[:｜|]\\s*([^\\n]+)");
	private static final Pattern CINEMA_LABEL = Pattern
			.compile("(?:放映影城|首映影城|活動影城|直播影城|影城據點)(?:[^\\n:：－—]*?[:：－—]\\s*(.*)|\\s*$)");
	/** 有寫「活動影城：」之類的標題（卻沒抓到影城）→ 不能當全連鎖 */
	private static final Pattern CINEMA_LABEL_ANY = Pattern.compile("(?:放映影城|首映影城|活動影城|直播影城|影城據點)\\s*[:：]");
	private static final Pattern LIST_PREFIX = Pattern.compile("^#[^—－\\-]*[—－-]\\s*");
	private static final Pattern LEADING_DECOR = Pattern.compile("^[┆\\s✦•·▪]+");
	private static final Pattern TOKEN_SPLIT = Pattern.compile("[、,，及｜|/・]");

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

		if (cls == Classification.INCOMPLETE || type == null) {
			String why = rules.incompleteNote(text);
			return List.of(build(postId, postUrl, username, text, published, movie, null, type, null, null,
					why != null ? why : "資訊不完整（尚未公布特典或未提到特典），請人工確認", Classification.INCOMPLETE, null, List.of()));
		}

		String body = normalize(rules.removeBoilerplate(text));
		LocalDate postDate = published == null ? null : published.toLocalDate();
		if (type == BenefitType.SPECIAL_UNTIL_OUT) {
			List<ParsedBenefitPost> byVersion = parseByVersion(postId, postUrl, username, text, published, movie, body,
					postDate, rules);
			if (!byVersion.isEmpty()) {
				return byVersion;
			}
		}
		Draft d = switch (type) {
		case REGULAR -> parseRegular(body, postDate, rules);
		case SPECIAL_BY_DATE -> parseSpecialByDate(body, postDate, rules);
		case SPECIAL_UNTIL_OUT -> parseUntilOut(body, postDate, rules);
		};
		if (type == BenefitType.SPECIAL_UNTIL_OUT && d.end() != null) {
			type = BenefitType.REGULAR; // 貼文有指定領取期間，期間結束就下架
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
			}
		}
		return new Draft(gift, start, end, rules.chainCinemaKeyword(), List.of());
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
		return items.isEmpty() ? null : String.join("、", items);
	}

	/** 入場贈送型特別場：特典在活動當天入場發，起訖＝放映日期。影城看「放映影城」那一段。 */
	private Draft parseSpecialByDate(String body, LocalDate postDate, BenefitPostRules rules) {
		String gift = findBundle(body);
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
		if (days.isEmpty() && body.contains("雙十") && postDate != null) { //「雙十特別場」＝10/10
			addDate("10", "10", postDate, days);
		}
		LocalDate start = days.stream().min(LocalDate::compareTo).orElse(null);
		LocalDate end = days.stream().max(LocalDate::compareTo).orElse(null);
		return new Draft(gift, start, end, null, findCinemas(body, rules));
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
			if (period.find()) { //「凡10/02(五)～10/09(五)期間，購買…」
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
				.replaceAll("\\([^)]*\\)", "");
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
			case '　' -> sb.append(' ');
			default -> sb.append(c >= '０' && c <= '９' ? (char) (c - '０' + '0') : c);
			}
		}
		return sb.toString();
	}
}