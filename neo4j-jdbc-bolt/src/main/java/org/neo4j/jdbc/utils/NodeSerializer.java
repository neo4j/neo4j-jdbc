package org.neo4j.jdbc.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.neo4j.driver.v1.types.Node;

import java.io.IOException;
import java.util.Map;

import static org.neo4j.jdbc.utils.DataConverterUtils.nodeToMap;

public class NodeSerializer extends JsonSerializer<Node> {
    @Override
    public void serialize(Node value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        Map<String, Object> nodeMap = nodeToMap(value);
        jsonGenerator.writeObject(nodeMap);
    }
}