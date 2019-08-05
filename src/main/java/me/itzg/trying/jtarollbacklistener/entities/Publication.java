package me.itzg.trying.jtarollbacklistener.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import me.itzg.trying.jtarollbacklistener.web.model.PublicationDTO;

@Entity
@Data
public class Publication {
  @Id @GeneratedValue
  Long id;

  @NotNull
  String title;

  @ManyToOne
  @NotNull
  Author author;

  public PublicationDTO toDTO() {
    return new PublicationDTO()
        .setId(id)
        .setTitle(title);
  }
}
