package fansirsqi.xposed.sesame.task.antSports

import android.annotation.SuppressLint
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.reflect.ReflectUtil
import fansirsqi.xposed.sesame.core.threads.CoroutineUtils
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.data.StatusFlags
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.task.ModelTask.ChildModelTask
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * @brief 蚂蚁运动：走路兑换子模块
 *
 * @details
 * 承载步数同步、新旧版行走路线、慈善捐能量与捐步兑换逻辑，
 * 由主类 {@link AntSports} 委托调用。
 */
internal class SportsExchangeManager(private val sports: AntSports) {

    /**
     * @brief 计算今日用于同步的随机步数
     *
     * @return 步数值（最大 100000）
     */
    internal fun tmpStepCount(): Int {
        if (sports.tmpStepCount >= 0) {
            return sports.tmpStepCount
        }
        sports.tmpStepCount = sports.syncStepCount.value
        if (sports.tmpStepCount > 0) {
            sports.tmpStepCount = RandomUtil.nextInt(sports.tmpStepCount, sports.tmpStepCount + 2000)
            if (sports.tmpStepCount > 100_000) {
                sports.tmpStepCount = 100_000
            }
        }
        return sports.tmpStepCount
    }

    /**
     * 步数同步任务
     */
    internal fun syncStepTask() {
        sports.addChildTask(
            ChildModelTask(
                "syncStep",
                Runnable {
                    val step = tmpStepCount()
                    try {
                        val loader = ApplicationHook.classLoader
                        if (loader == null) {
                            Log.error(AntSports.TAG, "ClassLoader is null, 跳过同步步数")
                            return@Runnable
                        }

                        val rpcClazz =
                            loader.loadClass("com.alibaba.health.pedometer.intergation.rpc.RpcManager")

                        // 反编译确认（支付宝 12.12.1.8000）：RpcManager 是普通类，
                        // 无静态工厂方法；同步步数入口为实例方法 a(int, boolean, String)。
                        // 因此：无参构造实例化 + 按参数签名查找实例方法（不依赖混淆名）。
                        val rpcManager = ReflectUtil.newInstance(rpcClazz)

                        // 兼容混淆名变化：按参数签名 (int, boolean, String) 查找同步方法
                        val syncMethod = ReflectUtil.findMethodBySignature(
                            rpcClazz, java.lang.Integer.TYPE, java.lang.Boolean.TYPE, String::class.java
                        )
                        if (syncMethod == null) {
                            dumpRpcManagerMethods(rpcClazz, "未找到同步步数方法 (int, boolean, String)")
                            return@Runnable
                        }
                        val success =
                            syncMethod.invoke(rpcManager, step, java.lang.Boolean.FALSE, "system") as Boolean

                        if (success) {
                            Log.other("同步步数🏃🏻‍♂️[$step 步]")
                            Status.setFlagToday(StatusFlags.FLAG_ANTSPORTS_SYNC_STEP_DONE)
                        } else {
                            Log.error(AntSports.TAG, "同步运动步数失败:$step")
                        }
                    } catch (t: Throwable) {
                        Log.printStackTrace(AntSports.TAG, t)
                    }
                }
            )
        )
    }

    /**
     * @brief 目标类方法签名 dump，用于在新版本 App 上定位混淆后的方法名
     */
    private fun dumpRpcManagerMethods(clazz: Class<*>, reason: String) {
        Log.error(AntSports.TAG, "$reason，RpcManager 方法清单:")
        clazz.declaredMethods.forEach { m ->
            Log.error(AntSports.TAG, "  ${m.toString()}")
        }
    }

    // ---------------------------------------------------------------------
    // 新版行走路线（SportsPlay）
    // ---------------------------------------------------------------------

