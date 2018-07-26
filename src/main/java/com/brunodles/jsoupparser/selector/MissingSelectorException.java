package com.brunodles.jsoupparser.selector;

/**
 * This exception is thrown when field is not annotated
 */
public class MissingSelectorException extends RuntimeException {
    public MissingSelectorException(String methodName) {
        super("Failed to get \"" + methodName + "\". Looks like it doesn't have Selector annotation.");
    }
}
