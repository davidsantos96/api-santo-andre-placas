package br.com.santoandreplacas.apisantoandreplacas.estoque;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ItemEstoqueRepository extends JpaRepository<ItemEstoque, Long> {

    @Query("SELECT i FROM ItemEstoque i WHERE i.quantidade <= i.quantidadeMinima")
    List<ItemEstoque> findBaixoEstoque();
}
