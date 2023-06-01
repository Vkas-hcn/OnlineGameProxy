package op.asd.utils

import com.tencent.mmkv.MMKV

object MMKVUtil {
    private val mmkv: MMKV by lazy {
        MMKV.initialize("OnlineGameProxy")
        MMKV.defaultMMKV()
    }

    fun saveString(key: String, value: String?) {
        mmkv.encode(key, value)
    }

    fun getString(key: String, defaultValue: String): String {
        return mmkv.decodeString(key, defaultValue)?:defaultValue
    }

    fun saveInt(key: String, value: Int) {
        mmkv.encode(key, value)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return mmkv.decodeInt(key, defaultValue)
    }

    // 添加其他数据类型的存储和读取方法，如Boolean、Float、Long等

    fun remove(key: String) {
        mmkv.remove(key)
    }

    fun clearAll() {
        mmkv.clearAll()
    }
}