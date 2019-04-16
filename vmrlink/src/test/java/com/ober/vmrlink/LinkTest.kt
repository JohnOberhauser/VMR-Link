package com.ober.vmrlink

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class LinkTest {

    private val testRepo = TestRepo()

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Test
    fun testLink() {
        val countDownLatch = CountDownLatch(1)
        val link = LoginLink(testRepo)

        link.value.observeForever {
            assert(it.data == "test")
            countDownLatch.countDown()
        }
        link.update("test", "test")
        countDownLatch.await()
    }

    class LoginLink(private val testRepo: TestRepo): Link<String>() {
        private var userName: String? = null
        private var password: String? = null

        fun update(username: String?, password: String?) {
            update()
        }

        override fun fetch(): LiveData<Resource<String>> {
            return testRepo.getData(userName, password)
        }
    }

    class TestRepo {
        fun getData(username: String?, password: String?): LiveData<Resource<String>> {
            val liveData = MutableLiveData<Resource<String>>()
            liveData.value = Resource.success("test", Source.DATABASE)
            return liveData
        }
    }
}