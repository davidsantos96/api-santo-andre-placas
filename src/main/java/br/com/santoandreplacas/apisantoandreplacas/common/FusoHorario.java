package br.com.santoandreplacas.apisantoandreplacas.common;

import java.time.ZoneId;

public final class FusoHorario {

    // Os timestamps do banco são LocalDateTime sem fuso; fixar São Paulo evita que
    // o horário gravado dependa do fuso padrão da máquina onde a API roda.
    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private FusoHorario() {
    }
}
