package me.itzg.trying.jtarollbacklistener.services;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import me.itzg.trying.jtarollbacklistener.entities.Author;
import me.itzg.trying.jtarollbacklistener.entities.Publication;
import me.itzg.trying.jtarollbacklistener.events.IncrementedPublicationsEvent;
import me.itzg.trying.jtarollbacklistener.repositories.AuthorRepository;
import me.itzg.trying.jtarollbacklistener.repositories.PublicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Slf4j
public class PublishingService {

  private final AuthorStatsService authorStatsService;
  private final AuthorRepository authorRepository;
  private final PublicationRepository publicationRepository;

  @Autowired
  public PublishingService(AuthorStatsService authorStatsService, AuthorRepository authorRepository,
                           PublicationRepository publicationRepository) {
    this.authorStatsService = authorStatsService;
    this.authorRepository = authorRepository;
    this.publicationRepository = publicationRepository;
  }

  @Transactional(rollbackFor = Exception.class)
  public Publication publish(String authorName, String publicationTitle) {
    authorStatsService.incrementPublications(authorName);

    final Author author = authorRepository.findByName(authorName)
        .orElseGet(() -> authorRepository.save(new Author().setName(authorName)));

    final Publication publication = new Publication()
        .setTitle(publicationTitle)
        .setAuthor(author);

    if (author.getPublications() == null) {
      author.setPublications(new ArrayList<>());
    }
    author.getPublications().add(publication);

    // if title is null, we'll let it slip through to allow for test variations
    if (publication.getTitle() != null && publication.getTitle().isBlank()) {
      throw new IllegalArgumentException("inducing exception after realizing title is blank");
    }

    return publicationRepository.save(publication);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
  public void rollbackInc(IncrementedPublicationsEvent event) {
    log.debug("Rolling back increment of author={} publication", event.getAuthor());
    authorStatsService.decrementPublications(event.getAuthor());
  }

  @Transactional(readOnly = true)
  public List<Publication> getPublications(String authorName) {
    final Author author = authorRepository.findByName(authorName)
        .orElseThrow();

    // copy off list since return value leaves entity manager scope
    return new ArrayList<>(author.getPublications());
  }

}
