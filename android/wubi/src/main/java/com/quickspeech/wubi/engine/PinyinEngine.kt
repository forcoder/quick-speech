package com.quickspeech.wubi.engine

/**
 * 拼音引擎 - 支持全拼、简拼、模糊音匹配
 *
 * 设计参考搜狗拼音的混合输入模式：
 * - 全拼：zhongguo -> 中国
 * - 简拼：zg -> 中国
 * - 模糊音：zongguo -> 中国 (zh/z 模糊)
 * - 自动分词：zhongguo -> ["zhong", "guo"]
 */
class PinyinEngine {

    companion object {
        /** 模糊音配对表 */
        val FUZZY_PAIRS = mapOf(
            "zh" to "z", "z" to "zh",
            "ch" to "c", "c" to "ch",
            "sh" to "s", "s" to "sh",
            "n" to "l", "l" to "n",
            "in" to "ing", "ing" to "in",
            "en" to "eng", "eng" to "en"
        )

        /** 所有合法拼音音节 */
        val VALID_SYLLABLES: Set<String> = buildSet {
            addAll(listOf(
                "a", "ai", "an", "ang", "ao",
                "ba", "bai", "ban", "bang", "bao", "bei", "ben", "beng", "bi", "bian", "biao", "bie", "bin", "bing", "bo", "bu",
                "ca", "cai", "can", "cang", "cao", "ce", "cen", "ceng", "cha", "chai", "chan", "chang", "chao", "che", "chen", "cheng", "chi", "chong", "chou", "chu", "chua", "chuai", "chuan", "chuang", "chui", "chun", "chuo", "ci", "cong", "cou", "cu", "cuan", "cui", "cun", "cuo",
                "da", "dai", "dan", "dang", "dao", "de", "dei", "den", "deng", "di", "dia", "dian", "diao", "die", "ding", "diu", "dong", "dou", "du", "duan", "dui", "dun", "duo",
                "e", "ei", "en", "eng", "er",
                "fa", "fan", "fang", "fei", "fen", "feng", "fo", "fou", "fu",
                "ga", "gai", "gan", "gang", "gao", "ge", "gei", "gen", "geng", "gong", "gou", "gu", "gua", "guai", "guan", "guang", "gui", "gun", "guo",
                "ha", "hai", "han", "hang", "hao", "he", "hei", "hen", "heng", "hong", "hou", "hu", "hua", "huai", "huan", "huang", "hui", "hun", "huo",
                "ji", "jia", "jian", "jiang", "jiao", "jie", "jin", "jing", "jiong", "jiu", "ju", "juan", "jue", "jun",
                "ka", "kai", "kan", "kang", "kao", "ke", "kei", "ken", "keng", "kong", "kou", "ku", "kua", "kuai", "kuan", "kuang", "kui", "kun", "kuo",
                "la", "lai", "lan", "lang", "lao", "le", "lei", "leng", "li", "lia", "lian", "liang", "liao", "lie", "lin", "ling", "liu", "lo", "long", "lou", "lu", "luan", "lun", "luo", "lv", "lve",
                "ma", "mai", "man", "mang", "mao", "me", "mei", "men", "meng", "mi", "mian", "miao", "mie", "min", "ming", "miu", "mo", "mou", "mu",
                "na", "nai", "nan", "nang", "nao", "ne", "nei", "nen", "neng", "ni", "nian", "niang", "niao", "nie", "nin", "ning", "niu", "nong", "nou", "nu", "nuan", "nun", "nuo", "nv", "nve",
                "o", "ou",
                "pa", "pai", "pan", "pang", "pao", "pei", "pen", "peng", "pi", "pian", "piao", "pie", "pin", "ping", "po", "pou", "pu",
                "qi", "qia", "qian", "qiang", "qiao", "qie", "qin", "qing", "qiong", "qiu", "qu", "quan", "que", "qun",
                "ran", "rang", "rao", "re", "ren", "reng", "ri", "rong", "rou", "ru", "rua", "ruan", "rui", "run", "ruo",
                "sa", "sai", "san", "sang", "sao", "se", "sen", "seng", "sha", "shai", "shan", "shang", "shao", "she", "shei", "shen", "sheng", "shi", "shou", "shu", "shua", "shuai", "shuan", "shuang", "shui", "shun", "shuo", "si", "song", "sou", "su", "suan", "sui", "sun", "suo",
                "ta", "tai", "tan", "tang", "tao", "te", "tei", "teng", "ti", "tian", "tiao", "tie", "ting", "tong", "tou", "tu", "tuan", "tui", "tun", "tuo",
                "wa", "wai", "wan", "wang", "wei", "wen", "weng", "wo", "wu",
                "xi", "xia", "xian", "xiang", "xiao", "xie", "xin", "xing", "xiong", "xiu", "xu", "xuan", "xue", "xun",
                "ya", "yan", "yang", "yao", "ye", "yi", "yin", "ying", "yo", "yong", "you", "yu", "yuan", "yue", "yun",
                "za", "zai", "zan", "zang", "zao", "ze", "zei", "zen", "zeng", "zha", "zhai", "zhan", "zhang", "zhao", "zhe", "zhei", "zhen", "zheng", "zhi", "zhong", "zhou", "zhu", "zhua", "zhuai", "zhuan", "zhuang", "zhui", "zhun", "zhuo", "zi", "zong", "zou", "zu", "zuan", "zui", "zun", "zuo"
            ))
        }

        /** 声母表 */
        val INITIALS = listOf(
            "zh", "ch", "sh",
            "b", "p", "m", "f", "d", "t", "n", "l",
            "g", "k", "h", "j", "q", "x",
            "z", "c", "s", "r", "y", "w"
        )
    }

