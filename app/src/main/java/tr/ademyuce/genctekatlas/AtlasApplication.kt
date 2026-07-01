package tr.ademyuce.genctekatlas

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp
import tr.ademyuce.genctekatlas.BuildConfig

@HiltAndroidApp
class AtlasApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.getApps(this).isNotEmpty()) {
            return
        }

        if (!hasFirebaseConfig()) {
            Log.w(TAG, "Firebase configuration is missing. App will use demo fallback data.")
            return
        }

        try {
            // Programmatically initialize Firebase using credentials from BuildConfig (.env).
            val options = FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                .setGcmSenderId(BuildConfig.FIREBASE_MESSAGING_SENDER_ID)
                .build()

            FirebaseApp.initializeApp(this, options)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed.", e)
        }
    }

    private fun hasFirebaseConfig(): Boolean {
        return listOf(
            BuildConfig.FIREBASE_API_KEY,
            BuildConfig.FIREBASE_APPLICATION_ID,
            BuildConfig.FIREBASE_PROJECT_ID,
            BuildConfig.FIREBASE_STORAGE_BUCKET,
            BuildConfig.FIREBASE_MESSAGING_SENDER_ID
        ).all { it.isConfigured() }
    }

    private fun String.isConfigured(): Boolean {
        return isNotBlank() && !equals("none", ignoreCase = true)
    }

    private companion object {
        const val TAG = "AtlasApplication"
    }
}
