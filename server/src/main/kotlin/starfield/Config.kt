package starfield

object Config {
    val storagePath by lazy {
        System.getenv("STORAGE_PATH") ?: "./"
    }

    val connectionString by lazy {
        System.getenv("CONNECTION_STRING")!!
    }
}