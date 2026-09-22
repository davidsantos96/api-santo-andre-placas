package br.com.santoandreplacas.apisantoandreplacas.estoque;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estoque")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping("/itens")
    public List<ItemEstoqueResponse> listarItens() {
        return estoqueService.listarItens().stream()
                .map(ItemEstoqueResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/itens/baixo-estoque")
    public List<ItemEstoqueResponse> listarBaixoEstoque() {
        return estoqueService.listarBaixoEstoque().stream()
                .map(ItemEstoqueResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping("/itens")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ItemEstoqueResponse criarItem(@RequestBody ItemEstoque item) {
        return ItemEstoqueResponse.fromEntity(estoqueService.criarItem(item));
    }

    @PostMapping("/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public MovimentacaoEstoqueResponse registrarMovimentacao(@RequestBody NovaMovimentacaoRequest request) {
        return MovimentacaoEstoqueResponse.fromEntity(estoqueService.registrarMovimentacao(request));
    }

    @GetMapping("/vinculos")
    public List<VinculoServicoItemResponse> listarVinculos(@RequestParam Long servicoId) {
        return estoqueService.listarVinculosPorServico(servicoId).stream()
                .map(VinculoServicoItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping("/vinculos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public VinculoServicoItemResponse criarVinculo(@RequestBody NovoVinculoRequest request) {
        return VinculoServicoItemResponse.fromEntity(estoqueService.criarVinculo(request));
    }
}
