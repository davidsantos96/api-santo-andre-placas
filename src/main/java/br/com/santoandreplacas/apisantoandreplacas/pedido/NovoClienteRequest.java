package br.com.santoandreplacas.apisantoandreplacas.pedido;

public record NovoClienteRequest(String nome, String telefone, String cpfCnpj, String email) {}