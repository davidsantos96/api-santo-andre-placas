package br.com.santoandreplacas.apisantoandreplacas.estoque;

public record NovoVinculoRequest(
        Long servicoId,
        Long itemEstoqueId,
        int quantidadeNecessaria
) {
}
