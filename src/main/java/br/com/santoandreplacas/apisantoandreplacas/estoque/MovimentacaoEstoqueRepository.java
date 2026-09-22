package br.com.santoandreplacas.apisantoandreplacas.estoque;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    List<MovimentacaoEstoque> findByItemEstoqueIdOrderByCriadoEmDesc(Long itemEstoqueId);
}
