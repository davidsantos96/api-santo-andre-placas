package br.com.santoandreplacas.apisantoandreplacas.pedido;

public record NovoVeiculoRequest(String placa, String marcaModelo, int anoFabricacao, int anoModelo, String chassi) {}