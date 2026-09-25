package br.com.santoandreplacas.apisantoandreplacas.dashboard;

import br.com.santoandreplacas.apisantoandreplacas.estoque.ItemEstoqueRepository;
import br.com.santoandreplacas.apisantoandreplacas.financeiro.Pagamento;
import br.com.santoandreplacas.apisantoandreplacas.financeiro.PagamentoRepository;
import br.com.santoandreplacas.apisantoandreplacas.financeiro.StatusPagamento;
import br.com.santoandreplacas.apisantoandreplacas.pedido.Pedido;
import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoRepository;
import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoStatusHistorico;
import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoStatusHistoricoRepository;
import br.com.santoandreplacas.apisantoandreplacas.pedido.StatusPedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ItemEstoqueRepository itemEstoqueRepository;
    private final PedidoStatusHistoricoRepository historicoRepository;

    public DashboardService(PedidoRepository pedidoRepository,
                            PagamentoRepository pagamentoRepository,
                            ItemEstoqueRepository itemEstoqueRepository,
                            PedidoStatusHistoricoRepository historicoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.itemEstoqueRepository = itemEstoqueRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional(readOnly = true)
    public ResumoResponse resumo() {
        LocalDate hoje = LocalDate.now();
        List<Pedido> todosPedidos = pedidoRepository.findAll();

        long pedidosHoje = todosPedidos.stream()
                .filter(p -> p.getCriadoEm() != null && !p.getCriadoEm().toLocalDate().isBefore(hoje))
                .count();

        Map<StatusPedido, Long> pedidosPorStatus = todosPedidos.stream()
                .collect(Collectors.groupingBy(Pedido::getStatus, Collectors.counting()));

        long faturamentoHoje = pagamentoRepository
                .findByStatusAndPagoEmBetween(StatusPagamento.PAGO, hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay())
                .stream()
                .mapToLong(Pagamento::getValorCentavos)
                .sum();

        long itensBaixoEstoque = itemEstoqueRepository.findBaixoEstoque().size();

        return new ResumoResponse(pedidosHoje, pedidosPorStatus, faturamentoHoje, itensBaixoEstoque);
    }

    @Transactional(readOnly = true)
    public FaturamentoResponse faturamento(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }

        List<Pagamento> pagamentos = pagamentoRepository.findByStatusAndPagoEmBetween(
                StatusPagamento.PAGO, de.atStartOfDay(), ate.plusDays(1).atStartOfDay());

        Map<LocalDate, Long> porDia = pagamentos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPagoEm().toLocalDate(),
                        Collectors.summingLong(Pagamento::getValorCentavos)));

        List<FaturamentoPorDia> lista = porDia.entrySet().stream()
                .map(entry -> new FaturamentoPorDia(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FaturamentoPorDia::data))
                .collect(Collectors.toList());

        long total = pagamentos.stream().mapToLong(Pagamento::getValorCentavos).sum();

        return new FaturamentoResponse(de, ate, total, lista);
    }

    @Transactional(readOnly = true)
    public List<ServicoMaisVendidoResponse> servicosMaisVendidos() {
        List<Pedido> pedidos = pedidoRepository.findAll();

        Map<Long, List<Pedido>> porServico = pedidos.stream()
                .collect(Collectors.groupingBy(p -> p.getServico().getId()));

        return porServico.values().stream()
                .map(lista -> {
                    Pedido exemplo = lista.get(0);
                    long faturamentoNominal = lista.stream()
                            .mapToLong(p -> p.getServico().getPrecoCentavos())
                            .sum();
                    return new ServicoMaisVendidoResponse(
                            exemplo.getServico().getId(),
                            exemplo.getServico().getNome(),
                            lista.size(),
                            faturamentoNominal);
                })
                .sorted(Comparator.comparing(ServicoMaisVendidoResponse::quantidadePedidos).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Tempo médio de produção = intervalo entre o pedido entrar em
     * EM_PROCESSAMENTO ("Em produção") e chegar a PLACA_PRONTA ("Pronto
     * para retirada") — não usa RECEBIDO nem ENTREGUE porque esses
     * representam espera, não produção de fato (ver descrição dos enums
     * em StatusPedido).
     */
    @Transactional(readOnly = true)
    public TempoMedioProducaoResponse tempoMedioProducao() {
        List<PedidoStatusHistorico> entradas = historicoRepository.findByStatusNovoIn(
                List.of(StatusPedido.EM_PROCESSAMENTO, StatusPedido.PLACA_PRONTA));

        Map<Long, List<PedidoStatusHistorico>> porPedido = entradas.stream()
                .collect(Collectors.groupingBy(h -> h.getPedido().getId()));

        List<Long> duracoesEmMinutos = porPedido.values().stream()
                .map(this::calcularDuracaoProducao)
                .filter(duracao -> duracao != null)
                .collect(Collectors.toList());

        if (duracoesEmMinutos.isEmpty()) {
            return new TempoMedioProducaoResponse(0, 0);
        }

        double mediaEmMinutos = duracoesEmMinutos.stream().mapToLong(Long::longValue).average().orElse(0);

        return new TempoMedioProducaoResponse(mediaEmMinutos / 60.0, duracoesEmMinutos.size());
    }

    private Long calcularDuracaoProducao(List<PedidoStatusHistorico> historicoDoPedido) {
        LocalDateTime inicioProducao = historicoDoPedido.stream()
                .filter(h -> h.getStatusNovo() == StatusPedido.EM_PROCESSAMENTO)
                .map(PedidoStatusHistorico::getAlteradoEm)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime fimProducao = historicoDoPedido.stream()
                .filter(h -> h.getStatusNovo() == StatusPedido.PLACA_PRONTA)
                .map(PedidoStatusHistorico::getAlteradoEm)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        if (inicioProducao == null || fimProducao == null || fimProducao.isBefore(inicioProducao)) {
            return null;
        }

        return Duration.between(inicioProducao, fimProducao).toMinutes();
    }
}
