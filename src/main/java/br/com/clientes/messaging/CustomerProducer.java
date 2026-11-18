package br.com.clientes.messaging;

import br.com.clientes.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomerProducer {

    private static final Logger log = LoggerFactory.getLogger(CustomerProducer.class);

    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public CustomerProducer(KafkaTemplate<String, String> kafka,
                            @Value("${app.kafka.customer-topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public void publish(String type, Customer customer) {
        String message = type + ":" + customer.getId();
        try {
            // o send é assíncrono, então o resultado só chega depois, no whenComplete
            kafka.send(topic, customer.getId(), message).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Falha ao enviar evento {} do cliente {}", type, customer.getId(), error);
                } else {
                    log.info("Evento {} do cliente {} confirmado pelo Kafka", type, customer.getId());
                }
            });
        } catch (RuntimeException e) {
            // o cliente já foi salvo no Mongo, então só registra o erro
            log.error("Falha ao enviar evento {} do cliente {}", type, customer.getId(), e);
        }
    }
}
