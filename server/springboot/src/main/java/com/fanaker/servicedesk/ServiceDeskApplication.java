package com.fanaker.servicedesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import java.nio.file.*;
import java.util.*;

@SpringBootApplication
public class ServiceDeskApplication {
  public static void main(String[] args) { SpringApplication.run(ServiceDeskApplication.class,args); }
  @Bean CommandLineRunner seed(TicketRepository repo) {
    return args -> {
      if (repo.count()!=0) return;
      Path source=Paths.get("data/seed/tickets.tsv");
      if (!Files.isRegularFile(source)) return;
      List<SupportTicket> batch=new ArrayList<>();
      for (String line:Files.readAllLines(source)) {
        String[] s=line.split("\t",-1);
        if(s.length!=13)continue;
        SupportTicket t=new SupportTicket();
        t.id=Long.parseLong(s[0]);t.title=s[1];t.description=s[2];t.department=s[3];t.category=s[4];t.priority=s[5];t.status=s[6];t.requester=s[7];t.assignee=s[8];t.created=s[9];t.updated=s[10];t.due=s[11];t.asset=s[12];
        batch.add(t);
      }
      repo.saveAll(batch);
    };
  }
}
