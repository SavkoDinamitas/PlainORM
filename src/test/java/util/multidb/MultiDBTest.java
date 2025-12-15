package util.multidb;

import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ClassTemplate
@ExtendWith(MultiDBExtension.class)
public @interface MultiDBTest {
}
