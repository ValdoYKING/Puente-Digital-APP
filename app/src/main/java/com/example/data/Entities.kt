package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

enum class SocialPlatform(
    val displayName: String,
    val iconName: String,
    val baseUrl: String,
    val prefix: String
) {
    WHATSAPP("WhatsApp", "whatsapp", "https://wa.me/", "+"),
    INSTAGRAM("Instagram", "instagram", "https://instagram.com/", "@"),
    LINKEDIN("LinkedIn", "linkedin", "https://linkedin.com/in/", "in/"),
    X("X (Twitter)", "twitter", "https://x.com/", "@"),
    GITHUB("GitHub", "github", "https://github.com/", ""),
    TIKTOK("TikTok", "tiktok", "https://tiktok.com/@", "@"),
    WEBSITE("Sitio Web", "website", "", "")
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val bio: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val linkedin: String = "",
    val twitter: String = "",
    val github: String = "",
    val tiktok: String = "",
    val website: String = "",
    val avatarColor: Int = 0xFF4F46E5.toInt(), // Modern Indigo as default
    // Bitmask or comma-separated list of enabled networks for sharing
    val sharedPlatforms: String = "WHATSAPP,INSTAGRAM,LINKEDIN"
)

@Entity(tableName = "contact_encounters")
data class ContactEncounter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,
    val contactBio: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val whatsapp: String = "",
    val instagram: String = "",
    val linkedin: String = "",
    val twitter: String = "",
    val github: String = "",
    val tiktok: String = "",
    val website: String = "",
    val encounterType: String = "QR", // "QR" or "NFC"
    val notes: String = ""
)

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}

@Dao
interface ContactEncounterDao {
    @Query("SELECT * FROM contact_encounters ORDER BY timestamp DESC")
    fun getAllEncounters(): Flow<List<ContactEncounter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEncounter(encounter: ContactEncounter)

    @Query("DELETE FROM contact_encounters WHERE id = :id")
    suspend fun deleteEncounterById(id: Int)
}

@Database(entities = [UserProfile::class, ContactEncounter::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun contactEncounterDao(): ContactEncounterDao
}
