package com.promy.irontrack.data.auth

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthManager private constructor(@Suppress("unused") private val appContext: Context) {

    private val auth: FirebaseAuth = Firebase.auth

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(auth.currentUser?.isEmailVerified == true)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        _isEmailVerified.value = user?.isEmailVerified == true
    }

    init {
        auth.useAppLanguage()
        auth.addAuthStateListener(authStateListener)
    }

    fun getUserId(): String? = auth.currentUser?.uid

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    suspend fun reloadUser(): Boolean {
        auth.currentUser?.reload()?.await()
        val verified = auth.currentUser?.isEmailVerified == true
        _isEmailVerified.value = verified
        _currentUser.value = auth.currentUser
        return verified
    }

    suspend fun sendVerificationEmail() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    suspend fun updateDisplayName(name: String) {
        val user = auth.currentUser ?: return
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()
        reloadUser()
    }

    fun signOut(onComplete: () -> Unit = {}) {
        auth.signOut()
        _isEmailVerified.value = false
        onComplete()
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
