package com.app84soft.check_in.repositories;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;


@Repository
public class BaseRepository {

    @PersistenceContext
    private EntityManager entityManager;

    protected void flush() {
        entityManager.flush();
    }

    protected void detach(Object obj) {
        entityManager.detach(obj);
    }

    protected JPAQueryFactory query() {
        return new JPAQueryFactory(entityManager);
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

}
