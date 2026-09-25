package br.com.santoandreplacas.apisantoandreplacas.dashboard;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public ResumoResponse resumo() {
        return dashboardService.resumo();
    }

    @GetMapping("/faturamento")
    public FaturamentoResponse faturamento(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {

        LocalDate hoje = LocalDate.now();
        LocalDate inicio = de != null ? de : hoje;
        LocalDate fim = ate != null ? ate : inicio;

        return dashboardService.faturamento(inicio, fim);
    }

    @GetMapping("/servicos-mais-vendidos")
    public List<ServicoMaisVendidoResponse> servicosMaisVendidos() {
        return dashboardService.servicosMaisVendidos();
    }

    @GetMapping("/tempo-medio-producao")
    public TempoMedioProducaoResponse tempoMedioProducao() {
        return dashboardService.tempoMedioProducao();
    }
}
