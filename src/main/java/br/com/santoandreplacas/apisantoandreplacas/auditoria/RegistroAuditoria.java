package br.com.santoandreplacas.apisantoandreplacas.auditoria;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Trilha de auditoria para ações administrativas que não têm histórico próprio
 * (pedido usa PedidoStatusHistorico, pagamento e movimentação de estoque têm
 * o autor na própria linha). Uma linha por campo alterado, em ATUALIZACAO.
 *
 * Nunca guarda valor de senha — RESET_SENHA registra apenas que ocorreu.
 */
@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private EntidadeAuditada entidade;

    private Long entidadeId;

    // Snapshot de como identificar a entidade (nome do serviço, e-mail do
    // usuário) na hora da ação: sobrevive a renomeação posterior.
    private String entidadeDescricao;

    @Enumerated(EnumType.STRING)
    private AcaoAuditada acao;

    private String campo;
    private String valorAnterior;
    private String valorNovo;

    private String feitoPor;
    private LocalDateTime feitoEm;

    public Long getId() {
        return id;
    }

    public EntidadeAuditada getEntidade() {
        return entidade;
    }

    public void setEntidade(EntidadeAuditada entidade) {
        this.entidade = entidade;
    }

    public Long getEntidadeId() {
        return entidadeId;
    }

    public void setEntidadeId(Long entidadeId) {
        this.entidadeId = entidadeId;
    }

    public String getEntidadeDescricao() {
        return entidadeDescricao;
    }

    public void setEntidadeDescricao(String entidadeDescricao) {
        this.entidadeDescricao = entidadeDescricao;
    }

    public AcaoAuditada getAcao() {
        return acao;
    }

    public void setAcao(AcaoAuditada acao) {
        this.acao = acao;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(String valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }

    public void setValorNovo(String valorNovo) {
        this.valorNovo = valorNovo;
    }

    public String getFeitoPor() {
        return feitoPor;
    }

    public void setFeitoPor(String feitoPor) {
        this.feitoPor = feitoPor;
    }

    public LocalDateTime getFeitoEm() {
        return feitoEm;
    }

    public void setFeitoEm(LocalDateTime feitoEm) {
        this.feitoEm = feitoEm;
    }
}
