package starfield

object Config {
    val storagePath by lazy {
        System.getenv("STORAGE_PATH") ?: "./"
    }
}