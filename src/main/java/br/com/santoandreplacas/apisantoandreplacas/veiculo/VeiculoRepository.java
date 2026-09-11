package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    Optional<Veiculo> findByPlaca(String placa);
}