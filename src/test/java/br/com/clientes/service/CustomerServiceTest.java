package br.com.clientes.service;

import br.com.clientes.dto.CustomerRequest;
import br.com.clientes.messaging.CustomerProducer;
import br.com.clientes.model.Customer;
import br.com.clientes.repository.CustomerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepository repository;

    @Mock
    CustomerProducer producer;

    @InjectMocks
    CustomerService service;

    private final Customer customer = new Customer("123", "Ana", "ana@example.com");

    @Test
    void deveListarClientes() {
        when(repository.findAll()).thenReturn(List.of(customer));

        assertEquals(List.of(customer), service.findAll());
        verifyNoInteractions(producer);
    }

    @Test
    void deveBuscarClientePorId() {
        when(repository.findById("123")).thenReturn(Optional.of(customer));

        assertEquals(customer, service.findById("123"));
    }

    @Test
    void deveLancar404QuandoClienteNaoExiste() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.findById("999"));

        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void deveCriarClienteEPublicarEvento() {
        when(repository.save(any(Customer.class))).thenReturn(customer);

        Customer criado = service.create(new CustomerRequest("  Ana ", " ANA@example.com "));

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals("Ana", captor.getValue().getName());
        assertEquals("ana@example.com", captor.getValue().getEmail());
        verify(producer).publish("CREATED", criado);
        InOrder ordem = inOrder(repository, producer);
        ordem.verify(repository).save(any(Customer.class));
        ordem.verify(producer).publish("CREATED", criado);
    }

    @Test
    void deveLancar409QuandoEmailJaExisteNaCriacao() {
        when(repository.save(any(Customer.class))).thenThrow(new DuplicateKeyException("duplicado"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.create(new CustomerRequest("Ana", "ana@example.com")));

        assertEquals(409, erro.getStatusCode().value());
        verifyNoInteractions(producer);
    }

    @Test
    void naoDevePublicarEventoQuandoOBancoFalha() {
        when(repository.save(any(Customer.class))).thenThrow(new DataAccessResourceFailureException("banco fora"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> service.create(new CustomerRequest("Ana", "ana@example.com")));

        verifyNoInteractions(producer);
    }

    @Test
    void deveAtualizarClienteMantendoOId() {
        when(repository.findById("123")).thenReturn(Optional.of(customer));
        when(repository.save(customer)).thenReturn(customer);

        Customer atualizado = service.update("123", new CustomerRequest("Ana Maria", "ana.maria@example.com"));

        assertEquals("123", atualizado.getId());
        assertEquals("Ana Maria", atualizado.getName());
        assertEquals("ana.maria@example.com", atualizado.getEmail());
        verify(producer).publish("UPDATED", atualizado);
    }

    @Test
    void naoDeveAtualizarClienteInexistente() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.update("999", new CustomerRequest("Ana", "ana@example.com")));

        verify(repository, never()).save(any());
        verifyNoInteractions(producer);
    }

    @Test
    void deveLancar409QuandoEmailJaExisteNaAtualizacao() {
        when(repository.findById("123")).thenReturn(Optional.of(customer));
        when(repository.save(customer)).thenThrow(new DuplicateKeyException("duplicado"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.update("123", new CustomerRequest("Ana", "outro@example.com")));

        assertEquals(409, erro.getStatusCode().value());
        verifyNoInteractions(producer);
    }

    @Test
    void deveExcluirClienteEPublicarEvento() {
        when(repository.findById("123")).thenReturn(Optional.of(customer));

        service.delete("123");

        verify(repository).delete(customer);
        verify(producer).publish("DELETED", customer);
        InOrder ordem = inOrder(repository, producer);
        ordem.verify(repository).delete(customer);
        ordem.verify(producer).publish("DELETED", customer);
    }

    @Test
    void naoDeveExcluirClienteInexistente() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.delete("999"));

        verify(repository, never()).delete(any(Customer.class));
        verifyNoInteractions(producer);
    }
}
