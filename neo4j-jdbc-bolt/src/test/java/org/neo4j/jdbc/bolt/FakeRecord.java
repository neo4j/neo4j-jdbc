/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * <p>
 * Created on 06/09/2021
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class FakeRecord implements Record {

    private final Map<String, Value> values;

    public FakeRecord(LinkedHashMap<String, Value> values) {
        this.values = values;
    }

    @Override
    public List<String> keys() {
        return new ArrayList<>(values.keySet());
    }

    @Override
    public List<Value> values() {
        return new ArrayList<>(values.values());
    }

    @Override
    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    @Override
    public int index(String key) {
        return keys().indexOf(key);
    }

    @Override
    public Value get(String key) {
        return values.get(key);
    }

    @Override
    public Value get(int index) {
        return values().get(index);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public Map<String, Object> asMap() {
        return null;
    }

    @Override
    public <T> Map<String, T> asMap(Function<Value, T> mapper) {
        return null;
    }

    @Override
    public List<Pair<String, Value>> fields() {
        return null;
    }

    @Override
    public Value get(String key, Value defaultValue) {
        return null;
    }

    @Override
    public Object get(String key, Object defaultValue) {
        return null;
    }

    @Override
    public Number get(String key, Number defaultValue) {
        return null;
    }

    @Override
    public Entity get(String key, Entity defaultValue) {
        return null;
    }

    @Override
    public Node get(String key, Node defaultValue) {
        return null;
    }

    @Override
    public Path get(String key, Path defaultValue) {
        return null;
    }

    @Override
    public Relationship get(String key, Relationship defaultValue) {
        return null;
    }

    @Override
    public List<Object> get(String key, List<Object> defaultValue) {
        return null;
    }

    @Override
    public <T> List<T> get(String key, List<T> defaultValue, Function<Value, T> mapFunc) {
        return null;
    }

    @Override
    public Map<String, Object> get(String key, Map<String, Object> defaultValue) {
        return null;
    }

    @Override
    public <T> Map<String, T> get(String key, Map<String, T> defaultValue, Function<Value, T> mapFunc) {
        return null;
    }

    @Override
    public int get(String key, int defaultValue) {
        return 0;
    }

    @Override
    public long get(String key, long defaultValue) {
        return 0;
    }

    @Override
    public boolean get(String key, boolean defaultValue) {
        return false;
    }

    @Override
    public String get(String key, String defaultValue) {
        return null;
    }

    @Override
    public float get(String key, float defaultValue) {
        return 0;
    }

    @Override
    public double get(String key, double defaultValue) {
        return 0;
    }
}
