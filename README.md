This application tests the use of a `@TransactionalEventListener` with a phase of `AFTER_ROLLBACK` in order to perform custom rollback processing.

The listener method is declared as the following in `PublishingService`

```
  @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
  public void rollbackInc(IncrementedPublicationsEvent event) {
    log.debug("Rolling back increment of author={} publication", event.getAuthor());
    authorStatsService.decrementPublications(event.getAuthor());
  }
```

The event is published as the following in `AuthorStatsService`
```
      applicationContext.publishEvent(new IncrementedPublicationsEvent(author));
```

## Constraint violation during commit skips listener invocation

I consider this a bug in Spring Transactions that the test case `PublishingServiceTest.publish_nullTitle` fails (further confirmation is needed before opening an issue against Spring). 

The rollback listener is normally invoked [at this point in `TransactionAspectSupport`](https://github.com/spring-projects/spring-framework/blob/v5.1.8.RELEASE/spring-tx/src/main/java/org/springframework/transaction/interceptor/TransactionAspectSupport.java#L299); however, the constraint violation exception is thrown during the commit [later in the same `invokeWithinTransaction` method, at this point](https://github.com/spring-projects/spring-framework/blob/v5.1.8.RELEASE/spring-tx/src/main/java/org/springframework/transaction/interceptor/TransactionAspectSupport.java#L305).

The following captures some logs and the stack trace around the constraint violation:

```
2019-08-05 11:01:23.312 ERROR 30778 --- [           main] o.h.i.ExceptionMapperStandardImpl        : HHH000346: Error during managed flush [Validation failed for classes [me.itzg.trying.jtarollbacklistener.entities.Publication] during persist time for groups [javax.validation.groups.Default, ]
List of constraint violations:[
	ConstraintViolationImpl{interpolatedMessage='must not be null', propertyPath=title, rootBeanClass=class me.itzg.trying.jtarollbacklistener.entities.Publication, messageTemplate='{javax.validation.constraints.NotNull.message}'}
]]
2019-08-05 11:01:23.313 DEBUG 30778 --- [           main] cResourceLocalTransactionCoordinatorImpl : JDBC transaction marked for rollback-only (exception provided for stack trace)

java.lang.Exception: exception just for purpose of providing stack trace
	at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl$TransactionDriverControlImpl.markRollbackOnly(JdbcResourceLocalTransactionCoordinatorImpl.java:314) ~[hibernate-core-5.3.10.Final.jar:5.3.10.Final]
	at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl.beforeCompletionCallback(JdbcResourceLocalTransactionCoordinatorImpl.java:187) ~[hibernate-core-5.3.10.Final.jar:5.3.10.Final]
	at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl.access$300(JdbcResourceLocalTransactionCoordinatorImpl.java:39) ~[hibernate-core-5.3.10.Final.jar:5.3.10.Final]
	at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl$TransactionDriverControlImpl.commit(JdbcResourceLocalTransactionCoordinatorImpl.java:271) ~[hibernate-core-5.3.10.Final.jar:5.3.10.Final]
	at org.hibernate.engine.transaction.internal.TransactionImpl.commit(TransactionImpl.java:104) ~[hibernate-core-5.3.10.Final.jar:5.3.10.Final]
	at org.springframework.orm.jpa.JpaTransactionManager.doCommit(JpaTransactionManager.java:532) ~[spring-orm-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.transaction.support.AbstractPlatformTransactionManager.processCommit(AbstractPlatformTransactionManager.java:746) ~[spring-tx-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.transaction.support.AbstractPlatformTransactionManager.commit(AbstractPlatformTransactionManager.java:714) ~[spring-tx-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.transaction.interceptor.TransactionAspectSupport.commitTransactionAfterReturning(TransactionAspectSupport.java:534) ~[spring-tx-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:305) ~[spring-tx-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:98) ~[spring-tx-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186) ~[spring-aop-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:688) ~[spring-aop-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at me.itzg.trying.jtarollbacklistener.services.PublishingService$$EnhancerBySpringCGLIB$$a33d561f.publish(<generated>) ~[classes/:na]
	at me.itzg.trying.jtarollbacklistener.services.PublishingServiceTest.lambda$publish_nullTitle$2(PublishingServiceTest.java:90) ~[test-classes/:na]
	at org.assertj.core.api.ThrowableAssert.catchThrowable(ThrowableAssert.java:62) ~[assertj-core-3.11.1.jar:na]
	at org.assertj.core.api.AssertionsForClassTypes.catchThrowable(AssertionsForClassTypes.java:786) ~[assertj-core-3.11.1.jar:na]
	at org.assertj.core.api.Assertions.catchThrowable(Assertions.java:1200) ~[assertj-core-3.11.1.jar:na]
	at org.assertj.core.api.Assertions.assertThatThrownBy(Assertions.java:1094) ~[assertj-core-3.11.1.jar:na]
	at me.itzg.trying.jtarollbacklistener.services.PublishingServiceTest.publish_nullTitle(PublishingServiceTest.java:88) ~[test-classes/:na]

2019-08-05 11:01:23.314 TRACE 30778 --- [           main] cResourceLocalTransactionCoordinatorImpl : ResourceLocalTransactionCoordinatorImpl#afterCompletionCallback(false)
2019-08-05 11:01:23.314 TRACE 30778 --- [           main] .t.i.SynchronizationRegistryStandardImpl : SynchronizationRegistryStandardImpl.notifySynchronizationsAfterTransactionCompletion(5)
2019-08-05 11:01:23.315 DEBUG 30778 --- [           main] o.h.e.t.internal.TransactionImpl         : rollback() called on an inactive transaction
```