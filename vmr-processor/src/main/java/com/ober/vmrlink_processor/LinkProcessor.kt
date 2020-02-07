package com.ober.vmrlink_processor

import com.google.auto.service.AutoService
import com.ober.vmr_annotation.Link
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(LinkProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedAnnotationTypes("com.ober.vmr_annotation.Link")
@UseExperimental(KotlinPoetMetadataPreview::class)
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

            (methodElement as ExecutableElement).parameters.forEach { variableElement ->
                generateNewMethod(
                    methodElement,
                    variableElement,
                    processingEnv.elementUtils.getPackageOf(methodElement).toString()
                )
            }
        }
        return false
    }

    private fun generateNewMethod(method: ExecutableElement, variable: VariableElement?, packageOfMethod: String) {
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

        val parameters = ParameterSpec.parametersOf(method)
//        method.parameters[0].asType().asTypeName().javaClass.toImmutableKmClass()
//        String::class.toImmutableKmClass()
//        method.enclosingElement

        var parameterNamesCommaSeparated = ""
        var first = true
        parameters.forEach {
            if (!first) parameterNamesCommaSeparated += ", "
            first = false
            parameterNamesCommaSeparated += it.name
        }
        val resourceType = method
            .returnType
            .asTypeName()
            .toString()
            .substringAfterLast(".")
            .substringBefore(">")

        //${method.parameters[0].javaClass.toImmutableKmClass().typeParameters[0].name}
        val updateFunction = FunSpec.builder("update")
            .addParameters(parameters)
            .addCode(
                """
                    ${method.parameters[0]}
                    ${Taco::class.toImmutableKmClass().name}
                       mediator?.let {
                           if (it.hasObservers()) {
                               return
                           }
                       }
                       mediator = repository.${method.simpleName}($parameterNamesCommaSeparated).apply {
                           observeForever(object : Observer<Resource<$resourceType>> {
                               override fun onChanged(resource: Resource<$resourceType>?) {
                                   this@$className.value = resource
                                   extraProcessing()
                                   if (resource is Success || resource is Error) {
                                       mediator?.removeObserver(this)
                                   }
                               }
                           })
                       }
            """.trimIndent()
            )
            .build()

        val fetchFunction = FunSpec.builder("fetch")
            .addParameters(parameters)
            .returns(method.returnType.asTypeName())
            .addModifiers(KModifier.ABSTRACT, KModifier.PROTECTED)
            .build()

        val extraProcessingFunction = FunSpec.builder("extraProcessing")
            .addModifiers(KModifier.PROTECTED, KModifier.OPEN)
            .build()

        val mediatorVariable = PropertySpec.builder(
            "mediator",
            method.returnType.asTypeName().copy(nullable = true)
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
            .addImport("androidx.lifecycle", "Observer")
            .addType(
                TypeSpec.classBuilder(className)
                    .primaryConstructor(constructor)
                    .addProperty(repositoryProperty)
                    .addProperty(mediatorVariable)
                    .addModifiers(KModifier.ABSTRACT)
                    .superclass(method.returnType.asTypeName())
                    .addFunction(updateFunction)
                    .addFunction(fetchFunction)
                    .addFunction(extraProcessingFunction)
                    .build()
            )
            .build().writeTo(file)
    }

    fun ParameterSpec.Companion.build(method: ExecutableElement, types: Types): ParameterSpec {
        val enclosingClass = generateSequence<Element>(method) {
            it.enclosingElement
        }.first { it is TypeElement } as TypeElement

        val jvmSignature = method
    }
//    fun getParameter(element: Element): ParameterSpec {
//        val name = element.simpleName.toString()
//        val type = element.asType().asTypeName().javaToKotlin()
//        return ParameterSpec.builder(name, type)
//            .jvmModifiers(element.modifiers)
//            .build()
//    }

//    private fun TypeName.javaToKotlin(): TypeName {
//        return when (this) {
//            is ParameterizedTypeName -> {
//                (rawType.javaToKotlin() as ClassName).parameterizedBy(
//                    *typeArguments.map {
//                        it.javaToKotlin()
//                    }.toTypedArray()
//                )
//            }
//            is WildcardTypeName -> {
//                val type =
//                    if (inTypes.isNotEmpty()) WildcardTypeName.consumerOf(inTypes[0].javaToKotlin())
//                    else WildcardTypeName.producerOf(outTypes[0].javaToKotlin())
//                type
//            }
//
//            else -> {
//                val className = JavaToKotlinClassMap.INSTANCE
//                    .mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
//                if (className == null) {
//                    this
//                } else {
//                    ClassName.bestGuess(className)
//                }
//            }
//        }
//    }
//
//    private fun Element.javaToKotlinType(): ClassName? {
//        val className = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(this.asType().asTypeName().toString()))?.asSingleFqName()?.asString()
//        return if (className == null) {
//            null
//        } else {
//            ClassName.bestGuess(className)
//        }
//    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}