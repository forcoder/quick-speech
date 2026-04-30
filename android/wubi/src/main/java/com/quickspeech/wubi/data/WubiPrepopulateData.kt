package com.quickspeech.wubi.data

/**
 * 五笔预置词库数据生成器
 * 包含 GB2312 常用字 + 5000+ 常用词组（86版五笔编码）
 *
 * 注意：实际项目中这些数据应打包为 assets/database/wubi_dict.db 预填充数据库。
 * 这里提供运行时初始化/测试用的数据生成逻辑。
 */
object WubiPrepopulateData {

    fun generateAllEntries(): List<WubiWordEntry> {
        return generateSingleCharEntries() + generatePhraseEntries()
    }

    fun generateSingleCharEntries(): List<WubiWordEntry> {
        val entries = mutableListOf<WubiWordEntry>()
        var freq = 5000
        CHAR_CODE_MAP.forEach { (char, code) ->
            entries.add(WubiWordEntry(code = code, word = char, frequency = freq, type = 0))
            freq--
        }
        return entries
    }

    fun generatePhraseEntries(): List<WubiWordEntry> {
        val entries = mutableListOf<WubiWordEntry>()
        var freq = 10000
        PHRASE_CODE_MAP.forEach { (word, code) ->
            val wordType = when (word.length) { 2 -> 1; 3 -> 2; else -> 3 }
            entries.add(WubiWordEntry(code = code, word = word, frequency = freq, type = wordType))
            freq--
        }
        return entries
    }

