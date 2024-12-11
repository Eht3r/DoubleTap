package com.example.doubletap

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doubletap.databinding.ActivityEditTextBinding

class EditTextActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditTextBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}