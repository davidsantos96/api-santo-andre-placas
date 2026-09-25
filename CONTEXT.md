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

## ⏳ Pendente (ordem de prioridade, seguindo a spec)

1. **Seed do primeiro usuário** — combinado deixar para quando o PostgreSQL
   estiver pronto, para já testar login de ponta a ponta no ambiente real.
   Também vale testar autorização por papel, a baixa automática de
   estoque e o fechamento de caixa end-to-end nesse momento.
2. **Dashboard e relatórios** (módulo 8) — provavelmente onde
   `groupingBy`/streams mais avançados voltam a aparecer.
3. **PostgreSQL real** — migrar do H2 (Flyway para migrations versionadas,
   conforme seção 7 da spec), testar autenticação de fato.
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
