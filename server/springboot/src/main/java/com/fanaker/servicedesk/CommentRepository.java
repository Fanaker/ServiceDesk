package com.fanaker.servicedesk;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CommentRepository extends JpaRepository<ServiceComment,Long> {}
