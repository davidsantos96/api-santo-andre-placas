package br.com.santoandreplacas.apisantoandreplacas.cliente;
import br.com.santoandreplacas.apisantoandreplacas.exception.RecursoNaoEncontradoException;
import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;

import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoRepository;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioAutenticadoProvider;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public ClienteService(ClienteRepository clienteRepository,
                          PedidoRepository pedidoRepository,
                          UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    public ClienteResponse toResponse(Cliente cliente) {
        return ClienteResponse.fromEntity(cliente, pedidoRepository.countByClienteId(cliente.getId()));
    }

    public List<Cliente> listar(String busca) {
        String termo = busca == null ? "" : busca.trim();
        return clienteRepository.buscar(termo, termo.replaceAll("\\D", ""));
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    public Cliente criar(Cliente cliente) {
        validarCpfCnpjUnico(cliente.getCpfCnpj(), null);
        cliente.setCriadoPorUsuario(usuarioAutenticadoProvider.usuarioAtual().orElse(null));
        cliente.setCriadoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        cliente.setCriadoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));
        return clienteRepository.save(cliente);
    }

    public Cliente atualizar(Long id, Cliente dadosAtualizados) {
        Cliente existente = buscarPorId(id);
        validarCpfCnpjUnico(dadosAtualizados.getCpfCnpj(), id);
        existente.setNome(dadosAtualizados.getNome());
        existente.setTelefone(dadosAtualizados.getTelefone());
        existente.setCpfCnpj(dadosAtualizados.getCpfCnpj());
        existente.setEmail(dadosAtualizados.getEmail());
        existente.setAtualizadoPorUsuario(usuarioAutenticadoProvider.usuarioAtual().orElse(null));
        existente.setAtualizadoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        existente.setAtualizadoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));
        return clienteRepository.save(existente);
    }

    // Compara só os dígitos, então "529.982.247-25" e "52998224725" contam como o mesmo documento.
    public void validarCpfCnpjUnico(String cpfCnpj, Long idIgnorado) {
        if (cpfCnpj == null) {
            return;
        }
        String digitos = cpfCnpj.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return;
        }
        boolean duplicado = clienteRepository.buscarPorCpfCnpjDigitos(digitos).stream()
                .anyMatch(outro -> !outro.getId().equals(idIgnorado));
        if (duplicado) {
            throw new IllegalArgumentException("Já existe um cliente cadastrado com esse CPF/CNPJ.");
        }
    }
}
