package com.brunodles.jsoupparser.annotations;

import com.brunodles.jsoupparser.Transformer;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeTransformer {
    Class<? extends Transformer> value();
}
