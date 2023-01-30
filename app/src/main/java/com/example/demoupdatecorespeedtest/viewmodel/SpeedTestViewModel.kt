package com.example.demoupdatecorespeedtest.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.demoupdatecorespeedtest.util_speedtest.GetSpeedTestHostsHandler
import com.example.demoupdatecorespeedtest.util_speedtest.HttpDownloadTest
import com.example.demoupdatecorespeedtest.util_speedtest.HttpUploadTest
import com.example.demoupdatecorespeedtest.util_speedtest.PingTest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SpeedTestViewModel() : ViewModel() {
    //Ping Data
    private val pingDataListener: MutableLiveData<Double> by lazy {
        MutableLiveData()
    }
    val pingDataEvent: LiveData<Double> by lazy {
        pingDataListener
    }

    //Download Data
    private val downloadDataListener: MutableLiveData<Double> by lazy {
        MutableLiveData()
    }
    val downloadDataEvent: LiveData<Double> by lazy {
        downloadDataListener
    }

    //Upload Data
    private val uploadDataListener: MutableLiveData<Double> by lazy {
        MutableLiveData()
    }
    val uploadDataEvent: LiveData<Double> by lazy {
        uploadDataListener
    }
    private var getSpeedTestHostsHandler: GetSpeedTestHostsHandler = GetSpeedTestHostsHandler()
    private lateinit var downloadTest: HttpDownloadTest
    private lateinit var uploadTest: HttpUploadTest
    private lateinit var pingTest: PingTest

    init {
        Log.e(javaClass.simpleName, "---init---")
        initData()
    }

    //var of ping
    var pingTestStarted: Boolean = false
    var pingTestFinished: Boolean = false

    //var of download
    var downloadTestFinished: Boolean = false
    var downloadTestStarted: Boolean = false

    //var of upload
    var uploadTestFinished: Boolean = false
    var uploadTestStarted: Boolean = false

    //position
    private var position = 0
    private var lastPosition = 0

    private fun initData() {
        getSpeedTestHostsHandler.start()
        /* Init Data All*/
        while (!getSpeedTestHostsHandler.isFinished) {
            if (getSpeedTestHostsHandler.isFinished) {
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
                /* Init Ping*/
                pingTest = PingTest(info?.get(6)?.replace(":8080", "") ?: null, 3)
                /* Init Download*/
                downloadTest = HttpDownloadTest(
                    testAddr.replace(
                        testAddr.split("/").toTypedArray()[testAddr.split("/")
                            .toTypedArray().size - 1], ""
                    )
                )
                /* Init Upload*/
                uploadTest = HttpUploadTest(testAddr)
            }
        }
    }

    fun getPingTest() {
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
                        pingDataListener.postValue(-1.0)
                    } else {
                        //Success
                        pingDataListener.postValue(pingTest.avgRtt)
                    }
                    break
                } else {
                    pingDataListener.postValue(pingTest.avgRtt)
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

    fun getDownload() {
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
                        downloadDataListener.postValue(-1.0)
                        return@launch
                    } else {
                        //Success
                        downloadDataListener.postValue(downloadTest.finalDownloadRate)
                    }
                    break
                } else {
                    //Calc position
                    val downloadRate: Double = downloadTest.instantDownloadRate
                    position = getPositionByRate(downloadRate)
                    downloadDataListener.postValue(downloadTest.instantDownloadRate)
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

    fun getUpload() {
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
                        uploadDataListener.postValue(-1.0)
                        return@launch
                    } else {
                        //Success
                        uploadDataListener.postValue(uploadTest.finalUploadRate)
                    }
                    break
                } else {
                    //Calc position
                    val uploadRate: Double = uploadTest.instantUploadRate
                    position = getPositionByRate(uploadRate)
                    uploadDataListener.postValue(uploadTest.instantUploadRate)
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