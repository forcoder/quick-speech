package com.quickspeech.wubi.engine

import android.util.Log

enum class WubiScheme { WUBI_86, WUBI_98, WUBI_NEW }

class WubiEngine {

    companion object {
        private const val TAG = "WubiEngine"
        var isNativeLoaded = false
            private set

        init {
            try {
                System.loadLibrary("wubi-engine")
                isNativeLoaded = true
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
                isNativeLoaded = false
            } catch (e: Throwable) {
                Log.e(TAG, "Unexpected error loading native library", e)
                isNativeLoaded = false
            }
        }
    }

    private var currentScheme: WubiScheme = WubiScheme.WUBI_86

    // 内置五笔86词典（含前缀匹配联想）
    private val dictionary: Map<String, List<String>> = buildMap {
        // ===== 一级简码 =====
        put("a", listOf("工")); put("b", listOf("了")); put("c", listOf("以"))
        put("d", listOf("在")); put("e", listOf("有")); put("f", listOf("地"))
        put("g", listOf("一")); put("h", listOf("上")); put("i", listOf("不"))
        put("j", listOf("是")); put("k", listOf("中")); put("l", listOf("国"))
        put("m", listOf("同")); put("n", listOf("民")); put("o", listOf("为"))
        put("p", listOf("这")); put("q", listOf("我")); put("r", listOf("的"))
        put("s", listOf("要")); put("t", listOf("和")); put("u", listOf("产"))
        put("v", listOf("发")); put("w", listOf("人")); put("x", listOf("经"))
        put("y", listOf("主"))

        // ===== 二级简码（常用字） =====
        put("aa", listOf("式")); put("ab", listOf("节")); put("ac", listOf("攻")); put("ad", listOf("菜"))
        put("ae", listOf("蓝")); put("af", listOf("革")); put("ag", listOf("茉")); put("ah", listOf("牙"))
        put("ai", listOf("东")); put("aj", listOf("划")); put("ak", listOf("或")); put("al", listOf("苗"))
        put("am", listOf("黄")); put("an", listOf("世")); put("ao", listOf("燕")); put("ap", listOf("劳"))
        put("aq", listOf("区")); put("ar", listOf("共")); put("as", listOf("茶")); put("at", listOf("芽"))
        put("au", listOf("蓝")); put("aw", listOf("共")); put("ax", listOf("药")); put("ay", listOf("芳"))
        put("ba", listOf("陈")); put("bb", listOf("子")); put("bc", listOf("取")); put("bd", listOf("承"))
        put("be", listOf("阴")); put("bf", listOf("孙")); put("bg", listOf("卫")); put("bh", listOf("耻"))
        put("bi", listOf("际")); put("bj", listOf("阳")); put("bk", listOf("职")); put("bl", listOf("阵"))
        put("bm", listOf("出")); put("bn", listOf("也")); put("bo", listOf("耿")); put("bp", listOf("辽"))
        put("bq", listOf("隐")); put("br", listOf("孤")); put("bs", listOf("阿")); put("bt", listOf("降"))
        put("bu", listOf("联")); put("bv", listOf("限")); put("bw", listOf("队")); put("by", listOf("防"))
        put("ca", listOf("戏")); put("cb", listOf("叉")); put("cc", listOf("双")); put("cd", listOf("参"))
        put("ce", listOf("能")); put("cf", listOf("对")); put("cg", listOf("马")); put("ch", listOf("参"))
        put("ci", listOf("台")); put("cj", listOf("观")); put("ck", listOf("劝")); put("cl", listOf("坝"))
        put("cm", listOf("观")); put("cn", listOf("巴")); put("co", listOf("能")); put("cp", listOf("驼"))
        put("cq", listOf("马")); put("cr", listOf("参")); put("cs", listOf("能")); put("ct", listOf("对"))
        put("cu", listOf("巴")); put("cv", listOf("发")); put("cw", listOf("难")); put("cx", listOf("经"))
        put("cy", listOf("予")); put("da", listOf("左")); put("db", listOf("套")); put("dc", listOf("友"))
        put("dd", listOf("大")); put("de", listOf("有")); put("df", listOf("夺")); put("dg", listOf("石"))
        put("dh", listOf("丰")); put("di", listOf("砂")); put("dj", listOf("百")); put("dk", listOf("右"))
        put("dl", listOf("友")); put("dm", listOf("页")); put("dn", listOf("成")); put("do", listOf("灰"))
        put("dp", listOf("达")); put("dq", listOf("克")); put("dr", listOf("原")); put("ds", listOf("厅"))
        put("dt", listOf("帮")); put("du", listOf("磁")); put("dv", listOf("肆")); put("dw", listOf("春"))
        put("dx", listOf("龙")); put("dy", listOf("太")); put("ea", listOf("且")); put("eb", listOf("朋"))
        put("ec", listOf("县")); put("ed", listOf("须")); put("ee", listOf("月")); put("ef", listOf("肝"))
        put("eg", listOf("用")); put("eh", listOf("牙")); put("ei", listOf("县")); put("ej", listOf("胆"))
        put("ek", listOf("加")); put("el", listOf("肿")); put("em", listOf("肌")); put("en", listOf("肥"))
        put("eo", listOf("膛")); put("ep", listOf("爱")); put("eq", listOf("胸")); put("er", listOf("脏"))
        put("es", listOf("采")); put("et", listOf("用")); put("eu", listOf("胶")); put("ev", listOf("妥"))
        put("ew", listOf("脸")); put("ex", listOf("脂")); put("ey", listOf("及")); put("fa", listOf("载"))
        put("fb", listOf("寺")); put("fc", listOf("去")); put("fd", listOf("城")); put("fe", listOf("圾"))
        put("ff", listOf("土")); put("fg", listOf("二")); put("fh", listOf("干")); put("fi", listOf("示"))
        put("fj", listOf("日")); put("fk", listOf("口")); put("fl", listOf("甲")); put("fm", listOf("车"))
        put("fn", listOf("地")); put("fo", listOf("城")); put("fp", listOf("过")); put("fq", listOf("无"))
        put("fr", listOf("地")); put("fs", listOf("寺")); put("ft", listOf("寸")); put("fu", listOf("士"))
        put("fv", listOf("土")); put("fw", listOf("二")); put("fx", listOf("土")); put("fy", listOf("寸"))
        put("ga", listOf("开")); put("gb", listOf("到")); put("gc", listOf("至")); put("gd", listOf("天"))
        put("ge", listOf("表")); put("gf", listOf("于")); put("gg", listOf("五")); put("gh", listOf("下"))
        put("gi", listOf("不")); put("gj", listOf("理")); put("gk", listOf("事")); put("gl", listOf("画"))
        put("gm", listOf("现")); put("gn", listOf("与")); put("go", listOf("来")); put("gp", listOf("还"))
        put("gq", listOf("死")); put("gr", listOf("正")); put("gs", listOf("末")); put("gt", listOf("玫"))
        put("gu", listOf("平")); put("gv", listOf("妻")); put("gw", listOf("珍")); put("gx", listOf("互"))
        put("gy", listOf("玉")); put("ha", listOf("虎")); put("hb", listOf("皮")); put("hc", listOf("攴"))
        put("hd", listOf("此")); put("he", listOf("肯")); put("hf", listOf("睦")); put("hg", listOf("止"))
        put("hh", listOf("上")); put("hi", listOf("步")); put("hj", listOf("肯")); put("hk", listOf("占"))
        put("hl", listOf("卤")); put("hm", listOf("贞")); put("hn", listOf("卢")); put("ho", listOf("眯"))
        put("hp", listOf("瞎")); put("hq", listOf("餐")); put("hr", listOf("皮")); put("hs", listOf("卢"))
        put("ht", listOf("眠")); put("hu", listOf("瞳")); put("hv", listOf("眼")); put("hw", listOf("具"))
        put("hx", listOf("此")); put("hy", listOf("眩")); put("ia", listOf("江")); put("ib", listOf("池"))
        put("ic", listOf("汉")); put("id", listOf("尖")); put("ie", listOf("肖")); put("if", listOf("法"))
        put("ig", listOf("汪")); put("ih", listOf("小")); put("ii", listOf("水")); put("ij", listOf("浊"))
        put("ik", listOf("澡")); put("il", listOf("渐")); put("im", listOf("没")); put("in", listOf("沁"))
        put("io", listOf("淡")); put("ip", listOf("学")); put("iq", listOf("光")); put("ir", listOf("泊"))
        put("is", listOf("洒")); put("it", listOf("少")); put("iu", listOf("洋")); put("iv", listOf("当"))
        put("iw", listOf("兴")); put("ix", listOf("涨")); put("iy", listOf("注")); put("ja", listOf("虹"))
        put("jb", listOf("最")); put("jc", listOf("坚")); put("jd", listOf("晨")); put("je", listOf("明"))
        put("jf", listOf("时")); put("jg", listOf("量")); put("jh", listOf("早")); put("ji", listOf("晃"))
        put("jj", listOf("昌")); put("jk", listOf("蝇")); put("jl", listOf("曙")); put("jm", listOf("遇"))
        put("jn", listOf("电")); put("jo", listOf("显")); put("jp", listOf("晕")); put("jq", listOf("晚"))
        put("jr", listOf("蝗")); put("js", listOf("果")); put("jt", listOf("昨")); put("ju", listOf("暗"))
        put("jv", listOf("归")); put("jw", listOf("蛤")); put("jx", listOf("昆")); put("jy", listOf("景"))
        put("ka", listOf("呀")); put("kb", listOf("啊")); put("kc", listOf("吧")); put("kd", listOf("顺"))
        put("ke", listOf("吸")); put("kf", listOf("叶")); put("kg", listOf("呈")); put("kh", listOf("中"))
        put("ki", listOf("吵")); put("kj", listOf("虽")); put("kk", listOf("吕")); put("kl", listOf("另"))
        put("km", listOf("员")); put("kn", listOf("叫")); put("ko", listOf("噗")); put("kp", listOf("喧"))
        put("kq", listOf("啊")); put("kr", listOf("嘛")); put("ks", listOf("呆")); put("kt", listOf("呼"))
        put("ku", listOf("啼")); put("kv", listOf("哪")); put("kw", listOf("只")); put("kx", listOf("哟"))
        put("ky", listOf("嘛")); put("la", listOf("思")); put("lb", listOf("团")); put("lc", listOf("轻"))
        put("ld", listOf("因")); put("le", listOf("胃")); put("lf", listOf("轩")); put("lg", listOf("车"))
        put("lh", listOf("四")); put("li", listOf("辊")); put("lj", listOf("罪")); put("lk", listOf("加"))
        put("ll", listOf("男")); put("lm", listOf("轴")); put("ln", listOf("思")); put("lo", listOf("辘"))
        put("lp", listOf("边")); put("lq", listOf("罗")); put("lr", listOf("斩")); put("ls", listOf("困"))
        put("lt", listOf("力")); put("lu", listOf("较")); put("lv", listOf("轨")); put("lw", listOf("办"))
        put("lx", listOf("累")); put("ly", listOf("罚")); put("ma", listOf("财")); put("mb", listOf("骨"))
        put("mc", listOf("县")); put("md", listOf("央")); put("me", listOf("用")); put("mf", listOf("同"))
        put("mg", listOf("册")); put("mh", listOf("币")); put("mi", listOf("周")); put("mj", listOf("册"))
        put("mk", listOf("丹")); put("ml", listOf("央")); put("mm", listOf("册")); put("mn", listOf("曲"))
        put("mo", listOf("贼")); put("mp", listOf("赠")); put("mq", listOf("见")); put("mr", listOf("骨"))
        put("ms", listOf("财")); put("mt", listOf("贴")); put("mu", listOf("则")); put("mv", listOf("骨"))
        put("mw", listOf("内")); put("mx", listOf("骨")); put("my", listOf("丹")); put("na", listOf("民"))
        put("nb", listOf("敢")); put("nc", listOf("居")); put("nd", listOf("敢")); put("ne", listOf("敢"))
        put("nf", listOf("导")); put("ng", listOf("怀")); put("nh", listOf("收")); put("ni", listOf("慢"))
        put("nj", listOf("避")); put("nk", listOf("惭")); put("nl", listOf("忆")); put("nm", listOf("届"))
        put("nn", listOf("已")); put("no", listOf("悄")); put("np", listOf("懈")); put("nq", listOf("敢"))
        put("nr", listOf("怕")); put("ns", listOf("惶")); put("nt", listOf("改")); put("nu", listOf("习"))
        put("nv", listOf("恨")); put("nw", listOf("恰")); put("nx", listOf("尼")); put("ny", listOf("心"))
        put("oa", listOf("煤")); put("ob", listOf("籽")); put("oc", listOf("烃")); put("od", listOf("类"))
        put("oe", listOf("粗")); put("of", listOf("灶")); put("og", listOf("业")); put("oh", listOf("粘"))
        put("oi", listOf("炒")); put("oj", listOf("烛")); put("ok", listOf("炽")); put("ol", listOf("烟"))
        put("om", listOf("炯")); put("on", listOf("灿")); put("oo", listOf("炎")); put("op", listOf("迷"))
        put("oq", listOf("炮")); put("or", listOf("煌")); put("os", listOf("灯")); put("ot", listOf("烽"))
        put("ou", listOf("料")); put("ov", listOf("娄")); put("ow", listOf("粉")); put("ox", listOf("粒"))
        put("oy", listOf("米")); put("pa", listOf("宽")); put("pb", listOf("字")); put("pc", listOf("社"))
        put("pd", listOf("害")); put("pe", listOf("家")); put("pf", listOf("守")); put("pg", listOf("定"))
        put("ph", listOf("寂")); put("pi", listOf("宵")); put("pj", listOf("审")); put("pk", listOf("宫"))
        put("pl", listOf("军")); put("pm", listOf("宙")); put("pn", listOf("官")); put("po", listOf("灾"))
        put("pp", listOf("之")); put("pq", listOf("宛")); put("pr", listOf("宾")); put("ps", listOf("宁"))
        put("pt", listOf("客")); put("pu", listOf("实")); put("pv", listOf("安")); put("pw", listOf("空"))
        put("px", listOf("它")); put("py", listOf("社")); put("qa", listOf("钱")); put("qb", listOf("尔"))
        put("qc", listOf("色")); put("qd", listOf("然")); put("qe", listOf("角")); put("qf", listOf("鱼"))
        put("qg", listOf("钱")); put("qh", listOf("外")); put("qi", listOf("乐")); put("qj", listOf("旬"))
        put("qk", listOf("名")); put("ql", listOf("甸")); put("qm", listOf("负")); put("qn", listOf("包"))
        put("qo", listOf("炙")); put("qp", listOf("免")); put("qq", listOf("多")); put("qr", listOf("铁"))
        put("qs", listOf("钉")); put("qt", listOf("儿")); put("qu", listOf("匀")); put("qv", listOf("争"))
        put("qw", listOf("欠")); put("qx", listOf("久")); put("qy", listOf("义")); put("ra", listOf("找"))
        put("rb", listOf("报")); put("rc", listOf("反")); put("rd", listOf("拓")); put("re", listOf("反"))
        put("rf", listOf("持")); put("rg", listOf("后")); put("rh", listOf("年")); put("ri", listOf("朱"))
        put("rj", listOf("提")); put("rk", listOf("扣")); put("rl", listOf("押")); put("rm", listOf("抽"))
        put("rn", listOf("所")); put("ro", listOf("搂")); put("rp", listOf("近")); put("rq", listOf("换"))
        put("rr", listOf("打")); put("rs", listOf("手")); put("rt", listOf("手")); put("ru", listOf("拉"))
        put("rv", listOf("接")); put("rw", listOf("推")); put("rx", listOf("批")); put("ry", listOf("扩"))
        put("sa", listOf("械")); put("sb", listOf("李")); put("sc", listOf("权")); put("sd", listOf("本"))
        put("se", listOf("林")); put("sf", listOf("村")); put("sg", listOf("本")); put("sh", listOf("相"))
        put("si", listOf("档")); put("sj", listOf("查")); put("sk", listOf("可")); put("sl", listOf("楞"))
        put("sm", listOf("机")); put("sn", listOf("杨")); put("so", listOf("杰")); put("sp", listOf("棕"))
        put("sq", listOf("构")); put("sr", listOf("析")); put("ss", listOf("木")); put("st", listOf("格"))
        put("su", listOf("样")); put("sv", listOf("根")); put("sw", listOf("检")); put("sx", listOf("楷"))
        put("sy", listOf("术")); put("ta", listOf("长")); put("tb", listOf("季")); put("tc", listOf("么"))
        put("td", listOf("知")); put("te", listOf("白")); put("tf", listOf("生")); put("tg", listOf("长"))
        put("th", listOf("处")); put("ti", listOf("秒")); put("tj", listOf("得")); put("tk", listOf("各"))
        put("tl", listOf("务")); put("tm", listOf("向")); put("tn", listOf("秘")); put("to", listOf("秋"))
        put("tp", listOf("管")); put("tq", listOf("称")); put("tr", listOf("物")); put("ts", listOf("条"))
        put("tt", listOf("笔")); put("tu", listOf("科")); put("tv", listOf("委")); put("tw", listOf("答"))
        put("tx", listOf("第")); put("ty", listOf("入")); put("ua", listOf("关")); put("ub", listOf("闻"))
        put("uc", listOf("疗")); put("ud", listOf("差")); put("ue", listOf("前")); put("uf", listOf("半"))
        put("ug", listOf("关")); put("uh", listOf("站")); put("ui", listOf("冰")); put("uj", listOf("间"))
        put("uk", listOf("问")); put("ul", listOf("曾")); put("um", listOf("端")); put("un", listOf("决"))
        put("uo", listOf("普")); put("up", listOf("帝")); put("uq", listOf("交")); put("ur", listOf("瓣"))
        put("us", listOf("亲")); put("ut", listOf("产")); put("uv", listOf("妆")); put("uw", listOf("闪"))
        put("ux", listOf("北")); put("uy", listOf("六")); put("va", listOf("好")); put("vb", listOf("她"))
        put("vc", listOf("妈")); put("vd", listOf("姑")); put("ve", listOf("姐")); put("vf", listOf("女"))
        put("vg", listOf("她")); put("vh", listOf("即")); put("vi", listOf("录")); put("vj", listOf("既"))
        put("vk", listOf("如")); put("vl", listOf("娘")); put("vm", listOf("女")); put("vn", listOf("刀"))
        put("vo", listOf("灵")); put("vp", listOf("退")); put("vq", listOf("婚")); put("vr", listOf("奴"))
        put("vs", listOf("女")); put("vt", listOf("好")); put("vu", listOf("妈")); put("vv", listOf("女"))
        put("vw", listOf("媳")); put("vx", listOf("姆")); put("vy", listOf("姑")); put("wa", listOf("代"))
        put("wb", listOf("他")); put("wc", listOf("公")); put("wd", listOf("你")); put("we", listOf("份"))
        put("wf", listOf("他")); put("wg", listOf("个")); put("wh", listOf("你")); put("wi", listOf("你"))
        put("wj", listOf("介")); put("wk", listOf("保")); put("wl", listOf("佃")); put("wm", listOf("他"))
        put("wn", listOf("亿")); put("wo", listOf("你")); put("wp", listOf("你")); put("wq", listOf("你"))
        put("wr", listOf("你")); put("ws", listOf("你")); put("wt", listOf("作")); put("wu", listOf("你"))
        put("wv", listOf("你")); put("ww", listOf("人")); put("wx", listOf("化")); put("wy", listOf("信"))
        put("xa", listOf("红")); put("xb", listOf("纪")); put("xc", listOf("经")); put("xd", listOf("顷"))
        put("xe", listOf("组")); put("xf", listOf("结")); put("xg", listOf("母")); put("xh", listOf("细"))
        put("xi", listOf("纲")); put("xj", listOf("练")); put("xk", listOf("强")); put("xl", listOf("组"))
        put("xm", listOf("绢")); put("xn", listOf("纽")); put("xo", listOf("累")); put("xp", listOf("绍"))
        put("xq", listOf("约")); put("xr", listOf("绵")); put("xs", listOf("综")); put("xt", listOf("给"))
        put("xu", listOf("终")); put("xv", listOf("继")); put("xw", listOf("综")); put("xx", listOf("丝"))
        put("xy", listOf("纺")); put("ya", listOf("试")); put("yb", listOf("离")); put("yc", listOf("充"))
        put("yd", listOf("诚")); put("ye", listOf("衣")); put("yf", listOf("计")); put("yg", listOf("主"))
        put("yh", listOf("让")); put("yi", listOf("就")); put("yj", listOf("刘")); put("yk", listOf("文"))
        put("yl", listOf("亩")); put("ym", listOf("高")); put("yn", listOf("记")); put("yo", listOf("变"))
        put("yp", listOf("这")); put("yq", listOf("义")); put("yr", listOf("记")); put("ys", listOf("订"))
        put("yt", listOf("放")); put("yu", listOf("说")); put("yv", listOf("良")); put("yw", listOf("认"))
        put("yx", listOf("率")); put("yy", listOf("方"))

        // ===== 常用二字词（全码）=====
        put("adww", listOf("功夫")); put("wgkr", listOf("个人")); put("wwbn", listOf("人们"))
        put("wynb", listOf("你们")); put("wynu", listOf("他们")); put("wyny", listOf("我们"))
        put("fggg", listOf("一起")); put("gggg", listOf("一天")); put("gggt", listOf("一下"))
        put("ghgb", listOf("上下")); put("ghgo", listOf("上来")); put("ghgp", listOf("上去"))
        put("hhgg", listOf("眼睛")); put("jfhj", listOf("明天")); put("jtyj", listOf("昨天"))
        put("mfmf", listOf("周末")); put("nhjg", listOf("已经")); put("nhyq", listOf("以后"))
        put("rghg", listOf("起来")); put("sghg", listOf("根本")); put("tffh", listOf("和平"))
        put("tkgk", listOf("合适")); put("ttth", listOf("街道")); put("udjg", listOf("关闭"))
        put("ugdu", listOf("关于")); put("ujfb", listOf("问题")); put("ujjd", listOf("问答"))
        put("vath", listOf("姐姐")); put("vfqn", listOf("婚姻")); put("vtkm", listOf("姑娘"))
        put("vyve", listOf("娘")); put("wajg", listOf("借钱")); put("wawy", listOf("信念"))
    }

