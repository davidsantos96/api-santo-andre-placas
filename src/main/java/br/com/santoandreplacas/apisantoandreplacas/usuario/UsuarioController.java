package br.com.santoandreplacas.apisantoandreplacas.usuario;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream()
                .map(UsuarioResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping
    public UsuarioResponse criar(@RequestBody NovoUsuarioRequest request) {
        return UsuarioResponse.fromEntity(usuarioService.criar(request));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody AtualizarUsuarioRequest request) {
        return UsuarioResponse.fromEntity(usuarioService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse atualizarStatus(@PathVariable Long id, @RequestBody AtualizarStatusUsuarioRequest request) {
        return UsuarioResponse.fromEntity(usuarioService.atualizarStatus(id, request.ativo()));
    }
}
