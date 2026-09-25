package com.profconq.app.auth

import android.content.Context
import android.content.Intent
import com.profconq.app.analytics.ProfconqAnalytics
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
    private val prefs = appContext.getSharedPreferences("profconq_site_user", Context.MODE_PRIVATE)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var siteUser: AuthUser? = loadSiteUser()
    private val _authUser = MutableStateFlow(firebaseAuth.currentUser?.toAuthUser() ?: siteUser)
    val authUser: StateFlow<AuthUser?> = _authUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener {
        publishUser()
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

    fun setSiteUser(user: AuthUser?) {
        siteUser = user
        if (user == null) {
            prefs.edit().clear().apply()
        } else {
            prefs.edit()
                .putString("uid", user.uid)
                .putString("email", user.email)
                .putString("name", user.displayName)
                .apply()
        }
        publishUser()
    }

    fun currentSigningSha1(): String = SigningSha.sha1Colon(appContext)

    fun createGoogleSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun signInWithGoogleResult(data: Intent?) {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
        val idToken = account.idToken ?: throw IllegalStateException("Google idToken not found")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
    }

    suspend fun signOut() {
        runCatching { googleSignInClient.signOut().await() }
        firebaseAuth.signOut()
        ProfconqAnalytics.clearUser()
        setSiteUser(null)
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        val user = firebaseAuth.currentUser ?: return null
        return user.getIdToken(forceRefresh).await().token
    }

    private fun publishUser() {
        val user = firebaseAuth.currentUser?.toAuthUser() ?: siteUser
        _authUser.value = user
        // uid этого контракта может быть email-ом (см. ProfconqSessionAuth.parseUser), поэтому
        // наружу идёт только прошедший проверку серверный UUID; иначе user_id остаётся пустым.
        ProfconqAnalytics.identifyUser(user?.uid)
    }

    private fun loadSiteUser(): AuthUser? {
        val uid = prefs.getString("uid", null) ?: return null
        return AuthUser(
            uid = uid,
            displayName = prefs.getString("name", null),
            email = prefs.getString("email", null),
        )
    }

    private fun resolveWebClientId(): String {
        val stringId = appContext.resources.getIdentifier(
            "default_web_client_id",
            "string",
            appContext.packageName,
        )
        if (stringId != 0) {
            val fromJson = appContext.getString(stringId)
            if (fromJson.isNotBlank()) return fromJson
        }
        return WEB_CLIENT_ID
    }

    companion object {
        const val WEB_CLIENT_ID =
            "763640645988-87c0p8j35mbe1qg1ojt3navrnfnfaqr6.apps.googleusercontent.com"
    }
}

private fun com.google.firebase.auth.FirebaseUser.toAuthUser(): AuthUser {
    return AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
    )
}
