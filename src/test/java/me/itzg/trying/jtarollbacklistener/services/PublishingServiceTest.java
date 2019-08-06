package me.itzg.trying.jtarollbacklistener.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import me.itzg.trying.jtarollbacklistener.entities.Author;
import me.itzg.trying.jtarollbacklistener.entities.Publication;
import me.itzg.trying.jtarollbacklistener.repositories.AuthorRepository;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionSystemException;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
    "logging.level.org.hibernate.engine.transaction=debug",
    "logging.level.org.hibernate.resource.transaction.backend.jdbc.internal=trace",
    "logging.level.org.hibernate.resource.transaction.internal=trace"
})
public class PublishingServiceTest {

  @Rule
  public TestName testName = new TestName();

  @Autowired
  PublishingService publishingService;

  @Autowired
  AuthorStatsService authorStatsService;

  @Autowired
  AuthorRepository authorRepository;

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void publish_successful() {
    final String author = testName.getMethodName()+"-author-1";
    final String title = testName.getMethodName()+"-title-1";

    final Publication publication = publishingService.publish(author, title);

    assertThat(publication).isNotNull();

    assertThat(
        publishingService.getPublications(author)
    ).hasSize(1);

    assertThat(
        authorStatsService.getAuthorPublicationCount(author)
    ).isEqualTo(1);
  }

  @Test
  public void publish_blankTitle() {
    final String author = testName.getMethodName()+"-author-1";

    assertThatThrownBy(() -> {
      // induce IllegalArgumentException scenario with blank title
      publishingService.publish(author, "");
    }).isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> {
      publishingService.getPublications(author);
    }).isInstanceOf(NoSuchElementException.class);

    // verify that stats increment was rolled back
    assertThat(
        authorStatsService.getAuthorPublicationCount(author)
    ).isEqualTo(0);
  }

  @Test
  public void publish_rollbackFollowedBySuccess() {
    final String author = testName.getMethodName()+"-author-1";
    final String title = testName.getMethodName() + "-title-1";

    assertThatThrownBy(() -> {
      // induce IllegalArgumentException scenario with blank title
      publishingService.publish(author, "");
    }).isInstanceOf(IllegalArgumentException.class);

    final Publication publication = publishingService.publish(author,
        title
    );
    assertThat(publication).isNotNull();

    assertThat(
        publishingService.getPublications(author)
    ).hasSize(1);

    assertThat(
        authorStatsService.getAuthorPublicationCount(author)
    ).isEqualTo(1);
  }

  /**
   * THIS TEST CURRENTLY FAILS since the {@link PublishingService#rollbackInc(me.itzg.trying.jtarollbacklistener.events.IncrementedPublicationsEvent)}
   * listener method is not getting called by spring-tx since the constraint violation gets wrapped
   * in a RollbackException during the commit.
   */
  @Test
  public void publish_nullTitle() {
    final String author = testName.getMethodName()+"-author-1";

    assertThatThrownBy(() -> {
      // induce validation constraint violation with null title
      publishingService.publish(author, null);
    })
        .isInstanceOf(TransactionSystemException.class)
        .hasRootCauseInstanceOf(ConstraintViolationException.class);

    final Optional<Author> authorResult = authorRepository.findByName(author);
    assertThat(authorResult).isNotPresent();

    // verify that stats increment was rolled back
    assertThat(
        authorStatsService.getAuthorPublicationCount(author)
    ).isEqualTo(0);
  }
}