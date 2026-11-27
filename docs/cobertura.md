# Cobertura de testes

Linha de base medida em 27/11/2026 (commit `de24a74`, PR #17), com `./mvnw clean verify`.

## Como medir

```shell
./mvnw clean verify
open target/site/jacoco/index.html
```

O JaCoCo instrumenta o surefire (testes `*Test`) e o failsafe (testes `*IT`, que sobem um PostgreSQL via Testcontainers e precisam do Docker rodando). Os dois gravam no mesmo `target/jacoco.exec`, então o relatório cobre os dois tipos. O CSV fica em `target/site/jacoco/jacoco.csv`.

> Até este PR os `*IT` não rodavam no `./mvnw verify`: não havia failsafe, e o surefire só pega `*Test`. Eles passaram a rodar agora (50 testes, todos passando).

## Back end

| | Testes | Falhas |
|---|---:|---:|
| Unitários e de slice (`*Test`) | 522 | 0 |
| Integração com PostgreSQL (`*IT`) | 50 | 0 |

| Métrica | Cobertura |
|---|---:|
| Linhas | **90,5%** (3.516 de 3.883) |
| Branches | **89,5%** |

### Por camada

| Pacote | Linhas | Branches |
|---|---:|---:|
| `domain.model.*` | ~99% | ~98% |
| `application.usecase` | 97,9% | 93,4% |
| `infrastructure.adapter.out.persistence.adapter` | 92,4% | 90,0% |
| `infrastructure.adapter.out.persistence.mapper` | 87,0% | 100% |
| `infrastructure.adapter.out.github` | 96,3% | 85,0% |
| `infrastructure.adapter.in.web.controller` | 95,2% | **50,0%** |
| `infrastructure.adapter.in.web` (`GlobalExceptionHandler`) | 59,3% | 61,1% |
| `infrastructure.adapter.in.web.security.oauth` | 69,0% | 30,0% |
| `infrastructure.adapter.in.scheduler` | 0% | — |
| `infrastructure.config` | 1,8% | 0% |

### Lacunas que importam

O que falta coberto e tem regra de negócio ou permissão, por ordem de prioridade:

- **Casos de uso sem teste:** `SyncWorkspacePullRequestsService`, `ListConnectedRepositoriesService`, `GetGithubConnectionService` e `RemoveGithubTokenService` (0%). Entram na PRU-25.
- **Adapters de persistência do GitHub:** `ConnectedRepositoryPersistenceAdapter`, `GithubConnectionPersistenceAdapter` e `GithubMapper` (0%). Hoje nenhum `*IT` passa por eles. Entram na PRU-25.
- **Endpoints:** `MemberController` (0%) e os caminhos de erro e permissão dos controllers (50% de branches), além de `GlobalExceptionHandler` (59%). Entram na PRU-26.
- **OAuth:** `LinkTokenAuthorizationRequestResolver` (0%), usado ao vincular o GitHub a uma conta existente. Entra na PRU-26.
- **Agendadores:** `PullRequestPoller` e `SprintSnapshotRecorder` (0%). São finos e só delegam para casos de uso já testados.

Não vale a pena perseguir cobertura em `infrastructure.config` (wiring de beans, `SecurityConfig`, `JwtConfig`), `LoginOpenApiCustomizer` e `PrumoApplication`. Eles são exercitados indiretamente quando a aplicação sobe, e a cadeia de segurança já é coberta pelos testes de controller com `spring-security-test`.

## Front end

**Sem testes e sem runner configurado** (0% de ~10,7 mil linhas em `frontend/src`). O `package.json` só tem `typecheck`. O runner entra na PRU-27 e os testes dos fluxos críticos (backlog, quadro, detalhe) na PRU-28.
