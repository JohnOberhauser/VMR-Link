package com.ober.vmrlink

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class LinkTest {

    private val testRepo = TestRepo()
    private val testErrorRepo = TestErrorRepo()
    private var countDownLatch = CountDownLatch(2)

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Test
    fun testLink() {
        val link = LoginLink(testRepo, this::extraWork)

        link.observeForever {
            assert(it.data == "test")
            countDownLatch.countDown()
        }
        link.update(LoginLink.Credentials("test", "test"))

        countDownLatch.await()
    }

    @Test
    fun testSimpleLink() {
        countDownLatch = CountDownLatch(1)
        val link = EasyLink(testRepo)

        link.observeForever {
            assert(it.data == "test")
            assert((it as Success).source == Source.UNSPECIFIED)
            countDownLatch.countDown()
        }

        link.update()

        countDownLatch.await()
    }

    @Test
    fun testSimpleLinkError() {
        countDownLatch = CountDownLatch(1)
        val link = EasyErrorLink(testErrorRepo)

        link.observeForever {
            assert(it.data == "test")
            assert(it is Error)
            assert((it as Error).throwable?.message == "message")
            countDownLatch.countDown()
        }

        link.update()

        countDownLatch.await()
    }

    private fun extraWork() {
        countDownLatch.countDown()
    }

    class EasyLink(private val testRepo: TestRepo) : SimpleLink<String>() {
        override fun fetch(): LiveData<Resource<String>> {
            return testRepo.getData("test", "test")
        }
    }

    class EasyErrorLink(private val testRepo: TestErrorRepo) : SimpleLink<String>() {
        override fun fetch(): LiveData<Resource<String>> {
            return testRepo.getData("test", "test")
        }
    }

    class LoginLink(private val testRepo: TestRepo, private val extraWork: () -> Unit) : Link<String, LoginLink.Credentials>() {

        override fun fetch(p: Credentials?): LiveData<Resource<String>> {
            return testRepo.getData(p?.userName, p?.password)
        }

        override fun extraProcessing() {
            extraWork()
        }

        class Credentials(var userName: String?, var password: String?)
    }

    class TestRepo {
        fun getData(username: String?, password: String?): LiveData<Resource<String>> {
            val liveData = MutableLiveData<Resource<String>>()
            liveData.value = Resource.success("test")
            return liveData
        }
    }

    class TestErrorRepo {
        fun getData(username: String?, password: String?): LiveData<Resource<String>> {
            val liveData = MutableLiveData<Resource<String>>()
            liveData.value = Resource.error("test", Throwable("message"))
            return liveData
        }

        fun getData2(username: String?, password: String?): LiveData<Resource<String>> {
            val liveData = MutableLiveData<Resource<String>>()
            liveData.value = Resource.error()
            return liveData
        }
    }
}