package com.quickspeech.wubi.engine

import android.util.Log

class WubiEngine {

    companion object {
        private const val TAG = "WubiEngine"
        var isNativeLoaded = false
            private set

        init {
            try {
                System.loadLibrary("wubi-engine")
                isNativeLoaded = true
                Log.e(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
                isNativeLoaded = false
            } catch (e: Throwable) {
                Log.e(TAG, "Unexpected error loading native library", e)
                isNativeLoaded = false
            }
        }
    }

    // 内置基础五笔86词典
    private val dictionary: Map<String, List<String>> = mapOf(
        // 一级简码
        "a" to listOf("工"), "b" to listOf("了"), "c" to listOf("以"),
        "d" to listOf("在"), "e" to listOf("有"), "f" to listOf("地"),
        "g" to listOf("一"), "h" to listOf("上"), "i" to listOf("不"),
        "j" to listOf("是"), "k" to listOf("中"), "l" to listOf("国"),
        "m" to listOf("同"), "n" to listOf("民"), "o" to listOf("为"),
        "p" to listOf("这"), "q" to listOf("我"), "r" to listOf("的"),
        "s" to listOf("要"), "t" to listOf("和"), "u" to listOf("产"),
        "v" to listOf("发"), "w" to listOf("人"), "x" to listOf("经"),
        "y" to listOf("主"),
        // 常用单字
        "aaaa" to listOf("工"), "adg" to listOf("三"), "afj" to listOf("华"),
        "aj" to listOf("划"), "akk" to listOf("匣"), "am" to listOf("黄"),
        "an" to listOf("世"), "ap" to listOf("劳"), "aq" to listOf("区"),
        "as" to listOf("茶"), "at" to listOf("芽"), "au" to listOf("蓝"),
        "aw" to listOf("共"), "ax" to listOf("药"), "ay" to listOf("芳"),
        "bbbb" to listOf("子"), "bc" to listOf("取"), "bd" to listOf("陈"),
        "bf" to listOf("孙"), "bg" to listOf("卫"), "bh" to listOf("卫"),
        "bj" to listOf("阳"), "bk" to listOf("职"), "bl" to listOf("阵"),
        "bm" to listOf("出"), "bn" to listOf("也"), "bp" to listOf("辽"),
        "bq" to listOf("隐"), "br" to listOf("孤"), "bs" to listOf("阿"),
        "bt" to listOf("降"), "bu" to listOf("联"), "bv" to listOf("限"),
        "bw" to listOf("队"), "by" to listOf("防"),
        "cccc" to listOf("又"), "ccb" to listOf("叉"), "ccd" to listOf("戏"),
        "cck" to listOf("台"), "cd" to listOf("参"), "ce" to listOf("能"),
        "cf" to listOf("对"), "cg" to listOf("马"), "cl" to listOf("劝"),
        "cm" to listOf("观"), "cn" to listOf("巴"), "co" to listOf("菜"),
        "cp" to listOf("对"), "cq" to listOf("马"), "cr" to listOf("参"),
        "cs" to listOf("茶"), "ct" to listOf("对"), "cu" to listOf("巴"),
        "cv" to listOf("发"), "cw" to listOf("难"), "cx" to listOf("经"),
        "cy" to listOf("予"), "dddd" to listOf("大"), "ddb" to listOf("套"),
        "ddg" to listOf("磊"), "ddn" to listOf("左"), "ddw" to listOf("春"),
        "de" to listOf("有"), "df" to listOf("夺"), "dg" to listOf("石"),
        "dh" to listOf("丰"), "di" to listOf("砂"), "dj" to listOf("百"),
        "dk" to listOf("右"), "dl" to listOf("友"), "dm" to listOf("页"),
        "dn" to listOf("成"), "do" to listOf("灰"), "dp" to listOf("达"),
        "dq" to listOf("克"), "dr" to listOf("原"), "ds" to listOf("厅"),
        "dt" to listOf("帮"), "du" to listOf("磁"), "dv" to listOf("肆"),
        "dw" to listOf("春"), "dx" to listOf("龙"), "dy" to listOf("太"),
        "eeee" to listOf("月"), "eeb" to listOf("朋"), "eef" to listOf("且"),
        "eeg" to listOf("县"), "ef" to listOf("肝"), "eg" to listOf("用"),
        "eh" to listOf("牙"), "ei" to listOf("县"), "ej" to listOf("胆"),
        "ek" to listOf("加"), "ffff" to listOf("土"), "ffb" to listOf("寺"),
        "ffc" to listOf("去"), "ffg" to listOf("二"), "ffh" to listOf("干"),
        "ffi" to listOf("雨"), "ffj" to listOf("未"), "ffk" to listOf("吉"),
        "ffl" to listOf("直"), "ffn" to listOf("寺"), "ffp" to listOf("过"),
        "ffq" to listOf("元"), "ffr" to listOf("无"), "ffs" to listOf("才"),
        "fft" to listOf("寸"), "ffu" to listOf("士"), "fg" to listOf("二"),
        "fgg" to listOf("二"), "fh" to listOf("干"), "fhg" to listOf("止"),
        "fhh" to listOf("止"), "fhk" to listOf("赶"), "fhn" to listOf("起"),
        "fhp" to listOf("赴"), "fhq" to listOf("越"), "fhr" to listOf("趣"),
        "fht" to listOf("超"), "fhu" to listOf("走"), "fi" to listOf("示"),
        "fie" to listOf("霄"), "fii" to listOf("示"), "fip" to listOf("票"),
        "fiq" to listOf("票"), "fir" to listOf("票"), "fis" to listOf("票"),
        "fit" to listOf("票"), "fiu" to listOf("示"), "fiw" to listOf("票"),
        "fix" to listOf("票"), "fiy" to listOf("票"), "fj" to listOf("日"),
        "fja" to listOf("冒"), "fjb" to listOf("厚"), "fjd" to listOf("非"),
        "fjf" to listOf("厘"), "fjg" to listOf("百"), "fjh" to listOf("日"),
        "fjj" to listOf("旦"), "fjr" to listOf("年"), "fk" to listOf("口"),
        "fka" to listOf("喜"), "fkb" to listOf("喜"), "fkc" to listOf("喜"),
        "fkd" to listOf("喜"), "fke" to listOf("喜"), "fkf" to listOf("喜"),
        "fkg" to listOf("喜"), "fkh" to listOf("中"), "fki" to listOf("喜"),
        "fkj" to listOf("喜"), "fkk" to listOf("喜"), "fkl" to listOf("喜"),
        "fkm" to listOf("喜"), "fkn" to listOf("喜"), "fko" to listOf("喜"),
        "fkp" to listOf("喜"), "fkq" to listOf("喜"), "fkr" to listOf("喜"),
        "fks" to listOf("喜"), "fkt" to listOf("喜"), "fku" to listOf("喜"),
        "fkv" to listOf("喜"), "fkw" to listOf("喜"), "fkx" to listOf("喜"),
        "fky" to listOf("喜"), "fl" to listOf("甲"), "fm" to listOf("同"),
        "fn" to listOf("地"), "fo" to listOf("城"), "fp" to listOf("过"),
        "fq" to listOf("无"), "fr" to listOf("地"), "fs" to listOf("寺"),
        "ft" to listOf("寸"), "fu" to listOf("士"), "fv" to listOf("土"),
        "fw" to listOf("二"), "fx" to listOf("土"), "fy" to listOf("寸"),
        // 常用二字词
        "adww" to listOf("功夫"), "wgkr" to listOf("个人"), "wwbn" to listOf("人们"),
        "wynb" to listOf("你们"), "wynu" to listOf("他们"), "wyny" to listOf("我们"),
        "fggg" to listOf("一起"), "gggg" to listOf("一天"), "gggt" to listOf("一下"),
        "ghgb" to listOf("上下"), "ghgo" to listOf("上来"), "ghgp" to listOf("上去"),
        "hhgg" to listOf("眼睛"), "jfhj" to listOf("明天"), "jtyj" to listOf("昨天"),
        "mfmf" to listOf("周末"), "nhjg" to listOf("已经"), "nhyq" to listOf("以后"),
        "rghg" to listOf("起来"), "sghg" to listOf("根本"), "tffh" to listOf("和平"),
        "tkgk" to listOf("合适"), "ttth" to listOf("街道"), "udjg" to listOf("关闭"),
        "ugdu" to listOf("关于"), "ujfb" to listOf("问题"), "ujjd" to listOf("问答"),
        "vath" to listOf("好的"), "vfbh" to listOf("她们"), "wgah" to listOf("什么"),
        "wgen" to listOf("做作"), "wyth" to listOf("今天"), "xcmh" to listOf("给予"),
        "xtyy" to listOf("经验"), "xxal" to listOf("比较"), "ybfn" to listOf("语文"),
        "yctg" to listOf("应该"), "ygjg" to listOf("认识"), "ynjg" to listOf("户口"),
        "ytty" to listOf("说话"), "yvbg" to listOf("认真"),
        "wg" to listOf("个"), "adg" to listOf("三"), "dgtg" to listOf("石"),
        "fghg" to listOf("于"), "gjgj" to listOf("二"), "hgmg" to listOf("上"),
        "hghg" to listOf("止"), "hghn" to listOf("此"), "jfd" to listOf("时"),
        "jfh" to listOf("早"), "jghh" to listOf("是"), "khga" to listOf("中"),
        "lbd" to listOf("也"), "mcb" to listOf("骨"), "ntcy" to listOf("改"),
        "pfjs" to listOf("冒"), "qgab" to listOf("钱"), "qgtg" to listOf("长"),
        "rhfj" to listOf("年"), "sgds" to listOf("本"), "ssmy" to listOf("末"),
        "tfhh" to listOf("生"), "tgd" to listOf("长"), "thd" to listOf("处"),
        "thfn" to listOf("每"), "thgf" to listOf("算"), "tjgf" to listOf("得"),
        "tmgk" to listOf("复"), "udaf" to listOf("差"), "ugd" to listOf("关"),
        "ugdd" to listOf("并"), "ujf" to listOf("间"), "ujfb" to listOf("问"),
        "vcb" to listOf("好"), "vghg" to listOf("她"), "wfg" to listOf("他"),
        "wygg" to listOf("信"), "xanw" to listOf("经"), "xbm" to listOf("出"),
        "xcag" to listOf("红"), "xcci" to listOf("双"), "xfgg" to listOf("结"),
        "xfkg" to listOf("线"), "xggh" to listOf("母"), "xgui" to listOf("贯"),
        "xhgg" to listOf("细"), "xjgp" to listOf("练"), "xlgh" to listOf("组"),
        "xln" to listOf("乡"), "xmgg" to listOf("绢"), "xngg" to listOf("纽"),
        "xnhg" to listOf("纠"), "xtah" to listOf("给"), "xuad" to listOf("终"),
        "xur" to listOf("约"), "xvgg" to listOf("继"), "xwgg" to listOf("综"),
        "xxal" to listOf("比"), "xxgg" to listOf("丝"), "xxgg" to listOf("纪"),
        "gggg" to listOf("一"), "gggh" to listOf("一"), "gggj" to listOf("一"),
        "gggk" to listOf("一"), "gggl" to listOf("一")
    )

    fun search(code: String): List<String> {
        if (code.isEmpty()) return emptyList()
        return dictionary[code.lowercase()] ?: emptyList()
    }

    fun setScheme(scheme: WubiScheme) {
        if (isNativeLoaded) {
            nativeSetScheme(scheme.code)
        }
    }

    fun enableErrorCorrection(enabled: Boolean) {
        if (isNativeLoaded) {
            nativeEnableErrorCorrection(enabled)
        }
    }

    private external fun nativeSearch(code: String): List<String>
    private external fun nativeSetScheme(schemeCode: Int)
    private external fun nativeEnableErrorCorrection(enabled: Boolean)
}

enum class WubiScheme(val code: Int) {
    WUBI_86(0),
    WUBI_98(1)
}
