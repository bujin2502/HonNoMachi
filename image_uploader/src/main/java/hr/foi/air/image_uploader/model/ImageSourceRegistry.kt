package hr.foi.air.image_uploader.model

/**
 * Singleton registry za registraciju i dohvat izvora slika.
 */
object ImageSourceRegistry {
    private val sources = mutableListOf<ImageSource>()

    /**
     * Registrira novi izvor slika.
     */
    fun register(source: ImageSource) {
        if (sources.none { it.id == source.id }) {
            sources.add(source)
        }
    }

    /**
     * Dohvaća sve registrirane izvore.
     */
    fun getAllSources(): List<ImageSource> = sources.toList()

    /**
     * Dohvaća izvor po ID-u.
     */
    fun getSource(id: String): ImageSource? = sources.find { it.id == id }

    /**
     * Briše sve registrirane izvore (korisno za testiranje).
     */
    fun clear() {
        sources.clear()
    }
}
