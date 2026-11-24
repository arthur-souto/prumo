# Acesso pela rede local

Por padrão o Prumo só responde no computador em que está rodando. Este guia mostra, passo a passo, como deixar outros computadores da rede usarem o app (inclusive o login com GitHub) e como voltar ao modo restrito quando quiser.

- [Como funciona](#como-funciona)
- [Configuração inicial (uma vez)](#configuração-inicial-uma-vez)
- [Uso no dia a dia](#uso-no-dia-a-dia)
- [Rodando o backend pela IDE](#rodando-o-backend-pela-ide)
- [Se o nome ou o IP mudar](#se-o-nome-ou-o-ip-mudar)
- [Sem o script (manual)](#sem-o-script-manual)
- [Referência das configurações](#referência-das-configurações)
- [Problemas comuns](#problemas-comuns)
- [Segurança](#segurança)

## Como funciona

```
outro computador ──► Next (3000) ──► backend (8080) ──► PostgreSQL (5433)
                      front + proxy     só localhost       só localhost
```

O navegador só conversa com o front (Next). Quem fala com o backend e com o banco é o servidor do Next, dentro do computador que roda o Prumo. Por isso **só o front é exposto**. O login com GitHub também passa pelo front (`/oauth2/*` e `/login/oauth2/*` são encaminhados ao backend em `frontend/next.config.ts`).

Existem dois modos:

| Modo | Quem acessa | Login com GitHub | Como subir o front |
| --- | --- | --- | --- |
| `local` (padrão) | só este computador, em `http://localhost:3000` | app OAuth de `localhost` | `npm run dev` |
| `lan` | qualquer computador da rede, em `http://<host>:3000` | app OAuth da rede | `npm run dev:lan` |

`<host>` é o nome (`MeuMac.local`) ou o IP do computador que roda o Prumo. Login por e-mail e senha funciona nos dois modos sem nenhuma configuração extra. As etapas de OAuth abaixo só são necessárias para entrar com GitHub.

O script `scripts/mode.sh` troca de modo com um comando. Ele usa bash, então no Windows rode pelo WSL ou pelo Git Bash.

## Configuração inicial (uma vez)

### 1. Descobrir o endereço do computador

Escolha **um** endereço e use o mesmo em todo o guia.

**Nome na rede (recomendado)**, que não muda quando o IP muda:

| Sistema | Como descobrir | Resultado |
| --- | --- | --- |
| macOS | `scutil --get LocalHostName` | `MeuMac` → use `MeuMac.local` |
| Linux | `hostname` (precisa do `avahi-daemon`) | `meupc` → use `meupc.local` |
| Windows | `hostname` | `MEUPC` → use `MEUPC` ou `MEUPC.local` |

Nomes `.local` funcionam em macOS, iOS, Windows 10 ou superior e Linux com avahi. Alguns celulares Android não resolvem. Quem não resolver o nome ainda entra pelo IP, mas o login com GitHub só funciona pelo endereço cadastrado no app OAuth.

**IP fixo:** descubra com `ipconfig getifaddr en0` (macOS), `hostname -I` (Linux) ou `ipconfig` (Windows). O IP costuma mudar, então reserve-o no roteador (procure por "DHCP reservation", "Reserva de IP" ou "IP estático"). Se não reservar, terá de atualizar o app OAuth e o perfil toda vez que ele mudar.

### 2. Criar dois apps OAuth no GitHub

Um app OAuth do GitHub aceita **uma única** URL de callback. Por isso são dois apps: um para `localhost` e outro para a rede. Se você só vai usar o modo `local`, crie apenas o primeiro. Para criar cada um, abra https://github.com/settings/developers, vá em **OAuth Apps** e clique em **New OAuth App**.

**App de localhost**

| Campo | Valor |
| --- | --- |
| Application name | `Prumo (local)` |
| Homepage URL | `http://localhost:3000` |
| Authorization callback URL | `http://localhost:8080/login/oauth2/code/github` |

**App da rede**

| Campo | Valor |
| --- | --- |
| Application name | `Prumo (rede)` |
| Homepage URL | `http://<host>:3000` |
| Authorization callback URL | `http://<host>:3000/login/oauth2/code/github` |

Depois de registrar cada app, copie o **Client ID** e clique em **Generate a new client secret**. O GitHub mostra o secret uma única vez, então copie na hora e guarde em local seguro.

### 3. Criar os perfis

Cada modo tem um perfil na raiz do projeto, que **não é versionado** porque guarda secrets. Copie os modelos:

```shell
cp scripts/profiles/local.env.example .env.mode.local
cp scripts/profiles/lan.env.example .env.mode.lan
```

Edite os dois e preencha.

`.env.mode.local`:

```properties
GITHUB_CLIENT_ID=<client id do app de localhost>
GITHUB_CLIENT_SECRET=<client secret do app de localhost>
FRONTEND_OAUTH_CALLBACK_URL=http://localhost:3000/auth/callback
```

`.env.mode.lan` (troque `<host>` pelo endereço escolhido no passo 1):

```properties
GITHUB_CLIENT_ID=<client id do app da rede>
GITHUB_CLIENT_SECRET=<client secret do app da rede>
OAUTH_REDIRECT_URI=http://<host>:3000/login/oauth2/code/github
FRONTEND_OAUTH_CALLBACK_URL=http://<host>:3000/auth/callback
DEV_ALLOWED_ORIGINS=<host>
```

Em `DEV_ALLOWED_ORIGINS` vai só o nome ou o IP, sem `http://` e sem porta.

### 4. Aplicar o modo e subir

Antes, garanta que existe o `.env` do backend (`cp .env.example .env`, se ainda não existir).

```shell
scripts/mode.sh lan
```

O script escreve as variáveis do perfil num bloco marcado de `.env` e de `frontend/.env.local`. Em seguida:

1. **Reinicie o backend.** Ele só lê o `.env` ao iniciar.
2. **Suba o front em modo rede:** `cd frontend && npm run dev:lan`. O Next mostra a linha `Network: http://...:3000`.
3. **Recrie o banco uma vez**, para ele escutar só em `localhost`: `docker compose up -d --force-recreate`. Os dados ficam preservados, porque estão no volume.

### 5. Testar

1. No próprio computador, abra `http://<host>:3000/login` (com o nome, não `localhost`) e entre com e-mail e senha.
2. Ainda nessa URL, clique em entrar com GitHub. Você deve passar pelo GitHub e voltar logado.
3. Em outro computador da rede, abra a mesma URL e repita.

Se algo falhar, veja [Problemas comuns](#problemas-comuns).

## Uso no dia a dia

```shell
scripts/mode.sh lan      # liberar a rede
scripts/mode.sh local    # voltar ao modo restrito
scripts/mode.sh status   # ver o modo ativo (secrets mascarados)
```

Depois de trocar de modo, **reinicie o backend** e suba o front com o comando que o script indica: `npm run dev:lan` no modo `lan`, `npm run dev` no modo `local`.

Para **desfazer a rede por completo**, rode `scripts/mode.sh local`, reinicie o backend e use `npm run dev`. Nada do modo rede fica gravado no código.

**Como o script trabalha**
- Escreve as variáveis do perfil entre `# >>> prumo-mode` e `# <<< prumo-mode`, em `.env` e `frontend/.env.local`, e não mexe no resto desses arquivos.
- Guarda cópias em `.env.bak` e `frontend/.env.local.bak` a cada troca.
- Remove do `.env` as variáveis do modo escritas soltas (fora do bloco), porque numa lista de propriedades a última linha vence e elas passariam por cima do perfil.
- Recusa aplicar um perfil que tenha variável obrigatória vazia ou ausente.

| Perfil | Variáveis | Vai para |
| --- | --- | --- |
| `.env.mode.local` | `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `FRONTEND_OAUTH_CALLBACK_URL` | `.env` |
| `.env.mode.lan` | as três acima, mais `OAUTH_REDIRECT_URI` | `.env` |
| `.env.mode.lan` | `DEV_ALLOWED_ORIGINS` | `frontend/.env.local` |

## Rodando o backend pela IDE

O backend lê o `.env` da raiz do projeto, mas **variáveis de ambiente têm prioridade sobre ele**. Se a configuração de execução da IDE (ou o seu shell) define `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` ou qualquer outra variável que o `.env` também tem, o valor da IDE vence e `scripts/mode.sh` não consegue trocar de modo. Sintoma típico: no modo `lan` o GitHub recusa o login, porque a IDE continua enviando as credenciais do app de `localhost`.

**Deixe as variáveis só no `.env`.** No IntelliJ IDEA:

1. **Run → Edit Configurations…** e selecione a configuração do Spring Boot (`PrumoApplication`).
2. Em **Environment variables**, apague o conteúdo. Se o campo estiver recolhido, abra com *Modify options → Environment variables*.
3. Confirme que **Working directory** está vazio ou aponta para a raiz do projeto. É dela que o backend lê o `.env` e o caminho `./keys`.
4. Clique em **OK** e reinicie o backend.

Antes de apagar, confira que o `.env` já tem tudo o que estava na IDE: `JWT_*`, `REFRESH_COOKIE_SECURE`, `PRUMO_ENCRYPTION_KEY` (32 bytes em Base64, valide com `grep '^PRUMO_ENCRYPTION_KEY=' .env | cut -d= -f2- | base64 -d | wc -c`) e as credenciais do GitHub, que o script grava a partir do perfil do modo ativo. Os modelos de cada variável estão em `.env.example`.

`scripts/mode.sh` avisa quando encontra essas variáveis na configuração da IDE ou no shell, tanto ao trocar de modo quanto em `scripts/mode.sh status`. Para ver o que o backend realmente está usando, depois de iniciá-lo:

```shell
curl -s -o /dev/null -D - http://localhost:8080/oauth2/authorization/github | grep -i '^location' | grep -o 'redirect_uri=[^&]*'
```

No modo `lan` deve aparecer o endereço da rede, e no modo `local` deve aparecer `http://localhost:8080/login/oauth2/code/github`.

## Se o nome ou o IP mudar

1. No app OAuth da rede (GitHub → Settings do app), atualize a **Homepage URL** e a **Authorization callback URL**.
2. Edite o `.env.mode.lan`: `OAUTH_REDIRECT_URI`, `FRONTEND_OAUTH_CALLBACK_URL` e `DEV_ALLOWED_ORIGINS`.
3. Rode `scripts/mode.sh lan`, reinicie o backend e o front.

Usar nome `.local` ou reservar o IP no roteador evita esse trabalho.

## Sem o script (manual)

O script só automatiza a edição de dois arquivos. Para fazer à mão, no modo rede:

```properties
# .env (backend)
GITHUB_CLIENT_ID=<app da rede>
GITHUB_CLIENT_SECRET=<app da rede>
OAUTH_REDIRECT_URI=http://<host>:3000/login/oauth2/code/github
FRONTEND_OAUTH_CALLBACK_URL=http://<host>:3000/auth/callback

# frontend/.env.local
DEV_ALLOWED_ORIGINS=<host>
```

Suba o front com `npm run dev:lan`. Para voltar ao local, restaure as credenciais do app de `localhost`, apague `OAUTH_REDIRECT_URI` e `DEV_ALLOWED_ORIGINS`, ponha `FRONTEND_OAUTH_CALLBACK_URL=http://localhost:3000/auth/callback` e suba o front com `npm run dev`.

## Referência das configurações

### Front end (`frontend/.env.local`, modelo em `frontend/.env.example`)

| Configuração | Padrão | Para que serve |
| --- | --- | --- |
| `npm run dev` | escuta em `localhost` | Uso normal, só neste computador. |
| `npm run dev:lan` | escuta em `0.0.0.0` com `LAN_ACCESS=1` | Libera as redes privadas `192.168.*`, `10.*` e `172.*`. |
| `DEV_ALLOWED_ORIGINS` | vazio | Nomes ou IPs extras liberados em dev, separados por vírgula. Cada `*` vale um único trecho do nome (`192.168.1.*`). Necessária para acessar por nome (`MeuMac.local`), porque o `dev:lan` só libera IPs. |
| `BACKEND_URL` | `http://localhost:8080` | Onde o servidor do Next encontra o backend. Também é o destino do proxy de `/oauth2`. |

### Backend (`.env` na raiz, modelo em `.env.example`)

| Variável | Padrão | Para que serve |
| --- | --- | --- |
| `SERVER_ADDRESS` | `127.0.0.1` | Interface em que o backend escuta. `0.0.0.0` abre para toda a rede. Só mude se algo fora deste computador precisar chamar a API direto. |
| `SERVER_PORT` | `8080` | Porta do backend. Se mudar, ajuste `BACKEND_URL` no front. |
| `OAUTH_REDIRECT_URI` | segue o host em que o backend foi acessado | URL de callback enviada ao GitHub. Definida só no modo rede. |
| `FRONTEND_OAUTH_CALLBACK_URL` | `http://localhost:3000/auth/callback` | Para onde o backend manda o navegador ao fim do login com GitHub. Deve ser o mesmo endereço que o usuário abriu no navegador. |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | sem padrão | Credenciais do app OAuth em uso. |

### Banco (docker compose)

| Variável | Padrão | Para que serve |
| --- | --- | --- |
| `POSTGRES_BIND` | `127.0.0.1` | Interface publicada pelo contêiner. `0.0.0.0` abre o banco para a rede (não recomendado). |
| `POSTGRES_PORT` | `5433` | Porta publicada no computador. |

Se mudar `POSTGRES_BIND`, recrie o contêiner: `docker compose up -d --force-recreate`.

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
| --- | --- | --- |
| `scripts/mode.sh` diz que o perfil não foi encontrado | Falta o `.env.mode.<modo>` | Copie o modelo de `scripts/profiles/` e preencha (passo 3). |
| `scripts/mode.sh` diz que uma variável está vazia | Perfil incompleto | Preencha a variável indicada no perfil. |
| A página abre no outro computador mas os botões não respondem | O endereço não está liberado em dev | Suba com `npm run dev:lan` e confira o `DEV_ALLOWED_ORIGINS` (inclusive se você acessa por nome). Reinicie o front. |
| Não abre de jeito nenhum | Front rodando com `npm run dev`, ou firewall | Use `npm run dev:lan`. No macOS, permita conexões de entrada para o `node` em Ajustes do Sistema → Rede → Firewall. |
| O outro computador não resolve o nome `.local` | O aparelho não suporta mDNS | Acesse pelo IP, ou use um dispositivo compatível. |
| O GitHub mostra `redirect_uri_mismatch` | `OAUTH_REDIRECT_URI` diferente do callback cadastrado no app | As duas URLs precisam ser idênticas, incluindo nome ou IP e a porta `:3000`. |
| Depois do GitHub volta para `localhost` | `FRONTEND_OAUTH_CALLBACK_URL` aponta para `localhost`, ou o modo ativo é `local` | Rode `scripts/mode.sh status` e reinicie o backend depois de trocar. |
| O GitHub recusa o login no modo `lan` mesmo com tudo configurado | A IDE ou o shell sobrescreve `GITHUB_CLIENT_ID` e `GITHUB_CLIENT_SECRET` | Rode `scripts/mode.sh status`: ele avisa. Veja [Rodando o backend pela IDE](#rodando-o-backend-pela-ide). |
| O login com GitHub não funciona em `localhost:3000` | O modo `lan` está ativo (o callback aponta para a rede) | Rode `scripts/mode.sh local` e reinicie o backend, ou abra o app pelo endereço do modo `lan`. |
| O front não alcança o backend | `BACKEND_URL` aponta para outro endereço, ou `localhost` resolve para IPv6 | Use `BACKEND_URL=http://127.0.0.1:8080` em `frontend/.env.local`. |

## Segurança

- Em rede de casa ou de trabalho com gente desconhecida, ligue o modo `lan` só enquanto precisar e volte ao `local` depois.
- Este modo é para desenvolvimento. O `next dev` não deve ficar exposto em rede pública.
- O backend e o banco ficam restritos a `localhost` por padrão. Abri-los para a rede expõe a API e os dados diretamente, sem passar pelo front.
- Os perfis `.env.mode.*` e as cópias `.env*.bak` têm secrets. Estão no `.gitignore`: não faça commit deles nem os compartilhe em chat.
- Se um client secret vazar, gere outro no app OAuth do GitHub (**Generate a new client secret**) e apague o antigo.
