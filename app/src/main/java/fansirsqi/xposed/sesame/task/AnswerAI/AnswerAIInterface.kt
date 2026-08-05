package fansirsqi.xposed.sesame.task.AnswerAI

/**
 * AI答题服务接口
 * 定义了AI答题服务的基本操作，包括获取答案、设置模型等功能
 */
interface AnswerAIInterface {

    /**
     * 模型名称
     * getModelName 获取当前使用的模型名称；setModelName 设置模型名称
     */
    var modelName: String?
        get() = "" // 默认空实现
        set(value) {
            // 默认空实现
        }

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果，如果获取失败返回空字符串
     */
    fun getAnswerStr(text: String?): String

    /**
     * 获取AI回答结果，指定模型
     *
     * @param text  问题内容
     * @param model 模型名称
     * @return AI回答结果，如果获取失败返回空字符串
     */
    fun getAnswerStr(text: String?, model: String?): String

    /**
     * 获取AI答案
     *
     * @param title      问题标题
     * @param answerList 候选答案列表
     * @return 选中的答案索引，如果没有找到合适的答案返回-1
     */
    fun getAnswer(title: String?, answerList: List<String>?): Int?

    /**
     * 释放资源
     * 实现类应在此方法中清理所有使用的资源
     */
    fun release() {
        // 默认空实现
    }

    /**
     * 单例持有者，延迟加载
     */
    class SingletonHolder {
        companion object {
            val INSTANCE: AnswerAIInterface = object : AnswerAIInterface {
                override fun getAnswerStr(text: String?): String {
                    return ""
                }

                override fun getAnswerStr(text: String?, model: String?): String {
                    return ""
                }

                override fun getAnswer(title: String?, answerList: List<String>?): Int? {
                    return -1
                }
            }
        }
    }

    companion object {
        /**
         * 获取单例实例
         *
         * @return 默认的AI答题服务实现
         */
        @JvmStatic
        fun getInstance(): AnswerAIInterface {
            return SingletonHolder.INSTANCE
        }
    }
}
