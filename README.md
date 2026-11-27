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

### Configuração do login com GitHub

O login usa OAuth com o GitHub. Cada instância do Prumo precisa do próprio OAuth App:

1. Acesse https://github.com/settings/developers e clique em **New OAuth App**.
2. Preencha:
   - **Homepage URL:** `http://localhost:8080`
   - **Authorization callback URL:** `http://localhost:8080/login/oauth2/code/github`
3. Registre o app, copie o **Client ID** e gere um **Client secret**.
4. Copie `.env.example` para `.env` e preencha:

```shell
GITHUB_CLIENT_ID=<client id>
GITHUB_CLIENT_SECRET=<client secret>
```

O `.env` não é versionado e é lido automaticamente pela aplicação ao iniciar pela raiz do projeto. Sem essas duas variáveis a aplicação não sobe. Em produção, use outro OAuth App com a URL e o callback do domínio real.

### Chaves JWT

Os access tokens são JWT assinados com RS256. Gere o par de chaves uma vez (a pasta `keys/` não é versionada):

```shell
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem
openssl rsa -pubout -in keys/private.pem -out keys/public.pem
```

Outros caminhos podem ser definidos em `JWT_PRIVATE_KEY_PATH` e `JWT_PUBLIC_KEY_PATH`. Sem as chaves a aplicação não sobe. Para validar tokens em outros serviços, use a chave pública publicada em `/.well-known/jwks.json`.

### Autenticação

| Endpoint | Descrição |
|---|---|
| `POST /api/v1/auth/register` | Cria uma conta local |
| `POST /api/v1/auth/login` | Retorna `access_token`, `expires_in` e `refresh_token` |
| `POST /api/v1/auth/refresh` | Recebe `{"refresh_token": "..."}`, rotaciona o refresh token e retorna novos tokens |
| `POST /api/v1/auth/logout` | Recebe `{"refresh_token": "..."}` e revoga o refresh token |
| `GET /api/v1/users/me` | Retorna o usuário do access token (exige `Authorization: Bearer`) |

O access token dura 15 minutos e o refresh token 7 dias, ambos configuráveis. Reusar um refresh token já rotacionado revoga toda a sequência de tokens daquela sessão. No Swagger (`/swagger-ui.html`), use o botão **Authorize** com o `access_token` do login para chamar as rotas protegidas.

### Login com GitHub

1. O front envia o usuário para `GET /oauth2/authorization/github`.
2. Depois de autorizar, o GitHub volta para a aplicação, que busca ou cria o usuário (pelo e-mail verificado principal) e redireciona para `FRONTEND_OAUTH_CALLBACK_URL?code=<código de uso único>`. O código vale 60 segundos e só pode ser usado uma vez.
3. O front troca o código por tokens em `POST /api/v1/auth/oauth/exchange` com `{"code": "..."}`, e recebe o mesmo `access_token`, `expires_in` e `refresh_token` do login local.

Se o login for recusado, o redirect traz `?error=` com um destes valores:

| Erro | Motivo |
|---|---|
| `email_already_registered` | O e-mail já pertence a outra conta. Não há vínculo automático por e-mail, para evitar que alguém assuma uma conta criada com o e-mail de outra pessoa. Entre com a senha e vincule o GitHub (veja abaixo) |
| `email_unavailable` | A conta do GitHub não tem e-mail verificado |
| `account_disabled` | A conta está desativada |
| `oauth_failed` | Falha na autorização com o GitHub |
| `unsupported_provider` | Provedor sem suporte |

### Vincular o GitHub a uma conta existente

Quem já tem conta (por exemplo, criada com senha) vincula o GitHub estando logado, o que prova que a conta é dele:

1. `POST /api/v1/users/me/identities/github/link` (com `Authorization: Bearer`) retorna `{"authorization_path": "/oauth2/authorization/github?link_token=..."}`. O token vale 60 segundos e só pode ser usado uma vez.
2. O front envia o navegador para esse caminho. Depois de autorizar, o redirect para o front traz `?linked=github` ou `?error=<motivo>` (`link_token_invalid`, `identity_already_linked`, `provider_already_linked`).
3. Dali em diante, o login pelo GitHub entra nessa mesma conta.

