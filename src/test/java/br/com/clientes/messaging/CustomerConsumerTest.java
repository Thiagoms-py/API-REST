package br.com.clientes.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class CustomerConsumerTest {

    @Test
    void deveRegistrarOEventoRecebidoNoLog(CapturedOutput output) {
        CustomerConsumer consumer = new CustomerConsumer();

        consumer.consume("CREATED:123");

        assertTrue(output.getOut().contains("Evento recebido: CREATED:123"));
    }
}
