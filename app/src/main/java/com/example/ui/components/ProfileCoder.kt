package com.example.ui.components

import android.net.Uri
import com.example.data.UserProfile

object ProfileCoder {
    private const val BASE_URL = "https://puentedigital.app/share"

    fun encodeToUrl(profile: UserProfile, enabledPlatforms: Set<String>): String {
        val builder = Uri.parse(BASE_URL).buildUpon()
        builder.appendQueryParameter("n", profile.name)
        if (profile.bio.isNotEmpty()) builder.appendQueryParameter("b", profile.bio)
        
        if (enabledPlatforms.contains("WHATSAPP") && profile.whatsapp.isNotEmpty()) {
            builder.appendQueryParameter("wa", profile.whatsapp)
        }
        if (enabledPlatforms.contains("INSTAGRAM") && profile.instagram.isNotEmpty()) {
            builder.appendQueryParameter("ig", profile.instagram)
        }
        if (enabledPlatforms.contains("LINKEDIN") && profile.linkedin.isNotEmpty()) {
            builder.appendQueryParameter("li", profile.linkedin)
        }
        if (enabledPlatforms.contains("X") && profile.twitter.isNotEmpty()) {
            builder.appendQueryParameter("tw", profile.twitter)
        }
        if (enabledPlatforms.contains("GITHUB") && profile.github.isNotEmpty()) {
            builder.appendQueryParameter("gh", profile.github)
        }
        if (enabledPlatforms.contains("TIKTOK") && profile.tiktok.isNotEmpty()) {
            builder.appendQueryParameter("tk", profile.tiktok)
        }
        if (enabledPlatforms.contains("WEBSITE") && profile.website.isNotEmpty()) {
            builder.appendQueryParameter("web", profile.website)
        }
        
        return builder.build().toString()
    }

    fun decodeFromUrl(url: String): DecodedProfile? {
        return try {
            val uri = Uri.parse(url)
            if (uri.host != "puentedigital.app" || uri.path != "/share") {
                // Let's support loose matching as well in case of differences
                if (!url.contains("puentedigital.app/share")) {
                    return null
                }
            }
            DecodedProfile(
                name = uri.getQueryParameter("n") ?: "Usuario de Puente",
                bio = uri.getQueryParameter("b") ?: "",
                whatsapp = uri.getQueryParameter("wa") ?: "",
                instagram = uri.getQueryParameter("ig") ?: "",
                linkedin = uri.getQueryParameter("li") ?: "",
                twitter = uri.getQueryParameter("tw") ?: "",
                github = uri.getQueryParameter("gh") ?: "",
                tiktok = uri.getQueryParameter("tk") ?: "",
                website = uri.getQueryParameter("web") ?: ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class DecodedProfile(
    val name: String,
    val bio: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val linkedin: String = "",
    val twitter: String = "",
    val github: String = "",
    val tiktok: String = "",
    val website: String = ""
)
