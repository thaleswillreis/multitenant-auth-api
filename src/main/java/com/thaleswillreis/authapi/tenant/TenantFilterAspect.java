package com.thaleswillreis.authapi.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.UUID;

@Aspect
@Component
public class TenantFilterAspect {

    private static final String FILTER_NAME = "tenantFilter";

    @PersistenceContext
    private EntityManager entityManager;

    private final TransactionTemplate transactionTemplate;

    public TenantFilterAspect(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Around("execution(* com.thaleswillreis.authapi.repository.*.*(..))")
    public Object applyTenantFilter(ProceedingJoinPoint joinPoint) {
        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            return proceedUnchecked(joinPoint);
        }

        return transactionTemplate.execute(status -> {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter(FILTER_NAME) == null) {
                session.enableFilter(FILTER_NAME).setParameter("tenantId", tenantId);
            }
            return proceedUnchecked(joinPoint);
        });
    }

    private Object proceedUnchecked(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new UndeclaredThrowableException(t);
        }
    }

}