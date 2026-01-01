package hr.foi.air.honnomachi.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.data.AuthRepositoryImpl
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.data.BookRepositoryImpl
import hr.foi.air.honnomachi.data.ProfileRepository
import hr.foi.air.honnomachi.data.ProfileRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindBookRepository(bookRepositoryImpl: BookRepositoryImpl): BookRepository

    @Binds
    abstract fun bindProfileRepository(profileRepositoryImpl: ProfileRepositoryImpl): ProfileRepository
}
