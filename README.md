# VMR-Link
VMR stand for ViewModel-Repository.  This library creates a link between your view model and your repository layers.

## Purpose 

VMR-Link was created for two reasons:

1: Reduce boiler plate code for ensuring an update function in your view model is not call again while live data is in a loading state.

2: Add a wrapper class `Resource` to help you determine the state of your data at the UI layer.

## To add to project:

Be sure to add the jcenter repository

    repositories {
        jcenter()
    }

Then add the library to your dependencies

    implementation 'com.ober:vmr-link:3.0.15'
    implementation 'com.ober:vmr-annotation:3.0.15'
    kapt 'com.ober:vmr-processor:3.0.15'
    

## Usage

### Example
    
Return live data of type `LiveData<Resource<T>>` from your repository and use the @Link annotation

```kotlin
class TestRepository {
    @Link("MyLinkingLiveData")
    fun getData(): LiveData<Resource<String>> {
        val liveData = MutableLiveData<Resource<String>>()
        when {
            loadingConidtion -> liveData.value = Resource.loading(null, Source.NO_DATA)
            successCondition -> liveData.value = Resource.success("test", Source.DATABASE)
            errorCondition -> liveData.value = Resource.error("error x happened", null)
        }
        return liveData
    }
}
```
    
Create an instance of your new Link class in your View Model

```kotlin
val myLinkingLiveData = MyLinkingLiveData(testRepo)
``` 
    
Observe the link in your UI layer

```kotlin
private fun setupObservalbe() {
    viewModel.easyLinkingLiveData.observe(this, Observer { resource ->
        when (resource) {
            is Success -> showData()
            is Error -> showError()
            is Loading -> showLoading()
        }
    })
}
```
    
Call update where you when you want to update the data from your UI

```kotlin
viewModel.myLinkingLiveData.update()
```

If you need to do extra stuff whenever the live data's value is changed in the view model, you can use observeForever

```kotlin
val easyLinkingLiveData = MyLinkingLiveData(testRepo).apply {
    observeForever {
        when (value) {
            is Success -> {
                // do stuff
            }
        }
    }
}
``` 