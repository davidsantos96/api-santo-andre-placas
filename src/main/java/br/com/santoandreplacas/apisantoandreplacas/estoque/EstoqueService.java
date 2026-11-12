package br.com.santoandreplacas.apisantoandreplacas.estoque;
import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;
import br.com.santoandreplacas.apisantoandreplacas.auditoria.AuditoriaService;
import br.com.santoandreplacas.apisantoandreplacas.auditoria.EntidadeAuditada;
import br.com.santoandreplacas.apisantoandreplacas.exception.RecursoNaoEncontradoException;

import br.com.santoandreplacas.apisantoandreplacas.pedido.Pedido;
import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoRepository;
import br.com.santoandreplacas.apisantoandreplacas.servico.Servico;
import br.com.santoandreplacas.apisantoandreplacas.servico.ServicoRepository;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioAutenticadoProvider;
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
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;
    private final AuditoriaService auditoriaService;

    // Nome do "campo" nas linhas de auditoria do serviço que registram a
    // inclusão/remoção de um vínculo com item de estoque.
    private static final String CAMPO_AUDITORIA_VINCULO = "itemEstoque";

    public EstoqueService(ItemEstoqueRepository itemEstoqueRepository,
                          MovimentacaoEstoqueRepository movimentacaoEstoqueRepository,
                          ServicoItemEstoqueRepository servicoItemEstoqueRepository,
                          PedidoRepository pedidoRepository,
                          ServicoRepository servicoRepository,
                          UsuarioAutenticadoProvider usuarioAutenticadoProvider,
                          AuditoriaService auditoriaService) {
        this.itemEstoqueRepository = itemEstoqueRepository;
        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
        this.servicoItemEstoqueRepository = servicoItemEstoqueRepository;
        this.pedidoRepository = pedidoRepository;
        this.servicoRepository = servicoRepository;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
        this.auditoriaService = auditoriaService;
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
        item.setCriadoPorUsuario(usuarioAutenticadoProvider.usuarioAtual().orElse(null));
        item.setCriadoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        item.setCriadoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));
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
    public List<MovimentacaoEstoque> listarMovimentacoes(Long itemEstoqueId) {
        return itemEstoqueId == null
                ? movimentacaoEstoqueRepository.findAllByOrderByCriadoEmDesc()
                : movimentacaoEstoqueRepository.findByItemEstoqueIdOrderByCriadoEmDesc(itemEstoqueId);
    }

    @Transactional(readOnly = true)
    public List<ServicoItemEstoque> listarVinculosPorServico(Long servicoId) {
        return servicoItemEstoqueRepository.findByServicoId(servicoId);
    }

    @Transactional
    public ServicoItemEstoque criarVinculo(NovoVinculoRequest request) {
        if (request.servicoId() == null || request.itemEstoqueId() == null) {
            throw new IllegalArgumentException("Informe o serviço e o item de estoque.");
        }

        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado: " + request.servicoId()));
        ItemEstoque item = itemEstoqueRepository.findById(request.itemEstoqueId())
                .orElseThrow(() -> new IllegalArgumentException("Item de estoque não encontrado: " + request.itemEstoqueId()));

        // Dois vínculos do mesmo item no mesmo serviço dariam baixa em dobro sem
        // ninguém perceber. Para mudar a quantidade: remover e vincular de novo.
        if (servicoItemEstoqueRepository.existsByServicoIdAndItemEstoqueId(servico.getId(), item.getId())) {
            throw new IllegalArgumentException(
                    "\"" + item.getNome() + "\" já está vinculado a este serviço. Remova o vínculo para mudar a quantidade.");
        }

        ServicoItemEstoque vinculo = new ServicoItemEstoque();
        vinculo.setServico(servico);
        vinculo.setItemEstoque(item);
        vinculo.setQuantidadeNecessaria(request.quantidadeNecessaria());
        vinculo.setCriadoPorUsuario(usuarioAutenticadoProvider.usuarioAtual().orElse(null));
        vinculo.setCriadoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        vinculo.setCriadoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));

        ServicoItemEstoque salvo = servicoItemEstoqueRepository.save(vinculo);

        // Entra no histórico do serviço (mesma tela do preço): o vínculo decide o
        // que sai do estoque a cada pedido, então mudar isso precisa deixar rastro.
        auditoriaService.registrarAlteracao(EntidadeAuditada.SERVICO, servico.getId(), servico.getNome(),
                CAMPO_AUDITORIA_VINCULO, null, descreverVinculo(salvo));

        return salvo;
    }

    /**
     * Remove o vínculo: o serviço deixa de dar baixa nesse item dali em diante.
     * Não mexe nas movimentações já feitas. Como a linha some, o autor da
     * remoção fica no histórico do serviço (auditoria).
     */
    @Transactional
    public void removerVinculo(Long id) {
        ServicoItemEstoque vinculo = servicoItemEstoqueRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo não encontrado: " + id));

        Servico servico = vinculo.getServico();
        auditoriaService.registrarAlteracao(EntidadeAuditada.SERVICO, servico.getId(), servico.getNome(),
                CAMPO_AUDITORIA_VINCULO, descreverVinculo(vinculo), null);

        servicoItemEstoqueRepository.delete(vinculo);
    }

    private String descreverVinculo(ServicoItemEstoque vinculo) {
        return vinculo.getItemEstoque().getNome() + " × " + vinculo.getQuantidadeNecessaria();
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
        // Na baixa automática, o autor é quem moveu o pedido para EM_PROCESSAMENTO:
        // o contexto de segurança da requisição ainda é o mesmo.
        movimentacao.setRegistradoPorUsuario(usuarioAutenticadoProvider.usuarioAtual().orElse(null));
        movimentacao.setRegistradoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        movimentacao.setCriadoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));
        return movimentacaoEstoqueRepository.save(movimentacao);
    }
}
