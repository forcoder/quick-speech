package com.quickspeech.wubi.data

/**
 * 86版五笔字根表
 * 键位 -> 字根映射，包含主字根和辅助字根
 */
object WubiRadicals86 {

    /**
     * 完整字根表：字母键 -> 该键上的所有字根
     * 键名字根（每个键的第一个字根）用 isKeyRoot 标记
     */
    val radicalMap: Map<Char, List<RadicalEntry>> = mapOf(
        'g' to listOf(
            RadicalEntry("王", true), RadicalEntry("戋", false), RadicalEntry("五", false),
            RadicalEntry("一", false)
        ),
        'f' to listOf(
            RadicalEntry("土", true), RadicalEntry("士", false), RadicalEntry("干", false),
            RadicalEntry("十", false), RadicalEntry("寸", false), RadicalEntry("雨", false),
            RadicalEntry("二", false)
        ),
        'd' to listOf(
            RadicalEntry("大", true), RadicalEntry("犬", false), RadicalEntry("三", false),
            RadicalEntry("古", false), RadicalEntry("石", false), RadicalEntry("厂", false),
            RadicalEntry("丆", false)
        ),
        's' to listOf(
            RadicalEntry("木", true), RadicalEntry("丁", false), RadicalEntry("西", false)
        ),
        'a' to listOf(
            RadicalEntry("工", true), RadicalEntry("戈", false), RadicalEntry("廿", false),
            RadicalEntry("艹", false), RadicalEntry("廾", false), RadicalEntry("匚", false),
            RadicalEntry("七", false)
        ),
        'h' to listOf(
            RadicalEntry("目", true), RadicalEntry("具", false), RadicalEntry("上", false),
            RadicalEntry("止", false), RadicalEntry("卜", false), RadicalEntry("丨", false),
            RadicalEntry("亅", false)
        ),
        'j' to listOf(
            RadicalEntry("日", true), RadicalEntry("曰", false), RadicalEntry("早", false),
            RadicalEntry("虫", false), RadicalEntry("刂", false), RadicalEntry("丨", false)
        ),
        'k' to listOf(
            RadicalEntry("口", true), RadicalEntry("川", false), RadicalEntry("川", false)
        ),
        'l' to listOf(
            RadicalEntry("田", true), RadicalEntry("甲", false), RadicalEntry("囗", false),
            RadicalEntry("四", false), RadicalEntry("罒", false), RadicalEntry("车", false),
            RadicalEntry("力", false)
        ),
        'm' to listOf(
            RadicalEntry("山", true), RadicalEntry("由", false), RadicalEntry("贝", false),
            RadicalEntry("几", false), RadicalEntry("冂", false)
        ),
        't' to listOf(
            RadicalEntry("禾", true), RadicalEntry("竹", false), RadicalEntry("彳", false),
            RadicalEntry("丿", false)
        ),
        'r' to listOf(
            RadicalEntry("白", true), RadicalEntry("手", false), RadicalEntry("扌", false),
            RadicalEntry("斤", false), RadicalEntry("两撇", false)
        ),
        'e' to listOf(
            RadicalEntry("月", true), RadicalEntry("用", false), RadicalEntry("乃", false),
            RadicalEntry("豕", false), RadicalEntry("勹", false), RadicalEntry("臼", false),
            RadicalEntry("艮", false), RadicalEntry("豸", false)
        ),
        'w' to listOf(
            RadicalEntry("人", true), RadicalEntry("亻", false), RadicalEntry("八", false),
            RadicalEntry("登头", false)
        ),
        'q' to listOf(
            RadicalEntry("金", true), RadicalEntry("钅", false), RadicalEntry("儿", false),
            RadicalEntry("夕", false), RadicalEntry("乂", false), RadicalEntry("勹", false)
        ),
        'y' to listOf(
            RadicalEntry("言", true), RadicalEntry("讠", false), RadicalEntry("文", false),
            RadicalEntry("方", false), RadicalEntry("丶", false), RadicalEntry("广", false)
        ),
        'u' to listOf(
            RadicalEntry("立", true), RadicalEntry("六", false), RadicalEntry("辛", false),
            RadicalEntry("门", false), RadicalEntry("冫", false), RadicalEntry("丬", false),
            RadicalEntry("广", false)
        ),
        'i' to listOf(
            RadicalEntry("水", true), RadicalEntry("氵", false), RadicalEntry("小", false),
            RadicalEntry("氺", false), RadicalEntry("兴头", false)
        ),
        'o' to listOf(
            RadicalEntry("火", true), RadicalEntry("灬", false), RadicalEntry("米", false),
            RadicalEntry("业头", false)
        ),
        'p' to listOf(
            RadicalEntry("之", true), RadicalEntry("辶", false), RadicalEntry("廴", false),
            RadicalEntry("冖", false), RadicalEntry("宀", false)
        ),
        'n' to listOf(
            RadicalEntry("已", true), RadicalEntry("己", false), RadicalEntry("巳", false),
            RadicalEntry("尸", false), RadicalEntry("忄", false), RadicalEntry("心", false),
            RadicalEntry("羽", false), RadicalEntry("乙", false), RadicalEntry("乚", false),
            RadicalEntry("尸", false)
        ),
        'b' to listOf(
            RadicalEntry("子", true), RadicalEntry("孑", false), RadicalEntry("阝", false),
            RadicalEntry("卩", false), RadicalEntry("了", false), RadicalEntry("耳", false),
            RadicalEntry("凵", false), RadicalEntry("廴", false)
        ),
        'v' to listOf(
            RadicalEntry("女", true), RadicalEntry("刀", false), RadicalEntry("九", false),
            RadicalEntry("彐", false), RadicalEntry("臼", false)
        ),
        'c' to listOf(
            RadicalEntry("又", true), RadicalEntry("巴", false), RadicalEntry("马", false),
            RadicalEntry("厶", false)
        ),
        'x' to listOf(
            RadicalEntry("幺", true), RadicalEntry("弓", false), RadicalEntry("匕", false),
            RadicalEntry("纟", false)
        ),
        'z' to listOf(
            RadicalEntry("z", true)
        )
    )

