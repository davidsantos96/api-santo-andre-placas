package br.com.santoandreplacas.apisantoandreplacas.auditoria;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<RegistroAuditoriaResponse> listar(@RequestParam(required = false) EntidadeAuditada entidade,
                                                  @RequestParam(required = false) Long entidadeId) {
        return auditoriaService.listar(entidade, entidadeId).stream()
                .map(RegistroAuditoriaResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
