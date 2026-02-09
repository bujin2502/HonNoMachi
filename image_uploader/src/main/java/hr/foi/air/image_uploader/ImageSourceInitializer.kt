package hr.foi.air.image_uploader

import hr.foi.air.image_uploader.model.ImageSourceRegistry
import hr.foi.air.image_uploader.sources.CameraImageSource
import hr.foi.air.image_uploader.sources.GalleryImageSource

object ImageSourceInitializer {
    private var initialized = false

    fun initialize() {
        if (initialized) return

        ImageSourceRegistry.register(CameraImageSource())
        ImageSourceRegistry.register(GalleryImageSource())

        initialized = true
    }

    fun reset() {
        initialized = false
        ImageSourceRegistry.clear()
    }
}
