package com.brunodles.jsoupparser.smallanotation;

import com.brunodles.jsoupparser.MethodInvocation;
import com.brunodles.jsoupparser.MethodInvocationHandler;
import com.brunodles.jsoupparser.Transformer;
import com.brunodles.jsoupparser.exceptions.InvalidResultException;
import com.brunodles.jsoupparser.exceptions.MissingSelectorException;
import com.brunodles.jsoupparser.exceptions.ResultException;
import com.brunodles.jsoupparser.smallanotation.annotations.Mapping;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SmallAnnotationInvocationHandler implements MethodInvocationHandler {

    public final Map<Class<? extends Annotation>, Class<? extends Transformer>> transformerMap;

    public SmallAnnotationInvocationHandler() {
        this(new TransformersBuilder());
    }

    public SmallAnnotationInvocationHandler(@NotNull TransformersBuilder transformersBuilder) {
        this.transformerMap = transformersBuilder.build();
    }

    @Override
    public Object invoke(MethodInvocation invocation) {
        Annotation[] annotations = getAnnotations(invocation);
        if (annotations.length == 0)
            throw new MissingSelectorException(invocation.methodName);
        if (invocation.getMethodRealReturnType() == Void.TYPE)
            throw new InvalidResultException(invocation.methodName);
        List result = null;
        for (Annotation annotation : annotations) {
            Class<? extends Transformer> transformerClass = null;
            for (Map.Entry<Class<? extends Annotation>, Class<? extends Transformer>> entry : transformerMap.entrySet()) {
                if (entry.getKey().isInstance(annotation)) {
                    transformerClass = entry.getValue();
                    break;
                }
            }
            if (transformerClass != null) {
                try {
                    Transformer transformer = transformerClass.newInstance();
                    if (shouldUseWrapper(transformerClass))
                        transformer = new WrapperTransformer(transformer);
                    result = (List) transformer.transform(new AnnotationInvocation(invocation, annotation, result));
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return getResult(invocation, result);
    }

    private boolean shouldUseWrapper(Class<? extends Transformer> transformerClass) {
        try {
            Type[] genericInterfaces = transformerClass.getGenericInterfaces(); // List of interfaces for our transformer, expected: Transformer
            ParameterizedType type = (ParameterizedType) genericInterfaces[0]; // first generics should be: AnnotationInvocation
            Type[] actualTypeArguments = type.getActualTypeArguments(); // List of Generics of AnnotationInvocation
            Class<?> inputClass = (Class<?>) actualTypeArguments[1]; // second generics is the input
            return !Collection.class.isAssignableFrom(inputClass); // is it a Collection?
        } catch (Exception e) {
            return false;
        }
    }

    private Object getResult(MethodInvocation invocation, List result) {
        if (invocation.isMethodReturnTypeCollection()) {
            try {
                Collection collectionResult = (Collection) invocation.getMethodRawReturnType().newInstance();
                for (Object object : result)
                    collectionResult.add(object);
                return collectionResult;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ResultException(invocation.methodName, invocation.getMethodRawReturnType().getSimpleName(), e);
            }
        }
        if (result.size() > 0)
            return result.get(0);
        return null;
    }

    private Annotation[] getAnnotations(MethodInvocation invocation) {
        Mapping mappingAnnotation = invocation.getMethodAnnotation(Mapping.class);
        Annotation[] annotations;
        if (mappingAnnotation == null) {
            annotations = invocation.getMethodAnnotations();
        } else {
            String[] strings = mappingAnnotation.value();
            ArrayList<Annotation> annotationList = new ArrayList<>();
            for (String string : strings) {
                final String annotationName;
                final String annotationValue;
                if (string.contains("(")) {
                    int openBrackets = string.indexOf("(");
                    int closeBrackets = string.lastIndexOf(")");
                    annotationName = string.substring(0, openBrackets);
                    annotationValue = string.substring(openBrackets + 1, closeBrackets);
                } else {
                    annotationName = string;
                    annotationValue = null;
                }
                Class<? extends Annotation> annotationClass;
                try {
                    annotationClass = (Class<? extends Annotation>) Class.forName("com.brunodles.jsoupparser.smallanotation.annotations." + annotationName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                Annotation annotation = (Annotation) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{annotationClass}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        return annotationValue;
                    }
                });
                annotationList.add(annotation);
            }
            annotations = annotationList.toArray(new Annotation[annotationList.size()]);
        }
        return annotations;
    }
}
