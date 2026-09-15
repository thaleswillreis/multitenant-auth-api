package com.thaleswillreis.authapi.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class TenantFilterAspect {

    private static final String FILTER_NAME = "tenantFilter";

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.thaleswillreis.authapi.repository.*.*(..))")
    public void enableTenantFilter() {
        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            return;
        }

        Session session = entityManager.unwrap(Session.class);
        if (session.getEnabledFilter(FILTER_NAME) == null) {
            session.enableFilter(FILTER_NAME).setParameter("tenantId", tenantId);
        }
    }

}