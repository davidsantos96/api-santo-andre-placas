package br.com.santoandreplacas.apisantoandreplacas.servico;

import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;
import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoRepository;
import br.com.santoandreplacas.apisantoandreplacas.pedido.StatusPedido;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final PedidoRepository pedidoRepository;

    public ServicoService(ServicoRepository servicoRepository, PedidoRepository pedidoRepository) {
        this.servicoRepository = servicoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public ServicoResponse toResponse(Servico servico) {
        LocalDate primeiroDia = LocalDate.now(FusoHorario.SAO_PAULO).withDayOfMonth(1);
        long pedidosNoMes = pedidoRepository.contarNoPeriodo(servico.getId(), StatusPedido.CANCELADO,
                primeiroDia.atStartOfDay(), primeiroDia.plusMonths(1).atStartOfDay());
        return ServicoResponse.fromEntity(servico, pedidosNoMes);
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