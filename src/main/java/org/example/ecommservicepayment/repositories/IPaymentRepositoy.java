package org.example.ecommservicepayment.repositories;

import org.example.ecommservicepayment.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPaymentRepositoy extends JpaRepository<Payment,String> {
}
