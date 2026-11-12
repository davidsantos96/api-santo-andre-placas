package br.com.santoandreplacas.apisantoandreplacas;

import br.com.santoandreplacas.apisantoandreplacas.ApiDeTeste.Resposta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bordas que já deram problema ou que protegem o sistema de uso errado:
 * códigos de erro (o front decide o que mostrar pelo status), busca, e-mail
 * de usuário, administração de usuários e as regras de pedido/estoque.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrosEProtecoesTest {

    @Autowired MockMvc mvc;
    ApiDeTeste api;
    String admin;

    @BeforeEach
    void entrar() throws Exception {
        api = new ApiDeTeste(mvc);
        admin = api.loginAdmin();
    }

    private String criarELogar(String nome, String login, String papel) throws Exception {
        Resposta r = api.post("/api/usuarios", admin,
                "{\"nome\":\"" + nome + "\",\"login\":\"" + login + "\",\"papel\":\"" + papel + "\",\"senha\":\"senha123\"}");
        assertEquals(200, r.status(), r.texto());
        return api.login(login, "senha123");
    }

    // O front trata 401 como "sessão expirada" e abre o modal de login. Então
    // erro de uso (400/404) e falta de permissão (403) nunca podem sair como 401,
    // e todos precisam trazer { "mensagem": ... }.
    @Test
    void cada_tipo_de_erro_sai_com_o_status_certo_e_com_mensagem() throws Exception {
        Resposta semToken = api.get("/api/pedidos", null);
        assertEquals(401, semToken.status());
        assertNotNull(semToken.mensagem());

        Resposta enumInvalido = api.get("/api/pedidos?status=NAO_EXISTE", admin);
        assertEquals(400, enumInvalido.status());
        assertTrue(enumInvalido.mensagem().contains("status"), enumInvalido.mensagem());

        Resposta idInvalido = api.get("/api/clientes/abc", admin);
        assertEquals(400, idInvalido.status());
        assertNotNull(idInvalido.mensagem());

        Resposta parametroAusente = api.get("/api/estoque/vinculos", admin);
        assertEquals(400, parametroAusente.status());
        assertTrue(parametroAusente.mensagem().contains("servicoId"), parametroAusente.mensagem());

        Resposta rotaInexistente = api.get("/api/rota-que-nao-existe", admin);
        assertEquals(404, rotaInexistente.status());
        assertNotNull(rotaInexistente.mensagem());

        Resposta naoEncontrado = api.get("/api/pedidos/999999", admin);
        assertEquals(404, naoEncontrado.status());

        Resposta corpoQuebrado = api.post("/api/clientes", admin, "{ isto nao e json");
        assertEquals(400, corpoQuebrado.status());
        assertNotNull(corpoQuebrado.mensagem());

        // Sem permissão: 403 (e não 500, que é o que o handler genérico de
        // Exception faria se a negação do @PreAuthorize caísse nele).
        String atendente = criarELogar("Atendente Erros", "atendente.erros", "ATENDENTE");
        for (String rota : new String[]{"/api/usuarios", "/api/pagamentos", "/api/dashboard/resumo", "/api/auditoria?entidade=SERVICO&entidadeId=1"}) {
            Resposta negado = api.get(rota, atendente);
            assertEquals(403, negado.status(), rota);
            assertNotNull(negado.mensagem(), rota);
        }
    }

    // B17: a busca por nome extraía os dígitos do texto e casava com qualquer
    // telefone/CPF que tivesse aquele dígito.
    @Test
    void busca_de_cliente_por_nome_nao_casa_digitos_soltos_e_a_busca_por_digitos_continua() throws Exception {
        api.post("/api/clientes", admin,
                "{\"nome\":\"Marta Busca\",\"telefone\":\"(11) 92222-2222\",\"cpfCnpj\":\"222.222.222-22\",\"email\":\"\"}");

        assertEquals(0, api.get("/api/clientes?busca=Xyz2Inexistente", admin).corpo().size(),
                "o 2 no meio de um nome não deve casar com telefone/documento");

        assertTrue(api.get("/api/clientes?busca=marta", admin).corpo().size() >= 1, "nome, sem diferenciar caixa");
        assertTrue(api.get("/api/clientes?busca=22222222222", admin).corpo().size() >= 1, "documento só com dígitos");
        assertTrue(api.get("/api/clientes?busca=222.222.222-22", admin).corpo().size() >= 1, "documento mascarado");
        assertTrue(api.get("/api/clientes?busca=922222222", admin).corpo().size() >= 1, "telefone só com dígitos");
    }

    // B18 (agora no login) + login de rede: a pessoa entra pelo login, sem e-mail pessoal.
    @Test
    void login_nao_diferencia_maiusculas_e_aceita_login_ou_email_na_entrada() throws Exception {
        criarELogar("Caixa Um", "caixa.um", "ATENDENTE");

        Resposta duplicado = api.post("/api/usuarios", admin,
                "{\"login\":\"CAIXA.UM\",\"nome\":\"Caixa Dois\",\"papel\":\"ATENDENTE\",\"senha\":\"senha123\"}");
        assertEquals(400, duplicado.status());
        assertTrue(duplicado.mensagem().contains("Já existe"), duplicado.mensagem());

        // quem digita o login com outra caixa entra normalmente
        assertNotNull(api.login("Caixa.Um", "senha123"));

        // o formato antigo do corpo ({"email": ...}) continua sendo aceito
        Resposta antigo = api.post("/api/auth/login", null, "{\"email\":\"caixa.um\",\"senha\":\"senha123\"}");
        assertEquals(200, antigo.status(), antigo.texto());

        // senha errada: mesma mensagem para usuário inexistente e senha errada (não revela qual)
        Resposta senhaErrada = api.post("/api/auth/login", null, "{\"login\":\"caixa.um\",\"senha\":\"outra-senha\"}");
        Resposta inexistente = api.post("/api/auth/login", null, "{\"login\":\"ninguem.assim\",\"senha\":\"senha123\"}");
        assertEquals(400, senhaErrada.status());
        assertEquals(senhaErrada.mensagem(), inexistente.mensagem());
    }

    @Test
    void validacao_de_login_e_email_na_criacao_e_na_edicao() throws Exception {
        String base = "\"nome\":\"Fulano\",\"papel\":\"ATENDENTE\",\"senha\":\"senha123\"";

        for (String invalido : new String[]{"ab", "com espaco", "com@arroba", "Acentuação", "", "x".repeat(41)}) {
            Resposta r = api.post("/api/usuarios", admin, "{\"login\":\"" + invalido + "\"," + base + "}");
            assertEquals(400, r.status(), "login inválido deveria ser recusado: [" + invalido + "] " + r.texto());
        }
        assertEquals(400, api.post("/api/usuarios", admin, "{" + base + "}").status(), "sem login");

        // e-mail é opcional, mas se vier precisa ser válido e único
        assertEquals(400, api.post("/api/usuarios", admin, "{\"login\":\"fulano.a\",\"email\":\"nao-e-email\"," + base + "}").status());
        Resposta comEmail = api.post("/api/usuarios", admin, "{\"login\":\"fulano.b\",\"email\":\"Fulano.B@Exemplo.com\"," + base + "}");
        assertEquals(200, comEmail.status(), comEmail.texto());
        assertEquals("fulano.b", comEmail.corpo().get("login").asText());
        assertEquals(400, api.post("/api/usuarios", admin, "{\"login\":\"fulano.c\",\"email\":\"fulano.b@exemplo.com\"," + base + "}").status(),
                "e-mail repetido (outra caixa)");
        // dois usuários sem e-mail podem coexistir
        assertEquals(200, api.post("/api/usuarios", admin, "{\"login\":\"fulano.d\"," + base + "}").status());
        assertEquals(200, api.post("/api/usuarios", admin, "{\"login\":\"fulano.e\"," + base + "}").status());

        // quem tem e-mail entra por login OU por e-mail
        assertNotNull(api.login("fulano.b", "senha123"));
        assertNotNull(api.login("FULANO.B@exemplo.com", "senha123"));

        // ADMIN exige e-mail (recuperação de acesso)
        Resposta adminSemEmail = api.post("/api/usuarios", admin,
                "{\"login\":\"dono.sem.email\",\"nome\":\"Dono\",\"papel\":\"ADMIN\",\"senha\":\"senha123\"}");
        assertEquals(400, adminSemEmail.status());
        assertTrue(adminSemEmail.mensagem().contains("e-mail"), adminSemEmail.mensagem());
        // (criar um ADMIN válido aqui mudaria o banco compartilhado dos testes e quebraria o teste do último admin)

        // edição: trocar o login de outra pessoa funciona e o novo login passa a valer
        long id = comEmail.id();
        Resposta editado = api.put("/api/usuarios/" + id, admin,
                "{\"login\":\"fulano.novo\",\"nome\":\"Fulano\",\"email\":\"fulano.b@exemplo.com\",\"papel\":\"ATENDENTE\"}");
        assertEquals(200, editado.status(), editado.texto());
        assertNotNull(api.login("fulano.novo", "senha123"));
        assertEquals(400, api.post("/api/auth/login", null, "{\"login\":\"fulano.b\",\"senha\":\"senha123\"}").status(),
                "login antigo deixa de valer");
        // promover a ADMIN sem e-mail é recusado
        assertEquals(400, api.put("/api/usuarios/" + api.post("/api/usuarios", admin, "{\"login\":\"promovido\"," + base + "}").id(), admin,
                "{\"login\":\"promovido\",\"nome\":\"P\",\"papel\":\"ADMIN\"}").status());

        // o próprio login não pode ser alterado (derrubaria a sessão de quem edita)
        long adminId = -1;
        for (var u : api.get("/api/usuarios", admin).corpo()) {
            if ("admin".equals(u.get("login").asText())) adminId = u.get("id").asLong();
        }
        Resposta proprio = api.put("/api/usuarios/" + adminId, admin,
                "{\"login\":\"admin2\",\"nome\":\"Administrador\",\"email\":\"admin@santoandreplacas.com.br\",\"papel\":\"ADMIN\"}");
        assertEquals(400, proprio.status());
        assertTrue(proprio.mensagem().contains("próprio login"), proprio.mensagem());
    }

    @Test
    void usuario_antigo_sem_login_recebe_login_derivado_do_email_na_subida() throws Exception {
        // O admin do seed prova o caminho feliz; o derivado vem da parte local do e-mail.
        for (var u : api.get("/api/usuarios", admin).corpo()) {
            assertFalse(u.get("login").isNull() || u.get("login").asText().isBlank(), "todo usuário tem login: " + u);
        }
    }

    @Test
    void administracao_de_usuarios_tem_senha_minima_e_nao_deixa_o_sistema_sem_admin() throws Exception {
        Resposta senhaCurta = api.post("/api/usuarios", admin,
                "{\"login\":\"curta\",\"nome\":\"Curta\",\"papel\":\"ATENDENTE\",\"senha\":\"123\"}");
        assertEquals(400, senhaCurta.status());
        assertTrue(senhaCurta.mensagem().contains("pelo menos 6"), senhaCurta.mensagem());

        Resposta semPapel = api.post("/api/usuarios", admin,
                "{\"login\":\"sempapel\",\"nome\":\"Sem Papel\",\"senha\":\"senha123\"}");
        assertEquals(400, semPapel.status());

        long adminId = -1;
        for (var u : api.get("/api/usuarios", admin).corpo()) {
            if ("admin".equals(u.get("login").asText())) {
                adminId = u.get("id").asLong();
            }
        }

        Resposta resetCurto = api.patch("/api/usuarios/" + adminId + "/senha", admin, "{\"novaSenha\":\"abc\"}");
        assertEquals(400, resetCurto.status());

        Resposta desativarASiMesmo = api.patch("/api/usuarios/" + adminId + "/status", admin, "{\"ativo\":false}");
        assertEquals(400, desativarASiMesmo.status());
        assertTrue(desativarASiMesmo.mensagem().contains("próprio"), desativarASiMesmo.mensagem());

        // único ADMIN do banco de teste tentando virar GERENTE
        Resposta rebaixar = api.put("/api/usuarios/" + adminId, admin,
                "{\"login\":\"admin\",\"nome\":\"Administrador\",\"email\":\"admin@santoandreplacas.com.br\",\"papel\":\"GERENTE\"}");
        assertEquals(400, rebaixar.status());
        assertTrue(rebaixar.mensagem().contains("último administrador"), rebaixar.mensagem());

        // continua tudo como estava
        assertNotNull(api.loginAdmin());
        assertEquals(200, api.get("/api/usuarios", admin).status());
    }

    @Test
    void vinculo_servico_item_nao_duplica_pode_ser_removido_e_fica_no_historico_do_servico() throws Exception {
        Resposta servico = api.post("/api/servicos", admin,
                "{\"nome\":\"Servico Vinculo\",\"descricao\":\"\",\"categoria\":\"EMPLACAMENTO\",\"precoCentavos\":10000}");
        assertEquals(200, servico.status(), servico.texto());
        // resposta em DTO: sem campo interno, com o contador que a tela usa
        assertFalse(servico.corpo().has("codigoExterno"), servico.texto());
        assertTrue(servico.corpo().has("pedidosNoMes"), servico.texto());
        long servicoId = servico.id();

        long itemId = api.post("/api/estoque/itens", admin,
                "{\"nome\":\"Lacre Vinculo\",\"quantidade\":10,\"quantidadeMinima\":2}").id();
        String corpoVinculo = "{\"servicoId\":" + servicoId + ",\"itemEstoqueId\":" + itemId + ",\"quantidadeNecessaria\":2}";

        Resposta criado = api.post("/api/estoque/vinculos", admin, corpoVinculo);
        assertEquals(200, criado.status(), criado.texto());

        Resposta repetido = api.post("/api/estoque/vinculos", admin, corpoVinculo);
        assertEquals(400, repetido.status());
        assertTrue(repetido.mensagem().contains("já está vinculado"), repetido.mensagem());

        Resposta quantidadeZero = api.post("/api/estoque/vinculos", admin,
                "{\"servicoId\":" + servicoId + ",\"itemEstoqueId\":" + itemId + ",\"quantidadeNecessaria\":0}");
        assertEquals(400, quantidadeZero.status());

        String atendente = criarELogar("Atendente Vinculo", "atendente.vinculo", "ATENDENTE");
        assertEquals(403, api.delete("/api/estoque/vinculos/" + criado.id(), atendente).status());
        assertEquals(1, api.get("/api/estoque/vinculos?servicoId=" + servicoId, atendente).corpo().size(),
                "o atendente lê os vínculos (aviso de baixa no Kanban), só não altera");

        assertEquals(200, api.delete("/api/estoque/vinculos/" + criado.id(), admin).status());
        assertEquals(0, api.get("/api/estoque/vinculos?servicoId=" + servicoId, admin).corpo().size());
        assertEquals(404, api.delete("/api/estoque/vinculos/" + criado.id(), admin).status());

        // inclusão e remoção ficam no histórico do serviço, com autor
        int linhasDeVinculo = 0;
        for (var linha : api.get("/api/auditoria?entidade=SERVICO&entidadeId=" + servicoId, admin).corpo()) {
            if ("itemEstoque".equals(linha.get("campo").asText(""))) {
                linhasDeVinculo++;
                assertEquals("Administrador", linha.get("feitoPor").asText());
            }
        }
        assertEquals(2, linhasDeVinculo);

        // sem vínculo, entrar em produção não baixa nada
        long clienteId = api.post("/api/clientes", admin, "{\"nome\":\"Cliente Vinculo\"}").id();
        long veiculoId = api.post("/api/veiculos", admin, "{\"clienteId\":" + clienteId + ",\"placa\":\"VNC1A23\"}").id();
        long pedidoId = api.post("/api/pedidos", admin, "{\"clienteId\":" + clienteId + ",\"veiculoId\":" + veiculoId
                + ",\"servicoId\":" + servicoId + ",\"origem\":\"BALCAO\"}").id();
        assertEquals(200, api.patch("/api/pedidos/" + pedidoId + "/status", admin, "{\"novoStatus\":\"EM_PROCESSAMENTO\"}").status());
        assertEquals(0, api.get("/api/estoque/movimentacoes?itemEstoqueId=" + itemId, admin).corpo().size());
    }

    @Test
    void pedido_recusa_servico_inativo_veiculo_de_outro_cliente_e_campos_ausentes() throws Exception {
        long servicoId = api.post("/api/servicos", admin,
                "{\"nome\":\"Servico Regras\",\"descricao\":\"\",\"categoria\":\"SERVIÇOS\",\"precoCentavos\":5000}").id();
        long donoId = api.post("/api/clientes", admin, "{\"nome\":\"Dono do Veiculo\"}").id();
        long outroId = api.post("/api/clientes", admin, "{\"nome\":\"Outro Cliente Regras\"}").id();
        long veiculoId = api.post("/api/veiculos", admin, "{\"clienteId\":" + donoId + ",\"placa\":\"RGR1A23\"}").id();

        Resposta veiculoAlheio = api.post("/api/pedidos", admin, "{\"clienteId\":" + outroId + ",\"veiculoId\":" + veiculoId
                + ",\"servicoId\":" + servicoId + ",\"origem\":\"BALCAO\"}");
        assertEquals(400, veiculoAlheio.status());
        assertTrue(veiculoAlheio.mensagem().contains("não pertence"), veiculoAlheio.mensagem());

        Resposta semServico = api.post("/api/pedidos", admin,
                "{\"clienteId\":" + donoId + ",\"veiculoId\":" + veiculoId + ",\"origem\":\"BALCAO\"}");
        assertEquals(400, semServico.status());
        assertTrue(semServico.mensagem().startsWith("Informe"), semServico.mensagem());

        assertEquals(200, api.patch("/api/servicos/" + servicoId + "/status", admin, "{\"ativo\":false}").status());
        Resposta servicoInativo = api.post("/api/pedidos", admin, "{\"clienteId\":" + donoId + ",\"veiculoId\":" + veiculoId
                + ",\"servicoId\":" + servicoId + ",\"origem\":\"BALCAO\"}");
        assertEquals(400, servicoInativo.status());
        assertTrue(servicoInativo.mensagem().contains("inativo"), servicoInativo.mensagem());

        // reativado, volta a aceitar
        assertEquals(200, api.patch("/api/servicos/" + servicoId + "/status", admin, "{\"ativo\":true}").status());
        assertEquals(200, api.post("/api/pedidos", admin, "{\"clienteId\":" + donoId + ",\"veiculoId\":" + veiculoId
                + ",\"servicoId\":" + servicoId + ",\"origem\":\"BALCAO\"}").status());
    }
}
