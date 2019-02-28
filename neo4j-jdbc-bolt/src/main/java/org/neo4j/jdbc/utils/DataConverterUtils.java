package org.neo4j.jdbc.utils;

import org.neo4j.driver.internal.InternalIsoDuration;
import org.neo4j.driver.internal.value.*;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Point;
import org.neo4j.driver.v1.types.Relationship;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

public class DataConverterUtils {

    /**
     * Convert Value to sql.Time
     * @param value
     * @return
     */
    public static Time valueToTime(Value value){
        return valueToTime(value, Calendar.getInstance());
    }

    /**
     * Convert Value to sql.Time with timezone
     * @param value
     * @return
     */
    public static Time valueToTime(Value value, Calendar cal){
        if (value.isNull()){
            return null;
        }

        if (value instanceof TimeValue){
            ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(cal.get(Calendar.ZONE_OFFSET) / 1000);
            TimeValue timeValue = (TimeValue) value;
            OffsetTime offsetTime = timeValue.asOffsetTime().withOffsetSameInstant(zoneOffset);
            return offsetTimeToTime(offsetTime);
        }

        if (value instanceof LocalTimeValue){
            return localTimeToTime(((LocalTimeValue)value).asLocalTime());
        }

        return null;
    }

    /**
     * Convert LocalTime to sql.Time
     * @param localTime
     * @return
     */
    public static Time localTimeToTime(LocalTime localTime) {
        Time time = Time.valueOf(localTime);
        time.setTime(time.getTime()+localTime.getNano() / 1000_000L);
        return time;
    }

    /**
     * Convert OffsetTime to sql.Time
     * @param offsetTime
     * @return
     */
    public static Time offsetTimeToTime(OffsetTime offsetTime) {
        return localTimeToTime(offsetTime.toLocalTime());
    }


    /**
     * Convert Value to sql.Date
     * @param value
     * @return
     */
    public static Date valueToDate(Value value){
        if (value.isNull()){
            return null;
        }

        if (value instanceof DateValue){
            return localDateToDate(((DateValue)value).asLocalDate());
        }

        return null;
    }

    /**
     * Convert a LocalDate to sql.Date
     * @param localDate
     * @return
     */
    public static Date localDateToDate(LocalDate localDate) {
        return Date.valueOf(localDate);
    }

    /**
     * Convert Value to Timestamp with system timezone
     * @param value
     * @return
     */
    public static Timestamp valueToTimestamp(Value value){
        return valueToTimestamp(value, ZoneId.systemDefault() );
    }

    /**
     * Convert Value to Timestamp with timezone
     * @param value
     * @return
     */
    public static Timestamp valueToTimestamp(Value value, ZoneId zone) {
        if (value.isNull()){
            return null;
        }

        if (value instanceof DateTimeValue){
            return zonedDateTimeToTimestamp(value.asZonedDateTime().withZoneSameInstant(zone));
        }

        if (value instanceof LocalDateTimeValue){
            return localDateTimeToTimestamp(value.asLocalDateTime());
        }

        return null;
    }

    /**
     * Convert a LocalDataTime to sql.Timestamp
     * @param ldt
     * @return
     */
    public static Timestamp localDateTimeToTimestamp(LocalDateTime ldt) {
        return Timestamp.valueOf(ldt);
    }

    /**
     * Convert a ZonedDateTime to sql.Timestamp
     * @param zdt
     * @return
     */
    public static Timestamp zonedDateTimeToTimestamp(ZonedDateTime zdt){
        return new Timestamp(zdt.toInstant().toEpochMilli());
    }

    /**
     * It hides the neo4j type with standard Java type (or sql java type)
     * @param value
     * @return
     */
    public static Object convertObject(Object value) {
        Object converted = value;
        if (value instanceof List) {
            return convertList((List) value);
        }
        if (value instanceof ZonedDateTime) {
            return zonedDateTimeToTimestamp((ZonedDateTime) value);
        }
        if (value instanceof LocalDateTime) {
            return localDateTimeToTimestamp((LocalDateTime) value);
        }
        if (value instanceof LocalDate) {
            return localDateToDate((LocalDate) value);
        }
        if (value instanceof OffsetTime){
            return offsetTimeToTime((OffsetTime) value);
        }
        if (value instanceof LocalTime){
            return localTimeToTime((LocalTime) value);
        }
        if (value instanceof InternalIsoDuration){
            return durationToMap((InternalIsoDuration)value);
        }
        if (value instanceof Path) {
            return PathSerializer.toPath((Path) value);
        }
        if (value instanceof Node) {
            return nodeToMap((Node) value);
        }
        if (value instanceof Relationship) {
            return relationshipToMap((Relationship) value);
        }
        if (value instanceof Point) {
            return pointToMap((Point) converted);
        }

        return converted;
    }

    /**
     * It transforms a Neo4j Point (Spatial) into a java Map
     * @param point
     * @return
     */
    public static Map<String, Object> pointToMap(Point point) {
        Map<String, Object> map = new HashMap<>();
        map.put("srid",point.srid());
        map.put("x", point.x());
        map.put("y", point.y());

        switch(point.srid()){
            case 7203:
                map.put("crs","cartesian");
                break;
            case 9157:
                map.put("crs","cartesian-3d");
                map.put("z", point.z());
                break;
            case 4326:
                map.put("crs","wgs-84");
                map.put("longitude", point.x());
                map.put("latitude", point.y());
                break;
            case 4979:
                map.put("crs","wgs-84-3d");
                map.put("longitude", point.x());
                map.put("latitude", point.y());
                map.put("height",point.z());
                map.put("z", point.z());
                break;
        }

        return map;
    }


    /**
     * It builds a new List with the items converted into java type
     * @param list
     * @return
     */
    public static List convertList(List list) {
        List converted = new ArrayList(list.size());

        for (Object o : list) {
            converted.add(convertObject(o));
        }

        return converted;
    }

    /**
     * Convert a InternalIsoDuration to a Map with the same fields you can get with cypher
     * @param obj
     * @return
     */
    public static Map<String, Object> durationToMap(InternalIsoDuration obj) {
        Map<String, Object> converted = new HashMap<>(16);

        converted.put("duration", obj.toString());
        converted.put("months", obj.months());
        converted.put("days", obj.days());
        converted.put("seconds", obj.seconds());
        converted.put("nanoseconds", obj.nanoseconds());

        return converted;
    }

    /**
     * It build a new Map with the same keys but with pure java type, instead of Neo4j types
     * @param fields
     * @return
     */
    public static Map<String, Object> convertFields(Map<String, Object> fields){
        Map<String, Object> converted = new HashMap();

        Set<Map.Entry<String, Object>> entrySet = fields.entrySet();

        for (Map.Entry<String, Object> entry : entrySet) {
            converted.put(entry.getKey(), convertObject(entry.getValue()));
        }

        return converted;
    }

    public static Map<String, Object> nodeToMap(Node value) {
        Map<String, Object> nodeMap = new LinkedHashMap<>();
        nodeMap.put("_id", value.id());
        nodeMap.put("_labels", value.labels());
        nodeMap.putAll(convertFields(value.asMap()));
        return nodeMap;
    }

    public static Map<String, Object> relationshipToMap(Relationship value) {
        Map<String, Object> relMap = new LinkedHashMap<>();
        relMap.put("_id", value.id());
        relMap.put("_type", value.type());
        relMap.put("_startId", value.startNodeId());
        relMap.put("_endId", value.endNodeId());
        relMap.putAll(convertFields(value.asMap()));
        return relMap;
    }

}
