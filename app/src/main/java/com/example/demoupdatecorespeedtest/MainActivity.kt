@file:OptIn(DelicateCoroutinesApi::class)

package com.example.demoupdatecorespeedtest

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.demoupdatecorespeedtest.databinding.ActivityMainBinding
import com.example.demoupdatecorespeedtest.util_speedtest.GetSpeedTestHostsHandler
import com.example.demoupdatecorespeedtest.util_speedtest.HttpDownloadTest
import com.example.demoupdatecorespeedtest.util_speedtest.HttpUploadTest
import com.example.demoupdatecorespeedtest.util_speedtest.PingTest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var getSpeedTestHostsHandler: GetSpeedTestHostsHandler
    private lateinit var downloadTest: HttpDownloadTest
    private lateinit var uploadTest: HttpUploadTest
    private lateinit var pingTest: PingTest

    //var of ping
    private var pingTestStarted: Boolean = false
    private var pingTestFinished: Boolean = false

    //var of download
    private var downloadTestFinished: Boolean = false
    private var downloadTestStarted: Boolean = false

    //var of upload
    private var uploadTestFinished: Boolean = false
    private var uploadTestStarted: Boolean = false

    //position
    private var position = 0
    private var lastPosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getSpeedTestHostsHandler = GetSpeedTestHostsHandler()
        binding.btnStart.setOnClickListener {
            getSpeedTestHostsHandler.start()
            while (!getSpeedTestHostsHandler.isFinished) {
                if (getSpeedTestHostsHandler.isFinished) {
                    binding.btnStart.text = "isFinished"
                }
            }
        }

        binding.btnStart.setOnLongClickListener {
            initData()
            getPingTest()
            true
        }

        binding.btnDownload.setOnClickListener {
            initData()
            getDownload()
        }

        binding.btnUpload.setOnClickListener {
            initData()
            getUpload()
        }
    }

    private fun initData() {
        /* Init Data All*/
        val mapKey: HashMap<Int, String> = getSpeedTestHostsHandler.mapKey
        val mapValue: HashMap<Int, List<String>> = getSpeedTestHostsHandler.mapValue
        val selfLat: Double = getSpeedTestHostsHandler.selfLat
        val selfLon: Double = getSpeedTestHostsHandler.selfLon
        var tmp = 19349458.0
        var dist = 0.0
        var findServerIndex = 0
        for (index in mapKey.keys) {
            val source = Location("Source")
            source.latitude = selfLat
            source.longitude = selfLon
            val ls = mapValue[index]!!
            val dest = Location("Dest")
            dest.latitude = ls[0].toDouble()
            dest.longitude = ls[1].toDouble()
            val distance = source.distanceTo(dest).toDouble()
            if (tmp > distance) {
                tmp = distance
                dist = distance
                findServerIndex = index
            }
        }
        val testAddr = mapKey[findServerIndex]!!.replace("http://", "https://")
        val info = mapValue[findServerIndex]
        val distance = dist
        if (info == null) {
            Log.e(javaClass.simpleName, "distance: $distance")
        }
        /* Init PING*/
        pingTest = PingTest(info?.get(6)?.replace(":8080", "") ?: null, 3)
        /* Init Download*/
        downloadTest = HttpDownloadTest(
            testAddr.replace(
                testAddr.split("/").toTypedArray()[testAddr.split("/").toTypedArray().size - 1], ""
            )
        )
        /* Init Upload*/
        uploadTest = HttpUploadTest(testAddr)
    }

    private fun getPingTest() {
        //run get Ping
        GlobalScope.launch {
            while (true) {
                if (!pingTestStarted) {
                    pingTest.start()
                    pingTestStarted = true
                }
                if (pingTestFinished) {
                    //Failure
                    if (pingTest.avgRtt == 0.0) {
                        println("Ping error...")
                        runOnUiThread { binding.tvPing.text = "Ping error..." }
                    } else {
                        //Success
                        runOnUiThread {
                            binding.tvPing.text = "Ping --- " + pingTest.avgRtt.toString()
                        }
                    }
                    break
                } else {
                    runOnUiThread {
                        binding.tvPing.text = "Ping --- " + pingTest.avgRtt.toString()
                    }
                }
                Log.e(
                    "pingTest",
                    "-- instantRtt: ${pingTest.instantRtt} -- avgRtt: ${pingTest.avgRtt}"
                )
                if (pingTest.isFinished) {
                    pingTestFinished = true
                }
            }
        }
    }

    private fun getDownload() {
        GlobalScope.launch {
            while (true) {
                if (!downloadTestStarted) {
                    downloadTest.start()
                    downloadTestStarted = true
                }
                if (downloadTestFinished) {
                    //Failure
                    if (downloadTest.finalDownloadRate == 0.0) {
                        println("Download error...")
                        runOnUiThread { binding.tvDownload.text = "Download error" }
                        return@launch
                    } else {
                        //Success
                        runOnUiThread {
                            runOnUiThread {
                                binding.tvDownload.text =
                                    "Download -- ${downloadTest.finalDownloadRate}"
                            }
                        }
                    }
                    break
                } else {
                    //Calc position
                    val downloadRate: Double = downloadTest.instantDownloadRate
                    position = getPositionByRate(downloadRate)
                    runOnUiThread {
                        binding.tvDownload.text = "${downloadTest.instantDownloadRate}"
                    }
                    lastPosition = position
                }
                if (downloadTest.isFinished) {
                    downloadTestFinished = true
                }
                Log.e(
                    javaClass.simpleName,
                    "getDownload: -- instant :${downloadTest.instantDownloadRate}  -- finalDownloadRate: ${downloadTest.finalDownloadRate}"
                )
            }
        }
    }

    private fun getUpload() {
        GlobalScope.launch {
            while (true) {
                if (!uploadTestStarted) {
                    uploadTest.start()
                    uploadTestStarted = true
                }
                if (uploadTestFinished) {
                    //Failure
                    if (uploadTest.finalUploadRate == 0.0) {
                        println("Upload error...")
                        runOnUiThread { binding.tvUpload.text = "Upload error" }
                        return@launch
                    } else {
                        //Success
                        runOnUiThread { binding.tvUpload.text = "${uploadTest.finalUploadRate}" }
                    }
                    break
                } else {
                    //Calc position
                    val uploadRate: Double = uploadTest.instantUploadRate
                    position = getPositionByRate(uploadRate)
                    runOnUiThread { binding.tvUpload.text = "${uploadTest.instantUploadRate}" }
                    lastPosition = position
                }
                if (uploadTest.isFinished) {
                    uploadTestFinished = true
                }
                Log.e(
                    javaClass.simpleName,
                    "getUpload: -- instant :${uploadTest.instantUploadRate}  -- finalDownloadRate: ${uploadTest.finalUploadRate}"
                )
            }
        }
    }

    private fun getPositionByRate(rate: Double): Int {
        if (rate <= 1) {
            return (rate * 30).toInt()
        } else if (rate <= 10) {
            return (rate * 6).toInt() + 30
        } else if (rate <= 30) {
            return ((rate - 10) * 3).toInt() + 90
        } else if (rate <= 50) {
            return ((rate - 30) * 1.5).toInt() + 150
        } else if (rate <= 100) {
            return ((rate - 50) * 1.2).toInt() + 180
        }
        return 0
    }

}