| Endpoint | Descrição |
|---|---|
| `GET /api/v1/users/me/identities` | Lista os métodos externos vinculados |
| `POST /api/v1/users/me/identities/{provider}/link` | Inicia o vínculo |
| `DELETE /api/v1/users/me/identities/{provider}` | Desvincula. Recusado (422) se for o único jeito de entrar na conta |

### Workspaces

| Endpoint | Descrição |
|---|---|
| `POST /api/v1/workspaces` | Cria um workspace (`name`, `description` opcional). Quem cria vira `OWNER`. O `slug` é gerado do nome |
| `GET /api/v1/workspaces` | Lista os workspaces do usuário, do mais novo para o mais antigo |
| `GET /api/v1/workspaces/{id}` | Busca um workspace. Quem não é membro recebe 404 |
| `PUT /api/v1/workspaces/{id}` | Altera nome e descrição (o slug não muda). `OWNER` e `ADMIN`; `MEMBER` recebe 403 |
| `DELETE /api/v1/workspaces/{id}` | Exclui o workspace e seus vínculos. Só o `OWNER`; os demais recebem 403 |

Papéis por workspace: `OWNER`, `ADMIN` e `MEMBER`. Quem não é membro sempre recebe 404, para não revelar que o workspace existe. Excluir um usuário que é dono de workspace é bloqueado pelo banco: o workspace precisa ser excluído antes.

No front, `/` lista os workspaces (com busca e o botão **Create workspace**) e `/workspaces/{id}` mostra os detalhes, as configurações e a exclusão com confirmação pelo nome.

### User stories e quadro Kanban

Cada workspace tem um quadro criado no primeiro acesso, com as colunas *To Do*, *In Progress*, *In Review* e *Done* e um prefixo de chave derivado do nome (ex.: `PRU`, único no sistema). As histórias ganham chaves legíveis (`PRU-12`), numeradas de forma atômica no banco.

| Endpoint (base `/api/v1/workspaces/{id}`) | Descrição |
|---|---|
| `GET /board` | Colunas em ordem e todas as histórias |
| `PUT /board/settings` | Prefixo da chave (travado depois da primeira história), automação com o GitHub e consolidação das histórias (`consolidateStories`). `OWNER`/`ADMIN` |
| `POST /board/columns`, `PUT /board/columns/{id}`, `PUT /board/columns/order`, `DELETE /board/columns/{id}?moveTo=` | Gerência das colunas. `OWNER`/`ADMIN` |
| `POST /stories`, `GET /stories?label=&priority=&q=` | Cria e lista (filtros por label, prioridade e texto) |
| `GET/PUT/DELETE /stories/{key}` | Detalhe, edição e exclusão |
| `PATCH /stories/{key}/checklist/{itemId}` | Marca um critério de aceite |
| `PUT /stories/{key}/assignee` | Define ou remove o responsável (`assigneeId`, um membro do workspace) |
| `GET /members` | Membros do workspace, com `avatarUrl` (foto do GitHub, quando o usuário entrou por ele) |
| `PUT /stories/{key}/move` | Move para uma coluna entre dois vizinhos (`afterStoryId`/`beforeStoryId`); grava só o card movido |

A ordem dos cards usa rank fracionário em base 62 (`Rank`), então mover um card não reescreve os outros; se um rank esgotar, a coluna é reequilibrada. Qualquer membro trabalha com histórias; estrutura do quadro exige `OWNER`/`ADMIN`. Quem não é membro recebe 404. No front, o quadro fica em `/workspaces/{id}/board` (arrastar e soltar, diálogo de detalhe, filtros e busca). Os cards mostram descrição, responsável, progresso das subtarefas (`2/5`) e indicador de PR.

### Subtarefas

