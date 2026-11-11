package com.fanaker.servicedesk;
import jakarta.persistence.*;
@Entity @Table(name="ticket_comments")
public class ServiceComment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long ticket;
 public String author;
 public String time;
 @Column(length=1000) public String message;
 public ServiceComment() {}
 public ServiceComment(Long ticket,String author,String time,String message){this.ticket=ticket;this.author=author;this.time=time;this.message=message;}
}
