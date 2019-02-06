/*
 * Copyright (c) 2019 LARUS Business Automation [http://www.larus-ba.it]
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
 */
package org.neo4j.jdbc.utils;

import java.util.regex.Pattern;

/**
 * This class contains static methods used to sanitize data
 *
 * @author AgileLARUS
 * @since 3.3.1
 */
public class SanitizeUtils {

    private SanitizeUtils() {}

    private static final String BACKTICK = "`";

    /**
     * This method return a String that is the sanitized version of the string.
     * <br>
     * i.e. input: `property name` => output => property name
     *
     * @param raw The string to be sanitized.
     * @return The sanitized string.
     */
    public static String sanitizePropertyName(String property) {
        return property.replaceAll(BACKTICK, "");
    }

    /**
     * This method return a quoted String with backtick (<b>`</b>) if the input is not a valid java identifier, otherwise
     * returns the string itself
     * <br>
     * i.e. input: property name => output => `property name`
     *
     * @param raw The string to be quoted.
     * @return The quoted string.
     */
    public static String quote(String value) {
        return isValidJavaIdentifier(value) ? value : BACKTICK + value + BACKTICK;
    }

    private static boolean isValidJavaIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty() || !Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }
        for (int i = 1; i < identifier.length(); i++) {
            if (!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
