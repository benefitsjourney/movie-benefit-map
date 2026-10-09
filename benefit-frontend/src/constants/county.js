export const COUNTY_LABELS = {
  KEELUNG: '基隆市',
  TAIPEI: '臺北市',
  NEW_TAIPEI: '新北市',
  TAOYUAN: '桃園市',
  HSINCHU: '新竹市',
  HSINCHU_COUNTY: '新竹縣',
  YILAN: '宜蘭縣',
  MIAOLI: '苗栗縣',
  TAICHUNG: '臺中市',
  CHANGHUA: '彰化縣',
  NANTOU: '南投縣',
  YUNLIN: '雲林縣',
  CHIAYI: '嘉義市',
  CHIAYI_COUNTY: '嘉義縣',
  TAINAN: '臺南市',
  KAOHSIUNG: '高雄市',
  PINGTUNG: '屏東縣',
  HUALIEN: '花蓮縣',
  TAITUNG: '臺東縣',
  PENGHU: '澎湖縣',
  KINMEN: '金門縣',
  LIENCHIANG: '連江縣',
  UNKNOWN: '未知',
}

export function countyLabel(county) {
  return COUNTY_LABELS[county] ?? county ?? '未知'
}

const TICKET_COLOR_BY_REGION = {
  blue: [
    'KEELUNG',
    'TAIPEI',
    'NEW_TAIPEI',
    'TAOYUAN',
    'HSINCHU',
    'HSINCHU_COUNTY',
    'YILAN',
  ],
  green: ['MIAOLI', 'TAICHUNG', 'CHANGHUA', 'NANTOU', 'YUNLIN'],
  orange: ['CHIAYI', 'CHIAYI_COUNTY', 'TAINAN', 'KAOHSIUNG', 'PINGTUNG'],
  purple: ['HUALIEN', 'TAITUNG'],
  yellow: ['PENGHU', 'KINMEN', 'LIENCHIANG'],
}

export const COUNTY_TICKET_COLORS = Object.fromEntries(
  Object.entries(TICKET_COLOR_BY_REGION).flatMap(([color, counties]) =>
    counties.map((county) => [county, color]),
  ),
)

const DEFAULT_TICKET_COLOR = 'blue'

export function countyTicketColor(county) {
  return COUNTY_TICKET_COLORS[county] ?? DEFAULT_TICKET_COLOR
}
