package com.example.myapplicationw.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.camera.core.ExperimentalGetImage
import androidx.fragment.app.Fragment
import com.example.myapplicationw.R
import com.example.myapplicationw.ui.activity.MainActivity
import com.example.myapplicationw.ui.activity.TestActivity

@ExperimentalGetImage class HomeFragment : Fragment() {
    private lateinit var btntest:Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btntest=view.findViewById(R.id.buttonScan)
        btntest.setOnClickListener {
            val intent = Intent(requireContext(), TestActivity::class.java)
            startActivity(intent)
        }
    }

}