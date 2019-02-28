package org.neo4j.jdbc.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

import java.io.IOException;

public class Neo4jJdbcPrettyPrinter extends MinimalPrettyPrinter {
    private static final char COMMA = ',';
    private static final char WHITE_SPACE = ' ';

    private void writeSeparator(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeRaw(COMMA);
        jsonGenerator.writeRaw(WHITE_SPACE);
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator jsonGenerator) throws IOException {
        writeSeparator(jsonGenerator);
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator jsonGenerator) throws IOException {
        writeSeparator(jsonGenerator);
    }
}
