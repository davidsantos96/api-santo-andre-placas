# API Santo André Placas

API REST do sistema interno de gestão da **Santo André Placas**: clientes,
veículos, catálogo de serviços, pedidos, estoque, financeiro básico e
dashboard. É a **Fase 1** do projeto, independente do site público; o front
(React) é um repositório à parte.

- Escopo, modelo de dados e endpoints planejados: [`docs/spec.md`](docs/spec.md)
- Histórico de decisões, padrões e pendências: [`CONTEXT.md`](CONTEXT.md)

## Stack

| | |
|---|---|
| Linguagem / framework | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security) |
| Banco | H2 em memória por padrão; PostgreSQL (Supabase) pelo perfil `supabase` |
| Autenticação | JWT (jjwt) com senha em BCrypt; papéis `ADMIN`, `GERENTE`, `ATENDENTE` |
| Documentação | OpenAPI / Swagger UI (springdoc) |
| Build | Maven (wrapper incluso, não precisa instalar o Maven) |

## Como rodar

Requer apenas o **JDK 21**.

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080` com **H2 em memória**: os dados somem a
cada reinicialização, e o usuário administrador é recriado no início.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
  (faça login em `POST /api/auth/login` e cole o token em **Authorize**)
- Console do H2: `http://localhost:8080/h2-console`
  (URL JDBC `jdbc:h2:mem:santoandreplacas`, usuário `sa`, senha vazia)

Na primeira subida, se o banco não tiver nenhum administrador, a API cria um a partir
das variáveis `ADMIN_SEED_*` (veja abaixo). Sem `ADMIN_SEED_PASSWORD`, em desenvolvimento ela
gera uma senha aleatória e a mostra uma única vez no log da subida (procure por "senha gerada");
em produção ela recusa subir.

### Com PostgreSQL (Supabase)

```bash
export SUPABASE_DB_HOST=db.<id-do-projeto>.supabase.co
export SUPABASE_DB_PASSWORD=<senha-do-banco>
./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase
```

As tabelas são criadas e ajustadas automaticamente (`ddl-auto=update`). Não há
migrations ainda (Flyway está na lista de pendências).

### Variáveis de ambiente

