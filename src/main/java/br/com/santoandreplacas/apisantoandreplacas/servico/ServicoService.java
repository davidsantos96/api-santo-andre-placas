package br.com.santoandreplacas.apisantoandreplacas.servico;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    public List<Servico> listar(boolean incluirInativos) {
        return incluirInativos ? servicoRepository.findAll() : servicoRepository.findByAtivoTrue();
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

    public Servico atualizarStatus(Long id, boolean ativo) {
        Servico existente = buscarPorId(id);
        existente.setAtivo(ativo);
        return servicoRepository.save(existente);
    }

    public void deletar(Long id) {
        // "excluir" um serviço na prática é desativar: ele pode estar referenciado
        // por pedidos já feitos (FK servico_id em pedido), então apagar a linha de
        // verdade quebraria o histórico. Ver AtualizarStatusServicoRequest.
        atualizarStatus(id, false);
    }
}