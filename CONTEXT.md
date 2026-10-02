# Contexto do projeto — api-santo-andre-placas

> Este arquivo existe para dar contexto rápido ao Claude Code (ou a quem
> retomar o projeto) sem precisar reconstruir o histórico de decisões a
> partir do zero. Leia junto com `Spec — Santo André Placas.md`, que é a
> fonte de verdade sobre domínio, entidades e endpoints planejados.

## Stack e estado atual

- Spring Boot (Maven), H2 em memória por padrão (dev/testes). PostgreSQL real
  disponível via profile `supabase` (ver seção abaixo) — Flyway ainda
  pendente.
- Repositório Git criado e sincronizado no GitHub.
- Autenticação JWT funcionando: entidade `Usuario`, senha com hash BCrypt,
  geração/validação de token, filtro de segurança protegendo todas as rotas
  exceto `/api/auth/**`. **Seed do admin implementado** (`AdminUsuarioSeeder`,
  ver seção "Supabase" abaixo).
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
  `UsuarioResponse` nunca expõe `senhaHash`. **Sem rastreamento de
  "último acesso"** — ficou fora de escopo desta rodada. Reset de senha
  pelo admin foi implementado depois, em 2026-09-30 (ver seção abaixo) —
  reset pelo próprio usuário (self-service) ainda não.
- `LoginResponse` agora inclui `nome` (além de `token`/`papel`) — o
  front precisava disso pra sidebar e não tinha de onde tirar.

**As 8 decisões de design pendentes foram todas resolvidas e implementadas
em 2026-09-28** (testado via um segundo teste de integração MockMvc
descartável, removido depois de confirmar):

1. **Refresh token → não vai ter.** `JwtService.expiracaoMs` subiu de 1h
   pra 8h. Sem `POST /auth/refresh`.
2. **Consulta veicular → placeholder.** Nova interface
   `ConsultaVeicularProvider` (pacote `veiculo`) + implementação
   `ConsultaVeicularIndisponivelProvider` (sempre lança
   `IllegalStateException` → `400`). Endpoints novos:
   `POST /api/veiculos/{id}/consultar` e
   `GET /api/veiculos/{id}/historico-consultas` (sempre `[]`).
3. **Estoque — `ATENDENTE` liberado** para `POST /estoque/movimentacoes`
   (tirei o `@PreAuthorize` extra do método, cai no da classe). Cadastro de
   item e vínculo continuam `ADMIN`/`GERENTE`.
4. **Veículo e Cliente — front se adapta**, sem mudança de campo no
   backend (`marcaModelo`, `anoFabricacao`/`anoModelo`, `cpfCnpj`
   continuam como estavam).
5. **"Quem fez a ação" → implementado.** `UsuarioAutenticadoProvider`
   (pacote `usuario`) resolve o usuário autenticado via
   `SecurityContextHolder` + `UsuarioRepository`. `PedidoService` usa isso
   em `alteradoPor` (antes hardcoded `"sistema"`). `Pagamento` ganhou o
   campo `registradoPor`, preenchido por `PagamentoService` do mesmo jeito
   — vale tanto pra `PagamentoResponse` quanto `PagamentoListagemResponse`.
6. **`sku`/`unidade` adicionados a `ItemEstoque`** (`String`, opcionais).
7. **`POST /api/pedidos` trocou de `Pedido` cru pra `NovoPedidoRequest{
   clienteId, veiculoId, servicoId, origem }`** — `PedidoService.
   criarSimples` valida existência dos três ids com `IllegalArgumentException`
   (`400`), não mais erro de FK (`500`). `PedidoService.criar(Pedido)`
   continua existindo internamente, reaproveitado por `criarSimples` e
   por `criarPedidoCompleto`.

## ✅ Lote de pendências do backend (PENDENCIAS.md) — 2026-10-02

Fechado a partir do `PENDENCIAS.md` do front, verificado com MockMvc
descartável **e** contra o Supabase real (ver "Postgres" abaixo). 13 commits
pequenos; o que mudou de contrato:

- **B16** — acesso negado agora é `403` (JSON). O handler padrão usa
  `sendError`, que faz forward pra `/error`, e a segunda passada caía no
  entry point (401). `SecurityConfig` ganhou `accessDeniedHandler`; os dois
  handlers forçam UTF-8 (`getWriter` usava ISO-8859-1 e estragava acentos).
