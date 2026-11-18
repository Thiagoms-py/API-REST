package br.com.clientes.controller;

import br.com.clientes.dto.CustomerRequest;
import br.com.clientes.model.Customer;
import br.com.clientes.service.CustomerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private static final String BODY_VALIDO = "{\"name\":\"Ana\",\"email\":\"ana@example.com\"}";
    private static final String BODY_INVALIDO = "{\"name\":\"\",\"email\":\"invalido\"}";

    @Mock
    CustomerService service;

    private MockMvc mvc;
    private final Customer customer = new Customer("123", "Ana", "ana@example.com");

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CustomerController(service)).build();
    }

    @Test
    void deveListarClientes() throws Exception {
        when(service.findAll()).thenReturn(List.of(customer));

        mvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("123"))
                .andExpect(jsonPath("$[0].name").value("Ana"));
    }

    @Test
    void deveBuscarClientePorId() throws Exception {
        when(service.findById("123")).thenReturn(customer);

        mvc.perform(get("/customers/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void deveRetornar404AoBuscarClienteInexistente() throws Exception {
        when(service.findById("999")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mvc.perform(get("/customers/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveCriarClienteComHeaderLocation() throws Exception {
        when(service.create(any(CustomerRequest.class))).thenReturn(customer);

        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/customers/123"))
                .andExpect(jsonPath("$.id").value("123"));
        ArgumentCaptor<CustomerRequest> entrada = ArgumentCaptor.forClass(CustomerRequest.class);
        verify(service).create(entrada.capture());
        assertEquals("Ana", entrada.getValue().getName());
        assertEquals("ana@example.com", entrada.getValue().getEmail());
    }

    @Test
    void deveRetornar400AoCriarComDadosInvalidos() throws Exception {
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(BODY_INVALIDO))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void deveRetornar409AoCriarComEmailDuplicado() throws Exception {
        when(service.create(any(CustomerRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void deveAtualizarCliente() throws Exception {
        when(service.update(eq("123"), any(CustomerRequest.class))).thenReturn(customer);

        mvc.perform(put("/customers/123").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"));
    }

    @Test
    void deveRetornar400AoAtualizarComDadosInvalidos() throws Exception {
        mvc.perform(put("/customers/123").contentType(MediaType.APPLICATION_JSON).content(BODY_INVALIDO))
                .andExpect(status().isBadRequest());

        verify(service, never()).update(any(), any());
    }

    @Test
    void deveRetornar404AoAtualizarClienteInexistente() throws Exception {
        when(service.update(eq("999"), any(CustomerRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mvc.perform(put("/customers/999").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveExcluirClienteSemCorpoNaResposta() throws Exception {
        mvc.perform(delete("/customers/123"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete("123");
    }

    @Test
    void deveRetornar404AoExcluirClienteInexistente() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(service).delete("999");

        mvc.perform(delete("/customers/999"))
                .andExpect(status().isNotFound());
    }
}
