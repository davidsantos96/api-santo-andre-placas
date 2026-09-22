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
  `SecurityConfig`): `Cliente`, `Veiculo` e `Pedido` liberados para
  `ADMIN`/`GERENTE`/`ATENDENTE` em todos os endpoints; `Servico` liberado
  para os três papéis em leitura (`GET`), mas criar/atualizar/deletar
  (catálogo e preço) restrito a `ADMIN`/`GERENTE`. **Atenção:** a spec não
  estava disponível no repo no momento da implementação (arquivo
  `Spec — Santo André Placas.md` referenciado mas ausente) — essa matriz de
  permissões foi uma decisão razoável meu, não uma tabela extraída da
  seção 8. Revisar contra a spec original e ajustar se divergir.

## ⏳ Pendente (ordem de prioridade, seguindo a spec)

1. **Seed do primeiro usuário** — combinado deixar para quando o PostgreSQL
   estiver pronto, para já testar login de ponta a ponta no ambiente real.
   Também vale testar a autorização por papel end-to-end nesse momento.
2. **Módulo Estoque** — próximo passo da ordem da spec (seção 3 e 4). É onde
   o gancho comentado no `PedidoService` (baixa automática de estoque ao
   mudar status para `EM_PROCESSAMENTO`) precisa ser implementado de fato.
3. **Financeiro básico** (módulo 7) — pagamento por pedido, fechamento de
   caixa.
4. **Dashboard e relatórios** (módulo 8) — provavelmente onde
   `groupingBy`/streams mais avançados voltam a aparecer.
5. **PostgreSQL real** — migrar do H2 (Flyway para migrations versionadas,
   conforme seção 7 da spec), testar autenticação de fato.
6. **Fase 2 — Financeiro avançado** (contas a receber/pagar, balanço,
   comparativos, metas — seção 6 da spec). Só começar depois da Fase 1
   completa.
7. **DTOs de entrada** para `Cliente`, `Veiculo`, `Servico` — hoje só
   `Pedido` tem esse padrão; fechar essa lacuna nos outros três.

## Fundamentos de Java já estudados

Sintaxe, tipos, OO, encapsulamento, interfaces, herança, exceções, records,
pattern matching, sealed classes, streams/programação funcional,
concorrência moderna (virtual threads, com comparação real de performance
testada). Isso é background de estudo — não implica que todo código do
projeto já usa essas features, mas explica por que soluções mais modernas
(records, pattern matching, streams avançados) podem aparecer nos próximos
módulos.
