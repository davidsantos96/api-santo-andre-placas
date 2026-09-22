package br.com.santoandreplacas.apisantoandreplacas.pedido;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/{id}/historico")
    public List<PedidoStatusHistoricoResponse> listarHistorico(@PathVariable Long id) {
        return pedidoService.listarHistorico(id).stream()
                .map(PedidoStatusHistoricoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<Pedido> listar() {
        return pedidoService.listarTodos();
    }

    @GetMapping("/{id}")
    public Pedido buscarPorId(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    @PostMapping
    public Pedido criar(@RequestBody Pedido pedido) {
        return pedidoService.criar(pedido);
    }

    @PatchMapping("/{id}/status")
    public Pedido mudarStatus(@PathVariable Long id, @RequestBody MudarStatusRequest request) {
        return pedidoService.mudarStatus(id, request.novoStatus());
    }

    @PostMapping("/completo")
    public Pedido criarCompleto(@RequestBody PedidoCompletoRequest request) {
        return pedidoService.criarPedidoCompleto(request);
    }
}