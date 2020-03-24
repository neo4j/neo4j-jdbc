/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 18/12/17
 */
package org.neo4j.jdbc.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gianmarco Laggia @ Larus B.A.
 * @since 3.2.0
 */
public class ObjectConverter {

    private static final Map<String, Method> CONVERTERS = new HashMap<>();

    static {
        // Preload converters.
        Method[] methods = ObjectConverter.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 1) {
                if (method.getParameterTypes()[0] == Object.class) {
                    //generic method
                    CONVERTERS.put("any_" + method.getReturnType().getName(), method);
                } else {
                    // Converter should accept 1 argument. This skips the convert() method.
                    CONVERTERS.put(method.getParameterTypes()[0].getName() + "_"
                            + method.getReturnType().getName(), method);
                }
            }
        }
    }

    private ObjectConverter() {
    }

    // Action -------------------------------------------------------------------------------------

    /**
     * Convert the given object value to the given class.
     *
     * @param <T> the destination type of the conversion
     * @param from The object value to be converted.
     * @param to   The type class which the given object should be converted to.
     * @return The converted object value.
     * @throws NullPointerException          If 'to' is null.
     * @throws UnsupportedOperationException If no suitable converter can be found.
     * @throws RuntimeException              If conversion failed somehow. This can be caused by at least
     *                                       an ExceptionInInitializerError, IllegalAccessException or InvocationTargetException.
     */
    public static <T> T convert(Object from, Class<T> to) {

        // Null is just null.
        if (from == null) {
            return null;
        }

        // Can we cast? Then just do it.
        if (to.isAssignableFrom(from.getClass())) {
            return to.cast(from);
        }

        // Lookup the suitable converter.
        String converterId = from.getClass().getName() + "_" + to.getCanonicalName();
        String genericConverterId = "any_" + to.getCanonicalName();
        Method converter = CONVERTERS.get(converterId);
        if (converter == null) {
            converter = CONVERTERS.get(genericConverterId);
        }
        if (converter == null) {
            throw new UnsupportedOperationException("Cannot convert from "
                    + from.getClass().getName() + " to " + to.getCanonicalName()
                    + ". Requested converter does not exist.");
        }

        // Convert the value.
        try {
            return to.cast(converter.invoke(to, from));
        } catch (Exception e) {
            throw new ClassCastException("Cannot convert from "
                    + from.getClass().getName() + " to " + to.getName()
                    + ". Conversion failed with " + e.getMessage());
        }
    }

    // Converters ---------------------------------------------------------------------------------
    /**
     * Cast an object to a map
     * @param value the object to convert
     * @return the converted Map
     */
    public static Map anyToMap(Object value) {
        return (Map) value;
    }

    /**
     * Converts Integer to Boolean. If integer value is 0, then return FALSE, else return TRUE.
     *
     * @param value The Integer to be converted.
     * @return The converted Boolean value.
     */
    public static Boolean integerToBoolean(Integer value) {
        return value == 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    /**
     * Converts Boolean to Integer. If boolean value is TRUE, then return 1, else return 0.
     *
     * @param value The Boolean to be converted.
     * @return The converted Integer value.
     */
    public static Integer booleanToInteger(Boolean value) {
        return value ? 1 : 0;
    }

    /**
     * Converts Boolean to Short. If boolean value is TRUE, then return 1, else return 0.
     *
     * @param value The Boolean to be converted.
     * @return The converted Short value.
     */
    public static Short booleanToShort(Boolean value) {
        return value ? (short) 1 : (short) 0;
    }

    /**
     * Converts Boolean to Double. If boolean value is TRUE, then return 1, else return 0.
     *
     * @param value The Boolean to be converted.
     * @return The converted Double value.
     */
    public static Double booleanToDouble(Boolean value) {
        return value ? 1D : 0D;
    }

    /**
     * Converts Boolean to Float. If boolean value is TRUE, then return 1, else return 0.
     *
     * @param value The Boolean to be converted.
     * @return The converted Float value.
     */
    public static Float booleanToFloat(Boolean value) {
        return value ? 1F : 0F;
    }

    /**
     * Converts Boolean to Long. If boolean value is TRUE, then return 1, else return 0.
     *
     * @param value The Boolean to be converted.
     * @return The converted Long value.
     */
    public static Long booleanToLong(Boolean value) {
        return value ? 1L : 0L;
    }

    /**
     * Converts Integer to String.
     *
     * @param value The Integer to be converted.
     * @return The converted String value.
     */
    public static String integerToString(Integer value) {
        return value.toString();
    }

    /**
     * Converts Long to String.
     *
     * @param value The Long to be converted.
     * @return The converted String value.
     */
    public static String longToString(Long value) {
        return value.toString();
    }

    /**
     * Converts String to Integer.
     *
     * @param value The String to be converted.
     * @return The converted Integer value.
     */
    public static Integer stringToInteger(String value) {
        return Integer.valueOf(value);
    }

    /**
     * Converts String to Long.
     *
     * @param value The String to be converted.
     * @return The converted Long value.
     */
    public static Long stringToLong(String value) {
        return Long.valueOf(value);
    }

    /**
     * Converts Long to Float.
     *
     * @param value The Long to be converted.
     * @return The converted Float value.
     */
    public static Float longToFloat(Long value) {
        return value.floatValue();
    }

    /**
     * Converts Long to Short.
     *
     * @param value The Long to be converted.
     * @return The converted Short value.
     */
    public static Short longToShort(Long value) {
        return value.shortValue();
    }

    /**
     * Converts Long to Integer.
     *
     * @param value The Long to be converted.
     * @return The converted Integer value.
     */
    public static Integer longToInteger(Long value) {
        return value.intValue();
    }

    /**
     * Converts String to Float.
     *
     * @param value The String to be converted.
     * @return The converted Float value.
     */
    public static Float stringToFloat(String value) {
        return Float.valueOf(value);
    }

    /**
     * Converts Boolean to String.
     *
     * @param value The Boolean to be converted.
     * @return The converted String value.
     */
    public static String booleanToString(Boolean value) {
        return value.toString();
    }

    /**
     * Converts String to Boolean.
     *
     * @param value The String to be converted.
     * @return The converted Boolean value.
     */
    public static Boolean stringToBoolean(String value) {
        return Boolean.valueOf(value);
    }

    /**
     * Converts Long to Double.
     *
     * @param value The Long to be converted.
     * @return The converted Double value.
     */
    public static Double longToDouble(Long value) {
        return value.doubleValue();
    }

    /**
     * Converts String to Double.
     *
     * @param value The String to be converted.
     * @return The converted Double value.
     */
    public static Double stringToDouble(String value) {
        return Double.valueOf(value);
    }

    /**
     * Converts String to Short.
     *
     * @param value The String to be converted.
     * @return The converted Short value.
     */
    public static Short stringToShort(String value) {
        return Short.valueOf(value);
    }

}
