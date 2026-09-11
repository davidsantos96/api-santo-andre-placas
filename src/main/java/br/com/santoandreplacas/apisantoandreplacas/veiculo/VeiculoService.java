package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import br.com.santoandreplacas.apisantoandreplacas.cliente.Cliente;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;

    public VeiculoService (VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    public List<Veiculo> listarTodos() {
        return veiculoRepository.findAll();
    }

    public Veiculo buscarPorId(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Veiculo não encontrado: " + id));
    }

    public Veiculo criar(Veiculo veiculo) {
        veiculo.setCriadoEm(LocalDateTime.now());
        return veiculoRepository.save(veiculo);
    }
}
