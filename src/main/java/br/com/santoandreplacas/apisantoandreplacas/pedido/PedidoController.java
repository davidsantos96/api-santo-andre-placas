package br.com.santoandreplacas.apisantoandreplacas.pedido;

import br.com.santoandreplacas.apisantoandreplacas.financeiro.NovoPagamentoRequest;
import br.com.santoandreplacas.apisantoandreplacas.financeiro.PagamentoResponse;
import br.com.santoandreplacas.apisantoandreplacas.financeiro.PagamentoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PagamentoService pagamentoService;

    public PedidoController(PedidoService pedidoService, PagamentoService pagamentoService) {
        this.pedidoService = pedidoService;
        this.pagamentoService = pagamentoService;
    }

    @GetMapping("/{id}/historico")
    public List<PedidoStatusHistoricoResponse> listarHistorico(@PathVariable Long id) {
        return pedidoService.listarHistorico(id).stream()
                .map(PedidoStatusHistoricoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return pedidoService.listarTodos().stream()
                .map(PedidoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PedidoResponse buscarPorId(@PathVariable Long id) {
        return PedidoResponse.fromEntity(pedidoService.buscarPorId(id));
    }

    @PostMapping
    public PedidoResponse criar(@RequestBody Pedido pedido) {
        return PedidoResponse.fromEntity(pedidoService.criar(pedido));
    }

    @PatchMapping("/{id}/status")
    public PedidoResponse mudarStatus(@PathVariable Long id, @RequestBody MudarStatusRequest request) {
        return PedidoResponse.fromEntity(pedidoService.mudarStatus(id, request.novoStatus()));
    }

    @PostMapping("/completo")
    public PedidoResponse criarCompleto(@RequestBody PedidoCompletoRequest request) {
        return PedidoResponse.fromEntity(pedidoService.criarPedidoCompleto(request));
    }

    @PostMapping("/{id}/pagamento")
    public PagamentoResponse registrarPagamento(@PathVariable Long id, @RequestBody NovoPagamentoRequest request) {
        return PagamentoResponse.fromEntity(pagamentoService.registrarPagamento(id, request));
    }

    @GetMapping("/{id}/pagamentos")
    public List<PagamentoResponse> listarPagamentos(@PathVariable Long id) {
        return pagamentoService.listarPorPedido(id).stream()
                .map(PagamentoResponse::fromEntity)
                .collect(Collectors.toList());
    }
}