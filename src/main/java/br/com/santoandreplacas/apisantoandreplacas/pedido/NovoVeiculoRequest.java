package br.com.santoandreplacas.apisantoandreplacas.pedido;

public record NovoVeiculoRequest(String placa, String marcaModelo, Integer anoFabricacao, Integer anoModelo, String chassi) {}
