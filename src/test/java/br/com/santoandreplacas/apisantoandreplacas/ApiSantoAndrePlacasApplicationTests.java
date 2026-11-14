package br.com.santoandreplacas.apisantoandreplacas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApiSantoAndrePlacasApplicationTests {

    static { ApiDeTeste.prepararSenhaAdmin(); }

    @Test
    void contextLoads() {
    }

}
