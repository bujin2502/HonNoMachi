package hr.foi.air.honnomachi.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hr.foi.air.honnomachi.data.AdminRepository
import hr.foi.air.honnomachi.data.AdminRepositoryImpl
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.data.AuthRepositoryImpl
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.data.BookRepositoryImpl
import hr.foi.air.honnomachi.data.CartRepository
import hr.foi.air.honnomachi.data.CartRepositoryImpl
import hr.foi.air.honnomachi.data.CheckoutRepository
import hr.foi.air.honnomachi.data.CheckoutRepositoryImpl
import hr.foi.air.honnomachi.data.FirestoreUserDataSource
import hr.foi.air.honnomachi.data.OrderRepository
import hr.foi.air.honnomachi.data.OrderRepositoryImpl
import hr.foi.air.honnomachi.data.FirestoreUserDataSourceImpl
import hr.foi.air.honnomachi.data.ProfileRepository
import hr.foi.air.honnomachi.data.ProfileRepositoryImpl
import hr.foi.air.honnomachi.data.WalletRepository
import hr.foi.air.honnomachi.data.WalletRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindFirestoreUserDataSource(impl: FirestoreUserDataSourceImpl): FirestoreUserDataSource

    @Binds
    fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    fun bindBookRepository(bookRepositoryImpl: BookRepositoryImpl): BookRepository

    @Binds
    fun bindCartRepository(cartRepositoryImpl: CartRepositoryImpl): CartRepository

    @Binds
    fun bindCheckoutRepository(checkoutRepositoryImpl: CheckoutRepositoryImpl): CheckoutRepository

    @Binds
    fun bindOrderRepository(orderRepositoryImpl: OrderRepositoryImpl): OrderRepository

    @Binds
    fun bindProfileRepository(profileRepositoryImpl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    fun bindWalletRepository(walletRepositoryImpl: WalletRepositoryImpl): WalletRepository

    @Binds
    fun bindAdminRepository(adminRepositoryImpl: AdminRepositoryImpl): AdminRepository
}
