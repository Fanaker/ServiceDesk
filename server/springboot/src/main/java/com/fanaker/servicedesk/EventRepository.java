package com.fanaker.servicedesk;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventRepository extends JpaRepository<ServiceEvent,Long> {}
