package me.itzg.trying.jtarollbacklistener.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.itzg.trying.jtarollbacklistener.events.IncrementedPublicationsEvent;
import me.itzg.trying.jtarollbacklistener.web.model.AuthorPublicationCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Slf4j
public class AuthorStatsService {

  private final Map<String, Integer> publicationCounts = new HashMap<>();
  private final ApplicationContext applicationContext;

  @Autowired
  public AuthorStatsService(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  void incrementPublications(String author) {
    synchronized (publicationCounts) {
      final Integer count = publicationCounts.getOrDefault(author, 0);
      publicationCounts.put(author, count + 1);
      applicationContext.publishEvent(new IncrementedPublicationsEvent(author));
    }
  }

  void decrementPublications(String author) {
    synchronized (publicationCounts) {
      final Integer count = publicationCounts.getOrDefault(author, 0);
      if (count > 0) {
        publicationCounts.put(author, count - 1);
      }
      else {
        throw new IllegalStateException("Decrement would have been negative for author: " + author);
      }
    }
  }


  public List<AuthorPublicationCount> getTopPublishingAuthors(int limit) {
    synchronized (publicationCounts) {
      return publicationCounts.entrySet().stream()
          .sorted((o1, o2) -> Integer.compare(o2.getValue(), o1.getValue()))
          .limit(limit)
          .map(e -> new AuthorPublicationCount().setName(e.getKey()).setCount(e.getValue()))
          .collect(Collectors.toList());
    }
  }

  public int getAuthorPublicationCount(String author) {
    synchronized (publicationCounts) {
      return publicationCounts.getOrDefault(author, 0);
    }
  }
}
