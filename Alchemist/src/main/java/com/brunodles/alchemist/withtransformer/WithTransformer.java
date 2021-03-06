package com.brunodles.alchemist.withtransformer;

import com.brunodles.alchemist.Transmutation;

import java.lang.annotation.*;

/**
 * Use a simple transformer to transform the object.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WithTransformer {

    /**
     * Provide a class of the transformer.
     *
     * @return transformer class
     */
    Class<? extends Transmutation<String, ?>> value();
}
