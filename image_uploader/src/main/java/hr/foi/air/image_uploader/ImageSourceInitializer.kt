package hr.foi.air.image_uploader

import hr.foi.air.image_uploader.model.ImageSourceRegistry
import hr.foi.air.image_uploader.sources.CameraImageSource
import hr.foi.air.image_uploader.sources.GalleryImageSource

/**
 * Inicijalizira default izvore slika.
 * Poziva se automatski ili ručno na početku aplikacije.
 */
object ImageSourceInitializer {
    private var initialized = false

    fun initialize() {
        if (initialized) return

        ImageSourceRegistry.register(CameraImageSource())
        ImageSourceRegistry.register(GalleryImageSource())

        initialized = true
    }

    /**
     * Resetira inicijalizaciju (korisno za testiranje).
     */
    fun reset() {
        initialized = false
        ImageSourceRegistry.clear()
    }
}
