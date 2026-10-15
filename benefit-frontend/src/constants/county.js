export const COUNTY_LABELS = {
  KEELUNG: "基隆",
  TAIPEI: "臺北",
  NEW_TAIPEI: "新北市",
  TAOYUAN: "桃園",
  HSINCHU: "新竹",
  YILAN: "宜蘭",
  MIAOLI: "苗栗",
  TAICHUNG: "臺中",
  CHANGHUA: "彰化",
  NANTOU: "南投",
  YUNLIN: "雲林",
  CHIAYI: "嘉義",
  TAINAN: "臺南",
  KAOHSIUNG: "高雄",
  PINGTUNG: "屏東",
  HUALIEN: "花蓮",
  TAITUNG: "臺東",
  PENGHU: "澎湖",
  KINMEN: "金門",
  LIENCHIANG: "連江",
  UNKNOWN: "未知",
};

export function countyLabel(county) {
  return COUNTY_LABELS[county] ?? county ?? "未知";
}

const TICKET_COLOR_BY_REGION = {
  blue: [
    "KEELUNG", "TAIPEI", "NEW_TAIPEI",
    "TAOYUAN", "HSINCHU", "YILAN"
  ],
  green: [
    "MIAOLI", "TAICHUNG", "CHANGHUA",
    "NANTOU", "YUNLIN"
  ],
  orange: [
    "CHIAYI", "TAINAN", "KAOHSIUNG",
    "PINGTUNG", "HUALIEN", "TAITUNG",
    "PENGHU", "KINMEN", "LIENCHIANG"
  ],
};

const COUNTY_TICKET_COLORS = Object.fromEntries(
  Object.entries(TICKET_COLOR_BY_REGION).flatMap(([color, counties]) =>
    counties.map((county) => [county, color]),
  ),
);

const DEFAULT_TICKET_COLOR = "blue";

export function countyTicketColor(county) {
  return COUNTY_TICKET_COLORS[county] ?? DEFAULT_TICKET_COLOR;
}
