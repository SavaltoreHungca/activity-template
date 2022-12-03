package com.zxslsoft.general.activiti;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeUtil {

    private static String[] BASIC_TYPES = new String[]{
            Integer.class.getTypeName(), Byte.class.getTypeName(),
            Character.class.getTypeName(), Short.class.getTypeName(),
            Long.class.getTypeName(), Float.class.getTypeName(),
            Double.class.getTypeName(), Boolean.class.getTypeName(),
            String.class.getTypeName(), Integer[].class.getTypeName(),
            Byte[].class.getTypeName(), Character[].class.getTypeName(),
            Short[].class.getTypeName(), Long[].class.getTypeName(),
            Float[].class.getTypeName(), Double[].class.getTypeName(),
            Boolean[].class.getTypeName(), String[].class.getTypeName(),
            int.class.getTypeName(), byte.class.getTypeName(),
            char.class.getTypeName(), short.class.getTypeName(),
            long.class.getTypeName(), float.class.getTypeName(),
            double.class.getTypeName(), boolean.class.getTypeName(),
            int[].class.getTypeName(), byte[].class.getTypeName(),
            char[].class.getTypeName(), short[].class.getTypeName(),
            long[].class.getTypeName(), float[].class.getTypeName(),
            double[].class.getTypeName(), boolean[].class.getTypeName()
    };

    private static Map<String, Class<?>> PRIMITIVE_CLASS = new HashMap<String, Class<?>>() {{
        put("int", int.class);
        put("byte", byte.class);
        put("char", char.class);
        put("short", short.class);
        put("long", long.class);
        put("float", float.class);
        put("double", double.class);
        put("boolean", boolean.class);
        put("int[]", int[].class);
        put("byte[]", byte[].class);
        put("char[]", char[].class);
        put("short[]", short[].class);
        put("long[]", long[].class);
        put("float[]", float[].class);
        put("double[]", double[].class);
        put("boolean[]", boolean[].class);
    }};

    public static boolean isComponentType(Class clazz) {
        for (String type : BASIC_TYPES) {
            if (type.equals(clazz.getTypeName())) {
                return false;
            }
        }
        return true;
    }


    /**
     * According to method signature to get parameters class types,
     * the signature as below:
     * "public void com.pdd.springTest.Persion.say(java.lang.String,java.net.HttpCookie,int[],java.lang.Object[],java.util.List)"
     */
    private Class[] obtainParamTypes(String signature) throws ClassNotFoundException {
        Pattern pattern = Pattern.compile("[(][\\S|\\s]*[)]");
        Matcher matcher = pattern.matcher(signature);
        String paramSignatureStr = null;
        String[] paramSignatures;
        Class[] classes = null;
        String className;

        if (matcher.find()) {
            paramSignatureStr = matcher.group();
            paramSignatureStr = paramSignatureStr.substring(1, paramSignatureStr.length());
            paramSignatureStr = paramSignatureStr.substring(0, paramSignatureStr.length() - 1);
        }
        if (paramSignatureStr == null) return null;
        if (!paramSignatureStr.equals("")) {
            paramSignatures = paramSignatureStr.split(",");
            classes = new Class[paramSignatures.length];
            for (int i = 0; i < paramSignatures.length; i++) {
                className = paramSignatures[i].trim();
                if (className.contains(".")) {
                    if (className.contains("[]")) {
                        className = className.substring(0, className.length() - 2);
                        className = "[L" + className + ";";
                    }
                    classes[i] = Class.forName(className);
                } else if (!className.contains(".")) {
                    classes[i] = PRIMITIVE_CLASS.get(className);
                } else {
                    throw new ClassNotFoundException();
                }
            }
        }
        return classes;
    }
}
