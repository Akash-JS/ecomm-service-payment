package org.example.ecommservicepayment.controller;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.ecommservicepayment.InstantTypeAdapter;
import org.example.ecommservicepayment.KafkaProducer;
import org.example.ecommservicepayment.models.OrderItem;
import org.example.ecommservicepayment.models.Payment;

import org.example.ecommservicepayment.repositories.IPaymentRepositoy;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/payments")
public class PaymentController {

    IPaymentRepositoy paymentRepositoy;
    WebClient webClient;
    KafkaProducer kafkaProducer;

    PaymentController(IPaymentRepositoy paymentRepositoy, WebClient webClient, KafkaProducer kafkaProducer)
    {
        this.paymentRepositoy = paymentRepositoy;
        this.webClient = webClient;
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getPayments()
    {
        List<Payment> payments = paymentRepositoy.findAll();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("{paymentId}")
    public ResponseEntity<Optional<Payment>> getPayments(@PathVariable String paymentId)
    {
        Optional<Payment> payment = paymentRepositoy.findById(paymentId);

        if(payment.isEmpty())
        {
            return ResponseEntity.notFound().build();
        }


        return ResponseEntity.ok(payment);
    }

    @PostMapping
    public ResponseEntity<String> makePayment(@RequestBody Payment payment)
    {
        if(payment == null)
        {
            ResponseEntity.badRequest().build();
        }

        // Get Order details
        List<OrderItem> orderItems = getOrderDetails(payment.getOrderId());
        // Check Price
        if(!isCorrectAmount(payment.getAmount(), orderItems))
        {
            return ResponseEntity.badRequest().body("Incorrect amount");
        }
        // Make Payment
        payment.setId(UUID.randomUUID().toString());
        paymentRepositoy.save(payment);
        // Send Kafka Message

        kafkaProducer.sendMessage(payment.getOrderId());
        return ResponseEntity.ok("payment");
    }

    private  List<OrderItem> getOrderDetails(String orderId)
    {
        return webClient.get()
                .uri("ecomm-service-order/api/v1/order/"+orderId+"/order-items")
                .retrieve().bodyToFlux(OrderItem.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatusCode.valueOf(404)) {
                        return Mono.empty(); // Return an empty Mono
                    } else {
                        return Mono.error(ex);
                    }
                })
                .collectList()
                .block();
    }

    private boolean isCorrectAmount(BigDecimal amountPaid, List<OrderItem> orderItems)
    {
        BigDecimal total = orderItems.stream().map(OrderItem::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return  total.compareTo(amountPaid) == 0;
    }


}
