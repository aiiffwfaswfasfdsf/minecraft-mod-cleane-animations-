package io.github.aiiffwfaswfasfdsf.cleananimations.core.config;

import java.lang.annotation.*;

/** Marks a {@code float}/{@code double}/{@code int} config field as a slider in the config screen. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Slider {
	double min();

	double max();

	/** Snap step; {@code 0} means continuous. */
	double step() default 0.0;
}