Uma história se divide em **subtarefas**, e é a subtarefa (não a história) que recebe a branch, como no Jira. Cada subtarefa é uma entidade própria: chave na mesma sequência do workspace (história `PRU-12`, subtarefa `PRU-13`), título, descrição em Markdown opcional, ordem dentro da história e status próprio (`TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`), independente das colunas do quadro. Não têm responsável nem pontos.

| Endpoint (base `/api/v1/workspaces/{id}`) | Descrição |
|---|---|
| `POST /stories/{key}/subtasks` | Cria. Com `createBranch: true` e `branchName`, cria a branch no repositório vinculado à história (a branch é criada no GitHub antes de salvar, então uma falha não deixa subtarefa órfã) |
| `GET /stories/{key}/subtasks` | Lista as subtarefas da história |
| `GET/PUT/DELETE /subtasks/{key}` | Detalhe, edição (título e descrição) e exclusão |
| `PUT /subtasks/{key}/status` | Muda o status |
| `GET /subtasks/{key}/branch-suggestion` | Sugere `feature/PRU-13-titulo` |
| `POST /subtasks/{key}/branch` | Cria a branch depois. Uma subtarefa tem no máximo uma; exige repositório vinculado à história |

No front, as subtarefas ficam no diálogo da história (status inline, branch com link e botão de copiar, PRs com o estado, e "Adicionar subtarefa" com a opção **Criar branch**) e têm página própria em `/workspaces/{id}/subtasks/{key}`. Criar uma história não cria mais branch.

### Integração com o GitHub

Cada usuário salva um **personal access token** (escopo `repo`) em `PUT /api/v1/github/token`. O token é validado no GitHub, guardado cifrado (AES-GCM) e nunca volta em nenhuma resposta nem em log. Defina a chave de cifra antes de usar:

```shell
export PRUMO_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

Sem ela a API responde `503 NOT_CONFIGURED` nas rotas do GitHub; o resto do sistema funciona.

- `GET /api/v1/github/repositories`: repositórios acessíveis ao token.
- `POST /workspaces/{id}/repositories` (`OWNER`/`ADMIN`): conecta um repositório ao workspace; `GET` lista e `DELETE /{repoId}` desconecta; `POST /repositories/sync` força a sincronização.
- Na história: `PUT /stories/{key}/repository` (vincula o repositório, que é onde as branches das subtarefas são criadas) e `POST /stories/{key}/new-repository`. A criação de branch fica nas subtarefas (veja acima).

Como o `localhost` não recebe webhooks, os PRs são detectados por **polling** (ETag, então respostas 304 não gastam limite): um PR é associado pela chave que aparece no nome da branch ou no título. Se a chave é de uma **subtarefa**, o PR fica nela e também aparece na história; se cita só a chave da história, fica só na história. A automação é opcional e vem desligada: PR aberto leva a subtarefa para `IN_REVIEW` e PR mergeado para `DONE`. PR fechado sem merge devolve a subtarefa para `IN_PROGRESS`, mas só se ela estiver em `IN_REVIEW` e nenhum outro PR dela ainda estiver aberto (uma subtarefa já concluída ou sem PR em revisão não é mexida). PRs que citam só a história seguem a mesma regra entre as colunas *In Review*, *In Progress* e *Done*. Só vale na mudança de estado, então algo movido à mão não é puxado de volta. Com a **consolidação** ligada (`consolidateStories`, desligada por padrão), o evento que deixa todas as subtarefas em `DONE` move a história para *Done*; mover a história manualmente sempre prevalece. Erros do GitHub voltam com `code` estável (`MISSING_TOKEN`, `INVALID_TOKEN`, `RATE_LIMITED`, `NOT_FOUND`, `BRANCH_EXISTS`, `UNAVAILABLE`...) e não quebram o quadro.

| Variável | Padrão | Uso |
|---|---|---|
| `PRUMO_ENCRYPTION_KEY` | vazio | Chave AES de 32 bytes em Base64 |
| `GITHUB_POLLING_ENABLED` | `true` | Liga o polling |
| `GITHUB_POLLING_INTERVAL` | `60s` | Intervalo do polling |

Como usar pela interface:

1. Gere um token em https://github.com/settings/tokens (escopo `repo`) e salve em **Configurações** (`/settings`). Ele vale para todos os seus workspaces.
2. No workspace, abra **Repositórios** e conecte os repositórios que o time usa.
3. Na história, escolha o **Repositório** no painel lateral (ou crie um novo).
4. Ao adicionar uma subtarefa, marque **Criar branch** (o nome sugerido usa a chave da subtarefa e pode ser editado), ou crie a branch depois na própria subtarefa. Se a história não tem repositório, a opção fica desabilitada.
5. Abra o PR com a chave da subtarefa (`PRU-13`) no nome da branch ou no título. Ele aparece na subtarefa e na história em até um minuto.

### Migração para subtarefas

A migration `V11` converte os dados existentes: cada item do checklist de subtarefas vira uma subtarefa (`DONE` se estava marcado, senão `TO_DO`) com chave nova, sem colidir com as existentes; história que tinha branch ganha a subtarefa "Implementação inicial" com a branch e os PRs dessa branch (status conforme os PRs: mergeado `DONE`, aberto `IN_REVIEW`, senão `IN_PROGRESS`); os PRs de outras branches continuam na história. Nada fica órfão. O Flyway Community não tem `undo`, então o script de reversão manual está em `src/main/resources/db/undo/U11__create_story_subtasks.sql` (rode-o e remova a linha `11` de `flyway_schema_history`). A migração é coberta por `StorySubtaskMigrationIT`.

### Front end (Next.js)

O front fica em `frontend/` e funciona como BFF: os tokens ficam em cookies `httpOnly` e nunca chegam ao JavaScript do navegador. Todo o design vem de `frontend/src/styles/token.css`.

```shell
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Telas: `/login`, `/register`, `/` (workspaces), `/settings` (token do GitHub), `/workspaces/{id}` (configurações), `/board`, `/backlog`, `/repositories` e `/subtasks/{key}`. O callback do GitHub é `/auth/callback`, então mantenha `FRONTEND_OAUTH_CALLBACK_URL=http://localhost:3000/auth/callback` no backend (para outro endereço, veja [docs/rede-local.md](docs/rede-local.md)). O front encaminha `/oauth2/*` e `/login/oauth2/*` ao backend, por isso o login com GitHub começa e termina na origem do front.

