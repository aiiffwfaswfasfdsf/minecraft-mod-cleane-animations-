package io.github.aiiffwfaswfasfdsf.cleananimations.core.config;

import java.lang.annotation.*;

/**
 * Marks an enum field, or a {@code String} field with {@link #values()}, as a cycle button.
 * Value labels use the key {@code cleananimations.config.<section>.<field>.<value lowercase>}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Choice {
	/** Allowed values for a {@code String} field. Ignored for enum fields. */
	String[] values() default {};
}