    /**
     * 搜索候选词
     * @param code 五笔编码
     * @return 候选词列表
     */
    fun search(code: String): List<String> {
        if (code.isEmpty()) return emptyList()
        val lowerCode = code.lowercase()
        // 1. 精确匹配
        dictionary[lowerCode]?.let { return it }
        // 2. 前缀匹配
        val prefixMatches = dictionary.filterKeys { it.startsWith(lowerCode) }
            .flatMap { it.value }
            .distinct()
        if (prefixMatches.isNotEmpty()) return prefixMatches
        return emptyList()
    }

    /**
     * 设置五笔方案
     */
    fun setScheme(scheme: WubiScheme) {
        currentScheme = scheme
        if (isNativeLoaded) {
            try {
                nativeSetScheme(scheme.ordinal)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set native scheme", e)
            }
        }
    }

    /**
     * 是否启用纠错
     */
    var enableErrorCorrection: Boolean = false

    /**
     * 反向查询：根据汉字查编码
     */
    fun reverseLookup(word: String): List<String> {
        return dictionary.filterValues { word in it }.keys.toList()
    }

    // Native methods (JNI bridge, currently returns empty)
    private external fun nativeSearch(code: String): Array<String>
    private external fun nativeSetScheme(scheme: Int)
    private external fun nativeEnableErrorCorrection(enable: Boolean)
}