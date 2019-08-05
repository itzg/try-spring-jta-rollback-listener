package me.itzg.trying.jtarollbacklistener.events;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

@Data
public class IncrementedPublicationsEvent  {

  final String author;

}
