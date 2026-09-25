# Contexto do projeto — api-santo-andre-placas

> Este arquivo existe para dar contexto rápido ao Claude Code (ou a quem
> retomar o projeto) sem precisar reconstruir o histórico de decisões a
> partir do zero. Leia junto com `Spec — Santo André Placas.md`, que é a
> fonte de verdade sobre domínio, entidades e endpoints planejados.

## Stack e estado atual

- Spring Boot (Maven), H2 em arquivo — troca para PostgreSQL planejada, ainda
  não feita.
- Repositório Git criado e sincronizado no GitHub.
- Autenticação JWT funcionando: entidade `Usuario`, senha com hash BCrypt,
  geração/validação de token, filtro de segurança protegendo todas as rotas
  exceto `/api/auth/**`. Ainda **sem seed de usuário** (ver pendências).
- **Swagger UI** (`springdoc-openapi-starter-webmvc-ui` 3.1.1, compatível com
  Spring Boot 4): `http://localhost:8080/swagger-ui/index.html`. Rotas
  `/swagger-ui/**` e `/v3/api-docs/**` liberadas em `SecurityConfig`. Botão
  "Authorize" já configurado com esquema `bearerAuth` (Bearer JWT) via
  `config/OpenApiConfig` — colar o token do `POST /api/auth/login` ali
  autentica todas as chamadas feitas pela própria UI. Verificado subindo o
  app e abrindo no navegador: todos os controllers/endpoints aparecem e o
  modal de auth mostra "bearerAuth (http, Bearer)" corretamente. Usado para
  testar end-to-end enquanto não há front React (spec deixa o front pra
  depois — seção 9).

## Padrões já estabelecidos (manter consistência)

- **DTOs de saída**: toda entidade exposta via API tem um `*Response` (ex:
  `ClienteResponse`, `VeiculoResponse`, `ServicoResponse`, `PedidoResponse`
  — este último aninhado, resolvendo Cliente/Veículo/Serviço dentro do
  pedido).
- **DTOs de entrada**: só `Pedido` tem isso hoje (`NovoPedidoRequest`-style).
  `Cliente`, `Veiculo` e `Servico` ainda recebem a entidade JPA crua no
  `POST`/`PUT` — é dívida técnica pendente, não um padrão a seguir daqui pra
  frente.
- **Transações atômicas**: fluxos compostos usam `@Transactional`. Exemplo
  implementado: `POST /api/pedidos/completo`, que resolve "cliente novo +
  veículo novo + pedido" numa única transação.
- **Erros de negócio**: tratamento centralizado via `@RestControllerAdvice`
  — exceção de negócio vira `400` com mensagem clara, nunca `500` genérico.
- **Auditoria de status**: toda mudança de status de pedido grava em
  `PedidoStatusHistorico` (quem mudou, quando) — já implementado.
- **Camadas**: `controller` → `service` → `repository`, com
  `ConsultaVeicularProvider` e `ServicoRepository` desenhadas como
  interfaces desde já (ver seção 2 da spec — preparação para integração
  futura com o site público), mesmo com implementação única por enquanto.

## ✅ Concluído

- CRUD completo: Cliente, Veículo (`@ManyToOne` com Cliente), Serviço, Pedido
  (com 3 relacionamentos e enum de status via JPA)
