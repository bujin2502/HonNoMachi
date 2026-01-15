package hr.foi.air.image_uploader.di

import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hr.foi.air.image_uploader.FirebaseImageUploader
import hr.foi.air.image_uploader.ImageUploader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageUploaderModule {

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideImageUploader(storage: FirebaseStorage): ImageUploader {
        return FirebaseImageUploader(storage)
    }
}
