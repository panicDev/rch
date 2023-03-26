package com.paniclabs.rch.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val isEnable = rch.isEnabled()
        Toast.makeText(this, "IsEnable $isEnable", Toast.LENGTH_SHORT).show()
    }
}