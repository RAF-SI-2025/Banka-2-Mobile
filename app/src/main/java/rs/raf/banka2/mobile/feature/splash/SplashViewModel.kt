package rs.raf.banka2.mobile.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Splash ViewModel — pokusava da rekonstruise sesiju iz cuvanog tokena
 * i odlucuje gde da naviguje korisnika.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = authRepository.restoreSessionIfPossible()
            _state.value = if (profile == null) {
                SplashState.GoToLogin
            } else {
                SplashState.GoToHome(profile.role)
            }
        }
    }
}

sealed interface SplashState {
    data object Loading : SplashState
    data object GoToLogin : SplashState
    data class GoToHome(val role: UserRole) : SplashState
}
