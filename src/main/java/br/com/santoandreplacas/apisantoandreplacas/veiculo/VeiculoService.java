package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import br.com.santoandreplacas.apisantoandreplacas.cliente.Cliente;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ConsultaVeicularProvider consultaVeicularProvider;

    public VeiculoService(VeiculoRepository veiculoRepository, ConsultaVeicularProvider consultaVeicularProvider) {
        this.veiculoRepository = veiculoRepository;
        this.consultaVeicularProvider = consultaVeicularProvider;
    }

    public List<Veiculo> listar(String placa, Long clienteId) {
        return veiculoRepository.buscar(placa, clienteId);
    }

    public Veiculo buscarPorId(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Veiculo não encontrado: " + id));
    }

    public Veiculo criar(Veiculo veiculo) {
        veiculo.setCriadoEm(LocalDateTime.now());
        return veiculoRepository.save(veiculo);
    }

    public ConsultaVeicularResultado consultar(Long id) {
        Veiculo veiculo = buscarPorId(id);
        return consultaVeicularProvider.consultar(veiculo.getPlaca());
    }
}
