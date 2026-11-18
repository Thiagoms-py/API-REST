package br.com.clientes.service;

import br.com.clientes.dto.CustomerRequest;
import br.com.clientes.messaging.CustomerProducer;
import br.com.clientes.model.Customer;
import br.com.clientes.repository.CustomerRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerProducer producer;

    public CustomerService(CustomerRepository repository, CustomerProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    public List<Customer> findAll() {
        return repository.findAll();
    }

    public Customer findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }

    public Customer create(CustomerRequest request) {
        Customer customer = new Customer(null, request.getName().trim(), request.getEmail().trim().toLowerCase(Locale.ROOT));
        Customer saved = save(customer);
        producer.publish("CREATED", saved);
        return saved;
    }

    public Customer update(String id, CustomerRequest request) {
        Customer customer = findById(id);
        customer.setName(request.getName().trim());
        customer.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        Customer saved = save(customer);
        producer.publish("UPDATED", saved);
        return saved;
    }

    public void delete(String id) {
        Customer customer = findById(id);
        repository.delete(customer);
        producer.publish("DELETED", customer);
    }

    // o índice único do e-mail faz o Mongo lançar DuplicateKeyException
    private Customer save(Customer customer) {
        try {
            return repository.save(customer);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um cliente com esse e-mail");
        }
    }
}
