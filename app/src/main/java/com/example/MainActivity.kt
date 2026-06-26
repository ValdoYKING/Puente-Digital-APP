package com.example

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.graphics.Bitmap
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ContactEncounter
import com.example.data.SocialPlatform
import com.example.data.UserProfile
import com.example.ui.components.DecodedProfile
import com.example.ui.components.ProfileCoder
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var writeModeActive = false
    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Database & Repo init
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "puente_digital.db"
        ).fallbackToDestructiveMigration().build()
        repo = AppRepository(db.userProfileDao(), db.contactEncounterDao())

        // Setup NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            MyApplicationTheme {
                viewModel = viewModel(
                    factory = MainViewModelFactory(application, repo)
                )

                // Check for incoming deep link / URL intent in onCreate
                intent?.let { handleIntent(it) }

                AppContent(
                    viewModel = viewModel,
                    isNfcSupported = nfcAdapter != null,
                    isNfcEnabled = nfcAdapter?.isEnabled == true,
                    onStartNfcWrite = { active ->
                        writeModeActive = active
                        if (active) {
                            Toast.makeText(
                                this,
                                "Modo Escritura NFC: Acerca un Tag NFC para grabar tus redes.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onOpenNfcSettings = {
                        try {
                            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(this, "No se pudo abrir la configuración de NFC", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register foreground dispatch to intercept NFC Tags when write mode is active or when scanning
        nfcAdapter?.let { adapter ->
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            val filters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            )
            adapter.enableForegroundDispatch(this, pendingIntent, filters, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        // Handle NFC Tag Scanned
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action || NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            
            if (tag != null && writeModeActive) {
                val success = writeUrlToTag(tag, viewModel.shareUrl.value)
                if (success) {
                    Toast.makeText(this, "¡Redes grabadas con éxito en el Tag NFC! 🎉", Toast.LENGTH_LONG).show()
                    writeModeActive = false
                } else {
                    Toast.makeText(this, "Error al grabar en el Tag NFC. Asegúrate que esté vacío y reintenta.", Toast.LENGTH_LONG).show()
                }
            } else {
                // Read NFC Tag
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.forEach { rawMsg ->
                    val msg = rawMsg as NdefMessage
                    msg.records.forEach { record ->
                        val payloadUri = record.toUri()?.toString()
                        if (payloadUri != null && payloadUri.contains("puentedigital.app/share")) {
                            viewModel.handleScannedUrl(payloadUri, "NFC")
                            Toast.makeText(this, "¡Perfil recibido por NFC! ⚡", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            // Handle Web Deep Link clicks
            intent.dataString?.let { data ->
                if (data.contains("puentedigital.app/share")) {
                    viewModel.handleScannedUrl(data, "QR")
                    Toast.makeText(this, "¡Perfil escaneado con éxito! 📱", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun writeUrlToTag(tag: Tag, url: String): Boolean {
        val ndefRecord = android.nfc.NdefRecord.createUri(url)
        val ndefMessage = NdefMessage(arrayOf(ndefRecord))
        val ndef = Ndef.get(tag)
        
        return if (ndef != null) {
            try {
                ndef.connect()
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            val format = NdefFormatable.get(tag)
            if (format != null) {
                try {
                    format.connect()
                    format.format(ndefMessage)
                    format.close()
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else {
                false
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    viewModel: MainViewModel,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    onStartNfcWrite: (Boolean) -> Unit,
    onOpenNfcSettings: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val encounters by viewModel.encounters.collectAsState()
    val sharedPlatforms by viewModel.sharedPlatforms.collectAsState()
    val shareUrl by viewModel.shareUrl.collectAsState()
    val qrCodeBitmap by viewModel.qrCodeBitmap.collectAsState()
    
    val isEditingProfile by viewModel.isEditingProfile.collectAsState()
    val editState by viewModel.editProfileState.collectAsState()
    val receivedProfile by viewModel.receivedProfile.collectAsState()
    
    var currentTab by remember { mutableStateOf(0) }
    var nfcWritingMode by remember { mutableStateOf(false) }

    // Color avatars options
    val avatarColors = listOf(
        0xFF4F46E5, // Indigo
        0xFF0891B2, // Cyan
        0xFF059669, // Emerald
        0xFFDC2626, // Rose/Red
        0xFFD97706, // Amber
        0xFF7C3AED  // Violet
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                tonalElevation = 4.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = "Mi Código QR") },
                    label = { Text("Mi QR", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_qr")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Nfc, contentDescription = "Simulador NFC / Proximidad") },
                    label = { Text("NFC", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_nfc")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historial de Contactos") },
                    label = { Text("Contactos", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switching with AnimatedVisibility for beautiful animations
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    // Try to show generated image, fallback to elegant gradient brush
                    val bannerImage = painterResource(id = R.drawable.img_puente_banner)
                    Image(
                        painter = bannerImage,
                        contentDescription = "Fondo de Puente Digital",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay to keep text highly readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    // Profile Details inside Header Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(userProfile.avatarColor))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.name.take(1).uppercase(Locale.getDefault()),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile.name.ifEmpty { "Tu Nombre" },
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = userProfile.bio.ifEmpty { "Edita tu bio para presentarte" },
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.startEditingProfile() },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Perfil",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Divider line
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                // Render active screen
                when (currentTab) {
                    0 -> QrShareScreen(
                        userProfile = userProfile,
                        sharedPlatforms = sharedPlatforms,
                        qrCodeBitmap = qrCodeBitmap,
                        shareUrl = shareUrl,
                        onTogglePlatform = { viewModel.togglePlatformSharing(it) }
                    )
                    1 -> NfcSimulatorScreen(
                        isNfcSupported = isNfcSupported,
                        isNfcEnabled = isNfcEnabled,
                        nfcWritingMode = nfcWritingMode,
                        onToggleNfcWriting = { active ->
                            nfcWritingMode = active
                            onStartNfcWrite(active)
                        },
                        onTriggerMock = { index -> viewModel.triggerMockNfcExchange(index) },
                        mockProfiles = viewModel.mockSimulatedProfiles,
                        onOpenSettings = onOpenNfcSettings
                    )
                    2 -> ContactHistoryScreen(
                        encounters = encounters,
                        onDeleteEncounter = { viewModel.deleteEncounter(it) }
                    )
                }
            }

            // Edit Profile Modal Overlay
            if (isEditingProfile) {
                EditProfileModal(
                    editState = editState,
                    avatarColors = avatarColors,
                    onUpdateState = { viewModel.updateEditingProfile(it) },
                    onSave = { viewModel.saveProfileEdits() },
                    onCancel = { viewModel.cancelEditingProfile() }
                )
            }

            // Received Profile Overlay (QR scan, NFC scan, or Mock trigger)
            receivedProfile?.let { profile ->
                ReceivedProfileDialog(
                    profile = profile,
                    notes = viewModel.notesInput.collectAsState().value,
                    encounterType = viewModel.encounterType.collectAsState().value,
                    onUpdateNotes = { viewModel.updateNotesInput(it) },
                    onSave = { viewModel.saveReceivedContact() },
                    onDiscard = { viewModel.discardReceivedContact() }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QrShareScreen(
    userProfile: UserProfile,
    sharedPlatforms: Set<String>,
    qrCodeBitmap: Bitmap?,
    shareUrl: String,
    onTogglePlatform: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Explanatory card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tip",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Selecciona cuáles redes compartir en este encuentro. El código QR se actualizará al instante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QR Code Display Frame - Sleek styling with 32dp corner radius, white background, and thin border
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .size(280.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "Código QR Puente Digital",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Completa tu perfil para generar el QR",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NFC Ready status indicator from HTML
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "CONEXIÓN NFC LISTA",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "Acerca tu dispositivo a otro para conectar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Selective Sharing Grid Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Redes activas en este encuentro:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${sharedPlatforms.size} Seleccionadas",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid-based selective share pills
        val platformChunks = SocialPlatform.values().toList().chunked(3)
        platformChunks.forEach { chunk ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chunk.forEach { platform ->
                    val handle = getPlatformHandle(userProfile, platform)
                    val isEnabled = sharedPlatforms.contains(platform.name)
                    val hasHandle = handle.isNotEmpty()

                    // Render Pill Button matching Sleek Interface
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isEnabled && hasHandle) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isEnabled && hasHandle) {
                                    Color.Transparent
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = hasHandle) {
                                onTogglePlatform(platform.name)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = getPlatformIcon(platform),
                                contentDescription = platform.displayName,
                                tint = if (isEnabled && hasHandle) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else if (hasHandle) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = platform.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = if (isEnabled && hasHandle) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else if (hasHandle) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Fill row if incomplete
                if (chunk.size < 3) {
                    repeat(3 - chunk.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sleek Copy Permanent Link Button (Pill shaped, full width, 14-height equivalent)
        Button(
            onClick = {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Conéctate conmigo mediante mi Puente Digital: $shareUrl")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Compartir Puente Digital")
                context.startActivity(shareIntent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("share_link_button")
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Compartir Enlace Permanente",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun NfcSimulatorScreen(
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    nfcWritingMode: Boolean,
    onToggleNfcWriting: (Boolean) -> Unit,
    onTriggerMock: (Int) -> Unit,
    mockProfiles: List<DecodedProfile>,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Real NFC hardware status card
        Text(
            text = "Tecnología NFC Física",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNfcSupported && isNfcEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                 else MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "NFC Hardware",
                        tint = if (isNfcSupported && isNfcEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (!isNfcSupported) "NFC No Soportado en este Dispositivo"
                                   else if (!isNfcEnabled) "NFC Desactivado"
                                   else "NFC Activo y Listo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isNfcSupported && isNfcEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (!isNfcSupported) "La simulación NFC sigue disponible abajo."
                                   else if (!isNfcEnabled) "Haz clic abajo para activarlo en ajustes."
                                   else "Puedes grabar etiquetas físicas o recibir contactos tocando dispositivos.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                if (isNfcSupported && !isNfcEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Activar NFC", fontWeight = FontWeight.Bold)
                    }
                }

                if (isNfcSupported && isNfcEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Grabar en Sticker / Tarjeta NFC",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Escribe tus redes a un chip NFC físico.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Button(
                            onClick = { onToggleNfcWriting(!nfcWritingMode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (nfcWritingMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (nfcWritingMode) "Cancelar" else "Grabar Tag", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Writing tag animation indicator
        AnimatedVisibility(visible = nfcWritingMode) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Aproxima tu etiqueta NFC física a la parte trasera para escribir...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Simulated Proximity Area
        Text(
            text = "Simulador de Proximidad NFC",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Debido a que estás en un emulador, puedes simular acercarte a otras personas para intercambiar perfiles al instante.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Selecciona con quién chocar tu dispositivo:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                mockProfiles.forEachIndexed { index, mockProfile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onTriggerMock(index) }
                            .testTag("simulate_nfc_button_$index"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    mockProfile.name.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mockProfile.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = mockProfile.bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "Simular toque",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactHistoryScreen(
    encounters: List<ContactEncounter>,
    onDeleteEncounter: (Int) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial de Encuentros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${encounters.size} Contactos",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (encounters.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContactPage,
                    contentDescription = "No contactos",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aún no has guardado contactos",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Comparte tu QR o simula toques NFC para comenzar a poblar tu puente digital.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(encounters, key = { it.id }) { encounter ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("encounter_card_${encounter.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = encounter.contactName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = encounter.contactName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatDate(encounter.timestamp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }

                                // Encounter Type Tag
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (encounter.encounterType == "NFC") MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            RoundedCornerShape(50.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (encounter.encounterType == "NFC") Icons.Default.Nfc else Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = if (encounter.encounterType == "NFC") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = encounter.encounterType,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (encounter.encounterType == "NFC") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Delete button
                                IconButton(
                                    onClick = { onDeleteEncounter(encounter.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Borrar contacto",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (encounter.contactBio.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = encounter.contactBio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }

                            if (encounter.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Nota: ${encounter.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Grid of active social handles in this contact
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val platforms: List<Pair<SocialPlatform, String>> = listOf(
                                    SocialPlatform.WHATSAPP to encounter.whatsapp,
                                    SocialPlatform.INSTAGRAM to encounter.instagram,
                                    SocialPlatform.LINKEDIN to encounter.linkedin,
                                    SocialPlatform.X to encounter.twitter,
                                    SocialPlatform.GITHUB to encounter.github,
                                    SocialPlatform.TIKTOK to encounter.tiktok,
                                    SocialPlatform.WEBSITE to encounter.website
                                )

                                platforms.forEach { pair ->
                                    val platform = pair.first
                                    val value = pair.second
                                    if (value.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .background(
                                                    getPlatformColor(platform).copy(alpha = 0.1f),
                                                    RoundedCornerShape(50.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    getPlatformColor(platform).copy(alpha = 0.3f),
                                                    RoundedCornerShape(50.dp)
                                                )
                                                .clickable {
                                                    val url = formatPlatformUrl(platform, value)
                                                    if (url.isNotEmpty()) {
                                                        try {
                                                            uriHandler.openUri(url)
                                                        } catch (e: Exception) {
                                                            // error opening link
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = getPlatformIcon(platform),
                                                    contentDescription = null,
                                                    tint = getPlatformColor(platform),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${platform.displayName}: ${platform.prefix}$value",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = getPlatformColor(platform)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileModal(
    editState: UserProfile,
    avatarColors: List<Long>,
    onUpdateState: (UserProfile) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Editar tu Puente Digital",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Configura tus enlaces que se guardarán y compartirán.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar color select
                Text("Color de tu Puente", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    avatarColors.forEach { colorVal ->
                        val intColor = colorVal.toInt()
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(intColor))
                                .border(
                                    width = if (editState.avatarColor == intColor) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateState(editState.copy(avatarColor = intColor))
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Basic details
                OutlinedTextField(
                    value = editState.name,
                    onValueChange = { onUpdateState(editState.copy(name = it)) },
                    label = { Text("Tu Nombre Completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editState.bio,
                    onValueChange = { onUpdateState(editState.copy(bio = it)) },
                    label = { Text("Biografía / Ocupación") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_bio")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Tus Cuentas Sociales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Social platforms inputs
                SocialPlatform.values().forEach { platform ->
                    val handle = getPlatformHandle(editState, platform)
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { newValue ->
                            onUpdateState(updatePlatformHandle(editState, platform, newValue))
                        },
                        label = { Text(platform.displayName) },
                        placeholder = { Text(platform.prefix + "usuario") },
                        leadingIcon = {
                            Icon(
                                imageVector = getPlatformIcon(platform),
                                contentDescription = null,
                                tint = getPlatformColor(platform)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (platform == SocialPlatform.WHATSAPP) KeyboardType.Phone else KeyboardType.Text
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = getPlatformColor(platform),
                            focusedLabelColor = getPlatformColor(platform)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("input_${platform.name.lowercase(Locale.getDefault())}")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceivedProfileDialog(
    profile: DecodedProfile,
    notes: String,
    encounterType: String,
    onUpdateNotes: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Dialog(onDismissRequest = onDiscard) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Banner header inside dialog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (encounterType == "NFC") Icons.Default.Nfc else Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (encounterType == "NFC") "¡Encuentro NFC Detectado!" else "¡Código QR Escaneado!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile card details
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (profile.bio.isNotEmpty()) {
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Platforms checklist of networks they are sharing
                Text(
                    text = "Redes que te comparte:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val platforms: List<Pair<SocialPlatform, String>> = listOf(
                    SocialPlatform.WHATSAPP to profile.whatsapp,
                    SocialPlatform.INSTAGRAM to profile.instagram,
                    SocialPlatform.LINKEDIN to profile.linkedin,
                    SocialPlatform.X to profile.twitter,
                    SocialPlatform.GITHUB to profile.github,
                    SocialPlatform.TIKTOK to profile.tiktok,
                    SocialPlatform.WEBSITE to profile.website
                ).filter { it.second.isNotEmpty() }

                platforms.forEach { pair ->
                    val platform = pair.first
                    val value = pair.second
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(getPlatformColor(platform).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getPlatformIcon(platform),
                                contentDescription = null,
                                tint = getPlatformColor(platform),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${platform.displayName}: ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${platform.prefix}$value",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom private notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = onUpdateNotes,
                    label = { Text("Notas del encuentro (ej. Evento Tech)") },
                    placeholder = { Text("Escribe dónde se conocieron...") },
                    modifier = Modifier.fillMaxWidth().testTag("input_received_notes"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDiscard,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Descartar")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("save_received_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Contacto", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helpers
fun getPlatformHandle(profile: UserProfile, platform: SocialPlatform): String {
    return when (platform) {
        SocialPlatform.WHATSAPP -> profile.whatsapp
        SocialPlatform.INSTAGRAM -> profile.instagram
        SocialPlatform.LINKEDIN -> profile.linkedin
        SocialPlatform.X -> profile.twitter
        SocialPlatform.GITHUB -> profile.github
        SocialPlatform.TIKTOK -> profile.tiktok
        SocialPlatform.WEBSITE -> profile.website
    }
}

fun updatePlatformHandle(profile: UserProfile, platform: SocialPlatform, value: String): UserProfile {
    return when (platform) {
        SocialPlatform.WHATSAPP -> profile.copy(whatsapp = value)
        SocialPlatform.INSTAGRAM -> profile.copy(instagram = value)
        SocialPlatform.LINKEDIN -> profile.copy(linkedin = value)
        SocialPlatform.X -> profile.copy(twitter = value)
        SocialPlatform.GITHUB -> profile.copy(github = value)
        SocialPlatform.TIKTOK -> profile.copy(tiktok = value)
        SocialPlatform.WEBSITE -> profile.copy(website = value)
    }
}

fun getPlatformIcon(platform: SocialPlatform): androidx.compose.ui.graphics.vector.ImageVector {
    return when (platform) {
        SocialPlatform.WHATSAPP -> Icons.Default.Phone
        SocialPlatform.INSTAGRAM -> Icons.Default.AlternateEmail
        SocialPlatform.LINKEDIN -> Icons.Default.ContactPage
        SocialPlatform.X -> Icons.Default.AlternateEmail
        SocialPlatform.GITHUB -> Icons.Default.Link
        SocialPlatform.TIKTOK -> Icons.Default.AlternateEmail
        SocialPlatform.WEBSITE -> Icons.Default.Language
    }
}

fun getPlatformColor(platform: SocialPlatform): Color {
    return when (platform) {
        SocialPlatform.WHATSAPP -> Color(0xFF25D366)
        SocialPlatform.INSTAGRAM -> Color(0xFFE1306C)
        SocialPlatform.LINKEDIN -> Color(0xFF0077B5)
        SocialPlatform.X -> Color(0xFF1F2937) // Custom Dark Slate for X
        SocialPlatform.GITHUB -> Color(0xFF4B5563)
        SocialPlatform.TIKTOK -> Color(0xFFEF4444)
        SocialPlatform.WEBSITE -> Color(0xFF0D9488)
    }
}

fun formatPlatformUrl(platform: SocialPlatform, handle: String): String {
    if (handle.isEmpty()) return ""
    return if (platform.baseUrl.isEmpty()) {
        if (!handle.startsWith("http://") && !handle.startsWith("https://")) {
            "https://$handle"
        } else {
            handle
        }
    } else {
        // WhatsApp needs only numbers
        val cleanHandle = if (platform == SocialPlatform.WHATSAPP) {
            handle.replace(" ", "").replace("+", "").replace("-", "")
        } else {
            handle.trim().removePrefix("@")
        }
        platform.baseUrl + cleanHandle
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
