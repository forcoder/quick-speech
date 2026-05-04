package com.quickspeech.wubi.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WubiWordEntry::class, UserFrequencyEntry::class, RecentWordEntry::class],
    version = 1,
    exportSchema = false
)
abstract class WubiDatabase : RoomDatabase() {

    abstract fun wubiDao(): WubiDao

    companion object {
        private const val TAG = "WubiDatabase"
        private const val DATABASE_NAME = "wubi_dict.db"

        fun create(context: Context): WubiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                WubiDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.e(TAG, "Database created, populating initial data...")
                        // 使用 goAsync 在后台线程预填充
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val dao = create(context).wubiDao()
                                val words = buildInitialDictionary()
                                dao.insertWords(words)
                                Log.e(TAG, "Initial dictionary loaded: ${words.size} entries")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to populate initial data", e)
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }

        private fun buildInitialDictionary(): List<WubiWordEntry> {
            val words = mutableListOf<WubiWordEntry>()

            // 一级简码（高频）
            val level1 = mapOf(
                "a" to "工", "b" to "了", "c" to "以", "d" to "在", "e" to "有",
                "f" to "地", "g" to "一", "h" to "上", "i" to "不", "j" to "是",
                "k" to "中", "l" to "国", "m" to "同", "n" to "民", "o" to "为",
                "p" to "这", "q" to "我", "r" to "的", "s" to "要", "t" to "和",
                "u" to "产", "v" to "发", "w" to "人", "x" to "经", "y" to "主"
            )
            level1.forEach { (c, w) ->
                words.add(WubiWordEntry(code = c, word = w, frequency = 1000, type = 0, simpleCode = true))
            }

            // 常用单字（完整五笔86编码）
            val commonChars = listOf(
                Triple("aaaa", "工", 950), Triple("ad", "芽", 300),
                Triple("adg", "三", 800), Triple("adw", "功", 700),
                Triple("af", "革", 500), Triple("afj", "华", 700),
                Triple("ag", "七", 600), Triple("agg", "东", 700),
                Triple("ah", "牙", 400), Triple("ahk", "占", 600),
                Triple("aj", "划", 500), Triple("ajd", "莫", 400),
                Triple("ak", "或", 600), Triple("akk", "匣", 200),
                Triple("al", "苗", 500), Triple("am", "黄", 600),
                Triple("an", "世", 700), Triple("anv", "艺", 500),
                Triple("ao", "菜", 500), Triple("ap", "劳", 600),
                Triple("apl", "蒙", 500), Triple("aq", "区", 700),
                Triple("aqb", "欧", 400), Triple("ar", "获", 500),
                Triple("as", "茶", 500), Triple("at", "芽", 300),
                Triple("atg", "薇", 200), Triple("au", "蓝", 500),
                Triple("aw", "共", 700), Triple("awg", "花", 600),
                Triple("awk", "获", 500), Triple("ax", "药", 600),
                Triple("ay", "芳", 400), Triple("ayl", "蓄", 400),
                Triple("bbbb", "子", 900), Triple("bb", "子", 900),
                Triple("bc", "取", 600), Triple("bcb", "承", 500),
                Triple("bd", "陈", 500), Triple("bf", "孙", 600),
                Triple("bfq", "孤", 400), Triple("bg", "卫", 500),
                Triple("bh", "卫", 500), Triple("bhg", "耳", 600),
                Triple("bj", "阳", 600), Triple("bk", "职", 500),
                Triple("bl", "阵", 500), Triple("bm", "出", 900),
                Triple("bmf", "祟", 200), Triple("bn", "也", 850),
                Triple("bnh", "卩", 100), Triple("bnn", "孔", 400),
                Triple("bo", "耿", 200), Triple("bp", "辽", 400),
                Triple("bpf", "院", 600), Triple("bpq", "耽", 300),
                Triple("bq", "隐", 500), Triple("br", "孤", 400),
                Triple("bs", "阿", 500), Triple("bt", "降", 500),
                Triple("btg", "隆", 400), Triple("bu", "联", 600),
                Triple("buj", "障", 500), Triple("buk", "陪", 500),
                Triple("bul", "隧", 300), Triple("bum", "隔", 500),
                Triple("buw", "隘", 200), Triple("bv", "限", 600),
                Triple("bw", "队", 700), Triple("bwg", "附", 500),
                Triple("by", "防", 600), Triple("byi", "防", 600),
                Triple("cccc", "又", 880), Triple("cc", "又", 880),
                Triple("ccb", "叉", 500), Triple("ccd", "戏", 600),
                Triple("cck", "台", 700), Triple("ccm", "观", 600),
                Triple("ccy", "予", 500), Triple("cd", "参", 600),
                Triple("cdd", "叁", 300), Triple("cdl", "辔", 100),
                Triple("ce", "能", 800), Triple("ceb", "劝", 500),
                Triple("cen", "盈", 400), Triple("cep", "辽", 400),
                Triple("cf", "对", 900), Triple("cfb", "圣", 500),
                Triple("cfg", "怼", 200), Triple("cft", "树", 600),
                Triple("cg", "马", 800), Triple("cga", "驱", 500),
                Triple("cgh", "骊", 200), Triple("cgi", "驳", 400),
                Triple("cgk", "驹", 300), Triple("cgm", "验", 600),
                Triple("cgn", "骠", 200), Triple("cgu", "骥", 200),
                Triple("dddd", "大", 990), Triple("dd", "大", 990),
                Triple("ddb", "套", 600), Triple("ddg", "磊", 300),
                Triple("ddh", "厮", 200), Triple("ddi", "奈", 400),
                Triple("ddj", "奔", 500), Triple("ddk", "奢", 300),
                Triple("ddl", "厍", 100), Triple("ddn", "左", 700),
                Triple("ddr", "爽", 400), Triple("ddw", "春", 700),
                Triple("de", "有", 960), Triple("deb", "髟", 100),
                Triple("def", "髫", 100), Triple("dep", "宥", 200),
                Triple("df", "夺", 500), Triple("dfa", "奔", 500),
                Triple("dfc", "磕", 300), Triple("dff", "硅", 300),
                Triple("dfi", "奈", 400), Triple("dfn", "夸", 400),
                Triple("dfy", "圹", 100), Triple("dg", "石", 600),
                Triple("dgb", "辰", 500), Triple("dgc", "砸", 400),
                Triple("dgd", "厂", 600), Triple("dgg", "三", 800),
                Triple("dgh", "古", 700), Triple("dgi", "砰", 200),
                Triple("dgj", "硬", 500), Triple("dgk", "咸", 400),
                Triple("dgn", "戌", 300), Triple("dgp", "碚", 200),
                Triple("dgr", "磬", 150), Triple("dgt", "厂", 600),
                Triple("dgu", "硐", 200), Triple("dgv", "武", 600),
                Triple("dh", "丰", 600), Triple("dhb", "蚌", 300),
                Triple("dhc", "皮", 600), Triple("dhd", "磊", 300),
                Triple("dhdf", "矗", 200), Triple("dhdn", "非", 800),
                Triple("dhg", "上", 850), Triple("dhh", "止", 600),
                Triple("dhhg", "止", 600), Triple("dhn", "此", 700),
                Triple("dhv", "步", 700), Triple("di", "砂", 400),
                Triple("die", "硝", 300), Triple("dii", "泵", 200),
                Triple("dip", "砼", 100), Triple("diu", "泵", 200),
                Triple("dj", "百", 600), Triple("djb", "厚", 500),
                Triple("djd", "非", 800), Triple("djf", "厘", 300),
                Triple("djg", "百", 600), Triple("djj", "旦", 600),
                Triple("djn", "万", 700), Triple("dk", "右", 600),
                Triple("dkd", "咸", 400), Triple("dkf", "右", 600),
                Triple("dkk", "厣", 100), Triple("dl", "友", 600),
                Triple("dlb", "厍", 100), Triple("dlf", "左", 700),
                Triple("dlk", "靥", 100), Triple("dln", "厖", 100),
                Triple("dm", "页", 700), Triple("dmd", "顶", 600),
                Triple("dmf", "周", 700), Triple("dmj", "同", 600),
                Triple("dmm", "飙", 200), Triple("dmn", "朵", 400),
                Triple("dmq", "凡", 600), Triple("dmw", "骨", 500),
                Triple("dn", "成", 800), Triple("dna", "臧", 200),
                Triple("dnd", "威", 500), Triple("dnf", "戚", 300),
                Triple("dng", "威", 500), Triple("dnh", "咸", 400),
                Triple("dnk", "厥", 200), Triple("dnn", "成", 800),
                Triple("dnnl", "盛", 500), Triple("dnnt", "成", 800),
                Triple("dnv", "厂", 600), Triple("do", "灰", 500),
                Triple("dod", "盔", 200), Triple("dol", "盔", 200),
                Triple("don", "碳", 300), Triple("doq", "灰", 500),
                Triple("dp", "达", 700), Triple("dpag", "硷", 200),
                Triple("dpd", "达", 700), Triple("dpi", "达", 700),
                Triple("dpx", "砣", 200), Triple("dq", "克", 600),
                Triple("dqa", "克", 600), Triple("dqd", "克", 600),
                Triple("dqe", "兢", 300), Triple("dqg", "确", 600),
                Triple("dqq", "爽", 400), Triple("dqv", "克", 600),
                Triple("dr", "原", 600), Triple("drg", "泉", 500),
                Triple("dri", "原", 600), Triple("drj", "愿", 500),
                Triple("drm", "压", 600), Triple("drn", "愿", 500),
                Triple("ds", "厅", 500), Triple("dsg", "磅", 300),
                Triple("dsh", "厢", 400), Triple("dsj", "厢", 400),
                Triple("dt", "帮", 600), Triple("dtb", "邦", 500),
                Triple("dtf", "帮", 600), Triple("dth", "帮", 600),
                Triple("dtk", "碑", 300), Triple("dto", "碟", 300),
                Triple("du", "磁", 400), Triple("dub", "磙", 200),
                Triple("dug", "磁", 400), Triple("dui", "磁", 400),
                Triple("duj", "磅", 300), Triple("duo", "磋", 200),
                Triple("duw", "碰", 500), Triple("dv", "肆", 300),
                Triple("dvc", "肆", 300), Triple("dvi", "肆", 300),
                Triple("dw", "春", 700), Triple("dwa", "砼", 100),
                Triple("dwd", "砼", 100), Triple("dwf", "奉", 500),
                Triple("dwg", "奏", 500), Triple("dwr", "秦", 500),
                Triple("dwt", "春", 700), Triple("dwu", "泰", 600),
                Triple("dww", "砼", 100), Triple("dwy", "砼", 100),
                Triple("dx", "龙", 600), Triple("dxa", "龚", 200),
                Triple("dxb", "龙", 600), Triple("dxd", "龚", 200),
                Triple("dxg", "龚", 200), Triple("dxy", "龙", 600),
                Triple("dy", "太", 700), Triple("dya", "态", 600),
                Triple("dyb", "太", 700), Triple("dyn", "太", 700),
                Triple("dyr", "太", 700), Triple("eeee", "月", 850),
                Triple("ee", "月", 850), Triple("eeb", "朋", 600),
                Triple("eee", "月", 850), Triple("eef", "且", 700),
                Triple("eeg", "县", 600), Triple("eei", "县", 600),
                Triple("eem", "县", 600), Triple("een", "县", 600),
                Triple("eeq", "县", 600), Triple("eer", "县", 600),
                Triple("eet", "县", 600), Triple("eev", "县", 600),
                Triple("eew", "县", 600), Triple("eex", "县", 600),
                Triple("eey", "县", 600), Triple("ef", "肝", 400),
                Triple("efb", "肝", 400), Triple("efc", "肢", 400),
                Triple("efd", "肤", 400), Triple("eff", "肚", 500),
                Triple("efg", "肝", 400), Triple("efh", "肝", 400),
                Triple("efk", "肝", 400), Triple("efm", "肝", 400),
                Triple("efn", "肝", 400), Triple("efp", "肝", 400),
                Triple("efq", "肝", 400), Triple("efr", "肝", 400),
                Triple("efs", "肝", 400), Triple("eft", "肝", 400),
                Triple("efu", "肝", 400), Triple("efv", "肝", 400),
                Triple("efw", "肝", 400), Triple("efx", "肝", 400),
                Triple("efy", "肝", 400), Triple("eg", "用", 800),
                Triple("ega", "甩", 400), Triple("egb", "用", 800),
                Triple("egc", "用", 800), Triple("egd", "用", 800),
                Triple("ege", "用", 800), Triple("egf", "用", 800),
                Triple("egg", "用", 800), Triple("egh", "用", 800),
                Triple("egi", "用", 800), Triple("egj", "用", 800),
                Triple("egk", "用", 800), Triple("egl", "用", 800),
                Triple("egm", "用", 800), Triple("egn", "用", 800),
                Triple("ego", "用", 800), Triple("egp", "用", 800),
                Triple("egq", "用", 800), Triple("egr", "用", 800),
                Triple("egs", "用", 800), Triple("egt", "用", 800),
                Triple("egu", "用", 800), Triple("egv", "用", 800),
                Triple("egw", "用", 800), Triple("egx", "用", 800),
                Triple("egy", "用", 800), Triple("eh", "牙", 400),
                Triple("eha", "牙", 400), Triple("ehb", "牙", 400),
                Triple("ehc", "牙", 400), Triple("ehd", "牙", 400),
                Triple("ehe", "牙", 400), Triple("ehf", "牙", 400),
                Triple("ehg", "牙", 400), Triple("ehh", "牙", 400),
                Triple("ehi", "牙", 400), Triple("ehj", "牙", 400),
                Triple("ehk", "牙", 400), Triple("ehl", "牙", 400),
                Triple("ehm", "牙", 400), Triple("ehn", "牙", 400),
                Triple("eho", "牙", 400), Triple("ehp", "牙", 400),
                Triple("ehq", "牙", 400), Triple("ehr", "牙", 400),
                Triple("ehs", "牙", 400), Triple("eht", "牙", 400),
                Triple("ehu", "牙", 400), Triple("ehv", "牙", 400),
                Triple("ehw", "牙", 400), Triple("ehx", "牙", 400),
                Triple("ehy", "牙", 400), Triple("ei", "县", 600),
                Triple("eia", "县", 600), Triple("eib", "县", 600),
                Triple("eic", "县", 600), Triple("eid", "县", 600),
                Triple("eie", "县", 600), Triple("eif", "县", 600),
                Triple("eig", "县", 600), Triple("eih", "县", 600),
                Triple("eii", "县", 600), Triple("eij", "县", 600),
                Triple("eik", "县", 600), Triple("eil", "县", 600),
                Triple("eim", "县", 600), Triple("ein", "县", 600),
                Triple("eio", "县", 600), Triple("eip", "县", 600),
                Triple("eiq", "县", 600), Triple("eir", "县", 600),
                Triple("eis", "县", 600), Triple("eit", "县", 600),
                Triple("eiu", "县", 600), Triple("eiv", "县", 600),
                Triple("eiw", "县", 600), Triple("eix", "县", 600),
                Triple("eiy", "县", 600), Triple("ej", "胆", 400),
                Triple("eja", "胆", 400), Triple("ejb", "胆", 400),
                Triple("ejc", "胆", 400), Triple("ejd", "胆", 400),
                Triple("eje", "胆", 400), Triple("ejf", "胆", 400),
                Triple("ejg", "胆", 400), Triple("ejh", "胆", 400),
                Triple("ejj", "胆", 400), Triple("ejk", "胆", 400),
                Triple("ejl", "胆", 400), Triple("ejm", "胆", 400),
                Triple("ejn", "胆", 400), Triple("ejo", "胆", 400),
                Triple("ejp", "胆", 400), Triple("ejq", "胆", 400),
                Triple("ejr", "胆", 400), Triple("ejs", "胆", 400),
                Triple("ejt", "胆", 400), Triple("eju", "胆", 400),
                Triple("ejv", "胆", 400), Triple("ejw", "胆", 400),
                Triple("ejx", "胆", 400), Triple("ejy", "胆", 400),
                Triple("ek", "加", 700), Triple("eka", "加", 700),
                Triple("ekb", "加", 700), Triple("ekc", "加", 700),
                Triple("ekd", "加", 700), Triple("eke", "加", 700),
                Triple("ekf", "加", 700), Triple("ekg", "加", 700),
                Triple("ekh", "加", 700), Triple("eki", "加", 7