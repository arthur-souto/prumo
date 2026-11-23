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
