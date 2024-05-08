package org.example.ecommservicepayment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.time.Instant;

@SpringBootApplication
@EnableDiscoveryClient
public class EcommServicePaymentApplication {

    public static void main(String[] args) {

        SpringApplication.run(EcommServicePaymentApplication.class, args);
    }

}
