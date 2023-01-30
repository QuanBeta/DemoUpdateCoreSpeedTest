@file:OptIn(DelicateCoroutinesApi::class)

package com.example.demoupdatecorespeedtest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.demoupdatecorespeedtest.databinding.ActivityMainBinding
import com.example.demoupdatecorespeedtest.viewmodel.SpeedTestViewModel
import kotlinx.coroutines.DelicateCoroutinesApi

class SpeedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var speedTestViewModel: SpeedTestViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        speedTestViewModel =
            ViewModelProvider(
                this, ViewModelProvider.NewInstanceFactory()
            )[SpeedTestViewModel::class.java]
        binding.btnStart.setOnClickListener {
            speedTestViewModel.getPingTest()
            speedTestViewModel.getDownload()
            speedTestViewModel.getUpload()
        }

        binding.btnDownload.setOnClickListener {

        }

        speedTestViewModel.pingDataEvent.observe(this) {
            if (it != null) {
                binding.tvPing.text = "Ping: $it"
            }
        }
        speedTestViewModel.uploadDataEvent.observe(this) {
            if (it != null) {
                binding.tvUpload.text = "Upload: $it"
            }
        }
        speedTestViewModel.downloadDataEvent.observe(this) {
            if (it != null) {
                binding.tvDownload.text = "Download: $it"
            }
        }
    }

}