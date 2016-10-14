package org.neo4j.jdbc.example.mybatis;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.jdbc.example.mybatis.bean.Actor;
import org.neo4j.jdbc.example.mybatis.util.ActorManager;

public class ActorManagerIT {
	
	@Test
	@Ignore
	public void testSelectActorBornIn1973() {
		Actor actor = ActorManager.selectActorByBorn(1973);
		Assert.assertNotNull(actor);
		Assert.assertEquals(1973, actor.getBorn());
		Assert.assertEquals("Dave Chappelle", actor.getName());
	}
}