| Variável | Padrão | Para que serve |
|---|---|---|
| `JWT_SECRET` | aleatória a cada execução (só em desenvolvimento) | Chave de assinatura dos tokens (mín. 32 caracteres) |
| `ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Origens liberadas no CORS, separadas por vírgula |
| `ADMIN_SEED_LOGIN` | `admin` | Login do administrador criado no primeiro start |
| `ADMIN_SEED_EMAIL` | valor de desenvolvimento | E-mail desse administrador (administradores precisam ter e-mail) |
| `ADMIN_SEED_PASSWORD` | gerada e mostrada no log (só em desenvolvimento) | Senha desse administrador (em produção é obrigatória, mín. 10 caracteres) |
| `ADMIN_SEED_NOME` | `Administrador` | Nome exibido |
| `SUPABASE_DB_HOST` | — | Host do Postgres (perfil `supabase`) |
| `SUPABASE_DB_PASSWORD` | — | Senha do Postgres (perfil `supabase`) |
| `SUPABASE_DB_PORT` / `_NAME` / `_USER` | `5432` / `postgres` / `postgres` | Demais dados de conexão |

> **Antes de publicar em qualquer ambiente real:** defina `JWT_SECRET` e
> `ADMIN_SEED_PASSWORD` (não há valores padrão em produção), e saiba
> que `/h2-console` fica liberado sem login. Não use o perfil padrão (H2) fora
> da sua máquina.

## Testes

```bash
./mvnw test
```

Rodam contra o H2 em memória, sem configuração:

- `FluxoPrincipalTest`: o caminho do balcão de ponta a ponta (cadastros, pedido,
  baixa automática de estoque, pagamento parcelado, entrega e caixa).
- `ErrosEProtecoesTest`: códigos de erro, busca, e-mail de usuário, regras de
  administração de usuários e de pedido/estoque.
- `RespostasNaoVazamCredenciaisTest`: nenhuma resposta devolve o hash de senha.

O front tem uma suíte e2e (Playwright) que exercita a API de verdade pelo
navegador; veja o README do repositório do front.

## Endpoints

Todas as rotas exigem `Authorization: Bearer <token>`, exceto `/api/auth/**`.
A coluna de papéis mostra quem pode chamar; o contrato completo de cada rota
está no Swagger.

| Recurso | Rotas | Papéis |
|---|---|---|
| Autenticação | `POST /api/auth/login` (corpo `{login, senha}`; `login` aceita o login ou o e-mail) | público |
| Clientes | `GET/POST /api/clientes`, `GET/PUT /api/clientes/{id}` | todos |
| Veículos | `GET/POST /api/veiculos`, `GET/PUT /{id}`, `POST /{id}/consultar`, `GET /{id}/historico-consultas` | todos |
| Serviços | `GET /api/servicos`, `GET /{id}` | todos |
| | `POST /api/servicos`, `PUT/DELETE /{id}`, `PATCH /{id}/status` | admin, gerente |
| Pedidos | `GET/POST /api/pedidos`, `GET /{id}`, `PATCH /{id}/status`, `POST /completo`, `GET /{id}/historico` | todos |
| Pagamento do pedido | `POST /api/pedidos/{id}/pagamento`, `GET /{id}/pagamentos` | todos |
| Estoque | `GET /api/estoque/itens`, `GET /itens/baixo-estoque`, `GET/POST /movimentacoes`, `GET /vinculos?servicoId=` | todos |
| | `POST /api/estoque/itens`, `POST /vinculos`, `DELETE /vinculos/{id}` | admin, gerente |
| Financeiro | `GET /api/pagamentos`, `GET /api/financeiro/fechamento-caixa` | admin, gerente |
| Dashboard | `GET /api/dashboard/{resumo,faturamento,servicos-mais-vendidos,tempo-medio-producao}` | admin, gerente |
| Auditoria | `GET /api/auditoria` | admin, gerente |
| Usuários | `GET/POST /api/usuarios`, `PUT /{id}`, `PATCH /{id}/status`, `PATCH /{id}/senha` | admin |

## Convenções

- **Rastreabilidade:** toda escrita de domínio grava quem fez (`criadoPorId`,
  `atualizadoPorId` e o nome no momento). Alterações em serviços e usuários
  também ficam em `/api/auditoria`.
- **Respostas** sempre em DTO (`*Response`), nunca a entidade JPA crua: foi
  assim que o hash de senha vazou uma vez (B19, documentado no `CONTEXT.md`).
- **Valores** em centavos (`long`); **datas** sem fuso, gravadas no horário de
  São Paulo.
- **Erros:** `400` para regra de negócio ou requisição inválida, `404` para não
  encontrado, `401` só para falta de login (token ausente, inválido, expirado
  ou de usuário desativado), `403` sem permissão e `500` para falha interna,
  sempre com `{"mensagem": "..."}`.
- **Baixa automática de estoque:** cada serviço pode ter itens de estoque
  vinculados (`/api/estoque/vinculos`). Quando um pedido entra em
  `EM_PROCESSAMENTO`, as quantidades vinculadas saem do estoque; se faltar,
  a mudança de status é recusada e nada é baixado.
- **Consulta de placa:** a interface `ConsultaVeicularProvider` existe, mas
  hoje responde "indisponível" — falta escolher e integrar um provedor.

## Estrutura

```
src/main/java/br/com/santoandreplacas/apisantoandreplacas/
├── auth, security     login, JWT, filtro, CORS, handlers 401/403
├── usuario, auditoria usuários internos e log de ações administrativas
├── cliente, veiculo   cadastros
├── servico            catálogo de serviços
├── pedido             núcleo: pedidos, status e histórico
├── financeiro         pagamentos e fechamento de caixa
├── estoque            itens, movimentações e vínculo serviço↔item
├── dashboard          indicadores
├── exception, common  tratamento de erros e utilitários
└── config             OpenAPI
docs/spec.md           especificação do sistema
CONTEXT.md             decisões, padrões e pendências
```

## Login e usuários

- **Login de rede:** cada pessoa entra com um `login` próprio (ex.: `bruna.silva`: minúsculas, números, ponto, hífen ou underscore, 3 a 40 caracteres), sem precisar de e-mail pessoal. Quem tem e-mail também pode entrar digitando o e-mail. Nunca compartilhe login: a rastreabilidade (quem fez o quê) depende de um login por pessoa.
- **E-mail** é opcional, mas **obrigatório para administradores** (os donos), porque é por ele que a recuperação de acesso será feita no futuro. Hoje a senha é redefinida por outro administrador; por isso mantenha pelo menos dois.
- **Banco já existente:** na subida, `UsuarioLoginMigracao` tira o `NOT NULL` do e-mail e dá login a quem não tem (a parte antes do `@`). É uma ponte até as migrations do Flyway.
