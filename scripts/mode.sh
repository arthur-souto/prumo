#!/usr/bin/env bash
# Alterna o ambiente de desenvolvimento entre "local" (só esta máquina) e "lan" (outros computadores da rede).
# Veja docs/rede-local.md.
#
#   scripts/mode.sh local    # usa o app OAuth de localhost
#   scripts/mode.sh lan      # usa o app OAuth da rede
#   scripts/mode.sh status   # mostra o modo ativo
#
# Cada modo tem um perfil em .env.mode.<modo> (não versionado, tem secrets). O script escreve as variáveis do perfil
# num bloco marcado do .env (backend) e do frontend/.env.local. O resto desses arquivos não é tocado.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_ENV="$ROOT/.env"
FRONTEND_ENV="$ROOT/frontend/.env.local"

START="# >>> prumo-mode (gerenciado por scripts/mode.sh, não edite este bloco)"
END="# <<< prumo-mode"

BACKEND_KEYS="GITHUB_CLIENT_ID|GITHUB_CLIENT_SECRET|OAUTH_REDIRECT_URI|FRONTEND_OAUTH_CALLBACK_URL"
FRONTEND_KEYS="DEV_ALLOWED_ORIGINS"

die() { echo "erro: $*" >&2; exit 1; }

current_mode() {
  [ -f "$BACKEND_ENV" ] && sed -n 's/^# modo: //p' "$BACKEND_ENV" | head -1 || true
}

# Arquivo sem o bloco gerenciado e sem as chaves do modo escritas soltas (elas venceriam o perfil).
stripped() {
  awk -v s="$START" -v e="$END" -v keys="^($2)=" '
    $0 == s { skip = 1; next }
    $0 == e { skip = 0; next }
    skip { next }
    $0 ~ keys { next }
    { print }
  ' "$1" | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}'
}

write_block() { # arquivo chaves modo perfil
  local file="$1" keys="$2" mode="$3" profile="$4" body
  body="$(grep -E "^($keys)=" "$profile" || true)"
  {
    stripped "$file" "$keys"
    printf '\n%s\n# modo: %s\n' "$START" "$mode"
    [ -n "$body" ] && printf '%s\n' "$body"
    printf '%s\n' "$END"
  } > "$file.tmp"
  mv "$file.tmp" "$file"
}

block_of() { # arquivo chaves
  awk -v s="$START" -v e="$END" -v keys="^($2)=" '
    $0 == s { inside = 1; next }
    $0 == e { inside = 0; next }
    inside && $0 ~ keys { print "  " $0 }
  ' "$1" 2>/dev/null || true
}

# Variáveis de ambiente do sistema e da IDE vencem o .env. Se alguma das chaves do modo estiver definida lá,
# a troca de modo não surte efeito, então avisamos.
warn_overrides() {
  local found=0 key ide_file="$ROOT/.idea/workspace.xml"
  for key in ${BACKEND_KEYS//|/ } ${FRONTEND_KEYS//|/}; do
    if [ -n "${!key:-}" ]; then
      echo "aviso: $key está definida no seu shell e vai sobrescrever o .env (remova do ~/.zshrc ou do export atual)." >&2
      found=1
    fi
    if [ -f "$ide_file" ] && grep -q "<env name=\"$key\"" "$ide_file"; then
      echo "aviso: $key está definida na configuração de execução da IDE e vai sobrescrever o .env." >&2
      found=1
    fi
  done
  if [ "$found" = 1 ]; then
    echo "       Deixe as variáveis só no .env: na IDE, Run > Edit Configurations > Environment variables > apague." >&2
    echo "       Passo a passo em docs/rede-local.md (\"Rodando pela IDE\")." >&2
  fi
}

show_status() {
  local mode; mode="$(current_mode)"
  echo "modo ativo: ${mode:-nenhum (rode scripts/mode.sh local ou lan)}"
  [ -z "$mode" ] && return 0
  echo "backend ($BACKEND_ENV):"
  block_of "$BACKEND_ENV" "$BACKEND_KEYS" \
    | sed -E 's/^( *GITHUB_CLIENT_ID=).{4}.*/\1****/; s/^( *GITHUB_CLIENT_SECRET=).*/\1****/'
  echo "front ($FRONTEND_ENV):"
  block_of "$FRONTEND_ENV" "$FRONTEND_KEYS"
  warn_overrides
}

mode="${1:-}"
case "$mode" in
  status) show_status; exit 0 ;;
  local|lan) ;;
  *) echo "uso: scripts/mode.sh local|lan|status" >&2; exit 2 ;;
esac

profile="$ROOT/.env.mode.$mode"
[ -f "$profile" ] || die "perfil não encontrado: $profile (modelo em scripts/profiles/$mode.env.example)"
[ -f "$BACKEND_ENV" ] || die "$BACKEND_ENV não existe (copie de .env.example)"

for key in GITHUB_CLIENT_ID GITHUB_CLIENT_SECRET FRONTEND_OAUTH_CALLBACK_URL; do
  grep -Eq "^$key=.+" "$profile" || die "$key está vazio ou ausente em $profile"
done
if [ "$mode" = "lan" ]; then
  for key in OAUTH_REDIRECT_URI DEV_ALLOWED_ORIGINS; do
    grep -Eq "^$key=.+" "$profile" || die "$key está vazio ou ausente em $profile"
  done
fi

[ -f "$FRONTEND_ENV" ] || cp "$ROOT/frontend/.env.example" "$FRONTEND_ENV"

cp "$BACKEND_ENV" "$BACKEND_ENV.bak"
cp "$FRONTEND_ENV" "$FRONTEND_ENV.bak"

write_block "$BACKEND_ENV" "$BACKEND_KEYS" "$mode" "$profile"
write_block "$FRONTEND_ENV" "$FRONTEND_KEYS" "$mode" "$profile"

echo "modo '$mode' aplicado (cópias de segurança: .env.bak e frontend/.env.local.bak)."
warn_overrides
echo
echo "Falta reiniciar:"
echo "  1. o backend (ele só lê o .env ao iniciar)"
if [ "$mode" = "lan" ]; then
  echo "  2. o front:  cd frontend && npm run dev:lan"
else
  echo "  2. o front:  cd frontend && npm run dev"
fi
