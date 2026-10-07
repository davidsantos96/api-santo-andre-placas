package br.com.santoandreplacas.apisantoandreplacas.veiculo;
import br.com.santoandreplacas.apisantoandreplacas.exception.RecursoNaoEncontradoException;
import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;

import br.com.santoandreplacas.apisantoandreplacas.cliente.Cliente;
import br.com.santoandreplacas.apisantoandreplacas.cliente.ClienteRepository;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioAutenticadoProvider;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final ConsultaVeicularProvider consultaVeicularProvider;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public VeiculoService(VeiculoRepository veiculoRepository,
                          ClienteRepository clienteRepository,
                          ConsultaVeicularProvider consultaVeicularProvider,
                          UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.veiculoRepository = veiculoRepository;
        this.clienteRepository = clienteRepository;
        this.consultaVeicularProvider = consultaVeicularProvider;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    public List<Veiculo> listar(String placa, Long clienteId) {
        // "" em vez de null: no PostgreSQL um String nulo dentro de UPPER(CONCAT(...)) perde o tipo
        return veiculoRepository.buscar(placa == null ? "" : placa.trim(), clienteId);
    }

    public Veiculo buscarPorId(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo não encontrado: " + id));
    }

    public void validarPlacaUnica(String placa, Long idIgnorado) {
        if (placa == null || placa.isBlank()) {
            return;
        }
        boolean duplicada = veiculoRepository.findByPlacaIgnoreCase(placa.trim()).stream()
                .anyMatch(outro -> !outro.getId().equals(idIgnorado));
        if (duplicada) {
            throw new IllegalArgumentException("Já existe um veículo cadastrado com a placa " + placa.trim().toUpperCase() + ".");
        }
    }

    public Veiculo criar(NovoVeiculoRequest request) {
        validarPlacaUnica(request.placa(), null);
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + request.clienteId()));

        Veiculo veiculo = new Veiculo();
        veiculo.setCliente(cliente);
        veiculo.setPlaca(request.placa());
        veiculo.setMarcaModelo(request.marcaModelo());
        veiculo.setAnoFabricacao(request.anoFabricacao());
        veiculo.setAnoModelo(request.anoModelo());
        veiculo.setChassi(request.chassi());
        veiculo.setCriadoPorUsuario(usuarioAutenticadoProvider.usuarioAtual().orElse(null));
        veiculo.setCriadoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        veiculo.setCriadoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));

        return veiculoRepository.save(veiculo);
    }

    public Veiculo atualizar(Long id, AtualizarVeiculoRequest request) {
        Veiculo veiculo = buscarPorId(id);

        if (request.placa() != null) {
            validarPlacaUnica(request.placa(), id);
            veiculo.setPlaca(request.placa());
        }
        if (request.marcaModelo() != null) {
            veiculo.setMarcaModelo(request.marcaModelo());
        }
        if (request.anoFabricacao() != null) {
            veiculo.setAnoFabricacao(request.anoFabricacao());
        }
        if (request.anoModelo() != null) {
            veiculo.setAnoModelo(request.anoModelo());
        }
        if (request.chassi() != null) {
            veiculo.setChassi(request.chassi());
        }

        veiculo.setAtualizadoPorUsuario(usuarioAutenticadoProvider.usuarioAtual().orElse(null));
        veiculo.setAtualizadoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        veiculo.setAtualizadoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));

        return veiculoRepository.save(veiculo);
    }

    public ConsultaVeicularResultado consultar(Long id) {
        Veiculo veiculo = buscarPorId(id);
        return consultaVeicularProvider.consultar(veiculo.getPlaca());
    }
}
