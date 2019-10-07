package org.neo4j.jdbc.bolt;

import org.neo4j.driver.summary.SummaryCounters;

/**
 * A set of common functions for bolt connector
 */
public class BoltNeo4jUtils {

    /**
     * Calculate, using the summary, how many operations are executed in the statement
     * @param stats
     * @return
     */
    public static int calculateUpdateCount(SummaryCounters stats) {

		/*
		Neo4j has a detailed report to explain which entity is changed.
		But we need to collect all that value to only one.
		So a priority is defined:
		1 - nodes and relationships creation or deletion
		2 - schema changes
		3 - properties changes
		 */

        int objectCount = 0;
        objectCount += stats.nodesCreated();
        objectCount += stats.nodesDeleted();
        objectCount += stats.relationshipsCreated();
        objectCount += stats.relationshipsDeleted();

        int schemaCount = 0;
        schemaCount += stats.constraintsAdded();
        schemaCount += stats.constraintsRemoved();
        schemaCount += stats.indexesAdded();
        schemaCount += stats.indexesRemoved();
        schemaCount += stats.labelsAdded();
        schemaCount += stats.labelsRemoved();

        int updateCount = (objectCount == 0)?schemaCount:objectCount;

        return (updateCount == 0)?stats.propertiesSet():updateCount;
    }

}
