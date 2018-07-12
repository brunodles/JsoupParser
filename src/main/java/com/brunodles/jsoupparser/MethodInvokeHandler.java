package com.brunodles.jsoupparser;

import com.brunodles.jsoupparser.exceptions.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.select.Elements;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

class MethodInvokeHandler {

    private static final String METHOD_HASHCODE = "hashcode";
    private static final String METHOD_TO_STRING = "toString";
    private static final String METHOD_EQUALS = "equals";

    private final ProxyHandler proxyHandler;
    private final Method method;
    private final String methodName;
    private final Object[] objects;

    MethodInvokeHandler(ProxyHandler proxyHandler, Method method, Object[] objects) {
        this.proxyHandler = proxyHandler;
        this.method = method;
        this.objects = objects;
        this.methodName = method.getName();
    }

    public Object invoke() {
        checkReturnType();

        if (METHOD_HASHCODE.equalsIgnoreCase(methodName))
            return this.hashCode();
        if (METHOD_TO_STRING.equalsIgnoreCase(methodName))
            return "Proxy for \"" + proxyHandler.interfaceClass.getName() + "\".";
        if (METHOD_EQUALS.equalsIgnoreCase(methodName))
            return proxyEquals(objects[0]);

        final CssSelector annotation = getAnnotation();
        final String selector = annotation.selector();

        final Elements elements = getElements(selector);

        final Object elementValue = getElementsValue(elements, annotation);

        return getResult(annotation, elementValue);
    }

    private void checkReturnType() {
        if (method.getReturnType() == Void.TYPE)
            throw new InvalidResultException(methodName);
    }

    private Object proxyEquals(Object other) {
        if (other == null)
            return false;

        try {
            InvocationHandler handler = Proxy.getInvocationHandler(other);
            return proxyHandler.equals(handler);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @NotNull
    private CssSelector getAnnotation() {
        CssSelector cssSelector = method.getAnnotation(CssSelector.class);
        if (cssSelector == null)
            throw new MissingSelectorException(methodName);
        return cssSelector;
    }

    @NotNull
    private Elements getElements(String selector) {
        final Elements elements = proxyHandler.document.select(selector);
        if (elements == null || elements.isEmpty())
            throw new InvalidSelectorException(methodName, selector);
        return elements;
    }

    private Object getElementsValue(Elements elements, CssSelector cssSelector) {
        final Class<? extends ElementCollector> collectorClass = cssSelector.parser();
        final ElementCollector<?> collector = getElementCollector(methodName, collectorClass);

        Class<?> returnType = method.getReturnType();
        if (returnType.isAssignableFrom(Collections.class))
            throw new RuntimeException("We can't handle collections yet.");
        Object elementValue;
        try {
            elementValue = collector.collect(proxyHandler.jsoupParser, elements.first(), method);
        } catch (Exception e) {
            throw new CollectorException(methodName, collectorClass, e);
        }
        return elementValue;
    }

    private ElementCollector<?> getElementCollector(String methodName, Class<? extends ElementCollector> collectorClass) {
        final ElementCollector<?> collector;
        try {
            collector = collectorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InvalidCollectorException(methodName, collectorClass, e);
        }
        return collector;
    }

    @SuppressWarnings("unchecked")
    private Object getResult(CssSelector cssSelector, Object elementValue) {
        final Class<? extends Transformer> transformerClass = cssSelector.transformer();
        final Transformer transformer = getTransformer(methodName, transformerClass);

        final Object result;
        try {
            result = transformer.transform(elementValue);
        } catch (Exception e) {
            throw new TransformerException(methodName, transformerClass, e);
        }
        return result;
    }

    private Transformer getTransformer(String methodName, Class<? extends Transformer> transformerClass) {
        final Transformer transformer;
        try {
            transformer = transformerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InvalidTransformerException(methodName, transformerClass, e);
        }
        return transformer;
    }
}