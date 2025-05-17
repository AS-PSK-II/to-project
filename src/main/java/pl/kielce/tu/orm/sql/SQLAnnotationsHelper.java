package pl.kielce.tu.orm.sql;

import pl.kielce.tu.orm.annotations.*;

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
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        return oneToOneAnnotation != null || manyToOneAnnotation != null || manyToManyAnnotation != null;
    }

    public static boolean hasOneToOneAnnotation(Field field) {
        OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);

        return oneToOneAnnotation != null;
    }

    public static boolean hasManyToManyAnnotation(Field field) {
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        return manyToManyAnnotation != null;
    }

    public static boolean hasOneToManyAnnotation(Field field) {
        OneToMany oneToManyAnnotation = field.getAnnotation(OneToMany.class);

        return oneToManyAnnotation != null;
    }
}
