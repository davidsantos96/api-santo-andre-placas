package br.com.santoandreplacas.apisantoandreplacas.servico;
import br.com.santoandreplacas.apisantoandreplacas.exception.RecursoNaoEncontradoException;

import br.com.santoandreplacas.apisantoandreplacas.auditoria.AcaoAuditada;
import br.com.santoandreplacas.apisantoandreplacas.auditoria.AuditoriaService;
import br.com.santoandreplacas.apisantoandreplacas.auditoria.EntidadeAuditada;
import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;
import org.springframework.transaction.annotation.Transactional;
import br.com.santoandreplacas.apisantoandreplacas.pedido.PedidoRepository;
import br.com.santoandreplacas.apisantoandreplacas.pedido.StatusPedido;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final PedidoRepository pedidoRepository;
    private final AuditoriaService auditoriaService;

    public ServicoService(ServicoRepository servicoRepository,
                          PedidoRepository pedidoRepository,
                          AuditoriaService auditoriaService) {
        this.servicoRepository = servicoRepository;
        this.pedidoRepository = pedidoRepository;
        this.auditoriaService = auditoriaService;
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + id));
    }

    @Transactional
    public Servico criar(Servico servico) {
        servico.setAtivo(true);
        Servico salvo = servicoRepository.save(servico);
        auditoriaService.registrarAcao(EntidadeAuditada.SERVICO, salvo.getId(), salvo.getNome(),
                AcaoAuditada.CRIACAO);
        return salvo;
    }

    @Transactional
    public Servico atualizar(Long id, Servico dadosAtualizados) {
        Servico existente = buscarPorId(id);

        // Valores antigos precisam ser lidos antes de mexer na entidade gerenciada.
        String nomeAnterior = existente.getNome();
        String descricaoAnterior = existente.getDescricao();
        long precoAnterior = existente.getPrecoCentavos();
        String categoriaAnterior = existente.getCategoria();

        existente.setNome(dadosAtualizados.getNome());
        existente.setDescricao(dadosAtualizados.getDescricao());
        existente.setPrecoCentavos(dadosAtualizados.getPrecoCentavos());
        existente.setCategoria(dadosAtualizados.getCategoria());
        Servico salvo = servicoRepository.save(existente);

        auditar(salvo, "nome", nomeAnterior, salvo.getNome());
        auditar(salvo, "descricao", descricaoAnterior, salvo.getDescricao());
        auditar(salvo, "precoCentavos", precoAnterior, salvo.getPrecoCentavos());
        auditar(salvo, "categoria", categoriaAnterior, salvo.getCategoria());

        return salvo;
    }

    @Transactional
    public Servico atualizarStatus(Long id, boolean ativo) {
        Servico existente = buscarPorId(id);
        boolean mudou = existente.isAtivo() != ativo;
        existente.setAtivo(ativo);
        Servico salvo = servicoRepository.save(existente);

        if (mudou) {
            auditoriaService.registrarAcao(EntidadeAuditada.SERVICO, salvo.getId(), salvo.getNome(),
                    ativo ? AcaoAuditada.ATIVACAO : AcaoAuditada.DESATIVACAO);
        }

        return salvo;
    }

    private void auditar(Servico servico, String campo, Object anterior, Object novo) {
        auditoriaService.registrarAlteracao(EntidadeAuditada.SERVICO, servico.getId(), servico.getNome(),
                campo, anterior, novo);
    }

    public void deletar(Long id) {
        // "excluir" um serviço na prática é desativar: ele pode estar referenciado
        // por pedidos já feitos (FK servico_id em pedido), então apagar a linha de
        // verdade quebraria o histórico. Ver AtualizarStatusServicoRequest.
        atualizarStatus(id, false);
    }
}