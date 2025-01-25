package starfield.draft

class DefaultDict<K, V>(private val defaultFactory: () -> V) {

    private val contents = mutableMapOf<K, V>()

    val entries
        get() = contents.entries

    operator fun get(key: K): V {
        return if (contents.containsKey(key)) {
            contents[key]!!
        }
        else {
            defaultFactory().also { contents[key] = it }
        }
    }

    operator fun set(key: K, value: V) {
        contents[key] = value
    }

    fun copyOf(): DefaultDict<K, V> {
        val copy = DefaultDict<K, V>(defaultFactory)
        copy.contents.putAll(contents)
        return copy
    }
}