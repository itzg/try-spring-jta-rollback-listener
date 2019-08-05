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