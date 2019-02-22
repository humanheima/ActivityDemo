package com.hm.activitydemo.hook;

import java.lang.reflect.Field;

public class FieldUtil {

    public static Object getField(Class clazz, Object target, String name) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    /**
     * @param clazz
     * @param name
     * @return
     * @throws Exception
     */
    public static Field getField(Class clazz, String name) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static void setField(Class clazz, Object target, String name, Object value)
            throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }


}
