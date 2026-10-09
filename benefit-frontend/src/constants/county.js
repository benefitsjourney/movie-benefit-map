const COUNTY_LABELS = {
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
