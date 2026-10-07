package br.com.santoandreplacas.apisantoandreplacas.financeiro;
import br.com.santoandreplacas.apisantoandreplacas.exception.RecursoNaoEncontradoException;
import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;

import br.com.santoandreplacas.apisantoandreplacas.pedido.Pedido;
import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoRepository;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioAutenticadoProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public PagamentoService(PagamentoRepository pagamentoRepository, PedidoRepository pedidoRepository,
                            UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    public Pagamento registrarPagamento(Long pedidoId, NovoPagamentoRequest request) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + pedidoId));

        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setValorCentavos(request.valorCentavos());
        pagamento.setFormaPagamento(request.formaPagamento());
        pagamento.setStatus(StatusPagamento.PAGO);
        pagamento.setPagoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));
        pagamento.setRegistradoPorUsuario(usuarioAutenticadoProvider.usuarioAtual().orElse(null));
        pagamento.setRegistradoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());

        return pagamentoRepository.save(pagamento);
    }

    @Transactional(readOnly = true)
    public List<Pagamento> listarPorPedido(Long pedidoId) {
        return pagamentoRepository.findByPedidoId(pedidoId);
    }

    @Transactional(readOnly = true)
    public List<Pagamento> listar(LocalDate de, LocalDate ate, FormaPagamento forma) {
        if (ate.isBefore(de)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }

        List<Pagamento> pagamentos = pagamentoRepository.findByStatusAndPagoEmBetweenOrderByPagoEmDesc(
                StatusPagamento.PAGO, de.atStartOfDay(), ate.plusDays(1).atStartOfDay());

        if (forma == null) {
            return pagamentos;
        }

        return pagamentos.stream()
                .filter(pagamento -> pagamento.getFormaPagamento() == forma)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FechamentoCaixaResponse fecharCaixa(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }

        LocalDateTime inicio = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();

        List<Pagamento> pagamentos = pagamentoRepository.findByStatusAndPagoEmBetween(
                StatusPagamento.PAGO, inicio, fim);

        Map<FormaPagamento, List<Pagamento>> porForma = pagamentos.stream()
                .collect(Collectors.groupingBy(Pagamento::getFormaPagamento));

        List<TotalPorFormaPagamento> totalPorForma = porForma.entrySet().stream()
                .map(entry -> new TotalPorFormaPagamento(
                        entry.getKey(),
                        entry.getValue().stream().mapToLong(Pagamento::getValorCentavos).sum(),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(TotalPorFormaPagamento::formaPagamento))
                .collect(Collectors.toList());

        long totalGeral = pagamentos.stream().mapToLong(Pagamento::getValorCentavos).sum();

        return new FechamentoCaixaResponse(de, ate, totalGeral, pagamentos.size(), totalPorForma);
    }
}
