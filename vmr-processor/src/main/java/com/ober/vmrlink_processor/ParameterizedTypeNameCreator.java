package com.ober.vmrlink_processor;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.ParameterizedTypeName;
import com.squareup.kotlinpoet.TypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ParameterizedTypeNameCreator {

    public static ParameterizedTypeName buildParameterizedTypeName(ClassName rawType, List<TypeName> typeNames) {
        return new ParameterizedTypeName(
                null,
                rawType,
                typeNames,
                false,
                new ArrayList<>(),
                new HashMap<>()
        );
    }
}
