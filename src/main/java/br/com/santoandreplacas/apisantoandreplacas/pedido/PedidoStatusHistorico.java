package br.com.santoandreplacas.apisantoandreplacas.pedido;

import br.com.santoandreplacas.apisantoandreplacas.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedido_status_historico")
public class PedidoStatusHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private StatusPedido statusAnterior;

    @Enumerated(EnumType.STRING)
    private StatusPedido statusNovo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alterado_por_usuario_id")
    private Usuario alteradoPorUsuario; // nulo quando não há usuário autenticado no contexto

    private String alteradoPor; // snapshot do nome na hora da ação (ver UsuarioAutenticadoProvider)

    private LocalDateTime alteradoEm;

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public StatusPedido getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(StatusPedido statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public StatusPedido getStatusNovo() {
        return statusNovo;
    }

    public void setStatusNovo(StatusPedido statusNovo) {
        this.statusNovo = statusNovo;
    }

    public Usuario getAlteradoPorUsuario() {
        return alteradoPorUsuario;
    }

    public void setAlteradoPorUsuario(Usuario alteradoPorUsuario) {
        this.alteradoPorUsuario = alteradoPorUsuario;
    }

    public String getAlteradoPor() {
        return alteradoPor;
    }

    public void setAlteradoPor(String alteradoPor) {
        this.alteradoPor = alteradoPor;
    }

    public LocalDateTime getAlteradoEm() {
        return alteradoEm;
    }

    public void setAlteradoEm(LocalDateTime alteradoEm) {
        this.alteradoEm = alteradoEm;
    }
}