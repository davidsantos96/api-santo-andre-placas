package br.com.santoandreplacas.apisantoandreplacas.estoque;

import br.com.santoandreplacas.apisantoandreplacas.servico.Servico;
import br.com.santoandreplacas.apisantoandreplacas.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Vínculo entre um Servico e os ItemEstoque que ele consome — não faz parte
 * do modelo de domínio da spec (seção 3), mas é necessário para a regra de
 * negócio da seção 4 ("baixa automática nos itens de estoque vinculados ao
 * serviço do pedido"): sem essa tabela não há como saber quais itens dar
 * baixa. Ver CONTEXT.md.
 */
@Entity
@Table(name = "servico_item_estoque")
public class ServicoItemEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_estoque_id", nullable = false)
    private ItemEstoque itemEstoque;

    private int quantidadeNecessaria;

    // Só autoria de criação: não existe endpoint de edição nem de remoção do vínculo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_usuario_id")
    private Usuario criadoPorUsuario;

    private String criadoPor; // snapshot do nome na hora da ação

    private LocalDateTime criadoEm;

    public Usuario getCriadoPorUsuario() {
        return criadoPorUsuario;
    }

    public void setCriadoPorUsuario(Usuario criadoPorUsuario) {
        this.criadoPorUsuario = criadoPorUsuario;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(String criadoPor) {
        this.criadoPor = criadoPor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public Servico getServico() {
        return servico;
    }

    public void setServico(Servico servico) {
        this.servico = servico;
    }

    public ItemEstoque getItemEstoque() {
        return itemEstoque;
    }

    public void setItemEstoque(ItemEstoque itemEstoque) {
        this.itemEstoque = itemEstoque;
    }

    public int getQuantidadeNecessaria() {
        return quantidadeNecessaria;
    }

    public void setQuantidadeNecessaria(int quantidadeNecessaria) {
        if (quantidadeNecessaria <= 0) {
            throw new IllegalArgumentException("A quantidade necessária deve ser maior que zero");
        }
        this.quantidadeNecessaria = quantidadeNecessaria;
    }
}
