## Pra rodar

```bash
docker compose up -d --wait
./mvnw spring-boot:run
```

Use `.\mvnw.cmd spring-boot:run` no Windows.

A API sobe em [http://localhost:8081](http://localhost:8081).

## Endpoints

| Método | Rota | O que faz |
| --- | --- | --- |
| POST | `/customers` | Cria cliente |
| GET | `/customers` | Lista clientes |
| GET | `/customers/{id}` | Busca cliente |
| PUT | `/customers/{id}` | Atualiza cliente |
| DELETE | `/customers/{id}` | Exclui cliente |

## Testes

```bash
./mvnw test
```
