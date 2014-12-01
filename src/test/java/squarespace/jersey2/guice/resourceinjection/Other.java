package squarespace.jersey2.guice.resourceinjection;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;
import com.google.inject.internal.Annotations;

@BindingAnnotation
@Retention(RUNTIME)
@javax.inject.Qualifier
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface Other {

    /**
     * Workaround to pass Guice check {@link Annotations#isAllDefaultMethods(annotationType)}
     */
    boolean unused() default true;
}
