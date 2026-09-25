package br.com.santoandreplacas.apisantoandreplacas.cliente;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listar(String busca) {
        return clienteRepository.buscar(busca);
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + id));
    }

    public Cliente criar(Cliente cliente) {
        cliente.setCriadoEm(LocalDateTime.now());
        return clienteRepository.save(cliente);
    }

    public Cliente atualizar(Long id, Cliente dadosAtualizados) {
        Cliente existente = buscarPorId(id);
        existente.setNome(dadosAtualizados.getNome());
        existente.setTelefone(dadosAtualizados.getTelefone());
        existente.setCpfCnpj(dadosAtualizados.getCpfCnpj());
        existente.setEmail(dadosAtualizados.getEmail());
        return clienteRepository.save(existente);
    }
}