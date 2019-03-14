package org.neo4j.jdbc.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.neo4j.driver.v1.types.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.neo4j.jdbc.utils.DataConverterUtils.nodeToMap;
import static org.neo4j.jdbc.utils.DataConverterUtils.relationshipToMap;

public class PathSerializer extends JsonSerializer<Path> {
    @Override
    public void serialize(Path value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        List<Map<String, Object>> list = toPath(value);
        jsonGenerator.writeObject(list);
    }

    public static List<Map<String, Object>> toPath(Path value) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(nodeToMap(value.start()));
        for (Path.Segment s : value) {
            list.add(relationshipToMap(s.relationship()));
            list.add(nodeToMap(s.end()));
        }
        return list;
    }
}
