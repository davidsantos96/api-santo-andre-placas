package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/veiculos")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @GetMapping
    public List<VeiculoResponse> listar(@RequestParam(required = false) String placa,
                                         @RequestParam(required = false) Long clienteId) {
        return veiculoService.listar(placa, clienteId).stream()
                .map(VeiculoResponse::fromEntity)
                .collect(Collectors.toList());
    }
    @GetMapping("/{id}")
    public VeiculoResponse buscarPorId(@PathVariable Long id) {
        return VeiculoResponse.fromEntity(veiculoService.buscarPorId(id));
    }

    @PostMapping
    public Veiculo criar(@RequestBody Veiculo veiculo) {
        return veiculoService.criar(veiculo);
    }
}
