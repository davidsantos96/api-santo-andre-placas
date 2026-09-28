package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import org.springframework.stereotype.Component;

/**
 * Placeholder: nenhum provedor de consulta veicular foi escolhido/contratado
 * ainda (decisão registrada no CONTEXT.md). Troque por uma implementação
 * real de {@link ConsultaVeicularProvider} quando isso acontecer — o resto
 * do sistema (endpoint, front) já está pronto pra consumir.
 */
@Component
public class ConsultaVeicularIndisponivelProvider implements ConsultaVeicularProvider {

    @Override
    public ConsultaVeicularResultado consultar(String placa) {
        throw new IllegalStateException("Consulta veicular ainda não está disponível.");
    }
}
