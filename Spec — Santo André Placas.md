# Spec — Sistema interno de gestão (Santo André Placas)

Stack definida: **Java (Spring Boot) no back, React no front** (front fica para
depois — este documento cobre domínio, dados e API).

Escopo: **sistema independente do site público** na Fase 1 (sem banco, API ou
catálogo compartilhados). A estrutura, porém, já é desenhada para permitir
integração futura sem retrabalho — ver seção 2.

---

## 1. Módulos do sistema

**Fase 1 (agora):**
1. Autenticação e usuários internos (staff)
2. Clientes
3. Veículos
4. Catálogo de serviços
5. Pedidos — núcleo do sistema
6. Estoque
7. Financeiro básico (pagamento por pedido, fechamento de caixa)
8. Dashboard e relatórios

**Fase 2 (depois):**
9. Financeiro avançado — contas a receber/pagar, balanço, comparativos, metas
   (ver seção 6)
10. Integração com o site público (ver seção 2)

## 2. Preparação para integração futura

Os dois sistemas nascem separados, mas dois pontos de integração já são
esperados: **catálogo de serviços/preços** e **consulta veicular**. Para não
precisar reescrever nada quando isso acontecer, alguns cuidados na Fase 1:

- **Interfaces (portas) em vez de acesso direto:** a consulta veicular deve
  ficar atrás de uma interface `ConsultaVeicularProvider`, e a leitura de
  serviços atrás de uma interface `ServicoRepository`. Hoje ambas têm uma única
  implementação local; no futuro, uma delas pode virar uma implementação que
  chama a API do outro sistema, sem tocar no resto do código.
- **Campo de referência externa:** adicionar `codigoExterno` (nullable) em
  `Servico` e `origemLead` (nullable) em `Pedido` desde já. Ficam vazios por
  enquanto, mas evitam uma migration de schema quando a integração chegar.
- **API versionada e documentada:** manter o Swagger/OpenAPI atualizado desde
  o início (`springdoc-openapi`) — é o contrato que o outro sistema vai
  consumir no futuro, então documentar como se já fosse público ajuda a
  não gerar dívida técnica depois.
- **Endpoints idempotentes:** especialmente em `POST /api/servicos` e em um
  futuro endpoint de sincronização — receber o mesmo dado duas vezes não deve
  duplicar registro (usar `codigoExterno` como chave de idempotência).

Nenhuma dessas decisões atrasa a Fase 1 — são só escolhas de design que custam
pouco agora e evitam retrabalho depois.

## 3. Modelo de domínio (entidades JPA)

```
Usuario
├── id, nome, email, senhaHash, papel (ADMIN | GERENTE | ATENDENTE), ativo

Cliente
├── id, nome, telefone, cpfCnpj, email, criadoEm

Veiculo
├── id, clienteId (FK), placa, marcaModelo, anoFabricacao, anoModelo, chassi

Servico
├── id, nome, descricao, precoCentavos, categoria, ativo
├── codigoExterno   (nullable — preparado para integração futura)

Pedido
├── id, clienteId (FK), veiculoId (FK), servicoId (FK)
├── status (RECEBIDO | EM_PROCESSAMENTO | PLACA_PRONTA | ENTREGUE | CANCELADO)
├── origem (WHATSAPP | BALCAO | TELEFONE)
├── origemLead      (nullable — preparado para integração futura com o site)
├── criadoEm, atualizadoEm

PedidoStatusHistorico
├── id, pedidoId (FK), statusAnterior, statusNovo, usuarioId (FK), alteradoEm

Pagamento
├── id, pedidoId (FK), valorCentavos, formaPagamento, status, pagoEm

ItemEstoque
├── id, nome (ex: "Placa Mercosul par", "Lacre digital"), quantidade, quantidadeMinima

MovimentacaoEstoque
├── id, itemEstoqueId (FK), tipo (ENTRADA | SAIDA), quantidade, pedidoId (FK nullable), criadoEm
```

## 4. Fluxo central: ciclo de vida do pedido

```
RECEBIDO ──▶ EM_PROCESSAMENTO ──▶ PLACA_PRONTA ──▶ ENTREGUE
    │                                                  ▲
    └──────────────────▶ CANCELADO ◀───────────────────┘
```

Cada transição de status grava em `PedidoStatusHistorico` (quem mudou, quando).
Ao mudar para `EM_PROCESSAMENTO`, dá-se baixa automática nos itens de estoque
vinculados ao serviço do pedido.

## 5. API REST — endpoints por módulo (Fase 1)

### Autenticação
```
POST   /api/auth/login          { email, senha } → { token, refreshToken }
POST   /api/auth/refresh
```

### Usuários (ADMIN)
```
GET    /api/usuarios
POST   /api/usuarios
PUT    /api/usuarios/{id}
DELETE /api/usuarios/{id}
```

### Clientes
```
GET    /api/clientes?busca=
POST   /api/clientes
GET    /api/clientes/{id}
PUT    /api/clientes/{id}
GET    /api/clientes/{id}/veiculos
GET    /api/clientes/{id}/pedidos
```

### Veículos
```
GET    /api/veiculos?placa=
POST   /api/veiculos
GET    /api/veiculos/{id}
POST   /api/veiculos/{id}/consultar   -- via ConsultaVeicularProvider (interface)
GET    /api/veiculos/{id}/historico-consultas
```

