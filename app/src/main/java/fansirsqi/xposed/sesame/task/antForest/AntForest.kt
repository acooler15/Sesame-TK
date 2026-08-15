package fansirsqi.xposed.sesame.task.antForest

import android.annotation.SuppressLint
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.entity.CollectEnergyEntity
import fansirsqi.xposed.sesame.entity.KVMap
import fansirsqi.xposed.sesame.entity.OtherEntityProvider.listEcoLifeOptions
import fansirsqi.xposed.sesame.entity.OtherEntityProvider.listHealthcareOptions
import fansirsqi.xposed.sesame.entity.VitalityStore
import fansirsqi.xposed.sesame.entity.VitalityStore.Companion.getNameById
import fansirsqi.xposed.sesame.task.GameTask
import fansirsqi.xposed.sesame.hook.RequestManager.requestString
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.hook.internal.AlipayMiniMarkHelper
import fansirsqi.xposed.sesame.hook.internal.AuthCodeHelper
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.FixedOrRangeIntervalLimit
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.IntervalLimit
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.GlobalRpcRateLimiter.addIntervalLimit
import fansirsqi.xposed.sesame.data.StatusFlags
import fansirsqi.xposed.sesame.task.antForest.EnergyPvpChallengePolicy
import fansirsqi.xposed.sesame.task.antForest.EnergyPvpDecision
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField.ListJoinCommaToStringModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.task.TaskStatus
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasBombCard
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasShield
import fansirsqi.xposed.sesame.task.antForest.Privilege.studentSignInRedEnvelope
import fansirsqi.xposed.sesame.task.antForest.Privilege.youthPrivilege
import fansirsqi.xposed.sesame.ui.ObjReference
import fansirsqi.xposed.sesame.core.util.Average
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.ListUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.notify.Notify.updateLastExecText
import fansirsqi.xposed.sesame.core.notify.Notify.updateStatusText
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.app.TaskBlacklist
import fansirsqi.xposed.sesame.core.util.TimeCounter
import fansirsqi.xposed.sesame.core.util.TimeFormatter
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.math.abs
import kotlin.math.min

/**
 * 蚂蚁森林V2
 */
class AntForest : ModelTask(), EnergyCollectCallback {
    internal val taskCount = AtomicInteger(0)
    internal var selfId: String? = null

    /** lzw add begin */
    @Volatile
    internal var monday = false

    /** lzw add end */

    internal var collectEnergy: BooleanModelField? = null // 收集能量开关
    internal var pkEnergy: BooleanModelField? = null // PK能量开关
    private var energyPvpChallenge: BooleanModelField? = null // 1V1能量挑战开关
    private var energyRain: BooleanModelField? = null // 能量雨开关
    private var advanceTime: IntegerModelField? = null // 提前时间（毫秒）
    private var tryCount: IntegerModelField? = null // 尝试收取次数
    private var retryInterval: IntegerModelField? = null // 重试间隔（毫秒）
    private var dontCollectList: SelectModelField? = null // 不收取能量的用户列表
    private var collectWateringBubble: BooleanModelField? = null // 收取浇水金球开关
    internal var batchRobEnergy: BooleanModelField? = null // 批量收取能量开关
    internal var collectSelfEnergyType: ChoiceModelField? = null // 收自己能量方式
    internal var collectSelfEnergyThreshold: IntegerModelField? = null // 收自己能量阈值
    internal var robExpandCardLimt: IntegerModelField? = null//收取翻倍能量阈值
    internal var collectBombEnergyLimit: IntegerModelField? = null // 炸弹能量收取阈值
    internal var balanceNetworkDelay: BooleanModelField? = null // 平衡网络延迟开关
    var whackMoleMode: ChoiceModelField? = null // 6秒拼手速开关

    /** 6秒拼手速游戏局数配置 */
    var whackMoleGames: IntegerModelField? = null
    var whackMoleMoleCount: IntegerModelField? = null
    var whackMoleTime: StringModelField? = null // 6秒拼手速执行时间

    // 6秒拼手速模式选择
    val whackMoleModeNames = arrayOf("关闭", "兼容", "激进")
    private var collectProp: BooleanModelField? = null // 收集道具开关
    private var queryInterval: StringModelField? = null // 查询间隔时间
    private var collectInterval: StringModelField? = null // 收取间隔时间
    private var doubleCollectInterval: StringModelField? = null // 双击间隔时间
    internal var doubleCard: ChoiceModelField? = null // 双击卡类型选择
    internal var doubleCardTime: ListJoinCommaToStringModelField? = null // 双击卡使用时间列表
    var doubleCountLimit: IntegerModelField? = null // 双击卡使用次数限制

    internal var doubleCardConstant: BooleanModelField? = null // 双击卡永动机
    internal var stealthCard: ChoiceModelField? = null // 隐身卡
    internal var stealthCardConstant: BooleanModelField? = null // 隐身卡永动机
    internal var shieldCard: ChoiceModelField? = null // 保护罩
    internal var shieldCardConstant: BooleanModelField? = null // 限时保护永动机
    internal var helpFriendCollectType: ChoiceModelField? = null
    internal var helpFriendCollectList: SelectModelField? = null

    internal var alternativeAccountList: SelectModelField? = null

    // 显示背包内容
    private var showBagList: BooleanModelField? = null

    private var vitalityExchangeList: SelectAndCountModelField? = null
    internal var returnWater33: IntegerModelField? = null
    internal var returnWater18: IntegerModelField? = null
    internal var returnWater10: IntegerModelField? = null
    private var receiveForestTaskAward: BooleanModelField? = null
    private var waterFriendList: SelectAndCountModelField? = null
    private var waterFriendCount: IntegerModelField? = null
    internal var notifyFriend: BooleanModelField? = null
    private var vitalityExchange: BooleanModelField? = null
    private var userPatrol: BooleanModelField? = null
    internal var collectGiftBox: BooleanModelField? = null
    private var medicalHealth: BooleanModelField? = null //医疗健康开关
    private var forestMarket: BooleanModelField? = null
    private var combineAnimalPiece: BooleanModelField? = null
    private var consumeAnimalProp: BooleanModelField? = null
    internal var whoYouWantToGiveTo: SelectModelField? = null
    private var dailyCheckIn: BooleanModelField? = null //青春特权签到
    internal var bubbleBoostCard: ChoiceModelField? = null //加速卡
    internal var youthPrivilege: BooleanModelField? = null //青春特权 森林道具
    private var ecoLife: BooleanModelField? = null
    private var ecoLifeTime: StringModelField? = null // 绿色行动执行时间
    internal var giveProp: BooleanModelField? = null

    internal var robExpandCard: ChoiceModelField? = null //1.1倍能量卡
    private val robExpandCardTime: ListModelField? = null //1.1倍能量卡时间

    internal var cycleinterval: IntegerModelField? = null
    private var energyRainChance: BooleanModelField? = null
    private var energyRainTime: StringModelField? = null // 能量雨执行时间

    /**
     * 能量炸弹卡
     */
    internal var energyBombCardType: ChoiceModelField? = null
    private var ecoDailyTask: BooleanModelField? = null // 7天环保打卡
    private var gift7thSign: BooleanModelField? = null // 7天签到

    private var forestChouChouLe: BooleanModelField? = null //森林抽抽乐

    /**
     * 加速器定时
     */
    internal var bubbleBoostTime: ListJoinCommaToStringModelField? = null

    private val forestTaskTryCount: ConcurrentHashMap<String, AtomicInteger> = ConcurrentHashMap<String, AtomicInteger>()

    internal val shieldManager = ForestShieldManager(this)

    internal val itemManager = ForestItemManager(this)

    internal val energyCollector = ForestEnergyCollector(this)

    // {{ 新增接口定义：收自己能量的方式 }}
    interface CollectSelfType {
        companion object {
            const val ALL = 0
            const val OVER_THRESHOLD = 1
            const val BELOW_THRESHOLD = 2
            val nickNames = arrayOf("所有", "大于阈值", "小于阈值")
        }
    }

    override fun getName(): String {
        return "蚂蚁森林"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FOREST
    }

    override fun getIcon(): String {
        return "AntForest.png"
    }

    interface ApplyPropType {
        companion object {
            const val CLOSE: Int = 0
            const val ALL: Int = 1
            const val ONLY_LIMIT_TIME: Int = 2
            val nickNames: Array<String?> = arrayOf<String?>("关闭", "所有道具", "限时道具")
        }
    }

    interface HelpFriendCollectType {
        companion object {
            const val NONE: Int = 0
            const val HELP: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("关闭", "选中复活", "选中不复活")
        }
    }

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(
            BooleanModelField(
                "collectEnergy",
                "收集能量 | 开关",
                false
            ).also { collectEnergy = it })
        modelFields.addField(
            BooleanModelField(
                "batchRobEnergy",
                "一键收取 | 开关",
                false
            ).also { batchRobEnergy = it })
        modelFields.addField(
            BooleanModelField(
                "pkEnergy",
                "Pk榜收取 | 开关",
                false
            ).also { pkEnergy = it })
        modelFields.addField(
            BooleanModelField(
                "energyPvpChallenge",
                "1V1能量挑战 | 领奖",
                false
            ).also { energyPvpChallenge = it })
        // 在 ModelFields 定义中修改
        modelFields.addField(
            ChoiceModelField(
                "whackMoleMode",
                "🎮 6秒拼手速 | 运行模式",
                0, // 默认值为 0 (关闭)
                whackMoleModeNames
            ).also { whackMoleMode = it }
        )
        modelFields.addField(
            IntegerModelField(
                "whackMoleGames",
                "🎮 6秒拼手速 | 激进模式局数",
                5,
            ).also { whackMoleGames = it })
        modelFields.addField(
            IntegerModelField(
                "whackMoleMoleCount",
                "🎮 6秒拼手速 | 兼容模式击打数",
                15,
            ).also { whackMoleMoleCount = it })
        modelFields.addField(
            StringModelField(
                "whackMoleTime",
                "🎮 6秒拼手速 | 执行时间",
                "0820"
            ).also { whackMoleTime = it }
        )
        modelFields.addField(
            BooleanModelField(
                "energyRain",
                "能量雨 | 开关",
                false
            ).also { energyRain = it })
        modelFields.addField(
            StringModelField(
                "energyRainTime",
                "能量雨 | 默认8点10分后执行",
                "0810"
            ).also { energyRainTime = it })
        modelFields.addField(
            ChoiceModelField(
                "CollectSelfEnergyType",
                "收自己单个能量球 | 方式",
                CollectSelfType.ALL,
                CollectSelfType.nickNames
            ).also { collectSelfEnergyType = it }
        )
        modelFields.addField(
            IntegerModelField(
                "CollectSelfEnergyThreshold",
                "收自己单个能量球阈值",
                0,
                0,
                10000
            ).also { collectSelfEnergyThreshold = it }
        )
        modelFields.addField(
            IntegerModelField(
                "robExpandCardLimt",
                "收取翻倍能量阈值",
                20000,
                1,
                20000
            ).also { robExpandCardLimt = it }
        )

