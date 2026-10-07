package br.com.santoandreplacas.apisantoandreplacas.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    // entidade (enum) e entidadeId (Long) aceitam IS NULL no PostgreSQL;
    // ver a convenção de parâmetros opcionais no CONTEXT.md.
    @Query("""
            SELECT r FROM RegistroAuditoria r
            WHERE (:entidade IS NULL OR r.entidade = :entidade)
              AND (:entidadeId IS NULL OR r.entidadeId = :entidadeId)
            ORDER BY r.feitoEm DESC, r.id DESC
            """)
    List<RegistroAuditoria> buscar(@Param("entidade") EntidadeAuditada entidade,
                                   @Param("entidadeId") Long entidadeId);
}
