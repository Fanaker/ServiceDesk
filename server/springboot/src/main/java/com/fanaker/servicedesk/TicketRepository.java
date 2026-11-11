package com.fanaker.servicedesk;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TicketRepository extends JpaRepository<SupportTicket,Long> {}
