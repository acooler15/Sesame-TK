package fansirsqi.xposed.sesame.entity

import fansirsqi.xposed.sesame.core.app.Files
import fansirsqi.xposed.sesame.core.json.JsonUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.StringUtil
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

class FriendWatch(id: String, name: String) : MapperEntity() {

    var startTime: String? = null

    var allGet = 0

    var weekGet = 0

    init {
        this.id = id
        this.name = name
    }

    override fun compareTo(other: MapperEntity): Int {
        val another = other as FriendWatch
        if (this.weekGet > another.weekGet) {
            return -1
        } else if (this.weekGet < another.weekGet) {
            return 1
        }
        return super.compareTo(other)
    }

    companion object {
        private val TAG: String = FriendWatch::class.java.simpleName

        @JvmStatic
        var joFriendWatch: JSONObject? = JSONObject()

        @JvmStatic
        fun friendWatch(id: String, collectedEnergy: Int) {
            try {
                if (joFriendWatch == null) {
                    joFriendWatch = JSONObject()
                }
                var joSingle = joFriendWatch!!.optJSONObject(id)
                if (joSingle == null) {
                    joSingle = JSONObject()
                    joSingle.put("name", UserMap.getMaskName(id))
                    joSingle.put("allGet", 0)
                    joSingle.put("startTime", TimeUtil.getDateStr())
                    joFriendWatch!!.put(id, joSingle)
                }
                joSingle.put("weekGet", joSingle.optInt("weekGet", 0) + collectedEnergy)
            } catch (th: Throwable) {
                Log.record(TAG, "friendWatch err:")
                Log.printStackTrace(TAG, th)
            }
        }

        @JvmStatic
        @Synchronized
        fun save(userId: String) {
            try {
                if (joFriendWatch == null) {
                    joFriendWatch = JSONObject()
                    Log.record(TAG, "初始化joFriendWatch对象")
                }
                val notformat = joFriendWatch!!.toString()
                val formattedJson = JsonUtil.formatJson(joFriendWatch!!)
                if (formattedJson != null && formattedJson.trim().isNotEmpty()) {
                    Files.write2File(formattedJson, Files.getFriendWatchFile(userId)!!)
                } else {
                    Files.write2File(notformat, Files.getFriendWatchFile(userId)!!)
                }
            } catch (e: Exception) {
                Log.record(TAG, "friendWatch save err:")
                Log.printStackTrace(TAG, e)
            }
        }

        @JvmStatic
        fun updateDay(userId: String) {
            if (!needUpdateAll(Files.getFriendWatchFile(userId)!!.lastModified())) {
                return
            }
            try {
                val dateStr = TimeUtil.getDateStr()
                val ids = joFriendWatch!!.keys()
                while (ids.hasNext()) {
                    val id = ids.next()
                    val joSingle = joFriendWatch!!.getJSONObject(id)
                    joSingle.put("name", joSingle.optString("name"))
                    joSingle.put("allGet", joSingle.optInt("allGet", 0) + joSingle.optInt("weekGet", 0))
                    joSingle.put("weekGet", 0)
                    if (!joSingle.has("startTime")) {
                        joSingle.put("startTime", dateStr)
                    }
                    joFriendWatch!!.put(id, joSingle)
                }
                Files.write2File(joFriendWatch!!.toString(), Files.getFriendWatchFile(userId)!!)
            } catch (th: Throwable) {
                Log.record(TAG, "friendWatchNewWeek err:")
                Log.printStackTrace(TAG, th)
            }
        }

        @JvmStatic
        @Synchronized
        fun load(userId: String?): Boolean {
            try {
                if (userId == null) {
                    return false
                }

                val strFriendWatch = Files.readFromFile(Files.getFriendWatchFile(userId)!!)
                if (strFriendWatch.isNotEmpty()) {
                    joFriendWatch = JSONObject(strFriendWatch)
                } else {
                    joFriendWatch = JSONObject()
                }
                return true
            } catch (e: JSONException) {
                Log.printStackTrace(e)
                joFriendWatch = JSONObject()
            }
            return false
        }

        @JvmStatic
        @Synchronized
        fun unload() {
            joFriendWatch = JSONObject()
        }

        @JvmStatic
        fun needUpdateAll(last: Long): Boolean {
            if (last == 0L) {
                return true
            }
            val cLast = Calendar.getInstance()
            cLast.timeInMillis = last
            val cNow = Calendar.getInstance()
            if (cLast.get(Calendar.DAY_OF_YEAR) == cNow.get(Calendar.DAY_OF_YEAR)) {
                return false
            }
            return cNow.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
        }

        @JvmStatic
        fun getList(userId: String): List<FriendWatch> {
            val list = ArrayList<FriendWatch>()
            val strFriendWatch = Files.readFromFile(Files.getFriendWatchFile(userId)!!)
            try {
                val joFriendWatch: JSONObject = if (StringUtil.isEmpty(strFriendWatch)) {
                    JSONObject()
                } else {
                    JSONObject(strFriendWatch)
                }
                val ids = joFriendWatch.keys()
                while (ids.hasNext()) {
                    val id = ids.next()
                    var friend = joFriendWatch.optJSONObject(id)
                    if (friend == null) {
                        friend = JSONObject()
                    }
                    val name = friend.optString("name")
                    val friendWatch = FriendWatch(id, name)
                    friendWatch.startTime = friend.optString("startTime", "无")
                    friendWatch.weekGet = friend.optInt("weekGet", 0)
                    friendWatch.allGet = friend.optInt("allGet", 0) + friendWatch.weekGet
                    friendWatch.name = name + "(开始统计时间:" + friendWatch.startTime + ")\n\n" + "周收:" + friendWatch.weekGet + " 总收:" + friendWatch.allGet
                    list.add(friendWatch)
                }
            } catch (t: Throwable) {
                Log.record(TAG, "FriendWatch getList: ")
                Log.printStackTrace(TAG, t)
                try {
                    Files.write2File(JSONObject().toString(), Files.getFriendWatchFile(userId)!!)
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                }
            }
            return list
        }
    }
}
