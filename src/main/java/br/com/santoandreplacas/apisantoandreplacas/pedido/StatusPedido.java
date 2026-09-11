package br.com.santoandreplacas.apisantoandreplacas.pedido;

public enum StatusPedido {
    RECEBIDO("Aguardando início"),
    EM_PROCESSAMENTO("Em produção"),
    PLACA_PRONTA("Pronto para retirada"),
    ENTREGUE("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}