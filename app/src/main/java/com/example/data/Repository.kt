package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val userProfileDao: UserProfileDao,
    private val contactEncounterDao: ContactEncounterDao
) {
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    val allEncounters: Flow<List<ContactEncounter>> = contactEncounterDao.getAllEncounters()

    suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    suspend fun insertEncounter(encounter: ContactEncounter) {
        contactEncounterDao.insertEncounter(encounter)
    }

    suspend fun deleteEncounter(id: Int) {
        contactEncounterDao.deleteEncounterById(id)
    }
}
