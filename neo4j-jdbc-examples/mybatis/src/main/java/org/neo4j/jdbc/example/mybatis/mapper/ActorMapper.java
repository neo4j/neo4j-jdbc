package org.neo4j.jdbc.example.mybatis.mapper;

import org.neo4j.jdbc.example.mybatis.bean.Actor;

public interface ActorMapper {
	public Actor selectActorByBorn(int born);
}
