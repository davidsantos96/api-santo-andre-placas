package br.com.santoandreplacas.apisantoandreplacas.auditoria;

import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioAutenticadoProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AuditoriaService {

    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public AuditoriaService(RegistroAuditoriaRepository registroAuditoriaRepository,
                            UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.registroAuditoriaRepository = registroAuditoriaRepository;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> listar(EntidadeAuditada entidade, Long entidadeId) {
        return registroAuditoriaRepository.buscar(entidade, entidadeId);
    }

    public void registrarAcao(EntidadeAuditada entidade, Long entidadeId, String descricao, AcaoAuditada acao) {
        salvar(entidade, entidadeId, descricao, acao, null, null, null);
    }

    /** Só registra se o valor realmente mudou. */
    public void registrarAlteracao(EntidadeAuditada entidade, Long entidadeId, String descricao,
                                   String campo, Object valorAnterior, Object valorNovo) {
        if (Objects.equals(valorAnterior, valorNovo)) {
            return;
        }
        salvar(entidade, entidadeId, descricao, AcaoAuditada.ATUALIZACAO, campo,
                texto(valorAnterior), texto(valorNovo));
    }

    private void salvar(EntidadeAuditada entidade, Long entidadeId, String descricao, AcaoAuditada acao,
                        String campo, String valorAnterior, String valorNovo) {
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setEntidadeDescricao(descricao);
        registro.setAcao(acao);
        registro.setCampo(campo);
        registro.setValorAnterior(valorAnterior);
        registro.setValorNovo(valorNovo);
        registro.setFeitoPor(usuarioAutenticadoProvider.nomeUsuarioAtual());
        registro.setFeitoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));
        registroAuditoriaRepository.save(registro);
    }

    private String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}
