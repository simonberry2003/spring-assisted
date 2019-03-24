package org.springframework.assisted;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates an injected parameter whose value comes from an argument to a factory method.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Assisted {
}
