package com.fanaker.servicedesk;
import jakarta.persistence.*;
@Entity @Table(name="ticket_events")
public class ServiceEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String time;
 public Long ticket;
 public String actor;
 public String action;
 @Column(length=1000) public String detail;
 public ServiceEvent() {}
 public ServiceEvent(String time,Long ticket,String actor,String action,String detail){this.time=time;this.ticket=ticket;this.actor=actor;this.action=action;this.detail=detail;}
}
