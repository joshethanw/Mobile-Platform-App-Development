package com.example.ch06.profile

data class UserProfile(
    val username: String,
    val notificationsEnabled: Boolean
)

interface ProfileRepository {
    fun getProfile(): UserProfile
    fun updateUsername(username: String)
    fun updateNotificationsEnabled(enabled: Boolean)
}

class FakeProfileRepository : ProfileRepository {
    private var profile = UserProfile(
        username = "Mahasiswa Android",
        notificationsEnabled = true
    )

    override fun getProfile(): UserProfile = profile

    override fun updateUsername(username: String) {
        profile = profile.copy(username = username)
    }

    override fun updateNotificationsEnabled(enabled: Boolean) {
        profile = profile.copy(notificationsEnabled = enabled)
    }
}
