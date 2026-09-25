package br.com.santoandreplacas.apisantoandreplacas.financeiro;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pagamentos")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @GetMapping
    public List<PagamentoListagemResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) FormaPagamento forma) {

        LocalDate hoje = LocalDate.now();
        LocalDate inicio = de != null ? de : hoje;
        LocalDate fim = ate != null ? ate : inicio;

        return pagamentoService.listar(inicio, fim, forma).stream()
                .map(PagamentoListagemResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