    /**
     * @brief 新版行走路线主流程 主入口
     */
    internal suspend fun walk() {
        try {
            val user = JSONObject(AntSportsRpcCall.queryUser())
            if (!ResChecker.checkRes(AntSports.TAG, user)) {
                Log.error(AntSports.TAG, "查询用户失败: $user")
                return
            }

            val data = user.optJSONObject("data")
            val joinedPathId = data?.optString("joinedPathId") ?: ""
            if(joinedPathId.isEmpty()) {

                Log.error(AntSports.TAG, "未找到有效线路: $user")
            }
            val path = queryPath(joinedPathId)

            if (path == null) {
                Log.error(AntSports.TAG, "无法获取路线详情(PathId: $joinedPathId)")
                return
            }
            val userPathStep = path.getJSONObject("userPathStep")

            //如果是 JOIN 则还没走完
            if ("COMPLETED" == userPathStep.getString("pathCompleteStatus")) {
                Log.record(AntSports.TAG, "行走路线🚶🏻‍♂️路线[${userPathStep.getString("pathName")}]已完成")
                // 获取新路线 ID
                val newPathId = queryJoinPath(sports.walkPathThemeId)    //walkPathThemeId 在进入walk()之前已经获取了
                if (!newPathId.isNullOrEmpty()) {
                    Log.record(AntSports.TAG, "发现新路线，准备加入: $newPathId")
                    joinPath(newPathId)
                } else {
                    Log.error(AntSports.TAG, "未发现可加入的新路线，可能当前地图已全部走完")
                }
                return
            }

            val pathObj = path.getJSONObject("path")
            val minGoStepCount = pathObj.getInt("minGoStepCount")
            val pathStepCount = pathObj.getInt("pathStepCount")
            val forwardStepCount = userPathStep.getInt("forwardStepCount")
            val remainStepCount = userPathStep.getInt("remainStepCount")
            val needStepCount = pathStepCount - forwardStepCount

            if (remainStepCount >= minGoStepCount) {
                val useStepCount = min(remainStepCount, needStepCount)
                walkGo(userPathStep.getString("pathId"), useStepCount, userPathStep.getString("pathName"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "walk err:", t)
        }
    }

    /**
     * @brief 新版路线行走一步
     */
    private suspend fun walkGo(pathId: String, useStepCount: Int, pathName: String) {
        try {
            val date = Date()
            @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd")
            val jo = JSONObject(AntSportsRpcCall.walkGo(sdf.format(date), pathId, useStepCount))
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                Log.other(AntSports.TAG, "行走路线🚶🏻‍♂️路线[$pathName]#前进了${useStepCount}步")
                queryPath(pathId)
            } else {
                Log.error(AntSports.TAG, "walkGo失败： [pathId: $pathId]: $jo")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "walkGo err:", t)
        }
    }

    /**
     * @brief 查询世界地图
     */
    private suspend fun queryWorldMap(themeId: String?): JSONObject? {
        var theme: JSONObject? = null
        if (themeId.isNullOrEmpty()) return null
        try {
            val jo = JSONObject(AntSportsRpcCall.queryWorldMap(themeId))
            if (ResChecker.checkRes(AntSports.TAG + "queryWorldMap失败： [ThemeID: $themeId]: ", jo)) {
                theme = jo.getJSONObject("data")
            } else {
                Log.error(AntSports.TAG, "queryWorldMap失败： [ThemeID: $themeId]: $jo")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "queryWorldMap err:", t)
        }
        return theme
    }

    /**
     * @brief 查询指定城市的路线详情
     * @param cityId 城市 ID
     */
    private suspend fun queryCityPath(cityId: String): JSONObject? {
        var city: JSONObject? = null
        try {
            val jo = JSONObject(AntSportsRpcCall.queryCityPath(cityId))
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                city = jo.getJSONObject("data")
            } else {
                Log.error(AntSports.TAG, "queryCityPath失败： [CityID: $cityId]$jo")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "queryCityPath err:", t)
        }
        return city
    }

