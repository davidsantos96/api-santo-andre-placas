package br.com.santoandreplacas.apisantoandreplacas.servico;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }
    @GetMapping
    public List<ServicoResponse> listar() {
        return servicoService.listarAtivos().stream()
                .map(ServicoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ServicoResponse buscarPorId(@PathVariable Long id) {
        return ServicoResponse.fromEntity(servicoService.buscarPorId(id));
    }

    @PostMapping
    public Servico criar(@RequestBody Servico servico) {
        return servicoService.criar(servico);
    }

    @PutMapping("/{id}")
    public Servico atualizar(@PathVariable Long id, @RequestBody Servico servico) {
        return servicoService.atualizar(id, servico);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        servicoService.deletar(id);
    }
}