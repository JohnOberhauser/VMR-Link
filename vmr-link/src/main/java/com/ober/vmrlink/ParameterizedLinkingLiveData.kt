package com.ober.vmrlink

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

abstract class ParameterizedLinkingLiveData<T, in P> : LiveData<Resource<T>>() {

    private var mediator: LiveData<Resource<T>>? = null

    fun update(params: P? = null) {
        mediator?.let {
            if (it.hasObservers()) {
                return
            }
        }

        mediator = fetch(params).apply {
            observeForever(object : Observer<Resource<T>> {
                override fun onChanged(resource: Resource<T>?) {
                    this@ParameterizedLinkingLiveData.value = resource
                    onValueChanged()
                    if (resource is Success || resource is Error) {
                        mediator?.removeObserver(this)
                    }
                }
            })
        }
    }

    protected abstract fun fetch(p: P? = null): LiveData<Resource<T>>

    protected open fun onValueChanged() {}
}

abstract class LinkingLiveData<T> : ParameterizedLinkingLiveData<T, Unit>() {
    override fun fetch(p: Unit?): LiveData<Resource<T>> {
        return fetch()
    }

    protected abstract fun fetch(): LiveData<Resource<T>>
}