- Endpoint composto `/api/pedidos/completo`
- `PedidoStatusHistorico`
- Tratamento de erro centralizado
- Autenticação JWT + filtro de segurança
- **Autorização por papel** (`@PreAuthorize`, `@EnableMethodSecurity` em
  `SecurityConfig`): `Cliente`, `Veiculo`, `Pedido` e `Estoque` liberados
  para `ADMIN`/`GERENTE`/`ATENDENTE` em todos os endpoints de leitura;
  `Servico` liberado para os três papéis em leitura (`GET`). Escrita
  restrita a `ADMIN`/`GERENTE` em: catálogo de serviços (criar/atualizar/
  deletar), cadastro de item de estoque, movimentação manual de estoque e
  vínculo serviço↔item. **Decisão registrada:** a spec (seção 8) é mais
  restritiva — diz que ATENDENTE só "cria/edita pedidos" e "consulta
  veículos" (não cita Cliente, e Veículo é só leitura). Perguntei e ficou
  combinado **manter acesso amplo**: ATENDENTE mantém `GET`+`POST` em
  Cliente e Veículo, porque na prática o balcão precisa cadastrar cliente/
  veículo novos fora do fluxo de `/pedidos/completo` também. Se isso mudar,
  é só remover `ATENDENTE` do `@PreAuthorize` de `ClienteController` e do
  `POST` de `VeiculoController`.
- **Módulo Estoque** (spec seção 3, 4 e endpoints da seção 5, pacote
  `estoque`): `ItemEstoque` (nome, quantidade, quantidadeMinima),
  `MovimentacaoEstoque` (ENTRADA/SAIDA, ligada a um `Pedido` opcional).
  Endpoints: `GET/POST /api/estoque/itens`, `GET /api/estoque/itens/baixo-
  estoque`, `POST /api/estoque/movimentacoes`.
  **Gap de spec preenchido:** a seção 3 não define como um `Servico` se
  liga aos itens de estoque que consome, mas a seção 4 exige baixa
  automática "vinculada ao serviço do pedido". Criei a entidade
  `ServicoItemEstoque` (servico, itemEstoque, quantidadeNecessaria) e os
  endpoints `GET/POST /api/estoque/vinculos` (não estão na spec original)
  para tornar a regra funcional. `PedidoService.mudarStatus` chama
  `EstoqueService.baixarEstoquePorPedido` quando o novo status é
  `EM_PROCESSAMENTO`; se faltar estoque, lança `IllegalStateException`
  (vira `400`) e a transação inteira do pedido é revertida — ainda não
  testado ponta a ponta via HTTP porque não há usuário seedado para gerar
  JWT (ver pendência 1).
- **Financeiro básico** (módulo 7, pacote `financeiro`):
  - `Pagamento` (pedido FK, valorCentavos, `FormaPagamento` [DINHEIRO,
    CARTAO_CREDITO, CARTAO_DEBITO, PIX, BOLETO], `StatusPagamento` [PAGO,
    CANCELADO — hoje só PAGO é usado, CANCELADO existe pro futuro mas sem
    endpoint que o produza ainda], pagoEm). Endpoints (no
    `PedidoController`, path aninhado como a spec pede):
    `POST /api/pedidos/{id}/pagamento`, e um extra que não está na spec
    mas segue o padrão de `/historico`: `GET /api/pedidos/{id}/pagamentos`.
    Sempre grava como já pago (não existe fluxo de "pagamento pendente"
    na Fase 1 — isso é coisa de `ContaReceber` na Fase 2).
  - **Gap de spec preenchido:** "fechamento de caixa" está no nome do
    módulo 7 (seção 1) mas não tem entidade nem endpoint definido em
    nenhum outro lugar da spec. Perguntei e ficou decidido: **sem estado**
    — nada de "abrir/fechar caixa", só um relatório que soma os
    `Pagamento`s já registrados. Implementado como
    `GET /api/financeiro/fechamento-caixa?de=&ate=` (default: hoje),
    retornando total geral + total por forma de pagamento. Restrito a
    `ADMIN`/`GERENTE` (spec seção 8 só menciona financeiro básico para
    esses dois papéis).
