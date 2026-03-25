package net.jini.core.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a service to be automatically registered with the Lookup Service.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExportedService {
    /**
     * Optional service ID for the service.
     */
    String id() default "";

    /**
     * Optional instance ID to distinguish multiple instances of the same service.
     */
    String instanceId() default "";

    /**
     * Optional groups for the service to be part of.
     */
    String[] groups() default {};
}
