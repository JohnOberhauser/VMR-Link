package com.ober.vmrlink_processor

import com.google.auto.service.AutoService
import com.ober.vmr_annotation.Link
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(LinkProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedAnnotationTypes("com.ober.vmr_annotation.Link")
class LinkProcessor : AbstractProcessor() {

    override fun process(set: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        roundEnv?.getElementsAnnotatedWith(Link::class.java)?.forEach { methodElement ->

            if (methodElement.kind != ElementKind.METHOD) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Can only be applied to functions,  element: $methodElement"
                )
                return false
            }

            generateNewMethod(
                (methodElement as ExecutableElement),
                processingEnv.elementUtils.getPackageOf(methodElement).toString()
            )
        }
        return false
    }

    private fun generateNewMethod(method: ExecutableElement, packageOfMethod: String) {
        val generatedSourcesRoot: String = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
        if (generatedSourcesRoot.isEmpty()) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can't find the target directory for generated Kotlin files."
            )
            return
        }

        val className = method.getAnnotation(Link::class.java).className

        val file = File(generatedSourcesRoot)
        file.mkdir()

        val transformedParameters = mutableListOf<ParameterSpec>()
        var parameterNamesCommaSeparated = ""
        val parameters = ParameterSpec.parametersOf(method)
        parameters.forEach {
            transformedParameters.add(
                ParameterSpec.builder(it.name, it.type.javaToKotlin())
                    .build()
            )
        }

        var first = true
        transformedParameters.forEach {
            if (!first) parameterNamesCommaSeparated += ", "
            first = false
            parameterNamesCommaSeparated += it.name
        }


        val returnType = method
            .returnType
            .asTypeName()
            .javaToKotlin()

        if (!returnType.toString().contains("LiveData") || !returnType.toString().contains("Resource")) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Methods annotated with @Link must return LiveData<Resource<T>>"
            )
            return
        }

        val resourceType = returnType
            .toString()
            .substringAfter("<")
            .substringBeforeLast(">")

        val updateFunction = FunSpec.builder("update")
            .addParameters(transformedParameters)
            .addCode(
                """
                       mediator?.let {
                           if (it.hasObservers()) {
                               return
                           }
                       }
                       mediator = repository.${method.simpleName}($parameterNamesCommaSeparated).apply {
                           observeForever(object : Observer<$resourceType> {
                               override fun onChanged(resource: $resourceType?) {
                                   this@$className.value = resource
                                   onValueChanged()
                                   if (resource is Success || resource is Error) {
                                       mediator?.removeObserver(this)
                                   }
                               }
                           })
                       }
            """.trimIndent()
            )
            .build()

        val onValueChangedFunction = FunSpec.builder("onValueChanged")
            .addModifiers(KModifier.PROTECTED, KModifier.OPEN)
            .build()

        val mediatorVariable = PropertySpec.builder(
            "mediator",
            returnType.copy(nullable = true)
        )
            .mutable()
            .addModifiers(KModifier.PRIVATE)
            .initializer("null")
            .build()

        val constructor = FunSpec.constructorBuilder()
            .addParameter(
                "repository",
                method.enclosingElement.asType().asTypeName()
            )
            .build()

        val repositoryProperty = PropertySpec.builder(
            "repository",
            method.enclosingElement.asType().asTypeName()
        )
            .initializer("repository")
            .build()

        FileSpec.builder(packageOfMethod, className)
            .addImport("com.ober.vmrlink", "Success")
            .addImport("com.ober.vmrlink", "Error")
            .addImport("androidx.lifecycle", "Observer")
            .addType(
                TypeSpec.classBuilder(className)
                    .primaryConstructor(constructor)
                    .addProperty(repositoryProperty)
                    .addProperty(mediatorVariable)
                    .addModifiers(KModifier.OPEN)
                    .superclass(returnType)
                    .addFunction(updateFunction)
                    .addFunction(onValueChangedFunction)
                    .build()
            )
            .build().writeTo(file)
    }

    private fun TypeName.javaToKotlin(): TypeName {
        return when (this) {
            is ParameterizedTypeName -> {
                ParameterizedTypeNameCreator.buildParameterizedTypeName(
                    rawType.javaToKotlin() as ClassName,
                    typeArguments.map {
                        it.javaToKotlin()
                    }.toList()
                )
            }
            is WildcardTypeName -> {
                val type =
                    if (inTypes.isNotEmpty()) WildcardTypeName.consumerOf(inTypes[0].javaToKotlin())
                    else WildcardTypeName.producerOf(outTypes[0].javaToKotlin())
                type
            }

            else -> {
                val className = JavaToKotlinClassMap.INSTANCE
                    .mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
                if (className == null) {
                    this
                } else {
                    ClassName.bestGuess(className)
                }
            }
        }
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}