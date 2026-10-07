package br.com.santoandreplacas.apisantoandreplacas.cliente;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar(@RequestParam(required = false) String busca) {
        return clienteService.listar(busca).stream()
                .map(clienteService::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable Long id) {
        return clienteService.toResponse(clienteService.buscarPorId(id));
    }

    @PostMapping
    public ClienteResponse criar(@RequestBody Cliente cliente) {
        return clienteService.toResponse(clienteService.criar(cliente));
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(@PathVariable Long id, @RequestBody Cliente cliente) {
        return clienteService.toResponse(clienteService.atualizar(id, cliente));
    }
}