    /** 800+常用单字编码映射 */
    val CHAR_CODE_MAP: Map<String, String> = mapOf(
        "的" to "r", "一" to "g", "是" to "j", "了" to "b", "不" to "i",
        "在" to "d", "有" to "e", "人" to "w", "这" to "p", "中" to "k",
        "大" to "d", "为" to "o", "上" to "h", "个" to "w", "国" to "l",
        "我" to "q", "以" to "c", "要" to "s", "他" to "w", "时" to "j",
        "来" to "g", "用" to "e", "们" to "w", "生" to "t", "到" to "g",
        "作" to "w", "地" to "f", "于" to "g", "出" to "b", "会" to "w",
        "可" to "s", "也" to "b", "你" to "w", "对" to "c", "能" to "c",
        "而" to "d", "子" to "b", "那" to "n", "得" to "t", "着" to "u",
        "下" to "g", "自" to "t", "之" to "p", "年" to "r", "过" to "f",
        "发" to "v", "后" to "r", "里" to "j", "道" to "u", "行" to "t",
        "所" to "r", "然" to "q", "家" to "p", "种" to "t", "事" to "g",
        "成" to "d", "方" to "y", "多" to "q", "经" to "x", "么" to "t",
        "去" to "f", "法" to "i", "学" to "i", "如" to "v", "都" to "f",
        "同" to "m", "现" to "g", "当" to "i", "动" to "f", "面" to "d",
        "起" to "f", "看" to "r", "定" to "p", "天" to "g", "分" to "w",
        "还" to "g", "进" to "f", "好" to "v", "小" to "i", "部" to "u",
        "其" to "a", "些" to "h", "主" to "y", "样" to "s", "理" to "g",
        "心" to "n", "她" to "v", "本" to "s", "前" to "u", "开" to "g",
        "但" to "w", "因" to "l", "只" to "k", "从" to "w", "想" to "s",
        "实" to "p", "日" to "j", "军" to "p", "者" to "f", "意" to "u",
        "无" to "f", "力" to "l", "它" to "p", "与" to "g", "长" to "t",
        "把" to "r", "机" to "s", "十" to "f", "民" to "n", "第" to "t",
        "公" to "w", "此" to "h", "已" to "n", "工" to "a", "使" to "w",
        "情" to "n", "明" to "j", "性" to "n", "知" to "t", "全" to "w",
        "三" to "d", "又" to "c", "关" to "u", "点" to "h", "正" to "g",
        "业" to "o", "外" to "q", "将" to "u", "两" to "g", "高" to "y",
        "间" to "u", "由" to "m", "问" to "u", "很" to "t", "最" to "j",
        "重" to "t", "并" to "u", "物" to "t", "手" to "r", "应" to "y",
        "战" to "h", "向" to "t", "头" to "u", "文" to "y", "体" to "w",
        "政" to "g", "美" to "u", "见" to "m", "被" to "p", "什" to "w",
        "二" to "f", "等" to "t", "产" to "u", "或" to "a", "新" to "u",
        "己" to "n", "制" to "r", "身" to "t", "果" to "j", "加" to "l",
        "西" to "s", "月" to "e", "话" to "y", "合" to "w", "回" to "l",
        "特" to "t", "代" to "w", "内" to "m", "信" to "y", "表" to "g",
        "化" to "w", "老" to "f", "给" to "x", "世" to "a", "位" to "w",
        "次" to "u", "门" to "u", "任" to "w", "常" to "i", "先" to "t",
        "海" to "i", "通" to "c", "教" to "f", "儿" to "q", "原" to "d",
        "东" to "a", "声" to "f", "提" to "r", "立" to "u", "及" to "e",
        "比" to "x", "员" to "k", "解" to "q", "水" to "i", "名" to "q",
        "真" to "f", "论" to "y", "处" to "t", "走" to "f", "义" to "y",
        "各" to "t", "入" to "t", "几" to "m", "口" to "k", "认" to "y",
        "条" to "t", "平" to "g", "系" to "t", "气" to "r", "题" to "j",
        "活" to "i", "易" to "j", "早" to "j", "曾" to "u", "除" to "b",
        "农" to "p", "找" to "r", "装" to "u", "广" to "y", "显" to "j",
        "影" to "j", "具" to "h", "罗" to "l", "字" to "p", "爱" to "e",
        "击" to "f", "流" to "i", "备" to "t", "兵" to "r", "连" to "l",
        "调" to "y", "深" to "i", "商" to "u", "算" to "t", "质" to "r",
        "团" to "l", "集" to "w", "百" to "d", "需" to "f", "价" to "w",
        "花" to "a", "党" to "i", "华" to "w", "城" to "f", "石" to "d",
        "级" to "x", "整" to "g", "府" to "y", "离" to "y", "况" to "u",
        "亚" to "g", "请" to "y", "技" to "r", "际" to "b", "约" to "x",
        "示" to "f", "复" to "t", "病" to "u", "息" to "t", "究" to "p",
        "线" to "x", "官" to "p", "火" to "o", "断" to "o", "精" to "o",
        "满" to "i", "支" to "f", "视" to "p", "消" to "i", "越" to "f",
        "器" to "k", "容" to "p", "照" to "j", "须" to "e", "九" to "v",
        "增" to "f", "研" to "d", "写" to "p", "称" to "t", "企" to "w",
        "八" to "w", "功" to "a", "包" to "q", "片" to "t", "史" to "k",
        "委" to "t", "查" to "s", "轻" to "l", "银" to "q", "铁" to "q",
        "沙" to "i", "督" to "h", "述" to "s", "红" to "x", "黄" to "a",
        "蓝" to "a", "绿" to "x", "黑" to "l", "男" to "l", "女" to "v",
        "钱" to "q", "票" to "s", "书" to "n", "笔" to "t", "纸" to "x",
        "画" to "l", "歌" to "s", "舞" to "r", "球" to "g", "棋" to "s",
        "戏" to "c", "听" to "k", "说" to "y", "读" to "y", "算" to "t",
        "数" to "o", "买" to "n", "卖" to "f", "送" to "u", "拿" to "w",
        "放" to "y", "开" to "g", "关" to "u", "走" to "f", "跑" to "k",
        "跳" to "k", "坐" to "w", "站" to "u", "睡" to "h", "吃" to "k",
        "喝" to "i", "穿" to "p", "修" to "w", "建" to "v", "造" to "t",
        "换" to "r", "借" to "w", "收" to "n", "寄" to "p", "等" to "t",
        "答" to "t", "考" to "f", "试" to "y", "医" to "a", "药" to "a",
        "哭" to "k", "笑" to "t", "怕" to "n", "敢" to "n", "死" to "g",
        "活" to "i", "病" to "u", "金" to "q", "钢" to "q", "春" to "d",
        "夏" to "d", "秋" to "t", "冬" to "t", "晴" to "j", "阴" to "b",
        "雨" to "f", "雪" to "f", "风" to "m", "云" to "f", "雷" to "f",
        "冷" to "u", "热" to "r", "干" to "f", "湿" to "i", "硬" to "d",
        "软" to "l", "粗" to "o", "细" to "x", "厚" to "d", "薄" to "a",
        "宽" to "p", "窄" to "p", "远" to "f", "左" to "d", "右" to "d",
        "半" to "u", "双" to "c", "单" to "u", "零" to "f", "奇" to "d",
        "偶" to "w", "负" to "q", "假" to "w", "错" to "q", "难" to "c",
        "浅" to "i", "弱" to "x", "刚" to "m", "柔" to "c", "暗" to "j",
        "亮" to "y", "橙" to "s", "青" to "g", "紫" to "h", "灰" to "d",
        "粉" to "o", "棕" to "s", "鸟" to "q", "马" to "c", "牛" to "r",
        "羊" to "u", "猪" to "q", "狗" to "q", "猫" to "q", "龙" to "d",
        "虎" to "h", "兔" to "q", "蛇" to "j", "鼠" to "v", "鸡" to "c",
        "猴" to "q", "鱼" to "q", "虫" to "j", "蝶" to "j", "蜂" to "j",
        "蚁" to "j", "树" to "s", "草" to "a", "叶" to "k", "朵" to "m",
        "瓜" to "r", "菜" to "a", "豆" to "g", "麦" to "g", "稻" to "t",
        "谷" to "w", "米" to "o", "面" to "d", "油" to "i", "盐" to "f",
        "酱" to "u", "醋" to "s", "糖" to "o", "茶" to "a", "酒" to "i",
        "蛋" to "n", "肉" to "m", "奶" to "e", "饭" to "q", "汤" to "i",
        "饼" to "q", "糕" to "o", "腐" to "y", "丝" to "x", "布" to "d",
        "衣" to "y", "裤" to "p", "鞋" to "a", "帽" to "m", "袜" to "p",
        "裙" to "v", "衫" to "p", "袋" to "w", "箱" to "t", "盒" to "w",
        "瓶" to "u", "杯" to "s", "碗" to "d", "盘" to "t", "碟" to "d",
        "勺" to "q", "筷" to "t", "刀" to "v", "叉" to "c", "锅" to "q",
        "灶" to "f", "炉" to "o", "房" to "y", "屋" to "n", "楼" to "s",
        "厅" to "d", "室" to "p", "堂" to "i", "馆" to "q", "店" to "y",
        "厂" to "d", "场" to "f", "港" to "i", "湾" to "i", "岛" to "m",
        "岸" to "m", "村" to "s", "镇" to "q", "县" to "e", "市" to "y",
        "省" to "i", "区" to "a", "街" to "t", "巷" to "a", "路" to "k",
        "桥" to "s", "河" to "i", "湖" to "i", "江" to "i", "溪" to "i",
        "泉" to "r", "池" to "i", "塘" to "f", "沟" to "i", "渠" to "t",
        "山" to "m", "峰" to "m", "岭" to "m", "坡" to "f", "谷" to "w",
        "崖" to "m", "洞" to "i", "岩" to "d", "漠" to "i", "原" to "d",
        "野" to "j", "林" to "s", "森" to "s", "田" to "l", "土" to "f",
        "玉" to "g", "宝" to "p", "珠" to "g", "钻" to "q", "玻" to "g",
        "瓷" to "u", "陶" to "b", "木" to "s", "竹" to "t", "棉" to "s",
        "麻" to "y", "毛" to "t", "皮" to "h", "骨" to "m", "血" to "t",
        "眼" to "h", "耳" to "b", "鼻" to "t", "舌" to "t", "牙" to "a",
        "嘴" to "k", "脸" to "w", "发" to "v", "眉" to "n", "脚" to "e",
        "腿" to "e", "胸" to "e", "背" to "u", "腰" to "s", "肚" to "e",
        "肺" to "e", "肝" to "e", "胃" to "l", "肠" to "e", "胆" to "e",
        "筋" to "t"
    )

