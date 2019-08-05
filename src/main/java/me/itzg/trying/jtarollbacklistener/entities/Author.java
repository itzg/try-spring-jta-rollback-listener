package me.itzg.trying.jtarollbacklistener.entities;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import me.itzg.trying.jtarollbacklistener.web.model.AuthorDTO;

@Entity
@Data
public class Author {

  @Id @GeneratedValue
  Long id;

  @NotBlank
  String name;

  @OneToMany
  List<Publication> publications;

  public AuthorDTO toDTO() {
    return new AuthorDTO()
        .setId(id)
        .setName(name);
  }
}
