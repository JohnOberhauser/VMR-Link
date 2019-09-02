# VMR-Link
VMR stand for ViewModel-Repository.  This library creates a link between your view model and your repository layers.

## Purpose 

VMR-Link was created for two reasons:

1: Reduce boiler plate code for ensuring an update function in your view model is not call again while live data is in a loading state.  See the `update` function in the `Link.kt` class file to better understand what I mean.

2: Add a wrapper class `Resource` to help you determine the state of your data at the UI layer.

## To add to project:

Be sure to add the jcenter repository

    repositories {
        jcenter()
    }

Then add the library to your dependencies

    implementation 'com.ober:vmr-link:0.1.5'
    

## Usage

### SimpleLink Example

If your repository layer's function does not require any parameters, you can use `SimpleLink`

Create your Link class

``` kotlin
class EasyLink(private val testRepo: TestRepository) : SimpleLink<String>() {
    override fun fetch(): LiveData<Resource<String>> {
        return testRepo.getData()
    }
}
```
    
Return live data of type `LiveData<Resource<T>>` from your repository

```kotlin
class TestRepository {
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
val easyLink = EasyLink(testRepo)
``` 
    
Observe the link in your UI layer

```kotlin
private fun setupObservalbe() {
    viewModel.easyLink.value.observe(this, Observer { resource ->
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
viewModel.easyLink.update()
```

### Link Example

If your repository's function does require parameters, then you should use `Link`

```kotlin
class ComplexLink(private val testRepo: TestRepository) : Link<String, ParamsExample>() {
    override fun fetch(p: ParamsExample?): LiveData<Resource<String>> {
        return testRepo.getData(p?.foo, p?.bar)
    }
}
```
    
Then, you pass in your parameters when calling update

```kotlin
link.update(ParamsExample("foo", "bar"))
```
  
### Extra Processing

If you need to do extra processing whenever the live data's value is changed, you can override the extraProcessing function

```kotlin
class EasyLink(private val testRepo: TestRepo, private val extraWork: () -> Unit) : SimpleLink<String>() {
    override fun fetch(): LiveData<Resource<String>> {
        return testRepo.getData()
    }

    override fun extraProcessing() {
        extraWork()
    }
}
```