- **B13** — `JWT_SECRET` e `ALLOWED_ORIGINS` (vírgula) por variável de
  ambiente (`app.jwt.secret`, `app.cors.allowed-origins`), com os defaults
  de dev de antes. **Definir as duas em produção.**
- **C3** — `GET /servicos?incluirInativos=true`.
- **B7/B8** — CPF/CNPJ duplicado → `400`, comparando só dígitos
  (`REPLACE` no JPQL; guarda como o front manda, mascarado). Busca de
  clientes por dígitos acha CPF/telefone mascarados. Placa: normalizada
  (`trim` + maiúscula) em `Veiculo.setPlaca` e duplicada → `400`
  (`findByPlacaIgnoreCase`). Vale também pro `POST /pedidos/completo`.
- **C2/B15** — cadastro parcial de veículo: só `placa` (+ `clienteId`) é
  obrigatória; `marcaModelo`, `anoFabricacao`, `anoModelo` (agora `Integer`)
  e `chassi` são opcionais e podem vir `null` na resposta. Novo
  `PUT /veiculos/{id}` aplica **só os campos não-nulos** (completar
  cadastro). **Mudança de schema:** `veiculo.ano_fabricacao` e
  `ano_modelo` precisaram de `DROP NOT NULL` — o `ddl-auto=update` não
  relaxa constraint existente. Já aplicado no Supabase de teste; qualquer
  outro banco já criado precisa do mesmo `ALTER` (a Flyway resolveria).
- **P2** — `GET /pedidos?busca=` por placa, nome do cliente ou nº do pedido.
- **B9** — `GET /pagamentos` ordenado por `pagoEm` desc. Todo timestamp de
  negócio usa `FusoHorario.SAO_PAULO` (`common/`) em vez do fuso padrão da
  JVM (host em UTC jogaria pagamento da noite no dia seguinte). Testado
  com a JVM forçada pra UTC.
- **B10** — recurso inexistente **na URL** (`/{id}`) → `404`
  (`RecursoNaoEncontradoException`); id inexistente **no corpo**
  (`clienteId` num `POST /pedidos`) segue `400`.
- **B11/P29** — `servicos-mais-vendidos` ignora `CANCELADO` e aceita
  `de`/`ate` (as duas ou nenhuma). `tempo-medio-producao` aceita `de`/`ate`
  (pedidos que ficaram prontos no período) e devolve
  `horasMediaPeriodoAnterior` (período de mesma duração logo antes; `null`
  sem período ou sem dado).
- **P15** — `ClienteResponse.totalPedidos` (só em `/clientes`; `null`
  quando o cliente vem aninhado em outra resposta).
- **P21** — `ServicoResponse.pedidosNoMes` (não cancelados, mês corrente
  em SP; `null` quando aninhado). `POST/PUT/PATCH /servicos` ainda devolvem
  a entidade crua, sem esse campo.
- **P35** — `Usuario.ultimoAcessoEm`, gravado a cada login, em
  `UsuarioResponse`.

**Postgres (aprendizado importante):** parâmetro nulo sem tipo quebra no
PostgreSQL (`could not determine data type of parameter`) — `String` dentro
de `LOWER/UPPER(CONCAT(...))` e `LocalDateTime` em `:x IS NULL`. O H2 não
acusa. `GET /pedidos` sem datas, `/clientes` sem busca e `/veiculos` sem
placa já davam 500 no Supabase. Convenção daqui pra frente em `@Query`
com filtro opcional: texto vazio vira `""`, id de busca vira `-1`, datas
viram limites (`1970`/`2999`); `Long` e enum com `IS NULL` funcionam.
**Teste de consulta nova sempre contra o Supabase, não só H2.** Obs.: um
500 aparece como `401` porque `/error` exige auth (não está liberado no
`SecurityConfig`) — se virar `401` estranho, olhar o log do servidor.

## ✅ Chassi do veículo passou a ser opcional — 2026-09-30

`Veiculo.setChassi` validava e lançava `IllegalArgumentException` se
vazio/nulo (400 pro front, inclusive no `POST /pedidos/completo`, onde o
veículo é criado inline). Removida a validação — no balcão nem sempre dá
pra conferir o chassi na hora, só a placa. Coluna já era `nullable=true`
no banco (sem `@Column(nullable=false)` na entidade), então não precisou
de migração. Testado criando veículo sem `chassi` no corpo — `200`, campo
volta `null` na resposta.

