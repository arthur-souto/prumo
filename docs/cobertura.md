# Cobertura de testes

Linha de base medida em 27/11/2026 (commit `de24a74`, PR #17), com `./mvnw clean verify`.

## Como medir

```shell
./mvnw clean verify
open target/site/jacoco/index.html
```

O JaCoCo instrumenta o surefire (testes `*Test`) e o failsafe (testes `*IT`, que sobem um PostgreSQL via Testcontainers e precisam do Docker rodando). Os dois gravam no mesmo `target/jacoco.exec`, então o relatório cobre os dois tipos. O CSV fica em `target/site/jacoco/jacoco.csv`.

> Até este PR os `*IT` não rodavam no `./mvnw verify`: não havia failsafe, e o surefire só pega `*Test`. Eles passaram a rodar agora (50 testes, todos passando).

No CI, cada execução mostra a cobertura do back e do front no resumo da página do GitHub Actions e guarda os relatórios completos como artefatos (veja *Integração contínua* no README).

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

### Evolução

| PR | Linhas | Branches | Testes (`*Test` + `*IT`) |
|---|---:|---:|---:|
| PRU-24 (linha de base) | 90,5% | 89,5% | 522 + 50 |
| PRU-25 (testes de serviço) | 92,7% | 89,5% | 528 + 59 |
| PRU-26 (testes de endpoint) | 98,6% | 91,8% | 536 + 67 |

### Lacunas que importam

O que falta coberto e tem regra de negócio ou permissão, por ordem de prioridade:

- ~~**Casos de uso sem teste:** `SyncWorkspacePullRequestsService`, `ListConnectedRepositoriesService`, `GetGithubConnectionService` e `RemoveGithubTokenService`.~~ Cobertos na PRU-25.
- ~~**Adapters de persistência do GitHub:** `ConnectedRepositoryPersistenceAdapter`, `GithubConnectionPersistenceAdapter` e `GithubMapper`.~~ Cobertos na PRU-25 (`GithubPersistenceIT`).
- ~~**Endpoints:** `MemberController`, edição/início/encerramento de sprint e `GlobalExceptionHandler`.~~ Cobertos na PRU-26.
- ~~**Cadeia de segurança e permissões ponta a ponta:**~~ cobertas na PRU-26 pelo `SecurityIT` (veja abaixo), inclusive o `LinkTokenAuthorizationRequestResolver`.
- **Agendadores:** `PullRequestPoller` e `SprintSnapshotRecorder` (0%). São finos e só delegam para casos de uso já testados.

- **`domain.model.project.ProjectRole`** (0%): não é usado em lugar nenhum. Candidato a remoção, fora do escopo dos testes.

### Segurança

Os testes de controller usam `MockMvcBuilders.standaloneSetup`, que **não passa pela cadeia de segurança**: eles recebem o usuário pronto e só verificam o mapeamento de status. Até a PRU-26 nada garantia que a API exige token ou que as rotas públicas continuam públicas.

O `SecurityIT` sobe a aplicação inteira (`@SpringBootTest`, PostgreSQL via Testcontainers e um par de chaves RSA gerado no teste, já que `keys/` não é versionado) e cobre:

- 401 sem token e com token malformado, assinado por outra chave, expirado ou de outro emissor;
- login pelo filtro JSON real, inclusive credenciais erradas;
- rotas públicas sem token: JWKS (sem a chave privada), `/actuator/health` e `/v3/api-docs` com o endpoint de login;
- permissões ponta a ponta: quem não é do workspace recebe 404 sem descobrir que ele existe, membro lê mas não altera (403), admin altera e só o dono exclui;
- o `link_token` do login com GitHub guardado na sessão e descartado num login comum.

Por subir a aplicação, ele também cobre `infrastructure.config` (wiring dos casos de uso, `SecurityConfig`, `JwtConfig`) e o `LoginOpenApiCustomizer`.

## Front end

Runner configurado na PRU-27: Vitest com jsdom e Testing Library (`npm run coverage` em `frontend/`). Como usar está no README, em *Testes do front*.

| PR | Statements | Branches | Linhas | Testes |
|---|---:|---:|---:|---:|
| PRU-27 (runner e testes de exemplo) | 2,5% | 2,8% | 2,6% | 18 |
| PRU-28 (fluxos críticos) | 35,0% | 35,4% | 35,5% | 72 |

Os testes de exemplo cobrem:
- as traduções: o português tem as mesmas chaves e os mesmos placeholders do inglês;
- `board-utils`;
- `sendJson`;
- o `ConfirmDialog`, que confirma que portais do Radix, teclado e i18n funcionam no jsdom.

### Fluxos críticos (PRU-28)

| Tela | Linhas | O que os testes garantem |
|---|---:|---|
| Backlog (`BacklogView`) | 79% | Página, ordem e filtros vêm da URL; edição em linha manda a história inteira; ações em lote dizem quantas falharam; exclusão pede confirmação; tentar de novo; estados vazios |
| Quadro (`BoardView`) | 53% | Cartões por coluna e filtrados pela URL; criar história (botão, atalho `c`, `?create=1`); abrir por `?story=`; mover pelo drawer; sincronizar PRs com erros por repositório; excluir coluna escolhendo o destino; ferramentas só para quem administra |
| Detalhe da história (`StoryDrawer`) | 80% | Salvar só com mudanças e no formato da API; erros por campo; confirmação ao descartar edições; status, responsável e sprint; excluir pelo menu; GitHub |
| Subtarefas (`Subtasks`) | 85% | Criar com e sem branch; branches em lote, uma por vez, mantendo selecionadas as que falharam; status e exclusão |
| Detalhe da subtarefa (`SubtaskView`) | 90% | Salvar; status; branch sugerida ou digitada e erro do GitHub; voltar ao quadro depois de excluir |

O que ficou de fora:

- **Arrastar e soltar** no quadro (o resto dos 47% do `BoardView`). O jsdom não calcula layout, então o dnd-kit não tem posições para trabalhar. Isso pede um teste de ponta a ponta no navegador (Playwright, por exemplo). Mover pelo drawer e pelo backlog está coberto.
- Telas de sprint, configurações e repositórios, que não fazem parte dos fluxos críticos desta história.

Achados durante os testes, ainda não corrigidos:

- ~~**Plural nas mensagens.** O i18n não trata plural, então aparece "1 branches created", "Delete 1 stories?" e "1 stories deleted". Em português acontece o mesmo.~~ Corrigido: as mensagens que dependem de um número têm forma de singular e de plural (veja *Idiomas* no README).
- **Nome do drawer para leitores de tela.** O `Drawer` passa `aria-label="Story details"`, mas o Radix também liga o `aria-labelledby` ao título, e o título vence. O leitor de tela anuncia só "PRU-2".
