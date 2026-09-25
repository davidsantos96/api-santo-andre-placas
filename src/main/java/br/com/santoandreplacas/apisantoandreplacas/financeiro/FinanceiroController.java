package br.com.santoandreplacas.apisantoandreplacas.financeiro;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/financeiro")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class FinanceiroController {

    private final PagamentoService pagamentoService;

    public FinanceiroController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @GetMapping("/fechamento-caixa")
    public FechamentoCaixaResponse fecharCaixa(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        LocalDate hoje = LocalDate.now();
        LocalDate inicio = de != null ? de : hoje;
        LocalDate fim = ate != null ? ate : inicio;

        return pagamentoService.fecharCaixa(inicio, fim);
    }
}
