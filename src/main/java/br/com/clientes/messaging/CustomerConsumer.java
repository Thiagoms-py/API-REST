package br.com.clientes.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CustomerConsumer {

    private static final Logger log = LoggerFactory.getLogger(CustomerConsumer.class);

    @KafkaListener(topics = "${app.kafka.customer-topic}")
    public void consume(String message) {
        log.info("Evento recebido: {}", message);
    }
}
