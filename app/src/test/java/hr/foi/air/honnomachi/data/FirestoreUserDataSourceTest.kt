package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.model.UserModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FirestoreUserDataSourceTest {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var dataSource: FirestoreUserDataSourceImpl
    private lateinit var collectionRef: CollectionReference
    private lateinit var documentRef: DocumentReference

    @Before
    fun setup() {
        firestore = mockk()
        collectionRef = mockk()
        documentRef = mockk()
        dataSource = FirestoreUserDataSourceImpl(firestore)

        every { firestore.collection(FirestoreCollections.USERS) } returns collectionRef

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `getUserDocument returns document snapshot`() =
        runTest {
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()

            every { collectionRef.document("uid123") } returns documentRef
            every { documentRef.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot

            val result = dataSource.getUserDocument("uid123")

            assertEquals(mockSnapshot, result)
        }

    @Test
    fun `getUser returns UserModel from document`() =
        runTest {
            val user = UserModel(name = "Test User", email = "test@example.com", uid = "uid123")
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()

            every { collectionRef.document("uid123") } returns documentRef
            every { documentRef.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.toObject(UserModel::class.java) } returns user

            val result = dataSource.getUser("uid123")

            assertEquals(user, result)
        }

    @Test
    fun `createUser sets user document in firestore`() =
        runTest {
            val user = UserModel(name = "Test User", email = "test@example.com", uid = "uid123")
            val mockTask = mockk<Task<Void>>()

            every { collectionRef.document("uid123") } returns documentRef
            every { documentRef.set(user) } returns mockTask
            coEvery { mockTask.await() } returns mockk()

            dataSource.createUser(user)

            verify { documentRef.set(user) }
        }

    @Test
    fun `updateVerificationStatus updates isVerified field`() =
        runTest {
            val mockTask = mockk<Task<Void>>()

            every { collectionRef.document("uid123") } returns documentRef
            every { documentRef.update(FirestoreFields.IS_VERIFIED, true) } returns mockTask
            coEvery { mockTask.await() } returns mockk()

            dataSource.updateVerificationStatus("uid123", true)

            verify { documentRef.update(FirestoreFields.IS_VERIFIED, true) }
        }
}