#### Acesso pela rede local (opcional)

Por padrão tudo responde só em `localhost`: o front (`npm run dev`), o backend (`SERVER_ADDRESS=127.0.0.1`) e o banco (`POSTGRES_BIND=127.0.0.1`). Para outros computadores da rede usarem o app, rode `npm run dev:lan` e abra o endereço `Network: http://<ip>:3000` que o Next mostra. O login com GitHub a partir de outro computador precisa de configuração extra. Para desfazer, volte a usar `npm run dev`. Para alternar rápido entre o modo local e o de rede (credenciais OAuth e URLs incluídas) use `scripts/mode.sh local|lan|status`. O passo a passo, todas as variáveis e a solução de problemas estão em [docs/rede-local.md](docs/rede-local.md).

### Idiomas

O front fala inglês e português. O idioma vem do cookie `prumo_lang`; na primeira visita é escolhido pelo `Accept-Language` do navegador (qualquer variante de `pt` vira português, o resto vira inglês). O seletor fica no rodapé do login/cadastro e na barra superior. O BFF repassa o idioma ao backend, então as mensagens de validação também mudam. Os textos ficam em `frontend/src/i18n/en.ts` e `pt.ts`: para adicionar um idioma, crie o catálogo novo (o TypeScript exige as mesmas chaves do inglês) e registre-o em `core.ts`.

### Banco de dados

Suba o PostgreSQL local (usa o mesmo `.env`):

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

O `verify` roda os testes unitários (`*Test`) e os de integração (`*IT`, que precisam do Docker para o Testcontainers) e gera o relatório de cobertura em `target/site/jacoco/index.html`. A linha de base e as lacunas estão em [docs/cobertura.md](docs/cobertura.md).

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

## Licença

Este projeto é distribuído sob a [licença MIT](LICENSE).
