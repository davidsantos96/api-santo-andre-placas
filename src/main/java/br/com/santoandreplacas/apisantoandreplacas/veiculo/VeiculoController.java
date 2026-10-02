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
    public VeiculoResponse criar(@RequestBody NovoVeiculoRequest request) {
        return VeiculoResponse.fromEntity(veiculoService.criar(request));
    }

    @PutMapping("/{id}")
    public VeiculoResponse atualizar(@PathVariable Long id, @RequestBody AtualizarVeiculoRequest request) {
        return VeiculoResponse.fromEntity(veiculoService.atualizar(id, request));
    }

    @PostMapping("/{id}/consultar")
    public ConsultaVeicularResultado consultar(@PathVariable Long id) {
        return veiculoService.consultar(id);
    }

    @GetMapping("/{id}/historico-consultas")
    public List<Object> historicoConsultas(@PathVariable Long id) {
        veiculoService.buscarPorId(id); // valida que o veículo existe
        return List.of(); // nenhuma consulta é persistida enquanto não há provedor real (placeholder)
    }
}
