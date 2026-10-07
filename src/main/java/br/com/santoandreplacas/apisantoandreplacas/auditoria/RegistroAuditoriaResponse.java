package br.com.santoandreplacas.apisantoandreplacas.auditoria;

import java.time.LocalDateTime;

public record RegistroAuditoriaResponse(
        Long id,
        EntidadeAuditada entidade,
        Long entidadeId,
        String entidadeDescricao,
        AcaoAuditada acao,
        String campo,
        String valorAnterior,
        String valorNovo,
        String feitoPor,
        LocalDateTime feitoEm
) {
    public static RegistroAuditoriaResponse fromEntity(RegistroAuditoria registro) {
        return new RegistroAuditoriaResponse(
                registro.getId(),
                registro.getEntidade(),
                registro.getEntidadeId(),
                registro.getEntidadeDescricao(),
                registro.getAcao(),
                registro.getCampo(),
                registro.getValorAnterior(),
                registro.getValorNovo(),
                registro.getFeitoPor(),
                registro.getFeitoEm()
        );
    }
}
