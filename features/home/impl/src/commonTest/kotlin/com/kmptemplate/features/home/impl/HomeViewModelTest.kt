package com.kmptemplate.features.home.impl

import com.kmptemplate.libraries.flowroutines.testing.CoroutineTest
import com.kmptemplate.libraries.kmptemplate.User
import com.kmptemplate.libraries.kmptemplate.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Reference example for testing a SEAViewModel with the
 * :libraries:flowroutines:testing module.
 *
 * The recipe:
 *  - extend [CoroutineTest] — Main is a test dispatcher, so `viewModelScope`
 *    (Main.immediate) routes through the test scheduler and `takeAction`
 *    propagates to state in the same virtual tick
 *  - hand-roll a fake for each repository dependency (no mocking framework)
 *  - drive actions, assert on `vm.state`
 */
class HomeViewModelTest : CoroutineTest() {

    @Test
    fun loadsUserNameOnInit() = runUnitTest {
        val vm = HomeViewModel(FakeUserRepository(userWithName("Ada")))

        assertEquals("Ada", vm.state.userName)
    }

    @Test
    fun refreshPicksUpChangedName() = runUnitTest {
        val repository = FakeUserRepository(userWithName("Ada"))
        val vm = HomeViewModel(repository)

        repository.user.value = userWithName("Grace")
        vm.takeAction(HomeAction.Refresh)

        assertEquals("Grace", vm.state.userName)
    }

    @Test
    fun missingUserLeavesNameNull() = runUnitTest {
        val vm = HomeViewModel(FakeUserRepository(user = null))

        assertNull(vm.state.userName)
    }

    private fun userWithName(name: String) = User(
        name = name,
        createdAt = 0L,
        lastSessionAt = null,
        hasCompletedOnboarding = true,
        sessionsCount = 2,
        appOpenCount = 2,
    )
}

private class FakeUserRepository(user: User?) : UserRepository {
    val user = MutableStateFlow(user)

    override suspend fun ensureUserExists() = Unit
    override fun observeUser(): Flow<User?> = user
    override suspend fun getUser(): User? = user.value
    override suspend fun setName(name: String?) {
        user.value = user.value?.copy(name = name)
    }
    override suspend fun onSessionStarted() = Unit
    override suspend fun onAppOpened() = Unit
    override suspend fun setOnboardingComplete() = Unit
    override suspend fun onShakeDetected() = Unit
    override suspend fun deleteAll() {
        user.value = null
    }
}
