package br.com.santoandreplacas.apisantoandreplacas.servico;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    public List<Servico> listarAtivos() {
        return servicoRepository.findByAtivoTrue();
    }

    public Servico buscarPorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado: " + id));
    }

    public Servico criar(Servico servico) {
        servico.setAtivo(true);
        return servicoRepository.save(servico);
    }

    public Servico atualizar(Long id, Servico dadosAtualizados) {
        Servico existente = buscarPorId(id);
        existente.setNome(dadosAtualizados.getNome());
        existente.setDescricao(dadosAtualizados.getDescricao());
        existente.setPrecoCentavos(dadosAtualizados.getPrecoCentavos());
        existente.setCategoria(dadosAtualizados.getCategoria());
        return servicoRepository.save(existente);
    }

    public void deletar(Long id) {
        Servico existente = buscarPorId(id);
        servicoRepository.delete(existente);
    }
}