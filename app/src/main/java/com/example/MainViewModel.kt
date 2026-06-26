package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ContactEncounter
import com.example.data.SocialPlatform
import com.example.data.UserProfile
import com.example.ui.components.DecodedProfile
import com.example.ui.components.ProfileCoder
import com.example.ui.components.QrCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // User profile from DB
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    // History of encounters
    val encounters: StateFlow<List<ContactEncounter>> = repository.allEncounters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current active sharing platforms
    private val _sharedPlatforms = MutableStateFlow<Set<String>>(
        setOf("WHATSAPP", "INSTAGRAM", "LINKEDIN")
    )
    val sharedPlatforms = _sharedPlatforms.asStateFlow()

    // Initialize sharing platforms when profile loads
    init {
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile.name.isEmpty()) {
                    // Seed a default profile if database is newly created
                    repository.saveProfile(
                        UserProfile(
                            name = "Mi Perfil",
                            bio = "¡Hola! Conéctate conmigo.",
                            whatsapp = "+52 55 1234 5678",
                            instagram = "usuario_puente",
                            linkedin = "puentedigital",
                            twitter = "puentedigital"
                        )
                    )
                } else {
                    val parsed = profile.sharedPlatforms.split(",")
                        .filter { it.isNotEmpty() }
                        .toSet()
                    _sharedPlatforms.value = parsed
                }
            }
        }
    }

    // Temporary profile editing state
    private val _isEditingProfile = MutableStateFlow(false)
    val isEditingProfile = _isEditingProfile.asStateFlow()

    private val _editProfileState = MutableStateFlow(UserProfile())
    val editProfileState = _editProfileState.asStateFlow()

    fun startEditingProfile() {
        _editProfileState.value = userProfile.value
        _isEditingProfile.value = true
    }

    fun updateEditingProfile(updated: UserProfile) {
        _editProfileState.value = updated
    }

    fun saveProfileEdits() {
        viewModelScope.launch {
            val updated = _editProfileState.value.copy(
                sharedPlatforms = _sharedPlatforms.value.joinToString(",")
            )
            repository.saveProfile(updated)
            _isEditingProfile.value = false
        }
    }

    fun cancelEditingProfile() {
        _isEditingProfile.value = false
    }

    // Toggle platform active status in current sharing encounter
    fun togglePlatformSharing(platformName: String) {
        val current = _sharedPlatforms.value.toMutableSet()
        if (current.contains(platformName)) {
            current.remove(platformName)
        } else {
            current.add(platformName)
        }
        _sharedPlatforms.value = current
        
        // Persist the preference in profile
        viewModelScope.launch {
            val updatedProfile = userProfile.value.copy(
                sharedPlatforms = current.joinToString(",")
            )
            repository.saveProfile(updatedProfile)
        }
    }

    // Generate Share URL based on currently enabled networks
    val shareUrl: StateFlow<String> = combine(userProfile, _sharedPlatforms) { profile, enabled ->
        ProfileCoder.encodeToUrl(profile, enabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    // Generate QR Code dynamically
    val qrCodeBitmap: StateFlow<Bitmap?> = shareUrl.combine(_sharedPlatforms) { url, _ ->
        if (url.isNotEmpty()) {
            QrCodeGenerator.generateQrCode(
                content = url,
                size = 512,
                fgColor = userProfile.value.avatarColor,
                bgColor = Color.WHITE
            )
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Handle scanned/received profiles
    private val _receivedProfile = MutableStateFlow<DecodedProfile?>(null)
    val receivedProfile = _receivedProfile.asStateFlow()

    private val _encounterType = MutableStateFlow("QR")
    val encounterType = _encounterType.asStateFlow()

    private val _notesInput = MutableStateFlow("")
    val notesInput = _notesInput.asStateFlow()

    fun handleScannedUrl(url: String, type: String = "QR") {
        val decoded = ProfileCoder.decodeFromUrl(url)
        if (decoded != null) {
            _receivedProfile.value = decoded
            _encounterType.value = type
            _notesInput.value = ""
        }
    }

    fun updateNotesInput(notes: String) {
        _notesInput.value = notes
    }

    fun saveReceivedContact() {
        val profile = _receivedProfile.value ?: return
        viewModelScope.launch {
            repository.insertEncounter(
                ContactEncounter(
                    contactName = profile.name,
                    contactBio = profile.bio,
                    whatsapp = profile.whatsapp,
                    instagram = profile.instagram,
                    linkedin = profile.linkedin,
                    twitter = profile.twitter,
                    github = profile.github,
                    tiktok = profile.tiktok,
                    website = profile.website,
                    encounterType = _encounterType.value,
                    notes = _notesInput.value
                )
            )
            _receivedProfile.value = null
        }
    }

    fun discardReceivedContact() {
        _receivedProfile.value = null
    }

    fun deleteEncounter(id: Int) {
        viewModelScope.launch {
            repository.deleteEncounter(id)
        }
    }

    // NFC Simulation: Pre-made templates for nearby simulations
    val mockSimulatedProfiles = listOf(
        DecodedProfile(
            name = "Sonia Martínez",
            bio = "UX/UI Designer | Amante del Café ☕",
            whatsapp = "+52 55 9876 5432",
            instagram = "sonia_ux",
            linkedin = "soniamartinez-ux",
            website = "https://soniadesign.co"
        ),
        DecodedProfile(
            name = "Carlos Ruiz",
            bio = "Lead Android Engineer @ TechNova 🚀",
            linkedin = "cruiz-android",
            github = "cruiz-dev",
            twitter = "carlitos_ruiz"
        ),
        DecodedProfile(
            name = "Lucía Fernández",
            bio = "Fotógrafa de Retratos & Eventos 📸",
            instagram = "lucia_ph",
            tiktok = "lucia_studio",
            whatsapp = "+52 55 5432 1098"
        )
    )

    fun triggerMockNfcExchange(index: Int) {
        if (index in mockSimulatedProfiles.indices) {
            _receivedProfile.value = mockSimulatedProfiles[index]
            _encounterType.value = "NFC"
            _notesInput.value = ""
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