    private val pinyinDict: Map<String, List<String>> by lazy { buildPinyinDict() }

    /**
     * 搜索拼音候选词
     * @param code 拼音编码（全拼或简拼）
     * @return 候选汉字/词列表
     */
    fun search(code: String): List<String> {
        if (code.isBlank()) return emptyList()
        val lowerCode = code.lowercase()

        // 1. 精确匹配（全拼或简拼）
        pinyinDict[lowerCode]?.let { return it }

        // 2. 尝试分词后搜索
        val segments = segmentPinyin(lowerCode)
        if (segments.isNotEmpty() && segments[0].size > 1) {
            val combined = combineSegments(segments[0])
            if (combined.isNotEmpty()) return combined
        }

        // 3. 前缀匹配（输入过程中的实时候选）
        val prefixMatches = pinyinDict.filterKeys { it.startsWith(lowerCode) && it != lowerCode }
            .flatMap { it.value }
            .distinct()
        if (prefixMatches.isNotEmpty()) return prefixMatches.take(20)

        return emptyList()
    }

    /**
     * 模糊音搜索
     */
    fun searchFuzzy(code: String): List<String> {
        if (code.isBlank()) return emptyList()
        val lowerCode = code.lowercase()

        val exactResults = search(lowerCode)
        if (exactResults.isNotEmpty()) return exactResults

        val variants = generateFuzzyVariants(lowerCode)
        val results = mutableSetOf<String>()

        for (variant in variants) {
            results.addAll(search(variant))
        }

        return results.toList()
    }

    /**
     * 将连续拼音字符串分词为音节列表
     * 使用最大正向匹配算法
     * 例如："zhongguo" -> [["zhong", "guo"]]
     */
    fun segmentPinyin(input: String): List<List<String>> {
        if (input.isBlank()) return emptyList()
        val lowerInput = input.lowercase()
        val results = mutableListOf<List<String>>()
        segmentDfs(lowerInput, 0, mutableListOf(), results)
        return results.sortedBy { it.size }
    }

    private fun segmentDfs(
        input: String,
        start: Int,
        current: MutableList<String>,
        results: MutableList<List<String>>
    ) {
        if (start == input.length) {
            results.add(current.toList())
            return
        }
        val maxLen = minOf(6, input.length - start)
        for (len in maxLen downTo 1) {
            val syllable = input.substring(start, start + len)
            if (syllable in VALID_SYLLABLES) {
                current.add(syllable)
                segmentDfs(input, start + len, current, results)
                current.removeAt(current.size - 1)
            }
        }
    }

    private fun generateFuzzyVariants(code: String): List<String> {
        val variants = mutableListOf(code)
        for ((from, to) in FUZZY_PAIRS) {
            if (code.contains(from)) {
                variants.add(code.replace(from, to))
            }
        }
        val combined = mutableListOf<String>()
        for (variant in variants) {
            for ((from, to) in FUZZY_PAIRS) {
                if (variant.contains(from)) {
                    combined.add(variant.replace(from, to))
                }
            }
        }
        variants.addAll(combined)
        return variants.distinct()
    }

    private fun combineSegments(segments: List<String>): List<String> {
        if (segments.isEmpty()) return emptyList()
        val candidatesPerSegment = segments.map { syllable ->
            pinyinDict[syllable] ?: listOf(syllable)
        }
        if (segments.size == 2) {
            val twoCharWords = findWord("$segments[0]$segments[1]",
                "${segments[0]}${segments[1]}")
            if (twoCharWords.isNotEmpty()) return twoCharWords
        }
        val result = mutableListOf<String>()
        val firstChars = candidatesPerSegment.map { it.firstOrNull() ?: "" }
        if (firstChars.all { it.isNotEmpty() }) {
            result.add(firstChars.joinToString(""))
        }
        return result.distinct()
    }

    private fun findWord(vararg keys: String): List<String> {
        for (key in keys) {
            pinyinDict[key.lowercase()]?.let { return it }
        }
        return emptyList()
    }

    /**
     * 获取简拼，例如 "zhongguo" -> "zg"
     */
    fun getAbbreviation(code: String): String {
        val segments = segmentPinyin(code)
        if (segments.isEmpty()) return code
        return segments[0].map { it.first() }.joinToString("")
    }

