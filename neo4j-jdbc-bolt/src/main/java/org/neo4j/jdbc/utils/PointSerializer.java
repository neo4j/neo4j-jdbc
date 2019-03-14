package org.neo4j.jdbc.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.neo4j.driver.v1.types.Point;

import java.io.IOException;
import java.util.Map;

import static org.neo4j.jdbc.utils.DataConverterUtils.pointToMap;

public class PointSerializer extends JsonSerializer<Point> {

    @Override
    public void serialize(Point value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        Map<String, Object> point = pointToMap(value);
        jsonGenerator.writeObject(point);
    }

}
