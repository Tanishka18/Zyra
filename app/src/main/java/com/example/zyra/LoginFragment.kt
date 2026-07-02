package com.example.zyra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.zyra.R

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        btnLogin.setOnClickListener {
            // Navigate to TimelineFragment (dummy login)
            it.findNavController().navigate(R.id.action_loginFragment_to_timelineFragment)
        }

        return view
    }
}
