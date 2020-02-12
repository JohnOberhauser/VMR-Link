package com.ober.vmr_sample

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ober.vmr_annotation.Link
import com.ober.vmrlink.Resource

class TestRepository {

    val testLiveData = TestLiveData(TestRepository())

    @Link("TestLiveData")
    fun testFunction(
        optionalString: String?,
        string: String,
        complexType: Map<String?, String>,
        defaultValueString: String = "test"
    ): LiveData<Resource<String>> {
        return MutableLiveData<Resource<String>>()
    }
}