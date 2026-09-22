package br.com.santoandreplacas.apisantoandreplacas.estoque;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServicoItemEstoqueRepository extends JpaRepository<ServicoItemEstoque, Long> {
    List<ServicoItemEstoque> findByServicoId(Long servicoId);
}
