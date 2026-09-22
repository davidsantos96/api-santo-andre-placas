package br.com.santoandreplacas.apisantoandreplacas.estoque;

import br.com.santoandreplacas.apisantoandreplacas.pedido.Pedido;
import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoRepository;
import br.com.santoandreplacas.apisantoandreplacas.servico.Servico;
import br.com.santoandreplacas.apisantoandreplacas.servico.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EstoqueService {

    private final ItemEstoqueRepository itemEstoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ServicoItemEstoqueRepository servicoItemEstoqueRepository;
    private final PedidoRepository pedidoRepository;
    private final ServicoRepository servicoRepository;

    public EstoqueService(ItemEstoqueRepository itemEstoqueRepository,
                          MovimentacaoEstoqueRepository movimentacaoEstoqueRepository,
                          ServicoItemEstoqueRepository servicoItemEstoqueRepository,
                          PedidoRepository pedidoRepository,
                          ServicoRepository servicoRepository) {
        this.itemEstoqueRepository = itemEstoqueRepository;
        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
        this.servicoItemEstoqueRepository = servicoItemEstoqueRepository;
        this.pedidoRepository = pedidoRepository;
        this.servicoRepository = servicoRepository;
    }

    @Transactional(readOnly = true)
    public List<ItemEstoque> listarItens() {
        return itemEstoqueRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ItemEstoque> listarBaixoEstoque() {
        return itemEstoqueRepository.findBaixoEstoque();
    }

    public ItemEstoque criarItem(ItemEstoque item) {
        return itemEstoqueRepository.save(item);
    }

    @Transactional
    public MovimentacaoEstoque registrarMovimentacao(NovaMovimentacaoRequest request) {
        ItemEstoque item = itemEstoqueRepository.findById(request.itemEstoqueId())
                .orElseThrow(() -> new IllegalArgumentException("Item de estoque não encontrado: " + request.itemEstoqueId()));

        Pedido pedido = null;
        if (request.pedidoId() != null) {
            pedido = pedidoRepository.findById(request.pedidoId())
                    .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + request.pedidoId()));
        }

        aplicarMovimentacao(item, request.tipo(), request.quantidade());

        return salvarMovimentacao(item, request.tipo(), request.quantidade(), pedido);
    }

    /**
     * Baixa automática de estoque ao mover um pedido para EM_PROCESSAMENTO
     * (spec, seção 4). Consome os itens vinculados ao serviço do pedido via
     * ServicoItemEstoque.
     */
    @Transactional
    public void baixarEstoquePorPedido(Pedido pedido) {
        List<ServicoItemEstoque> vinculos = servicoItemEstoqueRepository.findByServicoId(pedido.getServico().getId());

        for (ServicoItemEstoque vinculo : vinculos) {
            ItemEstoque item = vinculo.getItemEstoque();
            aplicarMovimentacao(item, TipoMovimentacao.SAIDA, vinculo.getQuantidadeNecessaria());
            salvarMovimentacao(item, TipoMovimentacao.SAIDA, vinculo.getQuantidadeNecessaria(), pedido);
        }
    }

    @Transactional(readOnly = true)
    public List<ServicoItemEstoque> listarVinculosPorServico(Long servicoId) {
        return servicoItemEstoqueRepository.findByServicoId(servicoId);
    }

    public ServicoItemEstoque criarVinculo(NovoVinculoRequest request) {
        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado: " + request.servicoId()));
        ItemEstoque item = itemEstoqueRepository.findById(request.itemEstoqueId())
                .orElseThrow(() -> new IllegalArgumentException("Item de estoque não encontrado: " + request.itemEstoqueId()));

        ServicoItemEstoque vinculo = new ServicoItemEstoque();
        vinculo.setServico(servico);
        vinculo.setItemEstoque(item);
        vinculo.setQuantidadeNecessaria(request.quantidadeNecessaria());

        return servicoItemEstoqueRepository.save(vinculo);
    }

    private void aplicarMovimentacao(ItemEstoque item, TipoMovimentacao tipo, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade movimentada deve ser maior que zero");
        }

        if (tipo == TipoMovimentacao.ENTRADA) {
            item.setQuantidade(item.getQuantidade() + quantidade);
        } else {
            if (item.getQuantidade() < quantidade) {
                throw new IllegalStateException(
                        "Estoque insuficiente para \"" + item.getNome() + "\": disponível "
                                + item.getQuantidade() + ", necessário " + quantidade);
            }
            item.setQuantidade(item.getQuantidade() - quantidade);
        }

        itemEstoqueRepository.save(item);
    }

    private MovimentacaoEstoque salvarMovimentacao(ItemEstoque item, TipoMovimentacao tipo, int quantidade, Pedido pedido) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setItemEstoque(item);
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setPedido(pedido);
        movimentacao.setCriadoEm(LocalDateTime.now());
        return movimentacaoEstoqueRepository.save(movimentacao);
    }
}
