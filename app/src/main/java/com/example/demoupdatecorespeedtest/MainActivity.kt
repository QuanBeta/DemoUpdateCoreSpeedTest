package com.example.demoupdatecorespeedtest

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.demoupdatecorespeedtest.databinding.ActivityMainBinding
import com.example.demoupdatecorespeedtest.util_speedtest.GetSpeedTestHostsHandler
import com.example.demoupdatecorespeedtest.util_speedtest.PingTest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private var tempBlackList: HashSet<String> = HashSet()
    lateinit var getSpeedTestHostsHandler: GetSpeedTestHostsHandler
    lateinit var pingTest: PingTest

    //ping data list
    private val pingRateList: MutableList<Double> = ArrayList()

    private var pingTestStarted: Boolean = false
    private var pingTestFinished: Boolean = false
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
            initPingTest()
            getPingTest()
            true
        }
    }

    private fun initPingTest() {
        val mapKey: HashMap<Int, String> = getSpeedTestHostsHandler.mapKey
        val mapValue: HashMap<Int, List<String>> = getSpeedTestHostsHandler.mapValue
        val selfLat: Double = getSpeedTestHostsHandler.selfLat
        val selfLon: Double = getSpeedTestHostsHandler.selfLon
        var tmp = 19349458.0
        var dist = 0.0
        var findServerIndex = 0
        for (index in mapKey.keys) {
            if (tempBlackList.contains(mapValue[index]!![5])) {
                continue
            }
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
        val info = mapValue[findServerIndex]
        val distance = dist
        if (info == null) {
            Log.e(javaClass.simpleName, "distance: $distance")
        }
        pingTest = PingTest(info?.get(6)?.replace(":8080", "") ?: null, 3)
    }

    private fun getPingTest() {
        GlobalScope.launch {
            while (true) {
                if (!pingTestStarted) {
                    pingTest.start()
                    pingTestStarted = true
                }

//            if (pingTestFinished) {
//                binding.tvPing.text = "Ping --- " + pingTest.avgRtt.toString()
//                break
//            }
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
                        break
                    }
                } else {
                    pingRateList.add(pingTest.instantRtt)
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
}