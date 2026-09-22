package br.com.santoandreplacas.apisantoandreplacas.estoque;

import br.com.santoandreplacas.apisantoandreplacas.pedido.Pedido;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_estoque_id", nullable = false)
    private ItemEstoque itemEstoque;

    @Enumerated(EnumType.STRING)
    private TipoMovimentacao tipo;

    private int quantidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido; // nullable — só preenchido quando a movimentação vem de um pedido

    private LocalDateTime criadoEm;

    public Long getId() {
        return id;
    }

    public ItemEstoque getItemEstoque() {
        return itemEstoque;
    }

    public void setItemEstoque(ItemEstoque itemEstoque) {
        this.itemEstoque = itemEstoque;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacao tipo) {
        this.tipo = tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade movimentada deve ser maior que zero");
        }
        this.quantidade = quantidade;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
