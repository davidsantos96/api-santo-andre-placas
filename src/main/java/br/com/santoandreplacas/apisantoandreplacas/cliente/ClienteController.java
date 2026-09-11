package br.com.santoandreplacas.apisantoandreplacas.cliente;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listarTodos().stream()
                .map(ClienteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable Long id) {
        return ClienteResponse.fromEntity(clienteService.buscarPorId(id));
    }

    @PostMapping
    public Cliente criar(@RequestBody Cliente cliente) {
        return clienteService.criar(cliente);
    }
}