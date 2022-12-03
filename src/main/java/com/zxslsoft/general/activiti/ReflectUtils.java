package com.zxslsoft.general.activiti;

import com.alibaba.fastjson.JSON;
import com.zxslsoft.general.activiti.Utils;
import org.reflections.Reflections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static com.zxslsoft.general.activiti.Utils.*;

/**
 * 存放和java泛型和反射相关的工具类
 */
@SuppressWarnings("all")
public class ReflectUtils {

    /**
     * 集合类的默认实现映射
     */
    public static final Map<String, Class<?>> TYPE_REFLATION = new HashMap<String, Class<?>>() {{
        put(List.class.getName(), ArrayList.class);
        put(Map.class.getName(), HashMap.class);
        put(Set.class.getName(), HashSet.class);
        put(Collection.class.getName(), ArrayList.class);
    }};

    /**
     * 基本数据类型
     */
    public static final List<Class> ESSENTIAL_TYPE =
            asList(String.class, Integer.class, Long.class);

    /**
     * 基本数组类的类型
     */
    public static final List<Class> ESSENTIAL_ARRAY_TYPE =
            asList(Integer[].class, String[].class, Long[].class, Object[].class);

    /**
     * 获取字段值
     */
    public static Object getValue(Object o, String fieldName) throws NoSuchFieldException {
        try {
            Field field;
            try {
                field = o.getClass().getField(fieldName);
            } catch (NoSuchFieldException e) {
                field = o.getClass().getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            return field.get(o);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("获取对象成员值失败", e);
        }
    }

    public static Object getValue(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            try {
                return field.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("field should be a static field");
        }
    }

    /**
     * 设置字段值
     */
    public static void setValue(Object o, String fieldName, Object value) throws NoSuchFieldException {
        try {
            Field field;
            try {
                field = o.getClass().getField(fieldName);
            } catch (NoSuchFieldException e) {
                field = o.getClass().getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            field.set(o, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("获取对象成员值失败", e);
        }
    }

    /**
     * 存在字段？
     */
    public static boolean hasField(Class clazz, String fieldName) {
        Field field = null;
        try {
            field = clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e1) {
                //pass
            }
        }
        return null != field;
    }

    /**
     * 获取泛型 class
     */
    public static Class getGenericClass(Class clazz, Integer position) {
        return (Class) ((ParameterizedType) clazz.getGenericSuperclass())
                .getActualTypeArguments()[position];
    }

    public static Class<?> getMethodGenericClass(Method method, int paramPosi, int genericClassPosi) {
        try {
            Type[] type = method.getGenericParameterTypes();
            ParameterizedType pType = (ParameterizedType) type[paramPosi];
            return Class.forName(pType.getActualTypeArguments()[genericClassPosi].getTypeName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 深度复制对象的基本成员
     */
    public static <T> T copyObject(T o) {
        try {
            List<Field> fields = getFields(o.getClass(), true);
            T ans = (T) o.getClass().newInstance();
            for (Field i : nullSafe(fields)) {
                i.setAccessible(true);
                i.set(ans, i.get(o));
            }
            return ans;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取所有非静态 Field
     */
    public static List<Field> getFields(Class o, boolean hasParent) {
        List<Field> ans = new ArrayList<>();

        for (Field field : o.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                ans.add(field);
            }
        }

        while (hasParent && null != o.getSuperclass() && !Object.class.equals(o.getSuperclass())) {
            o = o.getSuperclass();
            for (Field field : o.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    ans.add(field);
                }
            }
        }

        return ans;
    }

    /**
     * 获取所有静态 Field
     */
    public static List<Field> getStaticFields(Class o, boolean hasParent) {
        List<Field> ans = new ArrayList<>();

        for (Field field : o.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                ans.add(field);
            }
        }
        while (hasParent && null != o.getSuperclass() && !Object.class.equals(o.getSuperclass())) {
            o = o.getSuperclass();
            for (Field field : o.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    ans.add(field);
                }
            }
        }

        return ans;
    }


    /**
     * 获取字段注解的值
     */
    public static Object getAnotationValueOfField(Class<? extends Annotation> anotationClass, Field field, String methodName) {
        try {
            Annotation annotation = field.getAnnotation(anotationClass);
            if (isEmptyObject(annotation)) {
                return null;
            }
            Method method = annotation.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(annotation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取类注解的值
     */
    public static Object getAnnotationValueOfClass(Class<? extends Annotation> anotationClass, Class clazz, String methodName) {
        try {
            Annotation annotation = clazz.getAnnotation(anotationClass);
            if (isEmptyObject(annotation)) {
                return null;
            }
            Method method = annotation.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(annotation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getAnnotationValue(Annotation annotation, String methodName) {
        try {
            Method method = annotation.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return (T) method.invoke(annotation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取Field
     */
    public static Field getField(Class clazz, String fieldName) {
        Field field;
        try {
            field = clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e1) {
                throw new RuntimeException(e1);
            }
        }
        return field;
    }

    /**
     * 获取方法的注解
     */
    public static List<Method> getMethodsAnnotatedWith(Class clazz, Class<? extends Annotation> annotationClass) {
        List<Method> ans = new ArrayList<>();
        for (Method i : asList(clazz.getDeclaredMethods())) {
            if (i.isAnnotationPresent(annotationClass)) {
                ans.add(i);
            }
        }
        return ans;
    }

    /**
     * 获取字段的注解
     */
    public static List<Field> getFieldsAnnotatedWith(Class clazz, Class<? extends Annotation> annotationClass) {
        List<Field> ans = new ArrayList<>();
        for (Field i : getFields(clazz, false)) {
            if (i.isAnnotationPresent(annotationClass)) {
                ans.add(i);
            }
        }
        return ans;
    }

    /**
     * 新建对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> clazz, Object... args) {
        try {
            if (null != TYPE_REFLATION.get(clazz.getName())) {
                clazz = TYPE_REFLATION.get(clazz.getName());
            }

            Constructor constructor;
            if (isEmpty(args)) {
                try {
                    constructor = clazz.getConstructor();
                } catch (NoSuchMethodException e) {
                    constructor = clazz.getDeclaredConstructor();
                }
                try {
                    return (T) constructor.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                Class<?> argsTyp[] = new Class<?>[args.length];

                int index = 0;
                for (Object i : args) {
                    argsTyp[index] = i.getClass();
                    ++index;
                }

                try {
                    constructor = clazz.getConstructor(argsTyp);
                } catch (NoSuchMethodException e) {
                    constructor = clazz.getConstructor(argsTyp);
                }

                try {
                    return (T) constructor.newInstance(args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getFieldNames(Class clazz) {
        List<Field> fields = nullSafe(getFields(clazz, true));
        List<String> ans = Utils.asList();
        for (Field i : fields) {
            ans.add(i.getName());
        }
        return ans;
    }

    public static String classToJson(Class clazz) {
        List<Field> fields = nullSafe(getFields(clazz, true));
        Map<String, String> ans = Utils.asMap();
        for (Field i : fields) {
            ans.put(i.getName(), i.getType().getSimpleName());
        }
        return JSON.toJSONString(ans);
    }

    public static boolean filedIsNotNull(Object object, String... fileds) {
        if (isEmptyObject(fileds)) return true;
        try {
            for (String filed : fileds) {
                if (hasField(object.getClass(), filed)) {
                    if (null == getValue(object, filed)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T clone(T obj) {
        try {
            T newInstance = newInstance(obj.getClass());
            for (Field field : getFields(obj.getClass(), true)) {
                field.setAccessible(true);
                field.set(newInstance, field.get(obj));
            }
            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取指定 package 下的 相关注释的类
     */
    public static Set<Class<?>> getTypesAnnotatedWith(String basePackage, Class<? extends Annotation> annotationClass) {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getTypesAnnotatedWith(annotationClass);
    }

    /**
     * 获取某个 package 下的 继承指定类的类
     */
    public static <E> Set<Class<? extends E>> getTypesWith(String basePackage, Class<E> parentClass) {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getSubTypesOf(parentClass);
    }

    public static byte[] serializeObject(Object obj) {
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        ) {
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserializeObject(byte[] bytes) {
        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        ) {
            return (T) objectInputStream.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
