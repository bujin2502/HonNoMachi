package hr.foi.air.image_uploader.model

object ImageSourceRegistry {
    private val sources = mutableListOf<ImageSource>()
    fun register(source: ImageSource) {
        if (sources.none { it.id == source.id }) {
            sources.add(source)
        }
    }
    fun getAllSources(): List<ImageSource> = sources.toList()
    fun getSource(id: String): ImageSource? = sources.find { it.id == id }

    fun clear() {
        sources.clear()
    }
}
