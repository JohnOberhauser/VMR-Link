package com.ober.vmrlink

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class LinkTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Test
    fun testLink() {
        val countDownLatch = CountDownLatch(1)
        val link = object : Link<String>() {
            override fun fetch(): LiveData<Resource<String>> {
                val liveData = MutableLiveData<Resource<String>>()
                liveData.value = Resource.success("test", Source.DATABASE)
                return liveData
            }
        }

        link.value.observeForever {
            assert(it.data == "test")
            countDownLatch.countDown()
        }
        link.update()
        countDownLatch.await()
    }
}