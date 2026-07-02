package com.example.zyra.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.zyra.R

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var editProfileButton: Button

    private var profileImageUri: Uri? = null

    // 1️⃣ Add this launcher for picking images
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            loadProfileImage() // display immediately
        }
    }

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) loadProfileImage()
        else profileImageView.setImageResource(R.drawable.ic_default_avatar)
    }

    // Activity result launcher for returning from EditProfile
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Reload profile data after editing
            loadUserProfile()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // ✅ Correct IDs assigned to correct types
        profileImageView = view.findViewById(R.id.ivProfilePic)
        nameTextView = view.findViewById(R.id.tvProfileName)
        emailTextView = view.findViewById(R.id.tvProfileEmail)
        editProfileButton = view.findViewById(R.id.btnEditProfile)

        editProfileButton.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        profileImageView.setOnClickListener {
            // Ask permission first
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*") // open gallery
            } else {
                requestPermissionLauncher.launch(permission)
            }
        }

        return view
    }

    private fun loadUserProfile() {
        // TODO: Replace these with your Supabase data fetching
        nameTextView.text = "User Name"
        emailTextView.text = "user@email.com"
        profileImageUri = null // Replace with actual Uri if available

        if (profileImageUri != null) checkPermissionAndLoadImage(profileImageUri!!)
        else profileImageView.setImageResource(R.drawable.ic_default_avatar)
    }

    private fun checkPermissionAndLoadImage(uri: Uri) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                loadProfileImage()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadProfileImage() {
        try {
            profileImageUri?.let { uri ->
                val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.createSource(requireContext().contentResolver, uri)
                } else {
                    @Suppress("DEPRECATION")
                    android.graphics.BitmapFactory.decodeStream(
                        requireContext().contentResolver.openInputStream(uri)
                    )
                    return
                }
                val drawable = ImageDecoder.decodeDrawable(source)
                profileImageView.setImageDrawable(drawable)
            } ?: profileImageView.setImageResource(R.drawable.ic_default_avatar)
        } catch (e: Exception) {
            e.printStackTrace()
            profileImageView.setImageResource(R.drawable.ic_default_avatar)
        }
    }
}
