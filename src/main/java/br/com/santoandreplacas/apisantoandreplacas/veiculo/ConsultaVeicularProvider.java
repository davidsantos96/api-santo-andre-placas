package br.com.santoandreplacas.apisantoandreplacas.veiculo;

/**
 * Porta para consulta veicular externa (spec, seção 2) — hoje só existe a
 * implementação placeholder {@link ConsultaVeicularIndisponivelProvider},
 * já que nenhum provedor foi escolhido/contratado ainda.
 */
public interface ConsultaVeicularProvider {
    ConsultaVeicularResultado consultar(String placa);
}
