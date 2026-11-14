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
 * O caminho que o balcão percorre todo dia, de ponta a ponta pela API:
 * serviço com item de estoque vinculado → cliente e veículo → pedido →
 * produção (baixa automática) → pagamento em duas parcelas → entrega → caixa.
 *
 * Se este teste quebrar, alguma regra central do sistema mudou.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoPrincipalTest {

    static { ApiDeTeste.prepararSenhaAdmin(); }

    @Autowired MockMvc mvc;
    ApiDeTeste api;
    String admin;

    @BeforeEach
    void entrar() throws Exception {
        api = new ApiDeTeste(mvc);
        admin = api.loginAdmin();
    }

    @Test
    void pedido_do_cadastro_a_entrega_com_baixa_de_estoque_e_pagamento_parcelado() throws Exception {
        // --- cadastros de apoio: serviço, item de estoque e o vínculo entre os dois
        long servicoId = api.post("/api/servicos", admin,
                "{\"nome\":\"Emplacamento Fluxo\",\"descricao\":\"\",\"categoria\":\"EMPLACAMENTO\",\"precoCentavos\":31690}").id();
        long itemId = api.post("/api/estoque/itens", admin,
                "{\"nome\":\"Par de placas Fluxo\",\"sku\":\"PL-FLUXO\",\"unidade\":\"par\",\"quantidade\":3,\"quantidadeMinima\":1}").id();
        Resposta vinculo = api.post("/api/estoque/vinculos", admin,
                "{\"servicoId\":" + servicoId + ",\"itemEstoqueId\":" + itemId + ",\"quantidadeNecessaria\":2}");
        assertEquals(200, vinculo.status(), vinculo.texto());

        long clienteId = api.post("/api/clientes", admin,
                "{\"nome\":\"Cliente Fluxo\",\"telefone\":\"(11) 90000-0001\",\"cpfCnpj\":\"\",\"email\":\"\"}").id();
        long veiculoId = api.post("/api/veiculos", admin,
                "{\"clienteId\":" + clienteId + ",\"placa\":\"flx1a23\"}").id();

        // --- pedido nasce RECEBIDO, com o preço do serviço congelado e sem pagamento
        String novoPedido = "{\"clienteId\":" + clienteId + ",\"veiculoId\":" + veiculoId
                + ",\"servicoId\":" + servicoId + ",\"origem\":\"BALCAO\"}";
        Resposta pedido = api.post("/api/pedidos", admin, novoPedido);
        assertEquals(200, pedido.status(), pedido.texto());
        long pedidoId = pedido.id();
        assertEquals("RECEBIDO", pedido.corpo().get("status").asText());
        assertEquals(31690, pedido.corpo().get("precoCentavos").asLong());
        assertFalse(pedido.corpo().get("pago").asBoolean());
        assertEquals("FLX1A23", pedido.corpo().get("veiculo").get("placa").asText(), "placa é normalizada");

        // --- entrar em produção dá baixa automática nos itens vinculados ao serviço
        assertEquals(200, mudarStatus(pedidoId, "EM_PROCESSAMENTO").status());
        assertEquals(1, quantidadeEmEstoque(itemId), "3 em estoque - 2 do vínculo");

        Resposta movimentacoes = api.get("/api/estoque/movimentacoes?itemEstoqueId=" + itemId, admin);
        assertEquals(1, movimentacoes.corpo().size());
        assertEquals("SAIDA", movimentacoes.corpo().get(0).get("tipo").asText());
        assertEquals(pedidoId, movimentacoes.corpo().get(0).get("pedidoId").asLong());
        assertEquals("Administrador", movimentacoes.corpo().get(0).get("registradoPor").asText());

        // --- sem estoque suficiente o pedido NÃO muda de status e nada é baixado
        long segundoPedido = api.post("/api/pedidos", admin, novoPedido).id();
        Resposta semEstoque = mudarStatus(segundoPedido, "EM_PROCESSAMENTO");
        assertEquals(400, semEstoque.status());
        assertTrue(semEstoque.mensagem().contains("Estoque insuficiente"), semEstoque.mensagem());
        assertEquals("RECEBIDO", api.get("/api/pedidos/" + segundoPedido, admin).corpo().get("status").asText());
        assertEquals(1, quantidadeEmEstoque(itemId));

        // --- pagamento parcial não quita; a soma das parcelas, sim
        assertEquals(200, api.post("/api/pedidos/" + pedidoId + "/pagamento", admin,
                "{\"valorCentavos\":10000,\"formaPagamento\":\"PIX\"}").status());
        assertFalse(api.get("/api/pedidos/" + pedidoId, admin).corpo().get("pago").asBoolean());

        assertEquals(200, api.post("/api/pedidos/" + pedidoId + "/pagamento", admin,
                "{\"valorCentavos\":21690,\"formaPagamento\":\"DINHEIRO\"}").status());
        assertTrue(api.get("/api/pedidos/" + pedidoId, admin).corpo().get("pago").asBoolean());

        assertEquals(400, api.post("/api/pedidos/" + pedidoId + "/pagamento", admin,
                "{\"valorCentavos\":0,\"formaPagamento\":\"PIX\"}").status(), "valor zero é recusado");

        // --- segue até a entrega; depois de entregue o status não muda mais
        assertEquals(200, mudarStatus(pedidoId, "PLACA_PRONTA").status());
        assertEquals(200, mudarStatus(pedidoId, "ENTREGUE").status());
        assertEquals(400, mudarStatus(pedidoId, "RECEBIDO").status());

        Resposta historico = api.get("/api/pedidos/" + pedidoId + "/historico", admin);
        assertEquals(4, historico.corpo().size(), "criação + 3 mudanças de status");
        assertEquals("ENTREGUE", historico.corpo().get(3).get("statusNovo").asText());
        assertEquals("Administrador", historico.corpo().get(3).get("alteradoPor").asText());

        // --- os dois pagamentos aparecem no caixa de hoje (outros testes também
        // registram pagamentos no mesmo banco em memória, então é "pelo menos")
        Resposta caixa = api.get("/api/financeiro/fechamento-caixa", admin);
        assertEquals(200, caixa.status());
        assertTrue(caixa.corpo().get("totalGeral").asLong() >= 31690);
        assertTrue(caixa.corpo().get("quantidadePagamentos").asInt() >= 2);

        Resposta resumo = api.get("/api/dashboard/resumo", admin);
        assertEquals(200, resumo.status());
        assertTrue(resumo.corpo().get("faturamentoHojeCentavos").asLong() >= 31690);
    }

    private Resposta mudarStatus(long pedidoId, String novoStatus) throws Exception {
        return api.patch("/api/pedidos/" + pedidoId + "/status", admin, "{\"novoStatus\":\"" + novoStatus + "\"}");
    }

    private int quantidadeEmEstoque(long itemId) throws Exception {
        for (var item : api.get("/api/estoque/itens", admin).corpo()) {
            if (item.get("id").asLong() == itemId) {
                return item.get("quantidade").asInt();
            }
        }
        throw new AssertionError("Item de estoque " + itemId + " não encontrado");
    }
}
