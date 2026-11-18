package br.com.clientes.messaging;

import br.com.clientes.model.Customer;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CustomerProducerTest {

    @Mock
    KafkaTemplate<String, String> kafka;

    private final Customer customer = new Customer("123", "Ana", "ana@example.com");

    @Test
    void deveEnviarMensagemUsandoOIdComoChave() {
        CompletableFuture<SendResult<String, String>> resposta = new CompletableFuture<>();
        when(kafka.send("customer-events", "123", "CREATED:123")).thenReturn(resposta);
        CustomerProducer producer = new CustomerProducer(kafka, "customer-events");

        producer.publish("CREATED", customer);

        verify(kafka).send("customer-events", "123", "CREATED:123");
    }

    @Test
    void deveRegistrarSucessoSoDepoisQueOKafkaConfirma(CapturedOutput output) {
        CompletableFuture<SendResult<String, String>> resposta = new CompletableFuture<>();
        when(kafka.send("customer-events", "123", "CREATED:123")).thenReturn(resposta);
        CustomerProducer producer = new CustomerProducer(kafka, "customer-events");

        producer.publish("CREATED", customer);
        assertFalse(output.getAll().contains("confirmado pelo Kafka"));

        resposta.complete(null);

        assertTrue(output.getAll().contains("Evento CREATED do cliente 123 confirmado pelo Kafka"));
    }

    @Test
    void deveRegistrarErroQuandoOEnvioFalhaDepois(CapturedOutput output) {
        CompletableFuture<SendResult<String, String>> resposta = new CompletableFuture<>();
        when(kafka.send("customer-events", "123", "CREATED:123")).thenReturn(resposta);
        CustomerProducer producer = new CustomerProducer(kafka, "customer-events");
        producer.publish("CREATED", customer);

        assertDoesNotThrow(() -> resposta.completeExceptionally(new KafkaException("fora do ar")));

        assertTrue(output.getAll().contains("Falha ao enviar evento CREATED do cliente 123"));
        assertFalse(output.getAll().contains("confirmado pelo Kafka"));
    }

    @Test
    void naoDeveLancarErroQuandoOKafkaFalhaNaHora(CapturedOutput output) {
        doThrow(new KafkaException("fora do ar"))
                .when(kafka).send("customer-events", "123", "CREATED:123");
        CustomerProducer producer = new CustomerProducer(kafka, "customer-events");

        assertDoesNotThrow(() -> producer.publish("CREATED", customer));

        assertTrue(output.getAll().contains("Falha ao enviar evento CREATED do cliente 123"));
    }
}
