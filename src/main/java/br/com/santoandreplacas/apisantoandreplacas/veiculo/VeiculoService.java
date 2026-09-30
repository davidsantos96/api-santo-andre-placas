package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import br.com.santoandreplacas.apisantoandreplacas.cliente.Cliente;
import br.com.santoandreplacas.apisantoandreplacas.cliente.ClienteRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final ConsultaVeicularProvider consultaVeicularProvider;

    public VeiculoService(VeiculoRepository veiculoRepository,
                          ClienteRepository clienteRepository,
                          ConsultaVeicularProvider consultaVeicularProvider) {
        this.veiculoRepository = veiculoRepository;
        this.clienteRepository = clienteRepository;
        this.consultaVeicularProvider = consultaVeicularProvider;
    }

    public List<Veiculo> listar(String placa, Long clienteId) {
        return veiculoRepository.buscar(placa, clienteId);
    }

    public Veiculo buscarPorId(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Veiculo não encontrado: " + id));
    }

    public Veiculo criar(NovoVeiculoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + request.clienteId()));

        Veiculo veiculo = new Veiculo();
        veiculo.setCliente(cliente);
        veiculo.setPlaca(request.placa());
        veiculo.setMarcaModelo(request.marcaModelo());
        veiculo.setAnoFabricacao(request.anoFabricacao());
        veiculo.setAnoModelo(request.anoModelo());
        veiculo.setChassi(request.chassi());
        veiculo.setCriadoEm(LocalDateTime.now());

        return veiculoRepository.save(veiculo);
    }

    public ConsultaVeicularResultado consultar(Long id) {
        Veiculo veiculo = buscarPorId(id);
        return consultaVeicularProvider.consultar(veiculo.getPlaca());
    }
}
