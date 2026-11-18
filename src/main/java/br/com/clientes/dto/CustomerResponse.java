package br.com.clientes.dto;

import br.com.clientes.model.Customer;

public record CustomerResponse(String id, String name, String email) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail());
    }
}
