package org.neo4j.jdbc.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.neo4j.driver.v1.types.Relationship;

import java.io.IOException;
import java.util.Map;

import static org.neo4j.jdbc.utils.DataConverterUtils.relationshipToMap;

public class RelationshipSerializer extends JsonSerializer<Relationship> {
    @Override
    public void serialize(Relationship value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        Map<String, Object> relMap = relationshipToMap(value);
        jsonGenerator.writeObject(relMap);
    }
}