    /**
     * 构建拼音字典 - 包含常用单字和常用词语
     */
    private fun buildPinyinDict(): Map<String, List<String>> {
        return buildMap {
            // ===== 常用单字 =====
            put("a", listOf("阿", "啊", "呵"))
            put("ai", listOf("爱", "碍", "哀", "埃", "挨", "艾", "唉", "矮", "癌"))
            put("an", listOf("安", "按", "案", "暗", "岸", "氨", "庵", "鞍"))
            put("ang", listOf("昂", "盎"))
            put("ao", listOf("奥", "傲", "澳", "熬", "袄", "凹"))
            put("ba", listOf("把", "八", "吧", "巴", "爸", "拔", "罢", "坝", "芭", "霸"))
            put("bai", listOf("白", "百", "败", "拜", "柏", "摆"))
            put("ban", listOf("办", "半", "班", "般", "版", "板", "伴", "搬", "扮"))
            put("bang", listOf("帮", "邦", "榜", "绑", "棒", "傍", "膀", "磅"))
            put("bao", listOf("报", "保", "包", "宝", "抱", "暴", "薄", "爆", "胞", "饱"))
            put("bei", listOf("被", "北", "备", "背", "悲", "辈", "杯", "倍", "贝"))
            put("ben", listOf("本", "奔", "笨"))
            put("beng", listOf("崩", "绷", "蹦", "迸", "泵"))
            put("bi", listOf("比", "必", "笔", "毕", "币", "避", "鼻", "壁", "逼", "彼", "秘", "闭", "碧", "弊"))
            put("bian", listOf("便", "边", "变", "编", "遍", "辩", "辨", "贬", "鞭", "扁"))
            put("biao", listOf("表", "标", "彪", "镖", "膘"))
            put("bie", listOf("别", "憋", "瘪"))
            put("bin", listOf("宾", "滨", "彬", "斌", "濒", "殡", "缤", "鬓"))
            put("bing", listOf("并", "兵", "病", "冰", "丙", "饼", "柄", "秉", "炳"))
            put("bo", listOf("波", "博", "播", "伯", "薄", "勃", "拨", "玻", "柏", "剥", "泊"))
            put("bu", listOf("不", "部", "布", "步", "补", "捕", "怖", "卜", "簿"))
            put("ca", listOf("擦"))
            put("cai", listOf("才", "采", "财", "材", "彩", "菜", "裁", "猜", "蔡"))
            put("can", listOf("参", "残", "餐", "惨", "灿", "蚕", "惭"))
            put("cang", listOf("藏", "仓", "苍", "舱", "沧"))
            put("cao", listOf("草", "操", "曹", "槽", "糙"))
            put("ce", listOf("策", "测", "侧", "册", "厕"))
            put("cen", listOf("参", "岑"))
            put("ceng", listOf("曾", "层", "蹭"))
            put("cha", listOf("查", "察", "差", "茶", "插", "叉", "刹", "岔", "诧"))
            put("chai", listOf("差", "拆", "柴", "豺"))
            put("chan", listOf("产", "颤", "缠", "禅", "蝉", "馋", "铲", "阐", "搀"))
            put("chang", listOf("长", "常", "场", "厂", "唱", "尝", "偿", "昌", "畅", "肠", "倡", "敞"))
            put("chao", listOf("超", "朝", "潮", "炒", "吵", "抄", "嘲", "钞", "巢"))
            put("che", listOf("车", "彻", "撤", "扯", "澈"))
            put("chen", listOf("称", "陈", "沉", "晨", "尘", "臣", "趁", "衬", "辰"))
            put("cheng", listOf("成", "城", "程", "承", "称", "盛", "乘", "呈", "诚", "撑", "惩"))
            put("chi", listOf("吃", "持", "迟", "尺", "赤", "池", "痴", "齿", "斥", "驰", "耻", "翅"))
            put("chong", listOf("重", "冲", "充", "虫", "崇", "宠", "憧", "忡"))
            put("chou", listOf("抽", "愁", "丑", "臭", "仇", "筹", "酬", "绸", "瞅", "稠"))
            put("chu", listOf("出", "处", "初", "除", "楚", "触", "储", "厨", "畜", "橱", "矗"))
            put("chuai", listOf("揣", "啜", "踹"))
            put("chuan", listOf("传", "川", "穿", "船", "串", "喘"))
            put("chuang", listOf("创", "窗", "床", "闯", "疮", "怆"))
            put("chui", listOf("吹", "垂", "锤", "捶", "椎", "槌"))
            put("chun", listOf("春", "纯", "唇", "蠢", "醇", "淳", "椿"))
            put("chuo", listOf("绰", "戳", "啜", "辍"))
            put("ci", listOf("此", "次", "词", "差", "刺", "辞", "慈", "磁", "赐", "瓷"))
            put("cong", listOf("从", "匆", "聪", "丛", "葱", "囱"))
            put("cou", listOf("凑", "揍"))
            put("cu", listOf("粗", "促", "醋", "簇", "蹴"))
            put("cuan", listOf("窜", "篡", "蹿", "攒"))
            put("cui", listOf("催", "脆", "翠", "粹", "摧", "崔", "淬"))
            put("cun", listOf("村", "存", "寸", "忖"))
            put("cuo", listOf("错", "措", "挫", "搓", "磋", "撮"))
            put("da", listOf("大", "打", "达", "答", "搭", "哒"))
            put("dai", listOf("大", "代", "带", "待", "袋", "戴", "呆", "逮", "贷", "怠"))
            put("dan", listOf("但", "单", "担", "弹", "淡", "蛋", "丹", "胆", "旦", "诞"))
            put("dang", listOf("当", "党", "挡", "荡", "档", "裆"))
            put("dao", listOf("到", "道", "导", "倒", "刀", "岛", "盗", "蹈", "悼", "稻"))
            put("de", listOf("的", "得", "德", "地"))
            put("dei", listOf("得"))
            put("deng", listOf("等", "登", "灯", "邓", "瞪", "凳", "蹬", "澄"))
            put("di", listOf("地", "第", "低", "底", "弟", "帝", "敌", "递", "滴", "迪", "堤", "笛", "蒂"))
            put("dia", listOf("嗲"))
            put("dian", listOf("点", "电", "店", "典", "颠", "殿", "甸", "垫", "淀", "惦"))
            put("diao", listOf("调", "掉", "钓", "雕", "吊", "叼", "刁", "凋"))
            put("die", listOf("跌", "叠", "蝶", "爹", "谍", "碟", "迭", "喋"))
            put("ding", listOf("定", "顶", "订", "丁", "盯", "钉", "鼎", "叮", "锭"))
            put("diu", listOf("丢"))
            put("dong", listOf("动", "东", "懂", "冬", "董", "冻", "洞", "栋", "咚"))
            put("dou", listOf("都", "斗", "豆", "抖", "兜", "陡", "逗", "窦"))
            put("du", listOf("都", "读", "独", "度", "毒", "杜", "肚", "渡", "督", "赌", "堵"))
            put("duan", listOf("断", "段", "短", "端", "缎", "锻", "煅"))
            put("dui", listOf("对", "队", "堆", "兑", "碓"))
            put("dun", listOf("顿", "吨", "蹲", "盾", "敦", "墩", "囤", "钝"))
            put("duo", listOf("多", "夺", "朵", "躲", "舵", "堕", "惰", "踱", "咄"))
            put("e", listOf("额", "恶", "饿", "哦", "鹅", "俄", "扼", "愕", "遏", "噩"))
            put("ei", listOf("诶"))
            put("en", listOf("恩", "摁"))
            put("eng", listOf("鞥"))
            put("er", listOf("而", "儿", "耳", "二", "尔", "饵", "洱", "贰"))
            put("fa", listOf("发", "法", "罚", "乏", "伐", "阀", "筏", "砝"))
            put("fan", listOf("反", "饭", "犯", "翻", "范", "烦", "繁", "泛", "番", "凡", "返", "贩"))
            put("fang", listOf("方", "放", "房", "防", "访", "芳", "仿", "纺", "坊", "妨", "肪"))
            put("fei", listOf("非", "飞", "费", "肥", "废", "肺", "匪", "啡", "菲", "沸", "妃", "匪"))
            put("fen", listOf("分", "份", "纷", "奋", "愤", "粉", "芬", "坟", "氛", "焚", "粪"))
            put("feng", listOf("风", "封", "峰", "丰", "凤", "奉", "疯", "锋", "逢", "缝", "讽", "冯"))
            put("fo", listOf("佛"))
            put("fou", listOf("否", "缶"))
            put("fu", listOf("服", "夫", "父", "府", "复", "副", "负", "富", "福", "付", "妇", "附", "幅", "伏", "扶", "浮", "符", "腐", "腹", "覆", "弗", "拂", "赋", "辐", "缚"))
            put("ga", listOf("咖", "嘎", "噶", "尕"))
            put("gai", listOf("改", "该", "概", "盖", "丐", "钙", "溉"))
            put("gan", listOf("感", "干", "敢", "赶", "甘", "肝", "杆", "干", "赣", "柑", "竿"))
            put("gang", listOf("刚", "港", "钢", "岗", "纲", "缸", "扛", "肛", "杠"))
            put("gao", listOf("高", "告", "稿", "搞", "糕", "膏", "羔", "镐", "睾"))
            put("ge", listOf("个", "格", "歌", "革", "哥", "隔", "阁", "葛", "割", "戈", "鸽", "胳", "搁"))
            put("gei", listOf("给"))
            put("gen", listOf("根", "跟", "亘", "艮"))
            put("geng", listOf("更", "耕", "耿", "梗", "庚", "羹", "埂"))
            put("gong", listOf("工", "公", "功", "共", "供", "攻", "宫", "恭", "巩", "龚", "弓", "躬"))
            put("gou", listOf("够", "购", "狗", "构", "沟", "勾", "钩", "苟", "垢", "篝"))
            put("gu", listOf("古", "故", "股", "顾", "固", "骨", "姑", "谷", "孤", "鼓", "古", "雇", "辜", "咕", "沽", "箍"))
            put("gua", listOf("挂", "瓜", "刮", "寡", "褂", "剐", "卦"))
            put("guai", listOf("怪", "乖", "拐", "掴"))
            put("guan", listOf("关", "观", "管", "官", "馆", "惯", "冠", "贯", "灌", "罐", "棺", "倌"))
            put("guang", listOf("光", "广", "逛", "犷"))
            put("gui", listOf("规", "归", "贵", "鬼", "桂", "柜", "跪", "轨", "龟", "硅", "瑰", "诡"))
            put("gun", listOf("滚", "棍"))
            put("guo", listOf("国", "过", "果", "锅", "郭", "裹"))
            put("ha", listOf("哈", "蛤"))
            put("hai", listOf("还", "海", "害", "孩", "咳", "亥", "骇", "骸"))
            put("han", listOf("汉", "喊", "寒", "含", "汗", "韩", "罕", "憾", "翰", "旱", "涵", "悍", "焊"))
            put("hang", listOf("行", "航", "杭", "巷", "夯", "吭"))
            put("hao", listOf("好", "号", "毫", "豪", "耗", "浩", "郝", "嚎", "壕"))
            put("he", listOf("和", "何", "合", "河", "核", "贺", "赫", "荷", "盒", "喝", "禾", "褐", "鹤"))
            put("hei", listOf("黑", "嘿"))
            put("hen", listOf("很", "恨", "狠", "痕"))
            put("heng", listOf("行", "横", "恒", "哼", "衡", "亨"))
            put("hong", listOf("红", "洪", "宏", "轰", "虹", "鸿", "弘", "烘"))
            put("hou", listOf("后", "候", "厚", "侯", "喉", "吼", "猴"))
            put("hu", listOf("和", "护", "户", "湖", "呼", "胡", "互", "虎", "忽", "糊", "核", "狐", "沪", "壶", "蝴", "弧", "葫", "唬"))
            put("hua", listOf("化", "话", "花", "华", "划", "画", "滑", "哗", "桦"))
            put("huai", listOf("怀", "坏", "淮", "徊"))
            put("huan", listOf("还", "欢", "换", "环", "缓", "患", "幻", "唤", "宦", "焕", "涣"))
            put("huang", listOf("黄", "皇", "荒", "煌", "晃", "慌", "惶", "凰", "蝗"))
            put("hui", listOf("会", "回", "汇", "挥", "灰", "辉", "毁", "悔", "惠", "慧", "绘", "讳", "贿", "晦", "秽", "卉", "溃"))
            put("hun", listOf("婚", "混", "魂", "昏", "浑", "荤"))
            put("huo", listOf("和", "活", "或", "火", "获", "货", "伙", "惑", "霍", "祸", "豁"))
            put("ji", listOf("就", "己", "机", "即", "几", "及", "级", "极", "给", "基", "计", "集", "记", "济", "继", "技", "际", "季", "激", "吉", "积", "纪", "寄", "迹", "鸡", "疾", "挤", "剂", "祭", "寂", "绩", "饥", "姬"))
            put("jia", listOf("家", "加", "价", "假", "甲", "架", "驾", "夹", "佳", "嫁", "嘉", "贾", "颊", "稼"))
            put("jian", listOf("见", "间", "建", "件", "简", "坚", "检", "健", "渐", "减", "剑", "监", "兼", "尖", "肩", "艰", "荐", "鉴", "践", "奸", "箭", "舰", "剪", "捡", "贱", "溅"))
            put("jiang", listOf("将", "强", "江", "讲", "降", "蒋", "奖", "姜", "僵", "疆", "匠", "酱", "浆"))
            put("jiao", listOf("叫", "教", "交", "角", "脚", "较", "觉", "校", "焦", "骄", "郊", "轿", "浇", "椒", "嚼", "搅", "绞", "饺", "缴", "酵"))
            put("jie", listOf("结", "接", "节", "界", "解", "姐", "介", "街", "阶", "截", "杰", "皆", "洁", "戒", "揭", "捷", "劫", "竭", "藉"))
            put("jin", listOf("进", "今", "金", "近", "尽", "紧", "仅", "禁", "劲", "津", "斤", "筋", "锦", "谨", "浸", "晋"))
            put("jing", listOf("经", "京", "精", "竟", "惊", "境", "静", "景", "警", "睛", "竞", "净", "劲", "径", "晶", "荆", "鲸", "井", "颈", "敬", "靖"))
            put("jiong", listOf("窘", "炯"))
            put("jiu", listOf("就", "九", "究", "久", "酒", "旧", "救", "纠", "揪", "舅", "咎"))
            put("ju", listOf("据", "且", "局", "举", "句", "具", "居", "剧", "巨", "聚", "拒", "俱", "惧", "柜", "矩", "拘", "菊", "鞠", "驹"))
            put("juan", listOf("卷", "捐", "圈", "娟", "倦", "绢", "鹃"))
            put("jue", listOf("决", "绝", "觉", "角", "爵", "掘", "崛", "倔", "嚼", "诀", "厥"))
            put("jun", listOf("军", "君", "均", "菌", "俊", "峻", "竣", "骏"))
            put("ka", listOf("卡", "咖", "喀"))
            put("kai", listOf("开", "凯", "慨", "楷"))
            put("kan", listOf("看", "刊", "砍", "堪", "勘", "坎"))
            put("kang", listOf("抗", "康", "慷", "扛", "炕", "亢"))
            put("kao", listOf("考", "靠", "烤", "拷"))
            put("ke", listOf("可", "克", "科", "客", "刻", "课", "颗", "柯", "渴", "棵", "磕", "咳", "壳", "苛"))
            put("ken", listOf("肯", "恳", "啃", "垦"))
            put("keng", listOf("坑", "吭"))
            put("kong", listOf("空", "孔", "恐", "控", "倥"))
            put("kou", listOf("口", "扣", "寇", "抠", "叩"))
            put("ku", listOf("苦", "库", "裤", "酷", "枯", "窟", "哭", "库"))
            put("kua", listOf("跨", "夸", "垮", "挎"))
            put("kuai", listOf("快", "块", "筷", "侩", "蒯"))
            put("kuan", listOf("宽", "款"))
            put("kuang", listOf("况", "狂", "矿", "框", "旷", "眶", "筐", "匡"))
            put("kui", listOf("亏", "愧", "溃", "窥", "魁", "葵", "馈", "盔", "傀"))
            put("kun", listOf("困", "昆", "坤", "捆", "鲲"))
            put("kuo", listOf("扩", "阔", "括", "廓"))
            put("la", listOf("拉", "啦", "辣", "蜡", "喇", "垃", "落"))
            put("lai", listOf("来", "赖", "莱", "籁", "徕"))
            put("lan", listOf("兰", "蓝", "栏", "烂", "懒", "览", "滥", "拦", "篮", "澜", "揽"))
            put("lang", listOf("浪", "郎", "朗", "狼", "廊", "琅", "榔"))
            put("lao", listOf("老", "劳", "牢", "捞", "姥", "涝", "酪", "唠"))
            put("le", listOf("了", "乐", "勒", "肋", "叻"))
            put("lei", listOf("类", "累", "泪", "雷", "蕾", "垒", "磊", "擂", "肋"))
            put("leng", listOf("冷", "愣", "棱", "楞"))
            put("li", listOf("里", "理", "力", "立", "利", "李", "离", "丽", "历", "礼", "例", "厉", "励", "黎", "璃", "莉", "粒", "隶"))
            put("lia", listOf("俩"))
            put("lian", listOf("联", "连", "脸", "练", "恋", "怜", "莲", "廉", "帘", "链", "敛", "炼"))
            put("liang", listOf("两", "量", "良", "亮", "凉", "梁", "粮", "谅", "晾", "靓"))
            put("liao", listOf("了", "料", "聊", "疗", "辽", "僚", "寥", "撩", "燎", "缭"))
            put("lie", listOf("列", "烈", "裂", "猎", "劣", "咧", "冽"))
            put("lin", listOf("林", "临", "邻", "淋", "琳", "霖", "磷", "凛", "吝", "赁"))
            put("ling", listOf("领", "令", "另", "零", "灵", "龄", "凌", "玲", "铃", "陵", "岭", "菱", "伶", "羚"))
            put("liu", listOf("六", "流", "留", "刘", "柳", "溜", "瘤", "硫", "浏", "榴"))
            put("lo", listOf("咯"))
            put("long", listOf("龙", "隆", "笼", "拢", "聋", "垄", "胧", "珑", "窿"))
            put("lou", listOf("楼", "漏", "搂", "陋", "露", "娄", "篓"))
            put("lu", listOf("路", "陆", "录", "露", "鲁", "炉", "卢", "鹿", "禄", "芦", "庐", "碌", "麓", "虏"))
            put("luan", listOf("乱", "卵", "峦", "挛", "孪", "栾"))
            put("lun", listOf("论", "轮", "伦", "沦", "仑", "抡"))
            put("luo", listOf("落", "罗", "络", "洛", "螺", "裸", "萝", "锣", "箩", "骡"))
            put("lv", listOf("绿", "旅", "律", "率", "虑", "履", "侣", "驴", "吕", "缕", "屡"))
            put("lve", listOf("略", "掠"))
            put("ma", listOf("马", "吗", "妈", "麻", "骂", "码", "玛", "蚂", "嘛"))
            put("mai", listOf("买", "卖", "麦", "脉", "埋", "迈", "霾"))
            put("man", listOf("满", "慢", "漫", "曼", "蛮", "馒", "瞒", "蔓", "幔", "谩"))
            put("mang", listOf("忙", "盲", "茫", "芒", "莽", "氓", "蟒"))
            put("mao", listOf("毛", "冒", "猫", "貌", "矛", "帽", "茅", "茂", "贸", "髦", "锚", "卯"))
            put("me", listOf("么", "麽"))
            put("mei", listOf("没", "美", "每", "妹", "梅", "眉", "媒", "煤", "霉", "玫", "枚", "昧", "媚", "魅"))
            put("men", listOf("们", "门", "闷", "扪", "焖", "懑"))
            put("meng", listOf("梦", "蒙", "猛", "盟", "孟", "朦", "萌", "锰", "檬", "勐"))
            put("mi", listOf("米", "密", "迷", "秘", "蜜", "谜", "弥", "弥", "眯", "靡", "糜", "泌", "觅"))
            put("mian", listOf("面", "免", "棉", "眠", "绵", "勉", "缅", "冕", "娩", "腼"))
            put("miao", listOf("秒", "妙", "描", "苗", "庙", "瞄", "渺", "淼", "藐", "缪"))
            put("mie", listOf("灭", "蔑", "咩", "篾"))
            put("min", listOf("民", "敏", "皿", "闽", "悯", "珉", "抿"))
            put("ming", listOf("名", "明", "命", "鸣", "铭", "冥", "茗", "溟", "酩"))
            put("miu", listOf("谬"))
            put("mo", listOf("么", "没", "模", "莫", "默", "摸", "摩", "磨", "魔", "抹", "墨", "漠", "陌", "沫", "膜", "摹", "蘑"))
            put("mou", listOf("某", "谋", "眸", "缪", "牟"))
            put("mu", listOf("目", "母", "木", "幕", "慕", "牧", "墓", "穆", "姆", "牡", "亩", "沐", "睦"))
            put("na", listOf("那", "拿", "哪", "纳", "娜", "呐", "钠", "捺"))
            put("nai", listOf("乃", "奶", "耐", "奈", "氖", "萘"))
            put("nan", listOf("南", "男", "难", "喃", "楠", "赧"))
            put("nang", listOf("囊", "囔", "馕", "攮"))
            put("nao", listOf("脑", "闹", "恼", "挠", "瑙", "淖", "孬"))
            put("ne", listOf("呢", "哪", "讷"))
            put("nei", listOf("内", "那", "馁"))
            put("nen", listOf("嫩", "恁"))
            put("neng", listOf("能"))
            put("ni", listOf("你", "尼", "泥", "拟", "逆", "妮", "腻", "匿", "溺", "倪", "霓", "昵"))
            put("nian", listOf("年", "念", "粘", "碾", "廿", "捻", "蔫", "撵"))
            put("niang", listOf("娘", "酿"))
            put("niao", listOf("鸟", "尿", "袅", "茑"))
            put("nie", listOf("捏", "聂", "孽", "涅", "啮", "镊", "镍", "蹑"))
            put("nin", listOf("您", "恁"))
            put("ning", listOf("宁", "凝", "拧", "柠", "狞", "泞", "咛", "佞"))
            put("niu", listOf("牛", "扭", "纽", "纽", "拗", "妞"))
            put("nong", listOf("农", "浓", "弄", "脓", "侬"))
            put("nou", listOf("耨"))
            put("nu", listOf("努", "怒", "奴", "弩", "驽", "孥"))
            put("nuan", listOf("暖"))
            put("nun", listOf("黁"))
            put("nuo", listOf("诺", "挪", "懦", "糯", "喏", "搦"))
            put("nv", listOf("女", "钕", "恧"))
            put("nve", listOf("虐", "疟"))
            put("o", listOf("哦", "噢", "喔"))
            put("ou", listOf("欧", "偶", "殴", "鸥", "藕", "呕", "讴", "瓯"))
            put("pa", listOf("怕", "爬", "帕", "趴", "啪", "扒", "耙", "琶"))
            put("pai", listOf("排", "牌", "派", "拍", "迫", "徘", "湃", "俳"))
            put("pan", listOf("判", "盘", "番", "攀", "盼", "叛", "畔", "潘", "攀", "磐", "蹒"))
            put("pang", listOf("旁", "胖", "庞", "膀", "磅", "乓", "镑", "彷"))
            put("pao", listOf("跑", "炮", "泡", "抛", "袍", "刨", "咆", "狍", "疱"))
            put("pei", listOf("配", "培", "陪", "佩", "赔", "沛", "裴", "胚", "呸", "帔"))
            put("pen", listOf("喷", "盆", "湓"))
            put("peng", listOf("朋", "碰", "彭", "鹏", "捧", "蓬", "膨", "篷", "砰", "烹", "抨", "硼"))
            put("pi", listOf("批", "皮", "匹", "屁", "疲", "脾", "劈", "披", "辟", "啤", "僻", "譬", "坯", "癖", "痞", "琵", "毗"))
            put("pian", listOf("片", "篇", "偏", "骗", "翩", "扁", "便", "骈", "胼", "蹁"))
            put("piao", listOf("票", "漂", "飘", "瓢", "嫖", "瞟", "剽", "缥", "殍"))
            put("pie", listOf("撇", "瞥", "苤", "氕"))
            put("pin", listOf("品", "贫", "频", "拼", "聘", "嫔", "颦", "牝"))
            put("ping", listOf("平", "评", "凭", "苹", "瓶", "萍", "屏", "坪", "乒", "娉", "俜"))
            put("po", listOf("破", "迫", "坡", "颇", "泼", "婆", "泊", "魄", "粕", "珀", "叵"))
            put("pou", listOf("剖", "裒"))
            put("pu", listOf("普", "扑", "铺", "朴", "葡", "蒲", "浦", "朴", "圃", "谱", "瀑", "曝", "脯", "菩", "溥"))
            put("qi", listOf("起", "其", "期", "气", "七", "器", "企", "奇", "旗", "汽", "妻", "弃", "启", "骑", "棋", "齐", "岂", "契", "泣", "祈", "漆", "栖", "凄", "歧"))
            put("qia", listOf("恰", "洽", "掐", "袷", "髂"))
            put("qian", listOf("前", "千", "钱", "签", "欠", "浅", "迁", "潜", "牵", "谦", "遣", "歉", "纤", "嵌", "乾", "黔", "倩", "茜"))
            put("qiang", listOf("强", "墙", "枪", "抢", "腔", "悄", "呛", "羌", "蔷", "羟", "跄"))
            put("qiao", listOf("桥", "巧", "悄", "敲", "瞧", "乔", "侨", "俏", "壳", "翘", "峭", "撬", "樵", "谯", "荞"))
            put("qie", listOf("且", "切", "窃", "怯", "茄", "趄", "惬", "锲", "妾"))
            put("qin", listOf("亲", "琴", "侵", "勤", "秦", "禽", "寝", "擒", "钦", "芹", "沁", "覃", "矜"))
            put("qing", listOf("情", "请", "清", "青", "轻", "晴", "庆", "倾", "卿", "顷", "氢", "擎", "氰", "蜻", "罄", "磬"))
            put("qiong", listOf("穷", "琼", "穹", "茕", "邛", "蛩"))
            put("qiu", listOf("求", "球", "秋", "丘", "邱", "囚", "酋", "裘", "龟", "泅", "鳅"))
            put("qu", listOf("去", "取", "区", "曲", "趣", "屈", "驱", "趋", "渠", "躯", "娶", "觑", "瞿", "衢", "蛐", "祛", "蛆"))
            put("quan", listOf("全", "权", "圈", "劝", "泉", "拳", "犬", "券", "诠", "荃", "蜷", "醛", "铨", "颧"))
            put("que", listOf("却", "确", "缺", "雀", "鹊", "阙", "炔", "榷", "瘸"))
            put("qun", listOf("群", "裙", "逡"))
            put("ran", listOf("然", "染", "燃", "冉", "苒", "髯"))
            put("rang", listOf("让", "嚷", "壤", "攘", "瓤", "禳"))
            put("rao", listOf("绕", "扰", "饶", "娆", "桡"))
            put("re", listOf("热", "惹", "喏"))
            put("ren", listOf("人", "任", "认", "忍", "仁", "刃", "韧", "纫", "饪", "壬", "仞", "葚"))
            put("reng", listOf("仍", "扔"))
            put("ri", listOf("日"))
            put("rong", listOf("容", "荣", "融", "绒", "溶", "熔", "蓉", "冗", "茸", "榕", "狨"))
            put("rou", listOf("肉", "柔", "揉", "蹂", "鞣"))
            put("ru", listOf("如", "入", "儒", "乳", "辱", "汝", "茹", "褥", "孺", "濡", "蠕", "嚅"))
            put("ruan", listOf("软", "阮", "朊"))
            put("rui", listOf("瑞", "锐", "蕊", "睿", "芮", "蚋", "枘"))
            put("run", listOf("润", "闰"))
            put("ruo", listOf("若", "弱", "偌", "箬"))
            put("sa", listOf("撒", "洒", "萨", "仨", "卅", "飒"))
            put("sai", listOf("赛", "塞", "腮", "噻", "鳃"))
            put("san", listOf("三", "散", "伞", "叁", "糁", "毵"))
            put("sang", listOf("桑", "丧", "嗓", "搡", "颡"))
            put("sao", listOf("扫", "嫂", "骚", "搔", "臊", "缫", "瘙"))
            put("se", listOf("色", "塞", "涩", "瑟", "啬", "穑", "铯"))
            put("sen", listOf("森"))
            put("seng", listOf("僧"))
            put("sha", listOf("杀", "沙", "啥", "傻", "砂", "纱", "刹", "莎", "煞", "杉", "鲨", "霎"))
            put("shai", listOf("晒", "筛", "色"))
            put("shan", listOf("山", "善", "闪", "衫", "扇", "删", "杉", "珊", "陕", "煽", "擅", "膳", "讪", "汕", "疝", "苫", "鳝"))
            put("shang", listOf("上", "商", "伤", "尚", "赏", "裳", "晌", "觞", "熵", "垧"))
            put("shao", listOf("少", "绍", "烧", "稍", "勺", "哨", "邵", "梢", "捎", "芍", "韶", "苕", "蛸"))
            put("she", listOf("社", "设", "射", "舍", "舌", "蛇", "摄", "涉", "赦", "慑", "奢", "赊", "猞"))
        }
    }
}

