package me.itzg.trying.jtarollbacklistener.web;

import java.util.List;
import java.util.stream.Collectors;
import me.itzg.trying.jtarollbacklistener.entities.Publication;
import me.itzg.trying.jtarollbacklistener.services.AuthorStatsService;
import me.itzg.trying.jtarollbacklistener.services.PublishingService;
import me.itzg.trying.jtarollbacklistener.web.model.AuthorPublicationCount;
import me.itzg.trying.jtarollbacklistener.web.model.NewPublication;
import me.itzg.trying.jtarollbacklistener.web.model.PublicationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

  private final PublishingService publishingService;
  private final AuthorStatsService authorStatsService;

  @Autowired
  public ApiController(PublishingService publishingService, AuthorStatsService authorStatsService) {
    this.publishingService = publishingService;
    this.authorStatsService = authorStatsService;
  }

  @PostMapping("/publications")
  public PublicationDTO createPublication(@RequestBody NewPublication newPublication) {
    return publishingService.publish(newPublication.getAuthorName(), newPublication.getPublicationTitle())
        .toDTO();
  }

  @GetMapping("/authors/{name}/publications")
  public List<PublicationDTO> getAuthorPublications(@PathVariable String name) {
    return publishingService.getPublications(name).stream()
        .map(Publication::toDTO)
        .collect(Collectors.toList());
  }

  @GetMapping("/publication-counts")
  public List<AuthorPublicationCount> getTopPublishingAuthors(@RequestParam(defaultValue = "10") int limit) {
    return authorStatsService.getTopPublishingAuthors(limit);
  }
}
