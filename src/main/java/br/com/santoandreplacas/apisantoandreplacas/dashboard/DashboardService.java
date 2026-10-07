package br.com.santoandreplacas.apisantoandreplacas.dashboard;
import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;

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
import java.time.temporal.ChronoUnit;
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
        LocalDate hoje = LocalDate.now(FusoHorario.SAO_PAULO);
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
    public List<ServicoMaisVendidoResponse> servicosMaisVendidos(LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate);

        List<Pedido> pedidos = pedidoRepository.findAll().stream()
                .filter(p -> p.getStatus() != StatusPedido.CANCELADO)
                .filter(p -> de == null || (p.getCriadoEm() != null
                        && !p.getCriadoEm().isBefore(de.atStartOfDay())
                        && p.getCriadoEm().isBefore(ate.plusDays(1).atStartOfDay())))
                .collect(Collectors.toList());

        Map<Long, List<Pedido>> porServico = pedidos.stream()
                .collect(Collectors.groupingBy(p -> p.getServico().getId()));

        return porServico.values().stream()
                .map(lista -> {
                    Pedido exemplo = lista.get(0);
                    long faturamentoNominal = lista.stream()
                            .mapToLong(Pedido::getPrecoCentavos)
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
    /**
     * Com período (de/ate), considera os pedidos que ficaram prontos dentro dele e
     * devolve também a média do período anterior (mesma duração, logo antes) em
     * horasMediaPeriodoAnterior, para o front mostrar a tendência. Sem período,
     * considera tudo e não há comparação.
     */
    @Transactional(readOnly = true)
    public TempoMedioProducaoResponse tempoMedioProducao(LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate);

        List<PedidoStatusHistorico> entradas = historicoRepository.findByStatusNovoIn(
                List.of(StatusPedido.EM_PROCESSAMENTO, StatusPedido.PLACA_PRONTA));

        Map<Long, List<PedidoStatusHistorico>> porPedido = entradas.stream()
                .collect(Collectors.groupingBy(h -> h.getPedido().getId()));

        List<Producao> producoes = porPedido.values().stream()
                .map(this::calcularProducao)
                .filter(producao -> producao != null)
                .collect(Collectors.toList());

        if (de == null) {
            return resumirProducoes(producoes, null);
        }

        long dias = ChronoUnit.DAYS.between(de, ate) + 1;
        LocalDate deAnterior = de.minusDays(dias);
        LocalDate ateAnterior = de.minusDays(1);

        TempoMedioProducaoResponse atual = resumirProducoes(filtrarPorFim(producoes, de, ate), null);
        List<Producao> anteriores = filtrarPorFim(producoes, deAnterior, ateAnterior);
        Double mediaAnterior = anteriores.isEmpty() ? null : mediaEmHoras(anteriores);

        return new TempoMedioProducaoResponse(atual.horasMedia(), atual.pedidosConsiderados(), mediaAnterior);
    }

    private void validarPeriodo(LocalDate de, LocalDate ate) {
        if ((de == null) != (ate == null)) {
            throw new IllegalArgumentException("Informe as duas datas (de e ate) ou nenhuma.");
        }
        if (de != null && ate.isBefore(de)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }
    }

    private List<Producao> filtrarPorFim(List<Producao> producoes, LocalDate de, LocalDate ate) {
        LocalDateTime inicio = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();
        return producoes.stream()
                .filter(p -> !p.fim().isBefore(inicio) && p.fim().isBefore(fim))
                .collect(Collectors.toList());
    }

    private TempoMedioProducaoResponse resumirProducoes(List<Producao> producoes, Double mediaAnterior) {
        if (producoes.isEmpty()) {
            return new TempoMedioProducaoResponse(0, 0, mediaAnterior);
        }
        return new TempoMedioProducaoResponse(mediaEmHoras(producoes), producoes.size(), mediaAnterior);
    }

    private double mediaEmHoras(List<Producao> producoes) {
        return producoes.stream().mapToLong(Producao::minutos).average().orElse(0) / 60.0;
    }

    private record Producao(LocalDateTime fim, long minutos) {
    }

    private Producao calcularProducao(List<PedidoStatusHistorico> historicoDoPedido) {
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

        return new Producao(fimProducao, Duration.between(inicioProducao, fimProducao).toMinutes());
    }
}