    /**
     * 简码映射：一级简码（高频字）
     * 按键 + 空格 = 该字
     */
    val level1SimpleCode: Map<Char, String> = mapOf(
        'g' to "一", 'f' to "地", 'd' to "在", 's' to "要", 'a' to "工",
        'h' to "上", 'j' to "是", 'k' to "中", 'l' to "国", 'm' to "同",
        't' to "和", 'r' to "的", 'e' to "有", 'w' to "人", 'q' to "我",
        'y' to "主", 'u' to "产", 'i' to "不", 'o' to "为", 'p' to "这",
        'n' to "民", 'b' to "了", 'v' to "发", 'c' to "以", 'x' to "经"
    )

    /**
     * 二级简码映射（常用字，只需输入前两码+空格）
     */
    val level2SimpleCode: Map<String, String> = buildMap {
        val pairs = listOf(
            "gf" to "五", "gg" to "于", "gd" to "天", "gs" to "末", "ga" to "开",
            "fh" to "过", "fj"到 "理", "fk" to "事", "fl" to "画", "fm" to "现",
            "gh" to "到", "gj" to "与", "gk" to "来", "gd" to "天", "gf" to "五",
            "hh" to "睛", "hj" to "旧", "hk" to "占", "hl" to "卤", "hm" to "贞",
            "jg" to "量", "jh" to "早", "jj" to "昌", "jk" to "蝇", "jl" to "曙",
            "kg" to "吗", "kh" to "跳", "kj" to "踢", "kk" to "啊", "kl" to "呗",
            "lg" to "辊", "lh" to "加", "lj" to "男", "lk" to "轴", "lm" to "边",
            "mg" to "册", "mh" to "财", "mj" to "央", "mk" to "见", "mm" to "骨",
            "tg" to "长", "th" to "自", "tj" to "得", "tk" to "各", "tl" to "笔",
            "rg" to "看", "rh" to "手", "rj" to "找", "rk" to "报", "rl" to "反",
            "eg" to "且", "eh" to "须", "ej" to "胆", "ek" to "肿", "el" to "肿",
            "wg" to "全", "wh" to "个", "wj" to "介", "wk" to "保", "wl" to "佃",
            "qg" to "错", "qh" to "外", "qj" to "锭", "qk" to "包", "ql" to "钉",
            "yg" to "证", "yh" to "计", "yj" to "就", "yk" to "应", "yl" to "亢",
            "ug" to "闰", "uh" to "半", "uj" to "间", "uk" to "部", "ul" to "曾",
            "ig" to "尖", "ih" to "少", "ij" to "当", "ik" to "兴", "il" to "光",
            "og" to "类", "oh" to "粘", "oj" to "烛", "ok" to "炎", "ol" to "迷",
            "pg" to "这", "ph" to "补", "pj" to "社", "pk" to "害", "pl" to "审",
            "ng" to "怀", "nh" to "收", "nj" to "慢", "nk" to "避", "nl" to "惭",
            "bg" to "卫", "bh" to "耻", "bj" to "阳", "bk" to "职", "bl" to "阵",
            "vg" to "好", "vh" to "巡", "vj" to "巢", "vk" to "妈", "vl" to "姑",
            "cg" to "难", "ch" to "双", "cj" to "对", "ck" to "台", "cl" to "桑",
            "xg" to "结", "xh" to "引", "xj" to "强", "xk" to "细", "xl" to "纲",
            "fg" to "城", "ff" to "寺", "fd" to "雪", "fs" to "某", "fa" to "功",
            "dd" to "三", "df" to "丰", "ds" to "百", "da" to "矿", "dw" to "厨",
            "ss" to "查", "sd" to "本", "sa" to "杠", "sw" to "楞"
        )
        pairs.forEach { (code, char) -> put(code, char) }
    }

    data class RadicalEntry(
        val radical: String,
        val isKeyRoot: Boolean
    )
}
