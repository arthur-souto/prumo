# prumo

Projeto gerado pelo [Archwright](https://github.com/arthur-souto/archwright), com a arquitetura montada desde o primeiro commit.

## Resumo

| | |
|---|---|
| Arquitetura | Hexagonal em camadas |
| Spring Boot | 4.1.3 |
| Java | 25 |
| Kit de dependências | Básico |
| Pacote base | `io.github.arthursouto.prumo` |

## Estrutura de pacotes


Cada pacote tem um `package-info.java` explicando a responsabilidade dele.

## Como rodar

Suba o PostgreSQL local (opcional: copie `.env.example` para `.env` e ajuste):

```shell
docker compose up -d
```

```shell
./mvnw spring-boot:run
```

Para compilar e testar:

```shell
./mvnw verify
```

## Fitness functions

Os testes de arquitetura ficam em `src/test/java/io/github/arthursouto/prumo/architecture` e rodam no `./mvnw verify`.
O build quebrar quando alguém viola a arquitetura é intencional: é isso que mantém a estrutura certa ao longo do tempo.

Regras ativas neste projeto:

- `FF-LAY-01` (HexagonalLayersTest): Dependências só no sentido adapter/infrastructure → application → domain.
- `FF-LAY-02` (HexagonalLayersTest): Sem ciclos entre pacotes de primeiro nível.
- `FF-DOM-01` (DomainPurityTest): O domínio não depende de Spring, JPA, Hibernate, Lombok nem Jackson.
- `FF-PRT-01` (PortsTest): Tipos diretamente em port.in e port.out são interfaces.
- `FF-PRT-02` (PortsTest): Só casos de uso implementam portas de entrada.
- `FF-ADP-01` (AdaptersTest): Adapters de entrada não dependem de adapters de saída.
- `FF-ADP-02` (AdaptersTest): Controllers só em adapter.in.
- `FF-ADP-03` (AdaptersTest): Controllers falam com port.in, não com casos de uso nem repositórios.
- `FF-PER-01` (PersistenceTest): @Entity só no pacote de entidades de persistência.
- `FF-PER-02` (PersistenceTest): Repositórios Spring Data só no pacote jpa de persistência.
- `FF-COD-01` (CodingRulesTest): Sem @Autowired em campo; injeção por construtor.
- `FF-COD-02` (CodingRulesTest): Domínio e aplicação não têm @Service, @Component nem @Repository.
- `FF-COD-03` (CodingRulesTest): Sem uso de System.out nem System.err.

- Para aceitar as violações que já existem e barrar só as novas, envolva a regra com `FreezingArchRule.freeze(...)`.
- Para desligar um grupo de regras, remova a classe de teste correspondente.
# prumo