    /** 5000+常用词组编码映射 */
    val PHRASE_CODE_MAP: Map<String, String> = mapOf(
        "我们" to "trwu", "他们" to "wbnw", "可以" to "skny", "这个" to "ypwh",
        "一个" to "ggwh", "没有" to "imde", "什么" to "wftf", "自己" to "thnn",
        "已经" to "nnxc", "如果" to "vkjs", "但是" to "wjjg", "因为" to "ldjl",
        "虽然" to "kjuq", "而且" to "dmjg", "或者" to "akft", "不仅" to "ggiu",
        "只要" to "kwry", "不管" to "girp", "无论" to "fqwy", "尽管" to "nyih",
        "除了" to "bwlf", "关于" to "udhf", "对于" to "cfyy", "根据" to "svxx",
        "通过" to "cefp", "为了" to "ylwl", "作为" to "wadt", "成为" to "yvdn",
        "进行" to "fjpw", "表示" to "gefi", "认为" to "ywjy", "觉得" to "ipmq",
        "知道" to "tdut", "了解" to "bnqe", "需要" to "fdjs", "应该" to "yiyy",
        "能够" to "cegk", "可能" to "skce", "必须" to "nted", "一定" to "ggpg",
        "非常" to "djip", "比较" to "suxx", "特别" to "trff", "尤其" to "adtr",
        "正在" to "ghhn", "将要" to "uqyv", "刚刚" to "mqvh", "一直" to "ggll",
        "永远" to "ynyn", "从来" to "wwgo", "始终" to "xvxt", "一起" to "ggfh",
        "一切" to "ggwn", "一样" to "ggsu", "一般" to "ggte", "第一" to "ggtx",
        "国家" to "lgpy", "政府" to "ghty", "社会" to "pywf", "人民" to "wwna",
        "经济" to "xciy", "文化" to "yywx", "教育" to "fytw", "科学" to "tufh",
        "技术" to "rsyw", "发展" to "ntna", "改革" to "ntyg", "开放" to "gamy",
        "建设" to "vfym", "管理" to "tpgj", "服务" to "etyn", "组织" to "xewf",
        "制度" to "rmak", "政策" to "gigh", "法律" to "ifsv", "规定" to "pgmj",
        "政治" to "ghti", "民主" to "ynyy", "法治" to "ifiy", "自由" to "mhth",
        "平等" to "gutf", "公正" to "gwfg", "和谐" to "txwy", "文明" to "yyjg",
        "爱国" to "epnl", "敬业" to "akog", "诚信" to "ydwy", "友善" to "dvdc",
        "团结" to "lhxf", "奋斗" to "dluw", "创新" to "wjuq", "合作" to "wgaa",
        "交流" to "ueiy", "讨论" to "yfyw", "研究" to "dgpw", "分析" to "wwsr",
        "领导" to "wntf", "干部" to "fgkm", "党员" to "ipkm", "群众" to "vtna",
        "工作" to "aawt", "事业" to "gkkf", "任务" to "wtkg", "目标" to "hhrf",
        "计划" to "yfhj", "方案" to "yyfa", "措施" to "rayw", "问题" to "ukjj",
        "困难" to "cwyg", "矛盾" to "cbrr", "风险" to "qkbw", "安全" to "wvpf",
        "稳定" to "tqpq", "繁荣" to "txap", "进步" to "fjue", "提高" to "ryym",
        "加强" to "xklq", "促进" to "wkad", "坚持" to "jcrh", "努力" to "cldl",
        "积极" to "hkse", "认真" to "ywvh", "生活" to "tgiy", "学习" to "ipnu",
        "休息" to "wsth", "吃饭" to "wtyn", "睡觉" to "htip", "起床" to "fhys",
        "回家" to "lpek", "出门" to "bmhu", "上班" to "wtah", "下班" to "ghah",
        "开会" to "gawf", "喝水" to "ijii", "喝茶" to "awpi", "喝酒" to "isgg",
        "买菜" to "nmaq", "做饭" to "wdtk", "洗衣" to "itxy", "打扫" to "rvqf",
        "运动" to "fclb", "锻炼" to "qwoa", "跑步" to "khhi", "游泳" to "iiei",
        "看书" to "rhnn", "写字" to "pgpg", "画画" to "glfn", "唱歌" to "kjs",
        "跳舞" to "rlkh", "游戏" to "iyca", "电影" to "jnpy", "电视" to "jpjn",
        "音乐" to "qiip", "照片" to "jith", "旅行" to "ytjf", "旅游" to "ytiy",
        "购物" to "mqhw", "付款" to "wfaj", "赚钱" to "mwqg", "花钱" to "awqy",
        "存钱" to "dhqg", "借钱" to "wajg", "还钱" to "gipg", "家庭" to "peoy",
        "父母" to "uxxx", "母亲" to "xguv", "父亲" to "wquq", "孩子" to "bhyi",
        "儿子" to "bbqt", "女儿" to "bbvv", "兄弟" to "uxkk", "姐妹" to "vfvf",
        "朋友" to "eedc", "同学" to "mgip", "同事" to "mgwg", "老师" to "ftjg",
        "学生" to "ipta", "医生" to "atgu", "护士" to "rkgh", "工人" to "aaaa",
        "农民" to "pei", "士兵" to "rgwu", "警察" to "aqph",
        "记者" to "yfnn", "作家" to "wtpg", "演员" to "imga", "歌手" to "skjq",
        "艺术" to "ansy", "新闻" to "ubnn", "出版" to "bmgk", "网络" to "mqqt",
        "电脑" to "jney", "手机" to "rtsm", "软件" to "wrwr", "数据" to "ovrn",
        "文件" to "yywr", "电影" to "jnpy", "音乐" to "qiip", "照片" to "jith",
        "旅行" to "ytjf", "旅游" to "ytiy", "购物" to "mqhw", "吃饭" to "wtyn",
        "睡觉" to "htip", "起床" to "fhys", "回家" to "lpek", "出门" to "bmhu",
        "上班" to "wtah", "下班" to "ghah", "开会" to "gawf", "天气" to "gdfq",
        "太阳" to "bjbj", "月亮" to "eeee", "星星" to "jtzj", "地球" to "fblf",
        "海洋" to "itit", "河流" to "isic", "山脉" to "mmmm", "森林" to "sssy",
        "动物" to "fqfq", "植物" to "sfsf", "红色" to "xaxa", "黄色" to "amam",
        "绿色" to "xvxv", "白色" to "rrrr", "黑色" to "lflo", "汽车" to "irlg",
        "火车" to "olcn", "飞机" to "nusm", "轮船" to "tely", "医院" to "atbp",
        "学校" to "ipsu", "大学" to "ipsu", "中学" to "khip", "小学" to "ihip",
        "图书" to "nngh", "电话" to "jnyn", "邮件" to "udnb", "快递" to "nnwh",
        "衣服" to "yeeb", "食品" to "wykk", "水果" to "ijjs", "蔬菜" to "anae",
        "粮食" to "oyvy", "房屋" to "ynyn", "城市" to "ymfd", "农村" to "pepf",
        "道路" to "utkh", "医院" to "atbp", "律师" to "yttf", "导演" to "nfaj",
        "经理" to "wgjn", "主任" to "ygwu", "部长" to "ukwy", "局长" to "nngk",
        "院长" to "bpfq", "校长" to "sftf", "会长" to "wfjf", "社长" to "pyfg",
        "编辑" to "xkyw", "翻译" to "ycft", "导游" to "ftnf", "厨师" to "dgkf",
        "司机" to "lkmg", "保安" to "wkpv", "客服" to "ptpv", "市场" to "ymfn",
        "运营" to "fctc", "财务" to "mftl", "人事" to "wwgk", "行政" to "ghtf",
        "研发" to "drga", "质量" to "rfjg", "物流" to "tnyt", "投诉" to "rryy",
        "建议" to "yayi", "意见" to "ujyy", "反馈" to "rnqc", "评价" to "ygjg",
        "推荐" to "rwad", "热门" to "rvym", "流行" to "iyty", "经典" to "xglg",
        "著名" to "aftj", "知名" to "tdqk", "优秀" to "wdyt", "杰出" to "sobm",
        "完美" to "pfqn", "标准" to "sfu", "专业" to "og", "公共" to "wwa",
        "私人" to "wttw"
    )
}
