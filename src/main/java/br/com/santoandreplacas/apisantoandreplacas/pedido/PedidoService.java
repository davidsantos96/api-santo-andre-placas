package br.com.santoandreplacas.apisantoandreplacas.pedido;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import br.com.santoandreplacas.apisantoandreplacas.cliente.Cliente;
import br.com.santoandreplacas.apisantoandreplacas.cliente.ClienteRepository;
import br.com.santoandreplacas.apisantoandreplacas.cliente.ClienteService;
import br.com.santoandreplacas.apisantoandreplacas.veiculo.Veiculo;
import br.com.santoandreplacas.apisantoandreplacas.veiculo.VeiculoRepository;
import br.com.santoandreplacas.apisantoandreplacas.veiculo.VeiculoService;
import br.com.santoandreplacas.apisantoandreplacas.servico.Servico;
import br.com.santoandreplacas.apisantoandreplacas.servico.ServicoRepository;
import br.com.santoandreplacas.apisantoandreplacas.estoque.EstoqueService;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioAutenticadoProvider;
import br.com.santoandreplacas.apisantoandreplacas.financeiro.PagamentoRepository;
import br.com.santoandreplacas.apisantoandreplacas.financeiro.StatusPagamento;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final PedidoStatusHistoricoRepository historicoRepository;
    private final EstoqueService estoqueService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;
    private final PagamentoRepository pagamentoRepository;
    private final ClienteService clienteService;
    private final VeiculoService veiculoService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         VeiculoRepository veiculoRepository,
                         ServicoRepository servicoRepository,
                         PedidoStatusHistoricoRepository historicoRepository,
                         EstoqueService estoqueService,
                         UsuarioAutenticadoProvider usuarioAutenticadoProvider,
                         PagamentoRepository pagamentoRepository,
                         ClienteService clienteService,
                         VeiculoService veiculoService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.servicoRepository = servicoRepository;
        this.historicoRepository = historicoRepository;
        this.estoqueService = estoqueService;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
        this.pagamentoRepository = pagamentoRepository;
        this.clienteService = clienteService;
        this.veiculoService = veiculoService;
    }

    @Transactional(readOnly = true)
    public boolean estaPago(Pedido pedido) {
        long totalPago = pagamentoRepository.somarValorPorPedidoEStatus(pedido.getId(), StatusPagamento.PAGO);
        return totalPago >= pedido.getServico().getPrecoCentavos();
    }

    @Transactional(readOnly = true)
    public PedidoResponse toResponse(Pedido pedido) {
        return PedidoResponse.fromEntity(pedido, estaPago(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoStatusHistorico> listarHistorico(Long pedidoId) {
        return historicoRepository.findByPedidoIdOrderByAlteradoEmAsc(pedidoId);
    }

    @Transactional(readOnly = true)
    public Page<Pedido> listar(StatusPedido status, Long clienteId, LocalDateTime de, LocalDateTime ate,
                               String busca, Pageable pageable) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return pedidoRepository.buscar(status, clienteId, de, ate, termo, parseId(termo), pageable);
    }

    private Long parseId(String termo) {
        if (termo == null) {
            return null;
        }
        try {
            return Long.parseLong(termo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + id));
    }

    public Pedido criar(Pedido pedido) {
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setCriadoEm(LocalDateTime.now());
        pedido.setAtualizadoEm(LocalDateTime.now());

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        registrarHistorico(pedidoSalvo, null, StatusPedido.RECEBIDO);

        return pedidoSalvo;
    }

    public Pedido criarSimples(NovoPedidoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + request.clienteId()));
        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado: " + request.veiculoId()));
        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado: " + request.servicoId()));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setVeiculo(veiculo);
        pedido.setServico(servico);
        pedido.setOrigem(request.origem());

        return criar(pedido);
    }

    @Transactional
    public Pedido mudarStatus(Long id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);
        StatusPedido statusAnterior = pedido.getStatus();

        validarTransicao(statusAnterior, novoStatus);

        pedido.setStatus(novoStatus);
        pedido.setAtualizadoEm(LocalDateTime.now());

        registrarHistorico(pedido, statusAnterior, novoStatus);

        if (novoStatus == StatusPedido.EM_PROCESSAMENTO) {
            estoqueService.baixarEstoquePorPedido(pedido);
        }

        return pedidoRepository.save(pedido);
    }

    private void registrarHistorico(Pedido pedido, StatusPedido statusAnterior, StatusPedido statusNovo) {
        PedidoStatusHistorico historico = new PedidoStatusHistorico();
        historico.setPedido(pedido);
        historico.setStatusAnterior(statusAnterior);
        historico.setStatusNovo(statusNovo);
        historico.setAlteradoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        historico.setAlteradoEm(LocalDateTime.now());
        historicoRepository.save(historico);
    }

    private void validarTransicao(StatusPedido atual, StatusPedido novo) {
        if (atual == StatusPedido.ENTREGUE || atual == StatusPedido.CANCELADO) {
            throw new IllegalStateException(
                    "Pedido em status final (" + atual + ") não pode mudar de status.");
        }
    }

    @Transactional
    public Pedido criarPedidoCompleto(PedidoCompletoRequest request) {

        Cliente cliente = resolverCliente(request);
        Veiculo veiculo = criarVeiculo(request.veiculo(), cliente);
        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado: " + request.servicoId()));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setVeiculo(veiculo);
        pedido.setServico(servico);
        pedido.setOrigem(request.origem());

        return criar(pedido); // reaproveita o método que já força status = RECEBIDO
    }

    private Cliente resolverCliente(PedidoCompletoRequest request) {
        if (request.clienteId() != null) {
            return clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + request.clienteId()));
        }

        NovoClienteRequest dados = request.cliente();
        if (dados == null) {
            throw new IllegalArgumentException("Informe clienteId ou os dados de um novo cliente.");
        }

        clienteService.validarCpfCnpjUnico(dados.cpfCnpj(), null);

        Cliente novoCliente = new Cliente();
        novoCliente.setNome(dados.nome());
        novoCliente.setTelefone(dados.telefone());
        novoCliente.setCpfCnpj(dados.cpfCnpj());
        novoCliente.setEmail(dados.email());
        novoCliente.setCriadoEm(LocalDateTime.now());

        return clienteRepository.save(novoCliente);
    }

    private Veiculo criarVeiculo(NovoVeiculoRequest dados, Cliente cliente) {
        veiculoService.validarPlacaUnica(dados.placa(), null);

        Veiculo veiculo = new Veiculo();
        veiculo.setCliente(cliente);
        veiculo.setPlaca(dados.placa());
        veiculo.setMarcaModelo(dados.marcaModelo());
        veiculo.setAnoFabricacao(dados.anoFabricacao());
        veiculo.setAnoModelo(dados.anoModelo());
        veiculo.setChassi(dados.chassi());
        veiculo.setCriadoEm(LocalDateTime.now());

        return veiculoRepository.save(veiculo);
    }

}