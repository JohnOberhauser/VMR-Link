package com.ober.vmrlink_processor

import com.google.auto.service.AutoService
import com.ober.vmr_annotation.Link
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

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

        val updateFunction = FunSpec.builder("update")
            .addParameters(parameters)
            .addCode(
                """
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

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}