### Serviços
```
GET    /api/servicos              -- via ServicoRepository (interface)
POST   /api/servicos
PUT    /api/servicos/{id}
DELETE /api/servicos/{id}
```

### Pedidos
```
GET    /api/pedidos?status=&clienteId=&de=&ate=
POST   /api/pedidos
GET    /api/pedidos/{id}
PATCH  /api/pedidos/{id}/status    { novoStatus }
GET    /api/pedidos/{id}/historico
POST   /api/pedidos/{id}/pagamento { valorCentavos, formaPagamento }
```

### Estoque
```
GET    /api/estoque/itens
POST   /api/estoque/itens
POST   /api/estoque/movimentacoes  { itemEstoqueId, tipo, quantidade, pedidoId? }
GET    /api/estoque/itens/baixo-estoque
```

### Dashboard
```
GET    /api/dashboard/resumo
GET    /api/dashboard/faturamento?de=&ate=
GET    /api/dashboard/servicos-mais-vendidos
GET    /api/dashboard/tempo-medio-producao
```

## 6. Fase 2 — Financeiro avançado

Evolui o módulo 7 (financeiro básico) para uma visão completa do negócio:
contas a receber, contas a pagar, balanço geral com comparativos, e metas.

### Entidades novas

```
ContaReceber
├── id, pedidoId (FK nullable), clienteId (FK), descricao
├── valorCentavos, dataVencimento, dataRecebimento
├── status (PENDENTE | RECEBIDO | ATRASADO)

ContaPagar
├── id, fornecedor, categoria (ex: insumos, aluguel, salários)
├── descricao, valorCentavos, dataVencimento, dataPagamento
├── status (PENDENTE | PAGO | ATRASADO)

Meta
├── id, tipo (FATURAMENTO | QUANTIDADE_PEDIDOS | SERVICO_ESPECIFICO)
├── servicoId (FK nullable, usado quando tipo = SERVICO_ESPECIFICO)
├── periodoReferencia (ex: "2026-09"), valorAlvo
```

### Endpoints novos

```
GET    /api/financeiro/contas-receber?status=&de=&ate=
POST   /api/financeiro/contas-receber
PATCH  /api/financeiro/contas-receber/{id}/receber

GET    /api/financeiro/contas-pagar?status=&de=&ate=
POST   /api/financeiro/contas-pagar
PATCH  /api/financeiro/contas-pagar/{id}/pagar

GET    /api/financeiro/balanco?de=&ate=
         -- receitas, despesas, lucro líquido do período

GET    /api/financeiro/comparativo?periodoA=&periodoB=
         -- ex: mês atual vs mês anterior, ou vs mesmo mês do ano anterior

GET    /api/metas?periodo=
POST   /api/metas
GET    /api/metas/{id}/progresso
         -- valor realizado vs valor alvo, % de atingimento
```

### Observações de negócio

- `ContaReceber` pode nascer automaticamente a partir de um `Pedido` com
  pagamento pendente (ex: pagamento parcelado ou "pagar na entrega") — evita
  digitação duplicada entre os módulos de Pedidos e Financeiro.
- O balanço geral cruza `ContaReceber` recebidas + `ContaPagar` pagas no
  período; o comparativo aplica o mesmo cálculo em dois períodos e mostra a
  variação percentual.
- Metas por serviço específico (ex: "vender 100 pares de placas Mercosul em
  setembro") permitem ao dashboard mostrar progresso por linha de serviço, não
  só faturamento total.

## 7. Stack técnica recomendada

- **Spring Boot 3.x** (Web, Security, Data JPA, Validation)
- **PostgreSQL** — instância própria do sistema interno
- **Flyway** para migrations versionadas
- **Spring Security + JWT** para autenticação do painel interno
- **springdoc-openapi** para documentação Swagger (mantida como contrato para
  a futura integração — ver seção 2)
- Camadas simples (`controller` → `service` → `repository`), com as exceções
  de `ConsultaVeicularProvider` e `ServicoRepository` sendo interfaces desde
  o início, mesmo com implementação única por enquanto

## 8. Perfis de acesso (papéis)

| Papel | Pode fazer |
|---|---|
| ATENDENTE | Criar/editar pedidos, consultar veículos |
| GERENTE | Tudo do atendente + editar catálogo de serviços, ver dashboard/financeiro básico |
| ADMIN | Tudo + gerenciar usuários internos + financeiro avançado (Fase 2) e metas |

## 9. Fora de escopo por enquanto

- Front-end React (fica para depois)
- Integração de fato com o site público (estrutura preparada, implementação é
  posterior — seção 2)
- Notificações automáticas ao cliente (WhatsApp Business API)
- Multi-loja / multi-unidade

## 10. Próximos passos sugeridos

1. Modelar o schema definitivo no Flyway a partir da seção 3 (já incluindo os
   campos `codigoExterno` e `origemLead`, mesmo vazios)
2. Implementar módulos na ordem: Auth → Serviços → Clientes/Veículos → Pedidos
   → Estoque → Financeiro básico → Dashboard
3. Definir o provedor de consulta veicular do sistema interno (pode ser o
   mesmo do site público ou outro — a interface `ConsultaVeicularProvider`
   torna essa escolha reversível)
4. Fase 2: implementar Financeiro avançado (seção 6)
5. Fase 3: avaliar a integração real entre os sistemas (seção 2), quando fizer
   sentido para o negócio