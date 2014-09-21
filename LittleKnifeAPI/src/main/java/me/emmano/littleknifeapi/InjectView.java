package me.emmano.littleknifeapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
// This Class defines our custom Annotation.
// @Retention determines the life span of the Annotation.
// In this case we do not want to keep it around after compile time.
// @Target determines where we can use our Annotation. In this case @InjectViews can only be used on fields (member variables)

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface InjectView {

    int value();
}
