package br.com.santoandreplacas.apisantoandreplacas.cliente;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nome,
        String telefone,
        String cpfCnpj,
        String email,
        LocalDateTime criadoEm
) {
    public static ClienteResponse fromEntity(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getCpfCnpj(),
                cliente.getEmail(),
                cliente.getCriadoEm()
        );
    }
}