package com.profconq.app.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
)

class FirebaseAuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authUser = MutableStateFlow(firebaseAuth.currentUser?.toAuthUser())
    val authUser: StateFlow<AuthUser?> = _authUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _authUser.value = auth.currentUser?.toAuthUser()
    }

    private val googleSignInClient by lazy {
        val webClientId = resolveWebClientId()
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
        GoogleSignIn.getClient(appContext, options)
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
    }

    fun createGoogleSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun signInWithGoogleResult(data: Intent?) {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
        val idToken = account.idToken ?: throw IllegalStateException("Google idToken not found")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
    }

    suspend fun signOut() {
        googleSignInClient.signOut().await()
        firebaseAuth.signOut()
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        val user = firebaseAuth.currentUser ?: return null
        return user.getIdToken(forceRefresh).await().token
    }

    private fun resolveWebClientId(): String {
        val stringId = appContext.resources.getIdentifier(
            "default_web_client_id",
            "string",
            appContext.packageName,
        )
        if (stringId == 0) {
            throw IllegalStateException("Missing default_web_client_id in google-services.json")
        }
        return appContext.getString(stringId)
    }
}

private fun com.google.firebase.auth.FirebaseUser.toAuthUser(): AuthUser {
    return AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
    )
}
