package com.fanaker.servicedesk;
import jakarta.persistence.*;

@Entity @Table(name="service_tickets")
public class SupportTicket {
 @Id public Long id;
 @Column(nullable=false,length=140) public String title;
 @Column(length=4000) public String description;
 public String department;
 public String category;
 public String priority;
 public String status;
 public String requester;
 public String assignee;
 public String created;
 public String updated;
 public String due;
 public String asset;
 public SupportTicket() {}
}