- **Dashboard e relatórios** (módulo 8, pacote `dashboard`, spec seção 5):
  `GET /api/dashboard/resumo` (pedidos hoje, contagem por status, faturamento
  de hoje, itens em baixo estoque), `GET /api/dashboard/faturamento?de=&ate=`
  (total + série por dia, reaproveita `PagamentoRepository`),
  `GET /api/dashboard/servicos-mais-vendidos` (ranking por quantidade de
  pedidos; `faturamentoNominalCentavos` usa o **preço atual** do `Servico`,
  não o preço no momento da venda — `Servico` não guarda histórico de preço),
  `GET /api/dashboard/tempo-medio-producao`. Tudo restrito a `ADMIN`/
  `GERENTE`, igual ao financeiro.
  **Decisão de design (não estava na spec):** "tempo médio de produção" é
  calculado como o intervalo entre o pedido entrar em `EM_PROCESSAMENTO` e
  chegar a `PLACA_PRONTA` — usei a descrição dos próprios valores do enum
  `StatusPedido` ("Em produção" / "Pronto para retirada") como critério,
  não `RECEBIDO`→`ENTREGUE` (que inclui tempo de espera do cliente, não de
  produção). Se a intenção real for outra janela, é só trocar os dois
  `StatusPedido` usados em `DashboardService.tempoMedioProducao`.
- **CORS liberado** em `SecurityConfig` (`corsConfigurationSource`) para
  `http://localhost:5173`/`127.0.0.1:5173` (Vite dev server). Ajustar
  `allowedOrigins` quando o front for hospedado de verdade.
- **`PedidoResponse` ligado ao `PedidoController`** — todos os endpoints que
  devolvem pedido (`GET /pedidos`, `GET /pedidos/{id}`, `POST /pedidos`,
  `PATCH /pedidos/{id}/status`, `POST /pedidos/completo`) agora retornam
  `PedidoResponse` (cliente/veículo/serviço aninhados) em vez da entidade
  `Pedido` crua. O record já existia no código mas não estava sendo usado
  por nenhum controller até este ponto. `POST /pedidos` continua aceitando
  `Pedido` cru no corpo da requisição (isso não mudou).

## 🖥️ Front-end (React) — análise da spec

