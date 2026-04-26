package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.tenant.Customer;

import java.util.UUID;

public interface CustomerRepo extends JpaRepository<Customer, UUID> {
}
