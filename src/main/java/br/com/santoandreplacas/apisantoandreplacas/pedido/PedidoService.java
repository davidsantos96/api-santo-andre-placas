package br.com.santoandreplacas.apisantoandreplacas.pedido;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import br.com.santoandreplacas.apisantoandreplacas.cliente.Cliente;
import br.com.santoandreplacas.apisantoandreplacas.cliente.ClienteRepository;
import br.com.santoandreplacas.apisantoandreplacas.veiculo.Veiculo;
import br.com.santoandreplacas.apisantoandreplacas.veiculo.VeiculoRepository;
import br.com.santoandreplacas.apisantoandreplacas.servico.Servico;
import br.com.santoandreplacas.apisantoandreplacas.servico.ServicoRepository;
import br.com.santoandreplacas.apisantoandreplacas.estoque.EstoqueService;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final PedidoStatusHistoricoRepository historicoRepository;
    private final EstoqueService estoqueService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         VeiculoRepository veiculoRepository,
                         ServicoRepository servicoRepository,
                         PedidoStatusHistoricoRepository historicoRepository,
                         EstoqueService estoqueService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.servicoRepository = servicoRepository;
        this.historicoRepository = historicoRepository;
        this.estoqueService = estoqueService;
    }

    @Transactional(readOnly = true)
    public List<PedidoStatusHistorico> listarHistorico(Long pedidoId) {
        return historicoRepository.findByPedidoIdOrderByAlteradoEmAsc(pedidoId);
    }

    @Transactional(readOnly = true)
    public Page<Pedido> listar(StatusPedido status, Long clienteId, LocalDateTime de, LocalDateTime ate, Pageable pageable) {
        return pedidoRepository.buscar(status, clienteId, de, ate, pageable);
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
        historico.setAlteradoPor("sistema"); // temporário, até termos autenticação
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

        Cliente novoCliente = new Cliente();
        novoCliente.setNome(dados.nome());
        novoCliente.setTelefone(dados.telefone());
        novoCliente.setCpfCnpj(dados.cpfCnpj());
        novoCliente.setEmail(dados.email());
        novoCliente.setCriadoEm(LocalDateTime.now());

        return clienteRepository.save(novoCliente);
    }

    private Veiculo criarVeiculo(NovoVeiculoRequest dados, Cliente cliente) {
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