        modelFields.addField(
            IntegerModelField(
                "CollectBombEnergyLimit",
                "单个炸弹能量大于该值收取",
                0,
                0,
                100000
            ).also { collectBombEnergyLimit = it }
        )
        modelFields.addField(
            SelectModelField(
                "dontCollectList",
                "不收能量 | 配置列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also {
                dontCollectList = it
            })
        modelFields.addField(
            SelectModelField(
                "giveEnergyRainList",
                "赠送能量雨 | 配置列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also {
                giveEnergyRainList = it
            })
        modelFields.addField(
            BooleanModelField(
                "energyRainChance",
                "兑换使用能量雨次卡 | 开关",
                false
            ).also { energyRainChance = it })
        modelFields.addField(
            BooleanModelField(
                "collectWateringBubble",
                "收取浇水金球 | 开关",
                false
            ).also { collectWateringBubble = it })
        modelFields.addField(
            ChoiceModelField(
                "doubleCard",
                "双击卡开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { doubleCard = it })
        modelFields.addField(
            IntegerModelField(
                "doubleCountLimit",
                "双击卡 | 使用次数",
                6
            ).also { doubleCountLimit = it })
        modelFields.addField(
            ListJoinCommaToStringModelField(
                "doubleCardTime", "双击卡 | 使用时间/范围", ListUtil.newArrayList(
                    "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359"
                )
            ).also { doubleCardTime = it })
        // 双击卡永动机
        modelFields.addField(
            BooleanModelField(
                "DoubleCardConstant", "限时双击永动机 | 开关", false
            ).also { doubleCardConstant = it }
        )
        modelFields.addField(
            ChoiceModelField(
                "bubbleBoostCard",
                "加速器开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { bubbleBoostCard = it })
        modelFields.addField(
            ListJoinCommaToStringModelField(
                "bubbleBoostTime", "加速器 | 使用时间/不能范围", ListUtil.newArrayList(
                    "0030,0630",
                    "0700",
                    "1200",
                    "1730",
                    "2359"
                )
            ).also { bubbleBoostTime = it })
        modelFields.addField(
            ChoiceModelField(
                "shieldCard",
                "保护罩开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { shieldCard = it })
        modelFields.addField(
            BooleanModelField(
                "shieldCardConstant",
                "限时保护永动机 | 开关",
                false
            ).also { shieldCardConstant = it })

        modelFields.addField(
            ChoiceModelField(
                "energyBombCardType", "炸弹卡开关 | 消耗类型", ApplyPropType.CLOSE,
                ApplyPropType.nickNames, "若开启了保护罩，则不会使用炸弹卡"
            ).also { energyBombCardType = it })
        modelFields.addField(
            ChoiceModelField(
                "robExpandCard",
                "1.1倍能量卡开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { robExpandCard = it })
        //1.1倍能量卡时间
        modelFields.addField(
            ListJoinCommaToStringModelField(
                "robExpandCardTime", "1.1倍能量卡 | 使用时间/不能范围",
                ListUtil.newArrayList(
                    "0700",
                    "0730",
                    "1200",
                    "1230",
                    "1700",
                    "1730",
                    "2000",
                    "2030",
                    "2359"
                )
            )
        )
        modelFields.addField(
            ChoiceModelField(
                "stealthCard",
                "隐身卡开关 | 消耗类型",
                ApplyPropType.CLOSE,
                ApplyPropType.nickNames
            ).also { stealthCard = it })
        modelFields.addField(
            BooleanModelField(
                "stealthCardConstant",
                "限时隐身永动机 | 开关",
                false
            ).also { stealthCardConstant = it })
        modelFields.addField(
            IntegerModelField(
                "returnWater10",
                "返水 | 10克需收能量(关闭:0)",
                0
            ).also { returnWater10 = it })
        modelFields.addField(
            IntegerModelField(
                "returnWater18",
                "返水 | 18克需收能量(关闭:0)",
                0
            ).also { returnWater18 = it })
        modelFields.addField(
            IntegerModelField(
                "returnWater33",
                "返水 | 33克需收能量(关闭:0)",
                0
            ).also { returnWater33 = it })
        modelFields.addField(
            SelectAndCountModelField(
                "waterFriendList",
                "浇水 | 好友列表",
                LinkedHashMap<String?, Int?>(),
                { AlipayUser.getList() },
                "记得设置浇水次数"
            ).also { waterFriendList = it })
        modelFields.addField(
            IntegerModelField(
                "waterFriendCount",
                "浇水 | 克数(10 18 33 66)",
                66
            ).also { waterFriendCount = it })
        modelFields.addField(
            BooleanModelField(
                "notifyFriend",
                "浇水 | 通知好友",
                false
            ).also { notifyFriend = it })
        modelFields.addField(
            BooleanModelField(
                "giveProp",
                "赠送道具",
                false
            ).also { giveProp = it })
        modelFields.addField(
            SelectModelField(
                "whoYouWantToGiveTo",
                "赠送 | 道具",
                LinkedHashSet<String?>(),
                { AlipayUser.getList() },
                "所有可赠送的道具将全部赠"
            ).also { whoYouWantToGiveTo = it })
        modelFields.addField(
            BooleanModelField(
                "collectProp",
                "收集道具",
                false
            ).also { collectProp = it })
        modelFields.addField(
            ChoiceModelField(
                "helpFriendCollectType",
                "复活能量 | 选项",
                HelpFriendCollectType.NONE,
                HelpFriendCollectType.nickNames
            ).also { helpFriendCollectType = it })
        modelFields.addField(
            SelectModelField(
                "helpFriendCollectList",
                "复活能量 | 好友列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also {
                helpFriendCollectList = it
            })
        modelFields.addField(
            SelectModelField(
                "alternativeAccountList",
                "小号列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also {
                alternativeAccountList = it
            })
        modelFields.addField(BooleanModelField("vitalityExchange", "活力值 | 兑换开关", false).also { vitalityExchange = it })
        modelFields.addField(
            SelectAndCountModelField(
                "vitalityExchangeList", "活力值 | 兑换列表", LinkedHashMap<String?, Int?>(),
                VitalityStore::list,
                "记得填兑换次数..亲爱的"
            ).also { vitalityExchangeList = it })
        modelFields.addField(BooleanModelField("userPatrol", "保护地巡护", false).also { userPatrol = it })
        modelFields.addField(BooleanModelField("combineAnimalPiece", "合成动物碎片", false).also { combineAnimalPiece = it })
        modelFields.addField(BooleanModelField("consumeAnimalProp", "派遣动物伙伴", false).also { consumeAnimalProp = it })
        modelFields.addField(BooleanModelField("receiveForestTaskAward", "森林任务", false).also { receiveForestTaskAward = it })

        modelFields.addField(BooleanModelField("forestChouChouLe", "森林寻宝任务", false).also { forestChouChouLe = it })

        modelFields.addField(BooleanModelField("collectGiftBox", "领取礼盒", false).also { collectGiftBox = it })
        modelFields.addField(BooleanModelField("ecoDailyTask", "森林任务 | 环保打卡", false).also { ecoDailyTask = it })
        modelFields.addField(BooleanModelField("gift7thSign", "森林7天签到 | 新用户", false).also { gift7thSign = it })
        modelFields.addField(BooleanModelField("medicalHealth", "健康医疗任务 | 开关", false).also { medicalHealth = it })
        modelFields.addField(
            SelectModelField(
                "medicalHealthOption", "健康医疗 | 选项", LinkedHashSet<String?>(), listHealthcareOptions(),
                "医疗健康需要先完成一次医疗打卡"
            ).also { medicalHealthOption = it })

        modelFields.addField(BooleanModelField("forestMarket", "森林集市", false).also { forestMarket = it })
        modelFields.addField(BooleanModelField("youthPrivilege", "青春特权 | 森林道具", false).also { youthPrivilege = it })
        modelFields.addField(BooleanModelField("studentCheckIn", "青春特权 | 签到红包", false).also { dailyCheckIn = it })
        modelFields.addField(BooleanModelField("ecoLife", "绿色行动 | 开关", false).also { ecoLife = it })
        modelFields.addField(StringModelField("ecoLifeTime", "绿色行动 | 默认8点后执行", "0800").also { ecoLifeTime = it })
        modelFields.addField(BooleanModelField("ecoLifeOpen", "绿色任务 |  自动开通", false).also { ecoLifeOpen = it })
        modelFields.addField(
            SelectModelField(
                "ecoLifeOption", "绿色行动 | 选项", LinkedHashSet<String?>(), listEcoLifeOptions(), "光盘行动需要先完成一次光盘打卡"
            ).also { ecoLifeOption = it })

        modelFields.addField(StringModelField("queryInterval", "查询间隔(毫秒或毫秒范围)", "1000-2000").also { queryInterval = it })
        modelFields.addField(StringModelField("collectInterval", "收取间隔(毫秒或毫秒范围)", "1000-1500").also { collectInterval = it })
        modelFields.addField(StringModelField("doubleCollectInterval", "双击间隔(毫秒或毫秒范围)", "800-2400").also { doubleCollectInterval = it })
        modelFields.addField(BooleanModelField("balanceNetworkDelay", "平衡网络延迟", true).also { balanceNetworkDelay = it })
        modelFields.addField(IntegerModelField("advanceTime", "提前时间(毫秒)", 0, Int.MIN_VALUE, 500).also { advanceTime = it })
        modelFields.addField(IntegerModelField("tryCount", "尝试收取(次数)", 1, 0, 5).also { tryCount = it })
        modelFields.addField(IntegerModelField("retryInterval", "重试间隔(毫秒)", 1200, 0, 10000).also { retryInterval = it })
        modelFields.addField(IntegerModelField("cycleinterval", "循环间隔(毫秒)", 5000, 0, 10000).also { cycleinterval = it })
        modelFields.addField(BooleanModelField("showBagList", "显示背包内容", true).also { showBagList = it })
        return modelFields
    }

    override fun check(): Boolean {
        if (!super.check()) return false
        val currentTime = System.currentTimeMillis()
        // 1️⃣ 异常等待状态
        val forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime)
        if (forestPauseTime > currentTime) {
            Log.record(getName() + "任务-异常等待中，暂不执行检测！")
            return false
        }
        // -----------------------------
        // 3️⃣ 只收能量时间段判断
        // -----------------------------
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val isEnergyTime = TaskCommon.IS_ENERGY_TIME || hour == 7 && minute < 30
        if (isEnergyTime) {
            // 关键改动：将循环放入后台线程，避免阻塞TaskRunner
            GlobalThreadPools.execute({ this.energyCollector.startEnergyCollectionLoop() })
            return false // 只收能量期间不执行正常任务，check()立刻返回
        }
        return true
    }

    /**
     * 创建区间限制对象
     *
     * @param intervalStr 区间字符串，如 "1000-2000"
     * @param defaultMin 默认最小值
     * @param defaultMax 默认最大值
     * @param description 描述，用于日志
     * @return 区间限制对象
     */
    private fun createSafeIntervalLimit(
        intervalStr: String?,
        defaultMin: Int,
        defaultMax: Int,
        description: String?
    ): FixedOrRangeIntervalLimit {
        // 记录原始输入值
        Log.record(TAG, description + "原始设置值: [" + intervalStr + "]")

        // 使用自定义区间限制类，处理所有边界情况
        val limit = FixedOrRangeIntervalLimit(intervalStr, defaultMin, defaultMax)
        Log.record(TAG, description + "成功创建区间限制")
        return limit
    }

    override fun boot(classLoader: ClassLoader?) {
        super.boot(classLoader)
        instance = this


        // 安全创建各种区间限制
        val queryIntervalLimit = createSafeIntervalLimit(
            queryInterval!!.value, 10, 10000, "查询间隔"
        )

        // 添加RPC间隔限制
        addIntervalLimit("alipay.antforest.forest.h5.queryHomePage", queryIntervalLimit)
        addIntervalLimit("alipay.antforest.forest.h5.queryFriendHomePage", queryIntervalLimit)
        addIntervalLimit("alipay.antmember.forest.h5.collectEnergy", 300)
        addIntervalLimit("alipay.antmember.forest.h5.queryEnergyRanking", 300)
        addIntervalLimit("alipay.antforest.forest.h5.fillUserRobFlag", 500)

        // 设置其他参数
        energyCollector.tryCountInt = tryCount!!.value
        energyCollector.retryIntervalInt = retryInterval!!.value
        advanceTime!!.value


        energyCollector.jsonCollectMap = dontCollectList!!.value

        // 创建收取间隔实体
        energyCollector.collectIntervalEntity = createSafeIntervalLimit(
            collectInterval!!.value, 50, 10000, "收取间隔"
        )

        // 创建双击收取间隔实体
        energyCollector.doubleCollectIntervalEntity = createSafeIntervalLimit(
            doubleCollectInterval!!.value, 10, 5000, "双击间隔"
        )
        energyCollector.delayTimeMath.clear()


        AntForestRpcCall.init()

        // 设置蹲点管理器的回调
        EnergyWaitingManager.setEnergyCollectCallback(this)
    }

    override suspend fun runSuspend() {
        val runStartTime = System.currentTimeMillis()
        Log.record(TAG, "🌲🌲🌲 森林主任务开始执行 🌲🌲🌲")
        val authCode = AuthCodeHelper.getAuthCode("2060170000363691" )
        val MiniMark = AlipayMiniMarkHelper.getAlipayMiniMark("2060170000363691" ,"1.0.1")
        Log.record(TAG, "游戏 2060170000363691 获取到的 authCode: $authCode   Mark:$MiniMark")
        try {
            // 每次运行时检查并更新计数器
            checkAndUpdateCounters()
            // 正常流程会自动处理所有收取任务，无需特殊处理
            errorWait = false
            // 计数器和时间记录
            monday = true
            val tc = TimeCounter(TAG)
            if (showBagList!!.value) itemManager.showBag()

            Log.record(TAG, "执行开始-蚂蚁${getName()}")
            taskCount.set(0)
            selfId = UserMap.currentUid

            // -------------------------------
            // 自己使用道具
            // -------------------------------
            // 先查询主页，更新道具状态（双击卡、保护罩等的剩余时间）
            itemManager.updateSelfHomePage()
            tc.countDebug("查询道具状态")

            itemManager.usePropBeforeCollectEnergy(selfId)
            tc.countDebug("使用自己道具卡")

            // -------------------------------
            // 收好友能量
            // -------------------------------
            // 先尝试使用找能量功能快速定位有能量的好友（协程）
            Log.record(TAG, "🚀 执行找能量功能（协程）")
            energyCollector.collectEnergyByTakeLook()
            tc.countDebug("找能量收取（协程）")

            // -------------------------------
            // 收PK好友能量
            // -------------------------------
            Log.record(TAG, "🚀 异步执行PK好友能量收取")
            energyCollector.collectPKEnergyCoroutine()  // 好友道具在 collectFriendEnergy 内会自动处理
            tc.countDebug("收PK好友能量（同步）")

            // -------------------------------
            // 收自己能量
            // -------------------------------
            Log.record(TAG, "🌳 【正常流程】开始收取自己的能量...")
            val selfHomeObj = run {
                val obj = querySelfHome()
                tc.countDebug("获取自己主页对象信息")
                if (obj != null) {

                    energyCollector.collectEnergy(UserMap.currentUid, obj, "self")
                    Log.record(TAG, "✅ 【正常流程】收取自己的能量完成")
                    tc.countDebug("收取自己的能量")
                } else {
                    Log.error(TAG, "❌ 【正常流程】获取自己主页信息失败，跳过能量收取")
                    tc.countDebug("跳过自己的能量收取（主页获取失败）")
                }
                obj
            }

            handleEnergyPvpChallenge()

            // 然后执行传统的好友排行榜收取（协程）
            Log.record(TAG, "🚀 执行好友能量收取（协程）")
            energyCollector.collectFriendEnergyCoroutine() // 内部会自动调用 usePropBeforeCollectEnergy(userId, false)
            tc.countDebug("收取好友能量（同步）")

            // -------------------------------
            // 后续任务流程
            // -------------------------------
            if (selfHomeObj != null) {
                // 检查并处理打地鼠（每天一次）
                checkAndHandleWhackMole()
                tc.countDebug("拼手速")

                val processObj = if (isTeam(selfHomeObj)) {
                    selfHomeObj.optJSONObject("teamHomeResult")
                        ?.optJSONObject("mainMember")
                } else {
                    selfHomeObj
                }

                // 新用户7天签到
                if (gift7thSign!!.value) {
                    processGift7thSign()
                }

                if (collectWateringBubble!!.value) {
                    wateringBubbles(processObj)
                    tc.countDebug("收取浇水金球")
                }
                if (collectProp!!.value) {
                    givenProps(processObj)
                    tc.countDebug("收取道具")
                }
                if (userPatrol!!.value) {
                    queryUserPatrol()
                    tc.countDebug("动物巡护任务")
                }

                itemManager.handleUserProps(selfHomeObj)
                tc.countDebug("收取动物派遣能量")

                itemManager.collectEnergyBomb(selfHomeObj)
                tc.countDebug("收取炸弹卡能量")

                if (itemManager.canConsumeAnimalProp && consumeAnimalProp!!.value) {
                    queryAndConsumeAnimal()
                    tc.countDebug("森林巡护")
                } else {
                    Log.record("已经有动物伙伴在巡护森林~")
                }

                if (combineAnimalPiece!!.value) {
                    queryAnimalAndPiece()
                    tc.countDebug("合成动物碎片")
                }

                if (receiveForestTaskAward!!.value) {
                    receiveTaskAward()
                    tc.countDebug("森林任务")
                }
                if (ecoLife!!.value) {
                    // 检查是否到达执行时间
                    if (TaskTimeChecker.isTimeReached(ecoLifeTime?.value, "0800")) {
                        EcoLife.ecoLife()
                        tc.countDebug("绿色行动")
                    } else {
                        Log.record(TAG, "绿色行动未到执行时间，跳过")
                    }
                }

                waterFriends()
                tc.countDebug("给好友浇水")

                if (giveProp!!.value) {
                    itemManager.giveProp()
                    tc.countDebug("赠送道具")
                }

                if (vitalityExchange!!.value) {
                    handleVitalityExchange()
                    tc.countDebug("活力值兑换")
                }

                if (energyRain!!.value) {
                    // 检查是否到达执行时间
                    if (TaskTimeChecker.isTimeReached(energyRainTime?.value, "0810")) {
                        if (energyRainChance!!.value) {
                            itemManager.useEnergyRainChanceCard()
                            tc.countDebug("使用能量雨卡")
                        }
                        EnergyRainCoroutine.execEnergyRainCompat()
                        tc.countDebug("能量雨")
                    } else {
                        Log.record(TAG, "能量雨未到执行时间，跳过")
                    }
                }

                if (forestMarket!!.value) {
                    GreenLife.ForestMarket("GREEN_LIFE")
                    //  GreenLife.ForestMarket("ANTFOREST")  二级条目暂时关闭
                    tc.countDebug("森林集市")
                }

                if (medicalHealth!!.value) {
                    if (medicalHealthOption!!.value.contains("FEEDS")) {
                        Healthcare.queryForestEnergy("FEEDS")
                        tc.countDebug("绿色医疗")
                    }
                    if (medicalHealthOption!!.value.contains("BILL")) {
                        Healthcare.queryForestEnergy("BILL")
                        tc.countDebug("电子小票")
                    }
                }

                //青春特权森林道具领取
                if (youthPrivilege!!.value) {
                    youthPrivilege()
                }

                if (dailyCheckIn!!.value) {
                    studentSignInRedEnvelope()
                }

                if (forestChouChouLe!!.value) {
                    val chouChouLe = ForestChouChouLe()
                    chouChouLe.chouChouLe()
                    tc.countDebug("抽抽乐")
                }

                doforestgame()
                queryOptionalPlay() // 限时乐园活动

                tc.stop()
            }
        } catch (e: CancellationException) {
            // 协程被取消是正常行为，不记录错误日志
            Log.record(TAG, "蚂蚁森林任务协程被取消")
            throw e // 重新抛出，让协程系统处理
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "执行蚂蚁森林任务时发生错误: ", t)
        } finally {
            // 计算总耗时
            val totalTime = System.currentTimeMillis() - runStartTime
            val timeInSeconds = totalTime / 1000

            // 优化：不再等待蹲点任务完成，让主任务立即结束
            // 蹲点任务会在后台独立协程中继续运行，不影响其他模块
            val waitingTaskCount = EnergyWaitingManager.getWaitingTaskCount()

            Log.record(TAG, "=".repeat(50))
            Log.record(TAG, "🌲🌲🌲 森林主任务执行完毕 🌲🌲🌲")
            Log.record(TAG, "⏱️ 主任务耗时: ${timeInSeconds}秒 (${totalTime}ms)")
            Log.record(TAG, "📊 收取统计: 收${ForestStatistics.totalCollected}g 帮${ForestStatistics.TOTAL_HELP_COLLECTED}g 浇${ForestStatistics.TOTAL_WATERED}g")
            if (waitingTaskCount > 0) {
                Log.record(TAG, "⏰ 后台蹲点任务: $waitingTaskCount 个 (将在指定时间自动收取)")
                // 输出详细的蹲点任务状态，帮助调试
                val taskStatus = EnergyWaitingManager.getWaitingTasksStatus()
                Log.record(TAG, "📋 $taskStatus")
            } else {
                Log.record(TAG, "✅ 无后台蹲点任务")
            }
            Log.record(TAG, "=".repeat(50))

            energyCollector.clearRoundCaches()
            // 清空好友主页缓存
            val strTotalCollected =
                "本次总 收:" + ForestStatistics.totalCollected + "g 帮:" + ForestStatistics.TOTAL_HELP_COLLECTED + "g 浇:" + ForestStatistics.TOTAL_WATERED + "g"
            updateLastExecText(strTotalCollected)
        }
    }

    /**
     * 每日重置
     */
    // 上次检查的日期（用于判断是否跨天）
    private var lastCheckDate: String? = null

    private fun checkAndUpdateCounters() {
        val today = TimeUtil.getDateStr() // 获取当前日期，如 "2025-10-07"
        // 只在日期变化时重置计数器（跨天）
        if (lastCheckDate != today) {
            resetTaskCounters()
            lastCheckDate = today
            Log.record(TAG, "✅ 检测到新的一天[$today]，重置计数器")
        }
    }

    // 重置任务计数器（你需要根据具体任务的计数器来调整）
    private fun resetTaskCounters() {
        taskCount.set(0) // 重置任务计数
        // 每日重置时清空频率限制记录，让所有好友都有新的机会
        ForestUtil.clearAllFrequencyLimits()
        Log.record(TAG, "任务计数器已重置")
    }

    /**
     * 定义一个 处理器接口
     */
    private fun interface JsonArrayHandler {
        suspend fun handle(array: JSONArray?)
    }

    private suspend fun processJsonArray(
        initialObj: JSONObject?,
        arrayKey: String?,
        handler: JsonArrayHandler
    ) {
        var hasMore: Boolean
        var currentObj = initialObj
        do {
            val jsonArray = currentObj?.optJSONArray(arrayKey)
            if (jsonArray != null && jsonArray.length() > 0) {
                handler.handle(jsonArray)
                // 判断是否还有更多数据（比如返回满20个）
                hasMore = jsonArray.length() >= 20
            } else {
                hasMore = false
            }
            if (hasMore) {
                GlobalThreadPools.sleepCompat(2000L) // 防止请求过快被限制
                currentObj = querySelfHome() // 获取下一页数据
            }
        } while (hasMore)
    }

    private suspend fun wateringBubbles(selfHomeObj: JSONObject?) {
        processJsonArray(
            selfHomeObj,
            "wateringBubbles"
        ) { wateringBubbles: JSONArray? ->
            energyCollector.collectWateringBubbles(
                wateringBubbles!!
            )
        }
    }

    private suspend fun givenProps(selfHomeObj: JSONObject?) {
        processJsonArray(selfHomeObj, "givenProps") { givenProps: JSONArray? ->
            itemManager.collectGivenProps(
                givenProps!!
            )
        }
    }

    /**
     * 给好友浇水
     */
    private suspend fun waterFriends() {
        try {
            val friendMap = waterFriendList!!.value
            val notify = notifyFriend!!.value // 获取通知开关状态

            for (friendEntry in friendMap.entries) {
                val uid = friendEntry.key!!
                if (selfId == uid) {
                    continue
                }
                var waterCount = friendEntry.value
                if (waterCount == null || waterCount <= 0) {
                    continue
                }
                waterCount = min(waterCount, 3)

                if (Status.canWaterFriendToday(uid, waterCount)) {
                    try {
                        val response = AntForestRpcCall.queryFriendHomePage(uid, null)
                        val jo = JSONObject(response)
                        if (ResChecker.checkRes(TAG, jo)) {
                            val bizNo = jo.getString("bizNo")

                            // ✅ 关键改动：传入通知开关
                            val waterCountKVNode = returnFriendWater(
                                uid, bizNo, waterCount, waterFriendCount!!.value, notify
                            )

                            val actualWaterCount: Int = waterCountKVNode.key!!
                            if (actualWaterCount > 0) {
                                Status.waterFriendToday(uid, actualWaterCount)
                            }
                            if (java.lang.Boolean.FALSE == waterCountKVNode.value) {
                                break
                            }
                        } else {
                            Log.record(jo.getString("resultDesc"))
                        }
                    } catch (e: JSONException) {
                        Log.record(TAG, "waterFriends JSON解析错误: " + e.message)
                    } catch (t: Throwable) {
                        Log.printStackTrace(TAG, t)
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "未知错误: " + e.message, e)
        }
    }

    private fun handleVitalityExchange() {
        try {
//            JSONObject bag = getBag();

            Vitality.initVitality("SC_ASSETS")
            val exchangeList = vitalityExchangeList!!.value
            //            Map<String, Integer> maxLimitList = vitalityExchangeMaxList.value;
            for (entry in exchangeList.entries) {
                val skuId = entry.key!!
                val count = entry.value
                if (count == null || count <= 0) {
                    Log.record(TAG, "无效的count值: skuId=$skuId, count=$count")
                    continue
                }
                // 处理活力值兑换
                while (Status.canVitalityExchangeToday(skuId, count)) {
                    if (!Vitality.handleVitalityExchange(skuId)) {
                        Log.record(TAG, "活力值兑换失败: " + getNameById(skuId))
                        break
                    }
                    GlobalThreadPools.sleepCompat(1000L)
                }
            }
        } catch (t: Throwable) {
            handleException("handleVitalityExchange", t)
        }
    }

    internal fun notifyMain() {
        if (taskCount.decrementAndGet() < 1) {
            synchronized(this@AntForest) {
                (this@AntForest as Object).notifyAll()
            }
        }
    }

    /**
     * 获取自己主页对象信息
     *
     * @return 用户的主页信息，如果发生错误则返回null。
     */
    internal suspend fun querySelfHome(): JSONObject? {
        var userHomeObj: JSONObject? = null
        try {
            val start = System.currentTimeMillis()
            val response = AntForestRpcCall.queryHomePage()
            if (response.trim { it <= ' ' }.isEmpty()) {
                //               Log.error(TAG, "获取自己主页信息失败：响应为空$response")
                return null
            }
            userHomeObj = JSONObject(response)
            // 检查响应是否成功
            if (!ResChecker.checkRes(TAG + "查询自己主页失败:", userHomeObj)) {
                Log.error(TAG, "查询自己主页失败: " + userHomeObj.optString("resultDesc", "未知错误"))
                return null
            }

            itemManager.updateSelfHomePage(userHomeObj)
            val end = System.currentTimeMillis()
            // 安全获取服务器时间，如果没有则使用当前时间
            val serverTime = userHomeObj.optLong("now", System.currentTimeMillis())
            val offsetTime = offsetTimeMath.nextInteger(((start + end) / 2 - serverTime).toInt())
            // Log.record(TAG, "服务器时间：$serverTime，本地与服务器时间差：$offsetTime")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "查询自己主页异常", t)
        }
        return userHomeObj
    }

    /**
     * 更新好友主页信息
     *
     * @param userId 好友ID
     * @return 更新后的好友主页信息，如果发生错误则返回null。
     */
    internal suspend fun queryFriendHome(userId: String?, fromAct: String?): JSONObject? {
        var friendHomeObj: JSONObject? = null
        try {
            val start = System.currentTimeMillis()
            val response = AntForestRpcCall.queryFriendHomePage(userId, fromAct)
            if (response.trim { it <= ' ' }.isEmpty()) {
                //               Log.error( TAG, "获取好友主页信息失败：响应为空, userId: " + UserMap.getMaskName(userId) + response)
                return null
            }
            friendHomeObj = JSONObject(response)
            // 检查响应是否成功
            if (!ResChecker.checkRes(TAG + "查询好友主页失败:", friendHomeObj)) {
                // 检测并记录"手速太快"错误，避免日志刷屏
                ForestUtil.checkAndRecordFrequencyError(userId, friendHomeObj)
                return null
            }
            val end = System.currentTimeMillis()
            // 安全获取服务器时间，如果没有则使用当前时间
            val serverTime = friendHomeObj.optLong("now", System.currentTimeMillis())
            val offsetTime = offsetTimeMath.nextInteger(((start + end) / 2 - serverTime).toInt())
            //  Log.record(TAG, "服务器时间：$serverTime，本地与服务器时间差：$offsetTime")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "查询好友主页异常, userId: " + UserMap.getMaskName(userId), t)
        }
        return friendHomeObj // 返回用户主页对象
    }

    /**
     * 格式化时间差为人性化的字符串（保持向后兼容）
     * @param milliseconds 时差毫秒
     */
    internal fun formatTimeDifference(milliseconds: Long): String {
        return TimeFormatter.formatTimeDifference(milliseconds)
    }

    /**
     * 检查并处理6秒拼手速逻辑（每天主动执行一次）
     */
    private fun checkAndHandleWhackMole() {
        try {
            // 获取当前选择的索引 (0, 1, 或 2)
            val modeIndex = whackMoleMode?.value ?: 0

            // 如果索引为 0 (关闭)，直接返回
            if (modeIndex == 0) return

            // 检查执行时间
            val targetTime = whackMoleTime?.value ?: "0820"
            if (TaskTimeChecker.isTimeReached(targetTime, "0820")) {

                val whackMoleFlag = "forest::whackMole::executed"
                if (Status.hasFlagToday(whackMoleFlag)) return

                // 根据索引匹配模式
                when (modeIndex) {
                    1 -> { // 兼容模式
                        Log.record(TAG, "🎮 触发拼手速任务: 兼容模式")
                        WhackMole.setTotalGames(1)
                        WhackMole.setMoleCount(whackMoleMoleCount?.value ?: 15)
                        WhackMole.start(WhackMole.Mode.COMPATIBLE)
                    }

                    2 -> { // 激进模式
                        Log.record(TAG, "🎮 触发拼手速任务: 激进模式")
                        val configGames = whackMoleGames?.value ?: 5
                        WhackMole.setTotalGames(configGames)
                        WhackMole.start(WhackMole.Mode.AGGRESSIVE)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 为好友浇水并返回浇水次数和是否可以继续浇水的状态。
     *
     * @param userId       好友的用户ID
     * @param bizNo        业务编号
     * @param count        需要浇水的次数
     * @param waterEnergy  每次浇水的能量值
     * @param notifyFriend 是否通知好友
     * @return KVMap 包含浇水次数和是否可以继续浇水的状态
     */
    internal suspend fun returnFriendWater(
        userId: String?,
        bizNo: String?,
        count: Int,
        waterEnergy: Int,
        notifyFriend: Boolean
    ): KVMap<Int?, Boolean?> {
        // bizNo为空直接返回默认
        if (bizNo == null || bizNo.isEmpty()) {
            return KVMap(0, true)
        }

        var wateredTimes = 0 // 已浇水次数
        var isContinue = true // 是否可以继续浇水

        try {
            val energyId = getEnergyId(waterEnergy)

            var waterCount = 1
            label@ while (waterCount <= count) {
                // 调用RPC进行浇水，并传入是否通知好友
                val rpcResponse =
                    AntForestRpcCall.transferEnergy(userId, bizNo, energyId, notifyFriend)

                if (rpcResponse.isEmpty()) {
                    Log.record(TAG, "好友浇水返回空: " + UserMap.getMaskName(userId))
                    isContinue = false
                    break
                }

                val jo = JSONObject(rpcResponse)

                // 先处理可能的错误码
                val errorCode = jo.optString("error")
                if ("1009" == errorCode) { // 访问被拒绝
                    Log.record(TAG, "好友浇水🚿访问被拒绝: " + UserMap.getMaskName(userId))
                    isContinue = false
                    break
                } else if ("3000" == errorCode) { // 系统错误
                    Log.record(TAG, "好友浇水🚿系统错误，稍后重试: " + UserMap.getMaskName(userId))
                    delay(500)
                    waterCount-- // 重试当前次数
                    waterCount++
                    continue
                }

                // 处理正常返回
                val resultCode = jo.optString("resultCode")
                when (resultCode) {
                    "SUCCESS" -> {
                        val userBaseInfo = jo.optJSONObject("userBaseInfo")
                        val currentEnergy = userBaseInfo?.optInt(
                            "currentEnergy",
                            0
                        ) ?: "未知"
                        val totalEnergy = userBaseInfo?.optInt(
                            "totalEnergy",
                            0
                        ) ?: "未知"
                        Log.forest("好友浇水🚿[${UserMap.getMaskName(userId)}]#$waterEnergy g，当前能量状态 [$currentEnergy/$totalEnergy g]")
                        wateredTimes++
                        GlobalThreadPools.sleepCompat(1200L)
                    }

                    "WATERING_TIMES_LIMIT" -> {
                        Log.record(TAG, "好友浇水🚿今日已达上限: " + UserMap.getMaskName(userId))
                        wateredTimes = 3 // 上限假设3次
                        break@label
                    }

                    "ENERGY_INSUFFICIENT" -> {
                        Log.record(TAG, "好友浇水🚿" + jo.optString("resultDesc"))
                        isContinue = false
                        break@label
                    }

                    else -> {
                        Log.record(TAG, "好友浇水🚿" + jo.optString("resultDesc"))
                        Log.record(jo.toString())
                    }
                }
                waterCount++
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "returnFriendWater err", t)
        }

        return KVMap(wateredTimes, isContinue)
    }

    /**
     * 获取能量ID
     */
    private fun getEnergyId(waterEnergy: Int): Int {
        if (waterEnergy <= 0) return 0
        if (waterEnergy >= 66) return 42
        if (waterEnergy >= 33) return 41
        if (waterEnergy >= 18) return 40
        return 39
    }

    /**
     * 执行当天森林签到任务
     *
     * @param forestSignVOList 森林签到列表
     * @return 获得的能量，如果签到失败或已签到则返回 0
     */
    private suspend fun dailyTask(forestSignVOList: JSONArray): Int {
        try {
            val forestSignVO = forestSignVOList.getJSONObject(0)
            val currentSignKey = forestSignVO.getString("currentSignKey") // 当前签到的 key
            val signId = forestSignVO.getString("signId") // 签到ID
            val sceneCode = forestSignVO.getString("sceneCode") // 场景代码
            val signRecords = forestSignVO.getJSONArray("signRecords") // 签到记录
            for (i in 0..<signRecords.length()) { //遍历签到记录
                val signRecord = signRecords.getJSONObject(i)
                val signKey = signRecord.getString("signKey")
                val awardCount = signRecord.optInt("awardCount", 0)
                if (signKey == currentSignKey && !signRecord.getBoolean("signed")) {
                    val joSign = JSONObject(
                        AntForestRpcCall.antiepSign(
                            signId,
                            UserMap.currentUid,
                            sceneCode
                        )
                    )
                    GlobalThreadPools.sleepCompat(300) // 等待300毫秒
                    if (ResChecker.checkRes(TAG + "森林签到失败:", joSign)) {
                        Log.forest("森林签到📆成功")
                        return awardCount
                    }
                    break
                }
            }
            return 0 // 如果没有签到，则返回 0
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return 0
        }
    }

    /**
     * 森林任务:
     * 逛目标应用会员,去森林寻宝抽1t能量
     * 防治荒漠化和干旱日,给随机好友一键浇水
     * 开通高德活动领,去吉祥林许个愿
     * 逛森林集市得能量,逛一逛618会场
     * 逛一逛点淘得红包,去一淘签到领红包
     */
    private suspend fun receiveTaskAward() {
        try {
            // 使用统一的任务黑名单管理器，包含默认黑名单和用户自定义黑名单
            while (true) {
                var doubleCheck = false // 标记是否需要再次检查任务
                val s = AntForestRpcCall.queryTaskList() // 查询任务列表
                val jo = JSONObject(s) // 解析响应为 JSON 对象

                if (!ResChecker.checkRes(TAG + "查询森林任务失败:", jo)) {
                    Log.record(jo.getString("resultDesc")) // 记录失败描述
                    //Log.runtime(s) // 打印响应内容
                    break
                }
                // 提取森林任务列表
                val forestSignVOList = jo.getJSONArray("forestSignVOList")
                var sumawardCount = 0
                val dailyawardCount = dailyTask(forestSignVOList) // 执行每日任务
                sumawardCount += dailyawardCount

                // 提取森林任务
                val forestTasksNew = jo.optJSONArray("forestTasksNew")
                if (forestTasksNew == null || forestTasksNew.length() == 0) {
                    break // 如果没有新任务，则返回
                }

                // {{ 定义递归处理函数，支持处理嵌套子任务 }}
                suspend fun processTask(taskInfo: JSONObject): Boolean {
                    var actionTaken = false

                    // 1. 获取基础信息
                    val taskBaseInfo = taskInfo.optJSONObject("taskBaseInfo") ?: return false
                    val taskType = taskBaseInfo.getString("taskType")
                    val taskStatus = taskBaseInfo.getString("taskStatus")
                    val sceneCode = taskBaseInfo.getString("sceneCode")

                    // 2. 环保打卡任务过滤
                    if (taskType.contains("DAKA") && !ecoDailyTask!!.value) {
                        return false
                    }

                    // 3. 递归处理子任务 (childrenFirst)
                    val childTaskTypeList = taskInfo.optJSONArray("childTaskTypeList")
                    if (childTaskTypeList != null && childTaskTypeList.length() > 0) {
                        for (k in 0 until childTaskTypeList.length()) {
                            if (processTask(childTaskTypeList.getJSONObject(k))) {
                                actionTaken = true
                            }
                        }
                    }

                    // 4. 黑名单检查
                    if (TaskBlacklist.isTaskInBlacklist(taskType)) return actionTaken

                    val bizInfoStr = taskBaseInfo.optString("bizInfo")
                    val bizInfo = if (bizInfoStr.isNotEmpty()) JSONObject(bizInfoStr) else JSONObject()
                    val taskTitle = bizInfo.optString("taskTitle", taskType)

                    val taskRightsStr = taskInfo.optString("taskRights")
                    val taskRights = if (taskRightsStr.isNotEmpty()) JSONObject(taskRightsStr) else JSONObject()
                    val awardCount = taskRights.optInt("awardCount", 0)

                    // 5. 执行任务逻辑
                    if (TaskStatus.FINISHED.name == taskStatus) {
                        // 领取任务奖励
                        val joAward = JSONObject(AntForestRpcCall.receiveTaskAward(sceneCode, taskType))
                        if (ResChecker.checkRes(TAG + "领取森林任务奖励失败:", joAward)) {
                            Log.forest("森林奖励🎖️[$taskTitle]# ${awardCount}活力值")
                            sumawardCount += awardCount
                            actionTaken = true
                        } else {
                            Log.error(TAG, "领取失败: $taskTitle")
                            Log.record(joAward.toString())
                        }
                        GlobalThreadPools.sleepCompat(500)
                    } else if (TaskStatus.TODO.name == taskStatus) {
                        // 执行待完成任务
                        val bizKey = sceneCode + "_" + taskType
                        val count = forestTaskTryCount.computeIfAbsent(bizKey) { AtomicInteger(0) }.incrementAndGet()

                        val joFinishTask = JSONObject(AntForestRpcCall.finishTask(sceneCode, taskType))

                        if (!ResChecker.checkRes(TAG + "完成森林任务失败:", joFinishTask)) {
                            val errorCode = joFinishTask.optString("code", "")
                            TaskBlacklist.autoAddToBlacklist(taskType, taskTitle, errorCode)
                            if (count > 1) {
                                TaskBlacklist.addToBlacklist(taskType, taskTitle)
                            }
                        } else {
                            Log.forest("森林任务🧾️[$taskTitle]")
                            actionTaken = true
                        }
                    }

                    // 6. 特殊任务处理：游戏任务
                    if ("mokuai_senlin_hlz" == taskType) {
                        val gameUrl = bizInfo.optString("taskJumpUrl")
                        Log.record(TAG, "跳转到游戏: $gameUrl")
                        Log.record(TAG, "等待30S")
                        GlobalThreadPools.sleepCompat(30000)
                        val joFinishTask = JSONObject(AntForestRpcCall.finishTask(sceneCode, taskType))
                        val error = joFinishTask.optString("code", "")
                        if (ResChecker.checkRes(TAG + "完成游戏任务失败:", joFinishTask)) {
                            Log.forest("游戏任务完成 🎮️[$taskTitle]# ${awardCount}活力值")
                            sumawardCount += awardCount
                            actionTaken = true
                        } else {
                            TaskBlacklist.autoAddToBlacklist(taskType, taskTitle, error)
                        }
                    }

                    return actionTaken
                }

                // 遍历顶层任务列表
                for (i in 0..<forestTasksNew.length()) {
                    val forestTask = forestTasksNew.getJSONObject(i)
                    val taskInfoList = forestTask.getJSONArray("taskInfoList")
                    for (j in 0..<taskInfoList.length()) {
                        if (processTask(taskInfoList.getJSONObject(j))) {
                            doubleCheck = true
                        }
                    }
                }

                if (!doubleCheck) break
            }
        } catch (t: Throwable) {
            handleException("receiveTaskAward", t)
        }
    }

    /**
     * 查询并管理用户巡护任务
     */
    private suspend fun queryUserPatrol() {
        val waitTime = 300L //增大查询等待时间，减少异常
        try {
            do {
                // 查询当前巡护任务
                var jo = JSONObject(AntForestRpcCall.queryUserPatrol())
                // GlobalThreadPools.sleepCompat(waitTime);
                // 如果查询成功
                if (ResChecker.checkRes(TAG + "查询巡护任务失败:", jo)) {
                    // 查询我的巡护记录
                    var resData = JSONObject(AntForestRpcCall.queryMyPatrolRecord())
                    // GlobalThreadPools.sleepCompat(waitTime);
                    if (resData.optBoolean("canSwitch")) {
                        val records = resData.getJSONArray("records")
                        for (i in 0..<records.length()) {
                            val record = records.getJSONObject(i)
                            val userPatrol = record.getJSONObject("userPatrol")
                            // 如果存在未到达的节点，且当前模式为"silent"，则尝试切换巡护地图
                            if (userPatrol.getInt("unreachedNodeCount") > 0) {
                                if ("silent" == userPatrol.getString("mode")) {
                                    val patrolConfig = record.getJSONObject("patrolConfig")
                                    val patrolId = patrolConfig.getString("patrolId")
                                    resData =
                                        JSONObject(AntForestRpcCall.switchUserPatrol(patrolId))
                                    GlobalThreadPools.sleepCompat(waitTime)
                                    // 如果切换成功，打印日志并继续
                                    if (ResChecker.checkRes(TAG + "切换巡护地图失败:", resData)) {
                                        Log.forest("巡护⚖️-切换地图至$patrolId")
                                    }
                                    continue  // 跳过当前循环
                                }
                                break // 如果当前不是silent模式，则结束循环
                            }
                        }
                    }
                    // 获取用户当前巡护状态信息
                    val userPatrol = jo.getJSONObject("userPatrol")
                    val currentNode = userPatrol.getInt("currentNode")
                    val currentStatus = userPatrol.getString("currentStatus")
                    val patrolId = userPatrol.getInt("patrolId")
                    val chance = userPatrol.getJSONObject("chance")
                    val leftChance = chance.getInt("leftChance")
                    val leftStep = chance.getInt("leftStep")
                    val usedStep = chance.getInt("usedStep")
                    if ("STANDING" == currentStatus) { // 当前巡护状态为"STANDING"
                        if (leftChance > 0) { // 如果还有剩余的巡护次数，则开始巡护
                            jo = JSONObject(AntForestRpcCall.patrolGo(currentNode, patrolId))
                            GlobalThreadPools.sleepCompat(waitTime)
                            patrolKeepGoing(jo.toString(), currentNode, patrolId) // 继续巡护
                            continue  // 跳过当前循环
                        } else if (leftStep >= 2000 && usedStep < 10000) { // 如果没有剩余的巡护次数但步数足够，则兑换巡护次数
                            jo = JSONObject(AntForestRpcCall.exchangePatrolChance(leftStep))
                            // GlobalThreadPools.sleepCompat(waitTime);
                            if (ResChecker.checkRes(TAG + "兑换巡护次数失败:", jo)) { // 兑换成功，增加巡护次数
                                val addedChance = jo.optInt("addedChance", 0)
                                Log.forest("步数兑换⚖️[巡护次数*$addedChance]")
                                continue  // 跳过当前循环
                            } else {
                                Log.record(TAG, jo.getString("resultDesc"))
                            }
                        }
                    } else if ("GOING" == currentStatus) {
                        patrolKeepGoing(null, currentNode, patrolId)
                    }
                } else {
                    Log.record(TAG, jo.getString("resultDesc"))
                }
                break // 完成一次巡护任务后退出循环
            } while (true)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryUserPatrol err", t) // 打印异常堆栈
        }
    }

    /**
     * 持续巡护森林，直到巡护状态不再是"进行中"
     *
     * @param s         巡护请求的响应字符串，若为null将重新请求
     * @param nodeIndex 当前节点索引
     * @param patrolId  巡护任务ID
     */
    private suspend fun patrolKeepGoing(s: String?, nodeIndex: Int, patrolId: Int) {
        var s = s
        try {
            do {
                if (s == null) {
                    s = AntForestRpcCall.patrolKeepGoing(nodeIndex, patrolId, "image")
                }
                val jo: JSONObject?
                try {
                    jo = JSONObject(s)
                } catch (e: JSONException) {
                    Log.printStackTrace(TAG, "JSON解析错误: " + e.message, e)
                    return  // 解析失败，退出循环
                }
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.record(TAG, jo.getString("resultDesc"))
                    break
                }
                val events = jo.optJSONArray("events")
                if (events == null || events.length() == 0) {
                    return  // 无事件，退出循环
                }
                val event = events.getJSONObject(0)
                val userPatrol = jo.getJSONObject("userPatrol")
                val currentNode = userPatrol.getInt("currentNode")
                // 获取奖励信息，并处理动物碎片奖励
                val rewardInfo = event.optJSONObject("rewardInfo")
                if (rewardInfo != null) {
                    val animalProp = rewardInfo.optJSONObject("animalProp")
                    if (animalProp != null) {
                        val animal = animalProp.optJSONObject("animal")
                        if (animal != null) {
                            Log.forest("巡护森林🏇🏻[" + animal.getString("name") + "碎片]")
                        }
                    }
                }
                // 如果巡护状态不是"进行中"，则退出循环
                if ("GOING" != jo.getString("currentStatus")) {
                    return
                }
                // 请求继续巡护
                val materialInfo = event.getJSONObject("materialInfo")
                val materialType = materialInfo.optString("materialType", "image")
                s = AntForestRpcCall.patrolKeepGoing(currentNode, patrolId, materialType)
                GlobalThreadPools.sleepCompat(100) // 等待100毫秒后继续巡护
            } while (true)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "patrolKeepGoing err", t)
        }
    }

    /**
     * 查询并派遣伙伴
     */
    private suspend fun queryAndConsumeAnimal() {
        try {
            // 查询动物属性列表
            var jo = JSONObject(AntForestRpcCall.queryAnimalPropList())
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.record(TAG, jo.getString("resultDesc"))
                return
            }
            // 获取所有动物属性并选择可以派遣的伙伴
            val animalProps = jo.getJSONArray("animalProps")
            var bestAnimalProp: JSONObject? = null
            for (i in 0..<animalProps.length()) {
                jo = animalProps.getJSONObject(i)
                if (bestAnimalProp == null || jo.getJSONObject("main")
                        .getInt("holdsNum") > bestAnimalProp.getJSONObject("main")
                        .getInt("holdsNum")
                ) {
                    bestAnimalProp = jo // 默认选择最大数量的伙伴
                }
            }
            // 派遣伙伴
            consumeAnimalProp(bestAnimalProp)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryAnimalPropList err", t)
        }
    }

    /**
     * 派遣伙伴进行巡护
     *
     * @param animalProp 选择的动物属性
     */
    private suspend fun consumeAnimalProp(animalProp: JSONObject?) {
        if (animalProp == null) return  // 如果没有可派遣的伙伴，则返回

        try {
            // 获取伙伴的属性信息
            val propGroup = animalProp.getJSONObject("main").getString("propGroup")
            val propType = animalProp.getJSONObject("main").getString("propType")
            val name = animalProp.getJSONObject("partner").getString("name")
            // 调用API进行伙伴派遣
            val jo = JSONObject(AntForestRpcCall.consumeProp(propGroup, "", propType, false))
            if (ResChecker.checkRes(TAG + "巡护派遣失败:", jo)) {
                Log.forest("巡护派遣🐆[$name]")
            } else {
                Log.record(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "consumeAnimalProp err", t)
        }
    }

    /**
     * 查询动物及碎片信息，并尝试合成可合成的动物碎片。
     */
    private suspend fun queryAnimalAndPiece() {
        try {
            // 调用远程接口查询动物及碎片信息
            val response = JSONObject(AntForestRpcCall.queryAnimalAndPiece(0))
            val resultCode = response.optString("resultCode")
            // 检查接口调用是否成功
            if ("SUCCESS" != resultCode) {
                Log.record(TAG, "查询失败: " + response.optString("resultDesc"))
                return
            }
            // 获取动物属性列表
            val animalProps = response.optJSONArray("animalProps")
            if (animalProps == null || animalProps.length() == 0) {
                Log.record(TAG, "动物属性列表为空")
                return
            }
            // 遍历动物属性
            for (i in 0..<animalProps.length()) {
                val animalObject = animalProps.optJSONObject(i) ?: continue
                val pieces = animalObject.optJSONArray("pieces")
                if (pieces == null || pieces.length() == 0) {
                    Log.record(TAG, "动物碎片列表为空")
                    continue
                }
                val animalId =
                    animalObject.optJSONObject("animal")?.optInt("id", -1) ?: -1
                if (animalId == -1) {
                    Log.record(TAG, "动物ID缺失")
                    continue
                }
                // 检查碎片是否满足合成条件
                if (canCombinePieces(pieces)) {
                    combineAnimalPiece(animalId)
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "queryAnimalAndPiece err", e)
        }
    }

    /**
     * 检查碎片是否满足合成条件。
     *
     * @param pieces 动物碎片数组
     * @return 如果所有碎片满足合成条件，返回 true；否则返回 false
     */
    private fun canCombinePieces(pieces: JSONArray): Boolean {
        for (j in 0..<pieces.length()) {
            val pieceObject = pieces.optJSONObject(j)
            if (pieceObject == null || pieceObject.optInt("holdsNum", 0) <= 0) {
                return false
            }
        }
        return true
    }

    /**
     * 合成动物碎片。
     *
     * @param animalId 动物ID
     */
    private suspend fun combineAnimalPiece(animalId: Int) {
        var animalId = animalId
        try {
            while (true) {
                // 查询动物及碎片信息
                val response = JSONObject(AntForestRpcCall.queryAnimalAndPiece(animalId))
                var resultCode = response.optString("resultCode")
                if ("SUCCESS" != resultCode) {
                    Log.record(TAG, "查询失败: " + response.optString("resultDesc"))
                    break
                }
                val animalProps = response.optJSONArray("animalProps")
                if (animalProps == null || animalProps.length() == 0) {
                    Log.record(TAG, "动物属性数据为空")
                    break
                }
                // 获取第一个动物的属性
                val animalProp = animalProps.getJSONObject(0)
                val animal: JSONObject = checkNotNull(animalProp.optJSONObject("animal"))
                val id = animal.optInt("id", -1)
                val name = animal.optString("name", "未知动物")
                // 获取碎片信息
                val pieces = animalProp.optJSONArray("pieces")
                if (pieces == null || pieces.length() == 0) {
                    Log.record(TAG, "碎片数据为空")
                    break
                }
                var canCombineAnimalPiece = true
                val piecePropIds = JSONArray()
                // 检查所有碎片是否可用
                for (j in 0..<pieces.length()) {
                    val piece = pieces.optJSONObject(j)
                    if (piece == null || piece.optInt("holdsNum", 0) <= 0) {
                        canCombineAnimalPiece = false
                        Log.record(TAG, "碎片不足，无法合成动物")
                        break
                    }
                    // 添加第一个道具ID
                    piece.optJSONArray("propIdList")?.optString(0, "")?.let { propId ->
                        piecePropIds.put(propId)
                    }
                }
                // 如果所有碎片可用，则尝试合成
                if (canCombineAnimalPiece) {
                    val combineResponse =
                        JSONObject(AntForestRpcCall.combineAnimalPiece(id, piecePropIds.toString()))
                    resultCode = combineResponse.optString("resultCode")
                    if ("SUCCESS" == resultCode) {
                        Log.forest("成功合成动物💡[$name]")
                        animalId = id
                        GlobalThreadPools.sleepCompat(100) // 等待一段时间再查询
                        continue
                    } else {
                        Log.record(TAG, "合成失败: " + combineResponse.optString("resultDesc"))
                    }
                }
                break // 如果不能合成或合成失败，跳出循环
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "combineAnimalPiece err", e)
        }
    }

    private suspend fun processGift7thSign() {
        try {
            val sceneCode = "ANTFOREST_GIFT7TH_SIGN_202506"
            val s = AntForestRpcCall.queryCommonSign(sceneCode)
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询7天签到失败:", jo)) {
                val forestSignVO = jo.optJSONObject("forestSignVO") ?: return
                val currentSignKey = forestSignVO.optString("currentSignKey")
                val signRecords = forestSignVO.optJSONArray("signRecords") ?: return
                var signed = false
                for (i in 0 until signRecords.length()) {
                    val record = signRecords.getJSONObject(i)
                    if (record.optString("signKey") == currentSignKey) {
                        signed = record.optBoolean("signed")
                        break
                    }
                }
                if (!signed) {
                    val signRes = JSONObject(AntForestRpcCall.antiepSign(UserMap.currentUid, sceneCode))
                    if (ResChecker.checkRes(TAG + "7天签到失败:", signRes)) {
                        val awardName = signRes.optJSONObject("signModel")
                            ?.optJSONObject("signAward")
                            ?.optJSONObject("bizInfo")
                            ?.optString("awardName", "奖励")
                        Log.forest("7天签到📅[$awardName]")
                    }
                } else {
                    Log.record(TAG, "7天签到📅已完成")
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "processGift7thSign err", t)
        }
    }

    suspend fun doforestgame() {
        try {
            val response = AntForestRpcCall.queryGameList()
            val jo = JSONObject(response)

            // 验证请求是否成功
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.error(TAG, "queryGameList 失败: ${jo.optString("desc")}")
                return
            }

            val drawRights = jo.optJSONObject("gameCenterDrawRights")
            if (drawRights != null) {
                val perTime = drawRights.optInt("quotaPerTime", 100)

                // 换算实际宝箱次数
                val canUseCount = drawRights.optInt("quotaCanUse") / perTime
                val limitCount = drawRights.optInt("quotaLimit") / perTime
                val usedCount = drawRights.optInt("usedQuota") / perTime

                //Log.record(TAG, "游戏中心状态: 待开 $canUseCount 个, 已得 $usedCount/$limitCount")

                // 1. 处理待开启奖励 (批量开启)
                if (canUseCount > 0) {
                    Log.record(TAG, "正在一次性开启 $canUseCount 个宝箱...")
                    val drawResStr = AntForestRpcCall.drawGameCenterAward(canUseCount)
                    if(!ResChecker.checkRes(TAG, drawResStr)){
                        //Log.error(TAG,"开启宝箱失败 Res:$drawResStr")
                        return
                    }
                    val drawJo = JSONObject(drawResStr)
                    val resData = drawJo.optJSONObject("resData") ?: drawJo
                    if (resData.optString("desc") == "success") {
                        val awardList = resData.optJSONArray("gameCenterDrawAwardList")

                        var totalEnergy = 0
                        val otherAwards = mutableListOf<String>()

                        if (awardList != null) {
                            for (i in 0 until awardList.length()) {
                                val award = awardList.getJSONObject(i)
                                val type = award.optString("awardType")
                                val name = award.optString("awardName")
                                val count = award.optInt("awardCount")

                                if (type == "ENERGY") {
                                    totalEnergy += count
                                } else {
                                    otherAwards.add("${name}x${count}")
                                }
                            }
                        }

                        // 输出统计结果
                        val logMsg = StringBuilder("[开宝箱] ")
                        if (totalEnergy > 0) logMsg.append("获得能量: ${totalEnergy}g")
                        if (otherAwards.isNotEmpty()) {
                            if (totalEnergy > 0) logMsg.append(", ")
                            logMsg.append("其他: ${otherAwards.joinToString("/")}")
                        }
                        Log.forest(logMsg.toString())
                    } else {
                        //Log.error(TAG, "领奖请求失败: $drawResStr")
                    }
                }

                // 2. 判断是否需要刷任务 (接你之前的逻辑)
                val remainToTask = limitCount - usedCount
                if (remainToTask > 0) {

                        //Log.record(TAG, "任务进度未满，准备执行 $remainToTask 次上报...")
                GameTask.Forest_slxcc.report(remainToTask)


                } else {
                   // Log.record(TAG, "今日游戏中心任务已满额")
                }
            }

        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "doforestgame 流程异常", t)
        }
    }

    //森林乐园限定活动
    suspend fun queryOptionalPlay() {
        try {
            val jo = JSONObject(AntForestRpcCall.queryOptionalPlay())
            if (!ResChecker.checkRes(TAG+"森林乐园限定活动", jo)) {
                return
            }
            if (!jo.has("taskTriggerPlayInfo")) {
                return
            }
            val taskTriggerPlayInfo = jo.optJSONObject("taskTriggerPlayInfo")
            if (!taskTriggerPlayInfo.has("taskList")) {
                return
            }

            val taskList = taskTriggerPlayInfo.getJSONArray("taskList")
            for (j in 0 until taskList.length()) {
                val task = taskList.getJSONObject(j)
                val taskStatus = task.getString("taskStatus")
                val alreadyReceiveAwardCount = task.optInt("alreadyReceiveAwardCount")
                val awardCount = task.optInt("awardCount")
                val awardCountForReceive = awardCount - alreadyReceiveAwardCount
                var awardType = task.optString("awardType", "能量")
                val bizInfo = task.getJSONObject("bizInfo")
                val title = bizInfo.getString("title")
                val source = task.optString("source", "ch_appcenter__chsub_9patch")
                val sceneCode = task.optString("sceneCode", "")
                val taskType = task.optString("taskType", "")
                // 记录任务状态
                if (taskStatus == "FINISHED") {
                    if (awardCountForReceive > 0) {
                        // 领取奖励
                        val joReceived = JSONObject(AntForestRpcCall.receiveTaskAwardopengreen(source, sceneCode, taskType))
                        if (ResChecker.checkRes(TAG+"森林乐园限定活动领取奖励", joReceived)) {
                            val incAwardCount = joReceived.optInt("incAwardCount")
                            val taskConfigResultVO = joReceived.optJSONObject("taskConfigResultVO")
                            if (taskConfigResultVO != null) {
                                awardType = taskConfigResultVO.optString("awardType", awardType)
                            }
                            Log.forest("森林乐园🎖️领取[" + title + "]奖励[" + awardType + "*" + incAwardCount + "]")
                        } else {
                            Log.i(TAG, "森林乐园❌领取[" + title + "]奖励失败")
                            Log.e(TAG, "领取奖励失败响应: $joReceived")
                        }
                    }
                }
            }
        }catch (e: Exception) {
            Log.printStackTrace(TAG,"queryOptionalPlay err:", e)
        }
    }
    /**
     * 通用错误处理器
     * @param operation 操作名称
     * @param throwable 异常对象
     */
    internal fun handleException(operation: String?, throwable: Throwable) {
        if (throwable is JSONException) {
            // JSON解析错误通常是网络响应问题，只记录错误信息不打印堆栈，避免刷屏
            Log.error(TAG, operation + " JSON解析错误: " + throwable.message)
        } else {
            Log.error(TAG, operation + " 错误: " + throwable.message)
            Log.printStackTrace(TAG, throwable)
        }
    }

    companion object {
        val TAG: String = AntForest::class.java.getSimpleName()

        var instance: AntForest? = null


        private val offsetTimeMath = Average(5)


        var giveEnergyRainList: SelectModelField? = null //能量雨赠送列表
        var medicalHealthOption: SelectModelField? = null //医疗健康选项
        var ecoLifeOption: SelectModelField? = null

        /**
         * 异常返回检测开关
         */
        internal var errorWait = false
        var ecoLifeOpen: BooleanModelField? = null
    }

    /**
     * 实现EnergyCollectCallback接口
     * 为蹲点管理器提供能量收取功能（增强版）
     */
    override fun addToTotalCollected(energyCount: Int) {
        ForestStatistics.addToTotalCollected(energyCount)
    }

    override fun getWaitingCollectDelay(): Long {
        return 0L // 立即收取，无延迟
    }

    override suspend fun collectUserEnergyForWaiting(task: EnergyWaitingManager.WaitingTask): CollectResult {
        return energyCollector.collectUserEnergyForWaiting(task)
    }

    /**
     * 判断是否为团队
     *
     * @param homeObj 用户主页的JSON对象
     * @return 是否为团队
     */
    internal fun isTeam(homeObj: JSONObject): Boolean {
        return homeObj.optString("nextAction", "") == "Team"
    }

    /**
     * 手动触发森林打地鼠
     */
    suspend fun manualWhackMole(modeIndex: Int, games: Int) {
        try {
            val obj = querySelfHome()
            if (obj != null) {
                // 确定模式：1 为兼容，2 为激进
                val mode = if (modeIndex == 2) WhackMole.Mode.AGGRESSIVE else WhackMole.Mode.COMPATIBLE

                // 设置本次执行的总局数
                WhackMole.setTotalGames(games)
                WhackMole.setMoleCount(whackMoleMoleCount?.value ?: 15)

                Log.record(
                    TAG,
                    "🎮 手动触发拼手速任务: ${if (mode == WhackMole.Mode.AGGRESSIVE) "激进模式" else "兼容模式"}, 目标局数: $games"
                )

                // 执行游戏
                WhackMole.startSuspend(mode)
            } else {
                Log.record(TAG, "无法获取自己主页信息")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 手动运行能量雨逻辑
     * @param exchange 是否先尝试兑换并使用能量雨卡
     */
    suspend fun manualUseEnergyRain(exchange: Boolean) {
        try {
            Log.record(TAG, "🚀 开始执行手动能量雨任务...")
            val obj = querySelfHome()
            if (obj != null) {

                if (exchange) {
                    Log.record(TAG, "尝试兑换并激活能量雨卡...")
                    itemManager.useEnergyRainChanceCard()
                }

                EnergyRainCoroutine.execEnergyRainCompat()
                Log.record(TAG, "✅ 手动能量雨任务处理完毕")
            } else {
                Log.record(TAG, "无法获取自己主页信息")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "manualUseEnergyRain 异常:", t)
        }
    }

    private suspend fun handleEnergyPvpChallenge() {
        if (energyPvpChallenge?.value != true ||
            Status.hasFlagToday(StatusFlags.FLAG_ANTFOREST_ENERGY_PVP_CHALLENGE_DONE)
        ) {
            return
        }

        try {
            val entry = parseEnergyPvpResponse(AntForestRpcCall.queryEnergyPvpInfo())
            val home = parseEnergyPvpResponse(AntForestRpcCall.queryPvpHomeInfo())
            logEnergyPvpRecord(
                "当前场次",
                energyPvpPayload(home)?.optJSONObject("currentEnergyPvpBattleRecord")
            )
            logEnergyPvpRecord(
                "上一场次",
                energyPvpPayload(home)?.optJSONObject("previousEnergyPvpBattleRecord")
            )

            when (EnergyPvpChallengePolicy.decide(entry, home)) {
                EnergyPvpDecision.CLAIM -> {
                    if (claimEnergyPvpRewards()) {
                        Status.setFlagToday(
                            StatusFlags.FLAG_ANTFOREST_ENERGY_PVP_CHALLENGE_DONE
                        )
                    }
                }

                EnergyPvpDecision.RETRY_LATER ->
                    Log.forest("1V1能量挑战：状态未终结或响应不完整，保留后续重试")

                EnergyPvpDecision.DONE -> {
                    Log.forest("1V1能量挑战：今日暂无待领取奖励")
                    Status.setFlagToday(
                        StatusFlags.FLAG_ANTFOREST_ENERGY_PVP_CHALLENGE_DONE
                    )
                }
            }
        } catch (t: Throwable) {
            handleException("handleEnergyPvpChallenge", t)
        }
    }

    private suspend fun claimEnergyPvpRewards(): Boolean {
        val response = parseEnergyPvpResponse(AntForestRpcCall.receivePvpRewards())
            ?: run {
                Log.forest("1V1能量挑战领奖响应为空，可能是暂无可领取奖励")
                return true
            }
        if (!ResChecker.checkRes("$TAG 1V1能量挑战领奖失败:", response)) {
            val code = response.optString("resultCode")
            val message = response.optString("resultDesc")
                .ifBlank { response.optString("memo") }
            if (EnergyPvpChallengePolicy.isTerminalClaimResult(code, message)) {
                Log.forest("1V1能量挑战奖励已处理：$message")
                return true
            }
            Log.forest("1V1能量挑战领奖失败：$code $message")
            return false
        }

        val rewards = energyPvpPayload(response)?.optJSONArray("receivedRewards")
            ?: response.optJSONArray("receivedRewards")
        Log.forest(
            "1V1能量挑战领奖成功：" +
                EnergyPvpChallengePolicy.summarizeRewards(rewards)
        )
        reviewEnergyPvpRecords()
        return true
    }

    private suspend fun reviewEnergyPvpRecords() {
        val response = parseEnergyPvpResponse(
            AntForestRpcCall.queryPvpBattleRecords(5)
        ) ?: return
        if (ResChecker.checkRes("$TAG 复查1V1能量挑战记录失败:", response)) {
            val hasRewards = energyPvpPayload(response)
                ?.optBoolean("hasRewards", false)
                ?: false
            Log.forest("1V1能量挑战领奖复查：hasRewards=$hasRewards")
        }
    }

    private fun parseEnergyPvpResponse(raw: String): JSONObject? {
        if (raw.isBlank()) {
            return null
        }
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    private fun energyPvpPayload(response: JSONObject?): JSONObject? {
        return response?.optJSONObject("data")
            ?: response?.optJSONObject("result")
            ?: response
    }

    private fun logEnergyPvpRecord(label: String, record: JSONObject?) {
        if (record == null) {
            return
        }
        Log.forest(
            "1V1能量挑战：$label status=${record.optString("battleStatus")} " +
                "result=${record.optString("battleResult")} " +
                "energy=${record.optInt("attackerEnergy")}g:" +
                "${record.optInt("defenderEnergy")}g"
        )
    }
}