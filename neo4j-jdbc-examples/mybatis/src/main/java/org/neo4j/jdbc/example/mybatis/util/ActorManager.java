package org.neo4j.jdbc.example.mybatis.util;

import org.apache.ibatis.session.SqlSession;
import org.neo4j.jdbc.example.mybatis.bean.Actor;
import org.neo4j.jdbc.example.mybatis.mapper.ActorMapper;

public class ActorManager {
	public static Actor selectActorByBorn(int born) {
		SqlSession sqlSession = ConnectionFactory.getSqlSessionFactory()
				.openSession();
		try {
			ActorMapper categoryMapper = sqlSession
					.getMapper(ActorMapper.class);
			return categoryMapper.selectActorByBorn(born);
		} finally {
			sqlSession.close();
		}
	}
}
