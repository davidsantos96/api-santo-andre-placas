package br.com.santoandreplacas.apisantoandreplacas.estoque;

import java.time.LocalDateTime;

public record VinculoServicoItemResponse(
        Long id,
        Long servicoId,
        String servicoNome,
        Long itemEstoqueId,
        String itemEstoqueNome,
        int quantidadeNecessaria,
        LocalDateTime criadoEm,
        String criadoPor,
        Long criadoPorId
) {
    public static VinculoServicoItemResponse fromEntity(ServicoItemEstoque vinculo) {
        return new VinculoServicoItemResponse(
                vinculo.getId(),
                vinculo.getServico().getId(),
                vinculo.getServico().getNome(),
                vinculo.getItemEstoque().getId(),
                vinculo.getItemEstoque().getNome(),
                vinculo.getQuantidadeNecessaria(),
                vinculo.getCriadoEm(),
                vinculo.getCriadoPor(),
                vinculo.getCriadoPorUsuario() != null ? vinculo.getCriadoPorUsuario().getId() : null
        );
    }
}
