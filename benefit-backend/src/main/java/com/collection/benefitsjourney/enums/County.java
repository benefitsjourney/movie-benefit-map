package com.collection.benefitsjourney.enums;

/** 臺灣縣市代碼。排序數值越小越北。 */
public enum County {
    /** 基隆 */
    KEELUNG(0),

    /** 臺北 */
    TAIPEI(1),

    /** 新北市 */
    NEW_TAIPEI(2),

    /** 桃園 */
    TAOYUAN(3),

    /** 新竹 */
    HSINCHU(4),

    /** 宜蘭 */
    YILAN(5),

    /** 苗栗 */
    MIAOLI(6),

    /** 臺中 */
    TAICHUNG(7),

    /** 彰化 */
    CHANGHUA(8),

    /** 南投 */
    NANTOU(9),

    /** 雲林 */
    YUNLIN(10),

    /** 嘉義 */
    CHIAYI(11),

    /** 臺南 */
    TAINAN(12),

    /** 高雄 */
    KAOHSIUNG(13),

    /** 屏東 */
    PINGTUNG(14),

    /** 花蓮 */
    HUALIEN(15),

    /** 臺東 */
    TAITUNG(16),

    /** 澎湖 */
    PENGHU(17),

    /** 金門 */
    KINMEN(18),

    /** 連江 */
    LIENCHIANG(19),

    /** 未知 */
    UNKNOWN(20);

    private final int northToSouth;

    County(int northToSouth) {
        this.northToSouth = northToSouth;
    }

    public int northToSouth() {
        return northToSouth;
    }

    /** 產生由北到南的 JPQL CASE，數值越小越北 */
    public static String northToSouthExpression(String path) {
        StringBuilder expression = new StringBuilder("CASE ").append(path);
        for (County county : values()) {
            expression.append(" WHEN ")
                    .append(County.class.getName())
                    .append('.')
                    .append(county.name())
                    .append(" THEN ")
                    .append(county.northToSouth);
        }
        expression.append(" ELSE ").append(values().length).append(" END");
        return expression.toString();
    }
}
