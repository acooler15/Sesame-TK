package fansirsqi.xposed.sesame.model

class ModelFields : LinkedHashMap<String, ModelField<*>>() {
    fun addField(modelField: ModelField<*>) {
        put(modelField.code, modelField)
    }
}
