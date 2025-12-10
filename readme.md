# Action Token Login Customizado — Keycloak

Este projeto adiciona ao Keycloak um `AdminRealmResource` customizado capaz de gerar links de login via Action Token para usuários específicos, permitindo que um sistema externo envie um `userId` e `clientId` via POST e receba como resposta:

```json
{
  "userId": "...",
  "link": "..."
}
```

O link retornado autentica automaticamente o usuário (desde que já possua sessão ativa) no client correspondente, iniciando um fluxo de login seguro e customizável.

---

## ✨ Funcionalidades

- 🔐 Geração de Action Tokens customizados
- 🔄 Redirecionamento automático para o fluxo de login do client
- 🧩 API administrativa customizada
- 🎯 Permite implementar features como "Login como Usuário"
- 📡 Retorno em JSON com `userId` e `link`
- 🛡️ Segurança baseada nas chaves internas do Keycloak

---

## 📌 Endpoint Disponível

### `POST` /admin/realms/{realmName}/action-token-login

```
http://localhost:8081/admin/realms/<realmName>/action-token-login
```

Este recurso é registrado como parte das extensões providas no provider customizado.

---

## 📥 Request — Body

```json
{
  "userId": "<id-do-usuario>",
  "clientId": "<id-do-client>"
}
```

| Campo      | Tipo   | Obrigatório | Descrição                             |
|------------|--------|-------------|---------------------------------------|
| `userId`   | string | ✔           | ID do usuário dentro do Realm         |
| `clientId` | string | ✔           | ID do client onde o token será validado |

---

## 📤 Response — Exemplo

```json
{
  "userId": "23df8aa9-xxxx-xxxx-xxxx-7fa1a...",
  "link": "http://localhost:8081/realms/myrealm/login-actions/action-token?key=eyJhbGciOi..."
}
```

| Campo    | Descrição                                                    |
|----------|--------------------------------------------------------------|
| `userId` | O ID informado no request                                    |
| `link`   | URL única contendo o Action Token válida por tempo limitado  |

---

## 🧠 Como Funciona

1. O cliente externo envia `userId` e `clientId` para o endpoint.
2. O provider customizado:
   - valida o usuário,
   - valida o client,
   - gera um Action Token,
   - assina com a chave privada do Realm,
   - constrói a URL final.
3. A API retorna `{ userId, link }`.
4. O usuário abre o link → Keycloak valida o token → redireciona para o fluxo do client.

---

## 🔧 Instalação

### 1. Compile seu provider customizado:

```bash
mvn clean package
```

### 2. Copie o `.jar` para:

```bash
/opt/keycloak/providers/
```

### 3. Reinicie o Keycloak:

```bash
kc.sh start
```

### 4. O endpoint passa a estar disponível automaticamente.

---

## 🛠 Exemplo de Uso via cURL

```bash
curl -X POST "http://localhost:8081/admin/realms/Exitus/action-token-login" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
        "userId": "23df8aa9-fc34-4cc1-9dd5-fa33dda1",
        "clientId": "exitus_educacional"
      }'
```

---

## 🔒 Segurança

- ✅ Apenas usuários autorizados na API Admin podem gerar tokens.
- ⏱️ Tokens possuem expiração de 120s.
- 🔐 Tokens são assinados e impossíveis de falsificar.
- 🎯 Limitado ao `clientId` informado.
- 👤 O usuário precisa existir e estar ativo.

---

## 📚 Créditos

Este desenvolvimento foi inspirado no excelente repositório:

➡️ **[keycloak-extensions-demo](https://github.com/dasniko/keycloak-extensions-demo)**

Que forneceu a base conceitual e estrutural para criação de providers customizados.
