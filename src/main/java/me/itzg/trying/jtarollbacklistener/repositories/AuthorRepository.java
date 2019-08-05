package me.itzg.trying.jtarollbacklistener.repositories;

import java.util.Optional;
import me.itzg.trying.jtarollbacklistener.entities.Author;
import org.springframework.data.repository.CrudRepository;

public interface AuthorRepository extends CrudRepository<Author, Long> {

  Optional<Author> findByName(String name);
}
