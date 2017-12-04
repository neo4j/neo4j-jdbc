/*
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
 * Created on 25/03/16
 */
package org.neo4j.jdbc.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains static methods used to process a raw statement and create a valid string to be used as preparedStatement in neo4j
 *
 * @author AgileLARUS
 * @since 3.0.0
 */
public class PreparedStatementBuilder {

	private PreparedStatementBuilder() {}

	/**
	 * This method return a String that is the original raw string with all valid placeholders replaced with neo4j curly brackets notation for parameters.
	 * <br>
	 * i.e. MATCH n RETURN n WHERE n.name = ? is transformed in MATCH n RETURN n WHERE n.name = {1}
	 *
	 * @param raw The string to be translated.
	 * @return The string with the placeholders replaced.
	 */
	public static String replacePlaceholders(String raw) {
		int index = 1;
		String digested = raw;

		String regex = "\\?(?=[^\"]*(?:\"[^\"]*\"[^\"]*)*$)";
		Matcher matcher = Pattern.compile(regex).matcher(digested);

		while (matcher.find()) {
			digested = digested.replaceFirst(regex, "{" + index + "}");
			index++;
		}

		return digested;
	}

	/**
	 * Given a string (statement) it counts all valid placeholders
	 *
	 * @param raw The string of the statement
	 * @return The number of all valid placeholders
	 */
	public static int namedParameterCount(String raw) {
		int max = 0;
		String regex = "\\{\\s*`?\\s*(\\d+)\\s*`?\\s*\\}(?=[^\"]*(\"[^\"]*\"[^\"]*)*$)";
		Matcher matcher = Pattern.compile(regex).matcher(raw);
		while (matcher.find()) {
			max = Math.max(Integer.parseInt(matcher.group(1)),max);
		}
		return max;
	}

}