## ✅ Reset de senha pelo admin — 2026-09-30

Decisão: por enquanto só o admin reseta a senha de qualquer usuário
(`PATCH /api/usuarios/{id}/senha`, `{novaSenha: string}`, `ADMIN` apenas —
herda o `@PreAuthorize` de classe do `UsuarioController`). Fluxo real:
admin define uma senha nova e passa pro usuário por fora do sistema (não
há envio de e-mail configurado no projeto). Self-service (o próprio
usuário trocar a própria senha logado, informando a senha atual) **ainda
não foi implementado** — ficou como próximo passo natural caso apareça
essa necessidade.

Testado via MockMvc descartável: admin cria usuário, senha antiga
funciona, admin reseta, senha antiga passa a falhar (`400`), senha nova
funciona, e um não-admin tentando chamar o endpoint recebe `403`.

## ✅ Bugs encontrados testando com o front — 2026-09-30

Testado via um terceiro teste de integração MockMvc descartável (removido
depois de confirmar, 6 cenários, todos verdes):

1. **Login não checava `ativo`** — usuário desativado conseguia logar e o
   token continuava valendo pelo resto da expiração. Dois pontos
   corrigidos: `AuthController.login` agora rejeita (`400`, "Usuário
   inativo. Contate um administrador.") se `usuario.isAtivo() == false`;
   e `CustomUserDetailsService` agora propaga `ativo` pro `enabled` do
   `UserDetails`, então `JwtAuthFilter` **não autentica mais** um token
   cujo usuário foi desativado depois de emitido (mesmo com assinatura e
   expiração válidas) — a requisição cai como anônima e é rejeitada como
   `401` mais adiante.
2. **401 vs 403** — sem `formLogin()`/`httpBasic()` configurado, o Spring
   Security usa o `Http403ForbiddenEntryPoint` por padrão pra qualquer
   requisição sem autenticação válida. Adicionado um
   `authenticationEntryPoint` customizado em `SecurityConfig` que devolve
   `401` com `{"mensagem": "..."}` — cobre token ausente, inválido,
   expirado ou de usuário desativado.
3. **Serviço sem ativar/desativar de verdade** — `DELETE /api/servicos/{id}`
   apagava a linha (podia falhar/quebrar FK se o serviço já tivesse
   pedidos). Virou soft-delete (`ativo=false`) dentro do próprio
   `ServicoService.deletar`. Novo `PATCH /api/servicos/{id}/status`
   (`{ativo: boolean}`, mesmo padrão do `Usuario`) pra reativar.
4. **Sem `pago` no pedido** — `PedidoResponse` ganhou o campo `pago`
   (boolean), calculado em `PedidoService.estaPago` como
   `SUM(pagamento.valorCentavos WHERE status=PAGO) >= servico.precoCentavos`
   (nova query `PagamentoRepository.somarValorPorPedidoEStatus`). Permite
   pagamento parcial em várias parcelas; só fica `pago=true` quando a
   soma bate ou passa o preço do serviço. Todos os pontos que devolvem
   `PedidoResponse` passaram a usar `PedidoService.toResponse(pedido)`.
5. **Erros de validação de entidade virando "Bad Request" sem mensagem** —
   causa raiz: as entidades validam nos próprios setters (ex.:
   `Servico.setNome`), chamados pelo Jackson durante a desserialização do
   corpo da requisição; quando o setter lança, o Spring embrulha em
   `HttpMessageNotReadableException` **antes** de chegar nos handlers de
   `IllegalArgumentException`/`IllegalStateException` do
   `GlobalExceptionHandler` — a mensagem real se perdia. Novo handler pra
   `HttpMessageNotReadableException` que abre a causa mais específica e
   devolve a mensagem original se for uma dessas duas exceções.
6. **`POST /veiculos` inconsistente** — esperava `{cliente: {id}}` (entidade
   JPA crua) em vez de `clienteId` como o resto da API, e devolvia a
   entidade em vez de `VeiculoResponse`. Novo
   `br...veiculo.NovoVeiculoRequest` (flat, com `clienteId`) — mesmo
   padrão do `NovoPedidoRequest`. `VeiculoService.criar` agora resolve o
   `Cliente` via `ClienteRepository` e o controller devolve
   `VeiculoResponse.fromEntity(...)`.

## ✅ Supabase (Postgres real) conectado e seed do admin — 2026-09-30

- Projeto Supabase criado (`db.ksmxrzmayzahlyebbdaq.supabase.co`), só para
  testes — nenhum usuário/dado de produção ainda.
- Novo profile Spring `supabase` (`application-supabase.properties`):
  datasource Postgres via `${SUPABASE_DB_HOST}`/`${SUPABASE_DB_PASSWORD}`
  (variáveis de ambiente, nunca hardcoded/commitado). `ddl-auto=update`
  criou as 11 tabelas na primeira subida. Ativar com
  `-Dspring-boot.run.profiles=supabase`. Sem o profile, continua H2 como
  sempre (dev/testes automatizados não mudam).
- Driver `org.postgresql:postgresql` adicionado ao `pom.xml`.
- **`AdminUsuarioSeeder`** (pacote `usuario`, `ApplicationRunner`): na
  subida, cria o usuário `ADMIN` se `app.seed.admin.email` ainda não
  existir (idempotente — testado reiniciando duas vezes contra o Supabase
  real, só insere na primeira). Credenciais configuráveis via
  `ADMIN_SEED_EMAIL`/`ADMIN_SEED_PASSWORD`/`ADMIN_SEED_NOME`, com defaults
  (`admin@santoandreplacas.com.br` / `admin123` / `Administrador`) em
  `application.properties` — valem tanto pro H2 quanto pro Supabase.
- Testado ponta a ponta contra o banco real: subiu a app, o seeder
  inseriu o admin, `POST /api/auth/login` com essas credenciais devolveu
  token JWT válido com `papel: ADMIN`.

## ⏳ Pendente (ordem de prioridade, seguindo a spec do backend)

Fase 1 da spec do backend está com todos os módulos implementados, todas
as decisões de alinhamento com o front foram resolvidas, e agora já existe
um ambiente Postgres real (Supabase) com usuário admin pra testar end to
end. Resta:

1. **Migrations versionadas (Flyway)** — hoje o schema no Supabase foi
   criado via `ddl-auto=update` (Hibernate), só pra destravar os testes.
   Trocar por Flyway antes de depender desse banco pra valer (conforme
   seção 7 da spec).
2. **Testar manualmente end-to-end via HTTP** com o admin seedado:
   autorização por papel, baixa automática de estoque, fechamento de
   caixa e dashboard — só foi testado via testes de integração
   descartáveis até aqui.
3. **Fase 2 — Financeiro avançado** (contas a receber/pagar, balanço,
   comparativos, metas — seção 6 da spec). Só começar depois da Fase 1
   completa.
4. **DTOs de entrada** para `Cliente` e `Servico` (`Veiculo` já resolvido em
   2026-09-30 — ver seção de bugs acima) — hoje só `Pedido`/`Veiculo` têm
   esse padrão; fechar essa lacuna nos outros dois. Vale estender também a
   `ItemEstoque` e `ServicoItemEstoque`, que hoje também recebem a entidade
   JPA crua no `POST`.
5. **Itens de menor prioridade** (PENDENCIAS.md, ainda abertos no backend):
   filtro `pago=false` em `/pedidos` (o campo `pago` já existe na
   resposta); reset de senha self-service (o admin já reseta); proteger
   desativação/rebaixamento do último ADMIN ativo; política mínima de senha
   no backend (hoje só o front exige 6 caracteres); `observacao` em
   movimentação de estoque (B12); paginação de `/clientes` e `/veiculos`
   (P17); `Cliente`/`Servico` ainda devolvem a entidade crua em
   `POST/PUT/PATCH`; `/error` sem acesso público faz erro 500 aparecer
   como 401.

## Fundamentos de Java já estudados

Sintaxe, tipos, OO, encapsulamento, interfaces, herança, exceções, records,
pattern matching, sealed classes, streams/programação funcional,
concorrência moderna (virtual threads, com comparação real de performance
testada). Isso é background de estudo — não implica que todo código do
projeto já usa essas features, mas explica por que soluções mais modernas
(records, pattern matching, streams avançados) podem aparecer nos próximos
módulos.