    /**
     * @brief 查询路线详情（同时触发宝箱领取）
     */
    /*
    private fun queryPath(pathId: String): JSONObject? {
        var path: JSONObject? = null
        try {
            val date = Date()
            @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd")
            val jo = JSONObject(AntSportsRpcCall.queryPath(sdf.format(date), pathId))
            if (ResChecker.checkRes(TAG, jo)) {
                path = jo.getJSONObject("data")
                val ja = jo.getJSONObject("data").getJSONArray("treasureBoxList")
                for (i in 0 until ja.length()) {
                    val treasureBox = ja.getJSONObject(i)
                    receiveEvent(treasureBox.getString("boxNo"))
                }
            } else {
                Log.error(TAG, "queryPath失败： $jo")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryPath err:", t)
        }
        return path
    }*/


    //这里会返回路线详情
    private suspend fun queryPath(pathId: String): JSONObject? {
        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val response = AntSportsRpcCall.queryPath(dateStr, pathId)
            val jo = JSONObject(response)

            if (!ResChecker.checkRes(AntSports.TAG, jo)) {
                Log.error(AntSports.TAG, "queryPath 请求失败: $response")
                return null
            }

            // 2. 检查数据节点是否存在
            val data = jo.optJSONObject("data")
            if (data == null) {
                Log.error(AntSports.TAG, "queryPath 响应成功但 data 节点为空: $response")
                return null
            }

            // --- 逻辑处理 ---
            val userPath = data.optJSONObject("userPathStep")
            Log.record(AntSports.TAG, "路线: ${userPath?.optString("pathName")}, 进度: ${userPath?.optInt("pathProgress")}%")

            val boxList = data.optJSONArray("treasureBoxList")
            if (boxList != null && boxList.length() > 0) {
                for (i in 0 until boxList.length()) {
                    val boxNo = boxList.optJSONObject(i)?.optString("boxNo")
                    if (!boxNo.isNullOrEmpty()) receiveEvent(boxNo)
                }
            }

            return data
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "queryPath 过程中发生崩溃", t)
        }
        return null
    }

    /**
     * @brief 新版路线开启宝箱并打印奖励
     */
    private suspend fun receiveEvent(eventBillNo: String) {
        try {
            val jo = JSONObject(AntSportsRpcCall.receiveEvent(eventBillNo))
            if (!ResChecker.checkRes(AntSports.TAG, jo)) return

            val ja = jo.getJSONObject("data").getJSONArray("rewards")
            for (i in 0 until ja.length()) {
                val reward = ja.getJSONObject(i)
                Log.record(
                    AntSports.TAG,
                    "行走路线🎁开启宝箱[${reward.getString("rewardName")}]*${reward.getInt("count")}"
                )
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "receiveEvent err:", t)
        }
    }

    /**
     * @brief 根据主题 ID 挑选可加入的 pathId
     */
    private suspend fun queryJoinPath(themeId: String?): String? {
        // 🎯 自定义路径优先
        if (sports.walkCustomPath.value) {
            return sports.walkCustomPathId.value
        }

        return try {
            val theme = queryWorldMap(themeId)
            if (theme == null) {
                Log.error(AntSports.TAG, "queryJoinPath -> theme 为空，无法继续 当前walkPathThemeId[$themeId]")
                return null
            }

            val cityList = theme.optJSONArray("cityList") ?: return null
            var lastPathId: String? = null

            for (i in 0 until cityList.length()) {
                val cityObj = cityList.optJSONObject(i) ?: continue
                val cityId = cityObj.optString("cityId")
                val cityStatus = cityObj.optString("status")

                // 🚫 非 ONLINE 城市直接跳过
                if (cityStatus != "ONLINE") {
                    // Log.record(TAG, "⛔ 城市[$cityId] 状态=$cityStatus，跳过")
                    continue
                }

                val city = queryCityPath(cityId) ?: continue
                val cityPathList = city.optJSONArray("cityPathList") ?: continue

                for (j in 0 until cityPathList.length()) {
                    val cityPath = cityPathList.optJSONObject(j) ?: continue
                    val pathId = cityPath.optString("pathId")
                    val completeStatus = cityPath.optString("pathCompleteStatus")

                    lastPathId = pathId

                    // 🎯 找到第一个未完成路径，直接返回
                    if (completeStatus != "COMPLETED") {
                        Log.record(AntSports.TAG, "✅ 找到未完成路径 pathId=$pathId (cityId=$cityId)")
                        return pathId
                    }
                }
            }

            Log.record(AntSports.TAG, "⚠️ 所有城市路径均已完成，返回最后一个 pathId=$lastPathId")
            return lastPathId
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "queryJoinPath 异常:", t)
            null
        }
    }

    /**
     * @brief 加入新版路线
     */
    private suspend fun joinPath(pathId: String?) {
        var realPathId = pathId
        if (realPathId == null) {
            // 默认龙年祈福线
            realPathId = "p0002023122214520001"
        }
        try {
            val jo = JSONObject(AntSportsRpcCall.joinPath(realPathId))
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                val path = queryPath(realPathId)
                Log.record(AntSports.TAG, "行走路线🚶🏻‍♂️路线[${path?.getJSONObject("path")?.getString("name")}]已加入")
            } else {
                Log.error(AntSports.TAG, "行走路线🚶🏻‍♂️路线[$realPathId]有误，无法加入！")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "joinPath err:", t)
        }
    }

    /**
     * @brief 根据配置索引同步更新路线主题 ID
     */
    internal fun getWalkPathThemeIdOnConfig() {
        val index = sports.walkPathTheme.value
        if (index >= 0 && index < WalkPathTheme.themeIds.size) {
            sports.walkPathThemeId = WalkPathTheme.themeIds[index]
        } else {
            Log.error(AntSports.TAG, "非法的路线主题索引: $index，已回退至默认主题")
            sports.walkPathThemeId = WalkPathTheme.themeIds[WalkPathTheme.DA_MEI_ZHONG_GUO]
        }
    }

    // ---------------------------------------------------------------------
    // 旧版行走路线（保留兼容）
    // ---------------------------------------------------------------------

    /**
     * @brief 旧版行走路线首页逻辑（开宝箱 + 行走 + 加入路线）
     */
    internal suspend fun queryMyHomePage(loader: ClassLoader) {
        try {
            var s = AntSportsRpcCall.queryMyHomePage()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                val pathJoinStatus = jo.getString("pathJoinStatus")
                if ("GOING" == pathJoinStatus) {
                    if (jo.has("pathCompleteStatus")) {
                        if ("COMPLETED" == jo.getString("pathCompleteStatus")) {
                            jo = JSONObject(AntSportsRpcCall.queryBaseList())
                            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                                val allPathBaseInfoList = jo.getJSONArray("allPathBaseInfoList")
                                val otherAllPathBaseInfoList = jo.getJSONArray("otherAllPathBaseInfoList")
                                    .getJSONObject(0)
                                    .getJSONArray("allPathBaseInfoList")
                                join(loader, allPathBaseInfoList, otherAllPathBaseInfoList, "")
                            } else {
                                Log.record(AntSports.TAG, jo.getString("resultDesc"))
                            }
                        }
                    } else {
                        val rankCacheKey = jo.getString("rankCacheKey")
                        val ja = jo.getJSONArray("treasureBoxModelList")
                        for (i in 0 until ja.length()) {
                            parseTreasureBoxModel(loader, ja.getJSONObject(i), rankCacheKey)
                        }
                        val joPathRender = jo.getJSONObject("pathRenderModel")
                        val title = joPathRender.getString("title")
                        val minGoStepCount = joPathRender.getInt("minGoStepCount")
                        jo = jo.getJSONObject("dailyStepModel")
                        val consumeQuantity = jo.getInt("consumeQuantity")
                        val produceQuantity = jo.getInt("produceQuantity")
                        val day = jo.getString("day")
                        val canMoveStepCount = produceQuantity - consumeQuantity
                        if (canMoveStepCount >= minGoStepCount) {
                            go(loader, day, rankCacheKey, canMoveStepCount, title)
                        }
                    }
                } else if ("NOT_JOIN" == pathJoinStatus) {
                    val firstJoinPathTitle = jo.getString("firstJoinPathTitle")
                    val allPathBaseInfoList = jo.getJSONArray("allPathBaseInfoList")
                    val otherAllPathBaseInfoList = jo.getJSONArray("otherAllPathBaseInfoList")
                        .getJSONObject(0)
                        .getJSONArray("allPathBaseInfoList")
                    join(loader, allPathBaseInfoList, otherAllPathBaseInfoList, firstJoinPathTitle)
                }
            } else {
                Log.record(AntSports.TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "queryMyHomePage err:", t)
        }
    }

    /**
     * @brief 旧版路线加入逻辑（根据可解锁路径列表）
     */
    private suspend fun join(
        loader: ClassLoader,
        allPathBaseInfoList: JSONArray,
        otherAllPathBaseInfoList: JSONArray,
        firstJoinPathTitle: String
    ) {
        try {
            var index = -1
            var title: String? = null
            var pathId: String? = null
            var jo: JSONObject

            for (i in allPathBaseInfoList.length() - 1 downTo 0) {
                jo = allPathBaseInfoList.getJSONObject(i)
                if (jo.getBoolean("unlocked")) {
                    title = jo.getString("title")
                    pathId = jo.getString("pathId")
                    index = i
                    break
                }
            }
            if (index < 0 || index == allPathBaseInfoList.length() - 1) {
                for (j in otherAllPathBaseInfoList.length() - 1 downTo 0) {
                    jo = otherAllPathBaseInfoList.getJSONObject(j)
                    if (jo.getBoolean("unlocked")) {
                        if (j != otherAllPathBaseInfoList.length() - 1 || index != allPathBaseInfoList.length() - 1) {
                            title = jo.getString("title")
                            pathId = jo.getString("pathId")
                            index = j
                        }
                        break
                    }
                }
            }
            if (index >= 0) {
                val s = if (title == firstJoinPathTitle) {
                    AntSportsRpcCall.openAndJoinFirst()
                } else {
                    AntSportsRpcCall.join(pathId ?: "")
                }
                jo = JSONObject(s)
                if (ResChecker.checkRes(AntSports.TAG, jo)) {
                    Log.other("加入线路🚶🏻‍♂️[$title]")
                    queryMyHomePage(loader)
                } else {
                    Log.record(AntSports.TAG, jo.getString("resultDesc"))
                }
            } else {
                Log.record(AntSports.TAG, "好像没有可走的线路了！")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "join err:", t)
        }
    }

    /**
     * @brief 旧版路线行走逻辑
     */
    private suspend fun go(loader: ClassLoader, day: String, rankCacheKey: String, stepCount: Int, title: String) {
        try {
            val s = AntSportsRpcCall.go(day, rankCacheKey, stepCount)
            val jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                Log.other("行走线路🚶🏻‍♂️[$title]#前进了${jo.getInt("goStepCount")}步")
                val completed = "COMPLETED" == jo.getString("completeStatus")
                val ja = jo.getJSONArray("allTreasureBoxModelList")
                for (i in 0 until ja.length()) {
                    parseTreasureBoxModel(loader, ja.getJSONObject(i), rankCacheKey)
                }
                if (completed) {
                    Log.other("完成线路🚶🏻‍♂️[$title]")
                    queryMyHomePage(loader)
                }
            } else {
                Log.record(AntSports.TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "go err:", t)
        }
    }

    /**
     * @brief 解析旧版宝箱模型并按时间安排子任务开箱
     */
    private suspend fun parseTreasureBoxModel(loader: ClassLoader, jo: JSONObject, rankCacheKey: String) {
        try {
            val canOpenTime = jo.getString("canOpenTime")
            val issueTime = jo.getString("issueTime")
            val boxNo = jo.getString("boxNo")
            val userId = jo.getString("userId")
            if (canOpenTime == issueTime) {
                openTreasureBox(boxNo, userId)
            } else {
                val cot = canOpenTime.toLong()
                val now = rankCacheKey.toLong()
                val delay = cot - now
                if (delay <= 0) {
                    openTreasureBox(boxNo, userId)
                    return
                }
                if (delay < ApplicationHook.config.checkInterval.value) {
                    val taskId = "BX|$boxNo"
                    if (sports.hasChildTask(taskId)) return
                    Log.record(AntSports.TAG, "还有 $delay ms 开运动宝箱")
                    sports.addChildTask(
                        ChildModelTask(
                            taskId,
                            "BX",
                            Runnable {
                                Log.record(AntSports.TAG, "蹲点开箱开始")
                                val startTime = System.currentTimeMillis()
                                while (System.currentTimeMillis() - startTime < 5_000) {
                                    if (CoroutineUtils.runBlockingSafe { openTreasureBox(boxNo, userId) } ?: 0 > 0) {
                                        break
                                    }
                                    GlobalThreadPools.sleepCompat(200)
                                }
                            },
                            System.currentTimeMillis() + delay
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "parseTreasureBoxModel err:", t)
        }
    }

    /**
     * @brief 旧版宝箱开启
     * @return 获得的奖励数量
     */
    private suspend fun openTreasureBox(boxNo: String, userId: String): Int {
        try {
            val s = AntSportsRpcCall.openTreasureBox(boxNo, userId)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                val ja = jo.getJSONArray("treasureBoxAwards")
                var num = 0
                for (i in 0 until ja.length()) {
                    jo = ja.getJSONObject(i)
                    num += jo.getInt("num")
                    Log.other("运动宝箱🎁[$num${jo.getString("name")}]")
                }
                return num
            } else if ("TREASUREBOX_NOT_EXIST" == jo.getString("resultCode")) {
                Log.record(jo.getString("resultDesc"))
                return 1
            } else {
                Log.record(jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "openTreasureBox err:", t)
        }
        return 0
    }

    // ---------------------------------------------------------------------
    // 旧版捐步 & 慈善
    // ---------------------------------------------------------------------

    /**
     * @brief 查询慈善项目列表并执行捐赠
     */
    internal suspend fun queryProjectList(loader: ClassLoader) {
        try {
            var jo = JSONObject(AntSportsRpcCall.queryProjectList(0))
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                var charityCoinCount = jo.getInt("charityCoinCount")
                if (charityCoinCount < sports.donateCharityCoinAmount.value) return

                val ja = jo.getJSONObject("projectPage").getJSONArray("data")
                for (i in 0 until ja.length()) {
                    if (charityCoinCount < sports.donateCharityCoinAmount.value) break
                    val basicModel = ja.getJSONObject(i).getJSONObject("basicModel")
                    if ("DONATE_COMPLETED" == basicModel.getString("footballFieldStatus")) break
                    donate(
                        loader,
                        sports.donateCharityCoinAmount.value,
                        basicModel.getString("projectId"),
                        basicModel.getString("title")
                    )
                    Status.donateCharityCoin()
                    charityCoinCount -= sports.donateCharityCoinAmount.value
                    if (sports.donateCharityCoinType.value == DonateCharityCoinType.ONE) break
                }
            } else {
                Log.record(AntSports.TAG)
                Log.record(jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "queryProjectList err:", t)
        }
    }

    /**
     * @brief 执行一次慈善捐赠
     */
    private suspend fun donate(loader: ClassLoader, donateCharityCoin: Int, projectId: String, title: String) {
        try {
            val s = AntSportsRpcCall.donate(donateCharityCoin, projectId)
            val jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                Log.other("捐赠活动❤️[$title][$donateCharityCoin 能量🎈]")
            } else {
                Log.record(AntSports.TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "donate err:", t)
        }
    }

    /**
     * @brief 查询行走步数，并根据条件自动捐步
     */
    internal suspend fun queryWalkStep(loader: ClassLoader) {
        try {
            var s = AntSportsRpcCall.queryWalkStep()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                jo = jo.getJSONObject("dailyStepModel")
                val produceQuantity = jo.getInt("produceQuantity")
                val hour = TimeUtil.getFormatTime().split(":").first().toInt()

                if (produceQuantity >= sports.minExchangeCount.value || hour >= sports.latestExchangeTime.value) {
                    AntSportsRpcCall.walkDonateSignInfo(produceQuantity)
                    s = AntSportsRpcCall.donateWalkHome(produceQuantity)
                    jo = JSONObject(s)
                    if (!jo.getBoolean("isSuccess")) return
                    val walkDonateHomeModel = jo.getJSONObject("walkDonateHomeModel")
                    val walkUserInfoModel = walkDonateHomeModel.getJSONObject("walkUserInfoModel")
                    if (!walkUserInfoModel.has("exchangeFlag")) {
                        Status.exchangeToday(UserMap.currentUid ?: return)
                        return
                    }
                    val donateToken = walkDonateHomeModel.getString("donateToken")
                    val walkCharityActivityModel = walkDonateHomeModel.getJSONObject("walkCharityActivityModel")
                    val activityId = walkCharityActivityModel.getString("activityId")
                    s = AntSportsRpcCall.exchange(activityId, produceQuantity, donateToken)
                    jo = JSONObject(s)
                    if (jo.getBoolean("isSuccess")) {
                        val donateExchangeResultModel = jo.getJSONObject("donateExchangeResultModel")
                        val userCount = donateExchangeResultModel.getInt("userCount")
                        val amount = donateExchangeResultModel.getJSONObject("userAmount").getDouble("amount")
                        Log.other("捐出活动❤️[$userCount 步]#兑换$amount 元公益金")
                        Status.exchangeToday(UserMap.currentUid ?: return)
                    } else if (s.contains("已捐步")) {
                        Status.exchangeToday(UserMap.currentUid ?: return)
                    } else {
                        Log.record(AntSports.TAG, jo.getString("resultDesc"))
                    }
                }
            } else {
                Log.record(AntSports.TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "queryWalkStep err:", t)
        }
    }
}

/**
 * @brief 蚂蚁运动路线主题常量与映射表
 */
interface WalkPathTheme {
    companion object {
        const val DA_MEI_ZHONG_GUO = 0  ///< 大美中国 (默认)
        const val GONG_YI_YI_XIAO_BU = 1  ///< 公益一小步
        const val DENG_DING_ZHI_MA_SHAN = 2  ///< 登顶芝麻山
        const val WEI_C_DA_TIAO_ZHAN = 3  ///< 维C大挑战
        const val LONG_NIAN_QI_FU = 4  ///< 龙年祈福
        const val SHOU_HU_TI_YU_MENG = 5  ///< 守护体育梦

        /** @brief 界面显示的名称列表 */
        val nickNames = arrayOf(
            "大美中国",
            "公益一小步",
            "登顶芝麻山",
            "维C大挑战",
            "龙年祈福",
            "守护体育梦"
        )

        /**
         * @brief 对应目标应用接口的 ThemeID 映射表
         * @note 数组顺序必须与上方常量定义保持严格一致
         */
        val themeIds = arrayOf(
            "M202308082226",  ///< [0] 大美中国
            "M202401042147",  ///< [1] 公益一小步
            "V202405271625",  ///< [2] 登顶芝麻山
            "202404221422",   ///< [3] 维C大挑战
            "WF202312050200", ///< [4] 龙年祈福
            "V202409061650"   ///< [5] 守护体育梦
        )
    }
}

/**
 * @brief 慈善捐能量模式
 */
interface DonateCharityCoinType {
    companion object {
        const val ONE = 0
        // 保留原 ALL 选项的文案，方便以后扩充
        val nickNames = arrayOf("捐赠一个项目", "捐赠所有项目")
    }
}
