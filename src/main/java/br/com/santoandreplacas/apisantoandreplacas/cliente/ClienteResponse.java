package br.com.santoandreplacas.apisantoandreplacas.cliente;

import java.time.LocalDateTime;

// totalPedidos só vem preenchido nos endpoints de /clientes; quando o cliente
// aparece aninhado em outra resposta (ex.: PedidoResponse) fica null.
public record ClienteResponse(
        Long id,
        String nome,
        String telefone,
        String cpfCnpj,
        String email,
        LocalDateTime criadoEm,
        String criadoPor,
        Long criadoPorId,
        LocalDateTime atualizadoEm,
        String atualizadoPor,
        Long atualizadoPorId,
        Long totalPedidos
) {
    public static ClienteResponse fromEntity(Cliente cliente) {
        return fromEntity(cliente, null);
    }

    public static ClienteResponse fromEntity(Cliente cliente, Long totalPedidos) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getCpfCnpj(),
                cliente.getEmail(),
                cliente.getCriadoEm(),
                cliente.getCriadoPor(),
                cliente.getCriadoPorUsuario() != null ? cliente.getCriadoPorUsuario().getId() : null,
                cliente.getAtualizadoEm(),
                cliente.getAtualizadoPor(),
                cliente.getAtualizadoPorUsuario() != null ? cliente.getAtualizadoPorUsuario().getId() : null,
                totalPedidos
        );
    }
}
