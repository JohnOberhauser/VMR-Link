package com.ober.vmrlink

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

abstract class Link<T> {

    var value = MutableLiveData<Resource<T>>()
    private var mediator: LiveData<Resource<T>>? = null

    fun update() {
        mediator?.let {
            if (it.hasObservers()) {
                return
            }
        }

        mediator = fetch()
        mediator?.observeForever(object : Observer<Resource<T>> {
            override fun onChanged(resource: Resource<T>?) {
                value.value = resource
                extraProcessing()
                if (resource is Success || resource is Error) {
                    mediator?.removeObserver(this)
                }
            }
        })
    }

    protected abstract fun fetch(): LiveData<Resource<T>>

    protected open fun extraProcessing() {}
}