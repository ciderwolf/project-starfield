package starfield

object Config {
    val storagePath by lazy {
        System.getenv("STORAGE_PATH") ?: "./"
    }

    val connectionString by lazy {
        System.getenv("CONNECTION_STRING")!!
    }

    val entityPurgeTimeout by lazy {
        System.getenv("ENTITY_PURGE_TIMEOUT")?.toLong() ?: -1
    }
}