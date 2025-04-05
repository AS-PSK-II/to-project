package pl.kielce.tu.orm.annotations.processors;

import pl.kielce.tu.orm.annotations.Id;
import pl.kielce.tu.orm.annotations.ManyToOne;
import pl.kielce.tu.orm.annotations.OneToOne;

import java.lang.reflect.Field;

public class SQLAnnotationsHelper {

    private SQLAnnotationsHelper() {}

    public static boolean hasIdAnnotation(Field field) {
        Id idAnnotation = field.getAnnotation(Id.class);

        return idAnnotation != null;
    }

    public static boolean hasForeignTableAnnotation(Field field) {
        OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);
        ManyToOne manyToOneAnnotation = field.getAnnotation(ManyToOne.class);

        return oneToOneAnnotation != null || manyToOneAnnotation != null;
    }

    public static boolean hasOneToOneAnnotation(Field field) {
        OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);

        return oneToOneAnnotation != null;
    }
}