Recebi `Spec — Implementação React (Painel Santo André Placas).md`
(em `C:\Users\david\Downloads\`, fora deste repo) e comparei contra o
backend real, endpoint por endpoint. O documento tem as seções **14**
(mapeamento de endpoints) e **15** (divergências e decisões) com o
detalhe completo e já está atualizado com tudo abaixo — esta seção aqui
é só o resumo do lado do backend.

**Decisões tomadas e já implementadas (2026-09-25):**
- **Caixa fica sem estado**, definitivo — nada de abrir/fechar/ABERTO/
  FECHADO. `GET /api/financeiro/fechamento-caixa?de=&ate=` é a única
  fonte pra aba Caixa do front.
- **`FormaPagamento` completado no front** para os 5 valores reais do
  backend (`DINHEIRO`, `CARTAO_CREDITO`, `CARTAO_DEBITO`, `PIX`,
  `BOLETO`) — o backend não foi reduzido.

**Endpoints novos construídos nesta sessão** (todos testados via um
teste de integração MockMvc descartável — login → CRUD de cliente com
busca → veículo com filtros → pedido completo → filtros/paginação de
pedido → pagamento → usuário — depois removido, não faz parte da
suíte permanente):
- `Cliente`: `GET /api/clientes?busca=` (nome/telefone/cpfCnpj, LIKE
  case-insensitive no nome) e `PUT /api/clientes/{id}` (ainda recebe
  `Cliente` cru, mesma dívida técnica de sempre).
- `Veiculo`: `GET /api/veiculos?placa=&clienteId=` — o filtro
  `clienteId` não estava pedido, mas substitui o que seria
  `/clientes/{id}/veiculos`.
- `Pedido`: `GET /api/pedidos?status=&clienteId=&de=&ate=&page=&size=`
  via `@Query` JPQL com parâmetros opcionais + `Pageable`, retornando
  `PagedModel<PedidoResponse>` (formato padrão do Spring Data:
  `{content, page:{size,number,totalElements,totalPages}}`).
- `Pagamento`: novo `PagamentoController` em `/api/pagamentos`
  (`GET ?de=&ate=&forma=`, `ADMIN`/`GERENTE`) — lista todos os
  pagamentos (não só por pedido), com `PagamentoListagemResponse` já
  trazendo `placa`/`clienteNome`/`servicoNome` embutidos pra alimentar
  a tabela da aba Pagamentos sem N+1.
- **Usuário**: módulo inteiro novo (`UsuarioController`,
  `UsuarioService`) — `GET/POST /api/usuarios`, `PUT /api/usuarios/{id}`,
  `PATCH /api/usuarios/{id}/status` (`{ativo: boolean}`), `ADMIN` apenas.
  Senha com BCrypt (reaproveita o `PasswordEncoder` já existente),
  e-mail duplicado vira `400` (não constraint violation → `500`).
  `UsuarioResponse` nunca expõe `senhaHash`. **Sem endpoint de reset de
  senha** e **sem rastreamento de "último acesso"** — ficou fora de
  escopo desta rodada.
- `LoginResponse` agora inclui `nome` (além de `token`/`papel`) — o
  front precisava disso pra sidebar e não tinha de onde tirar.

**Ainda pendente — decisões de design não tomadas** (ver spec do front
§15.4 pro detalhe): refresh token; provedor de consulta veicular
(`ConsultaVeicularProvider` nunca foi implementado, nem escolhido o
provedor); se `ATENDENTE` pode fazer movimentação manual de estoque;
campos de Veículo (`marca`+`modelo` separados vs. `marcaModelo` único,
`ano` único vs. `anoFabricacao`+`anoModelo`) e Cliente (`documento` vs.
`cpfCnpj`); `sku`/`unidade` em `ItemEstoque` (não existem); `POST
/pedidos` ainda recebe `Pedido` cru sem validação amigável (id inválido
= erro 500 de FK, não 400); `alteradoPor` (histórico de pedido) e
"registrado por" (pagamento) — nenhum dos dois tem usuário autenticado
associado ainda, sempre aparece como `"sistema"` ou ausente.

## ⏳ Pendente (ordem de prioridade, seguindo a spec do backend)

Fase 1 da spec do backend está com todos os módulos implementados.
As decisões de alinhamento com o front que tinham prioridade (Caixa,
FormaPagamento, endpoints faltantes) foram resolvidas nesta sessão.
Resta:

1. **Seed do primeiro usuário** — combinado deixar para quando o PostgreSQL
   estiver pronto, para já testar login de ponta a ponta no ambiente real.
   Também vale testar autorização por papel, a baixa automática de
   estoque, o fechamento de caixa e o dashboard end-to-end nesse momento
   (só foi testado via teste de integração descartável até aqui, não
   manualmente via HTTP com um usuário real).
2. **PostgreSQL real** — migrar do H2 (Flyway para migrations versionadas,
   conforme seção 7 da spec).
3. **Decisões de design ainda pendentes da spec do front** (ver seção
   acima) — refresh token, consulta veicular, ATENDENTE+estoque, campos
   de Veículo/Cliente, DTO de entrada de `POST /pedidos`.
4. **Fase 2 — Financeiro avançado** (contas a receber/pagar, balanço,
   comparativos, metas — seção 6 da spec). Só começar depois da Fase 1
   completa.
5. **DTOs de entrada** para `Cliente`, `Veiculo`, `Servico` — hoje só
   `Pedido` tem esse padrão; fechar essa lacuna nos outros três. Vale
   estender também a `ItemEstoque` e `ServicoItemEstoque`, que hoje também
   recebem a entidade JPA crua no `POST`.

## Fundamentos de Java já estudados

Sintaxe, tipos, OO, encapsulamento, interfaces, herança, exceções, records,
pattern matching, sealed classes, streams/programação funcional,
concorrência moderna (virtual threads, com comparação real de performance
testada). Isso é background de estudo — não implica que todo código do
projeto já usa essas features, mas explica por que soluções mais modernas
(records, pattern matching, streams avançados) podem aparecer nos próximos
módulos.
