package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import br.com.santoandreplacas.apisantoandreplacas.cliente.Cliente;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "veiculo")

public class Veiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String placa;
    private String marcaModelo;
    private int anoFabricacao;
    private int anoModelo;
    private String chassi;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private  Cliente cliente;

    private LocalDateTime criadoEm;


    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Veículo precisa estar vinculado a um cliente.");
        }
        this.cliente = cliente;
    }
    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("Digite a placa do veiculo");
        }
        this.placa = placa;
    }

    public String getMarcaModelo() {
        return marcaModelo;
    }
    public void setMarcaModelo(String marcaModelo) {
        if (marcaModelo == null || marcaModelo.isBlank()) {
            throw new IllegalArgumentException("Marca e modelo não pode ser vazio");
        }
        this.marcaModelo = marcaModelo;
    }

    public int getAnoFabricacao() {
        return anoFabricacao;
    }

    public void setAnoFabricacao(int anoFabricacao) {
        if (anoFabricacao < 1900) {
            throw new IllegalArgumentException("Ano de fabricação inválido.");
        }
        this.anoFabricacao = anoFabricacao;
    }

    public int getAnoModelo() {
        return anoModelo;
    }

    public void setAnoModelo(int anoModelo) {
        if (anoModelo < 1900) {
            throw new IllegalArgumentException("Ano do modelo inválido.");
        }
        this.anoModelo = anoModelo;
    }

    public String getChassi() {
        return chassi;
    }

    public void setChassi(String chassi) {
        if (chassi == null || chassi.isBlank()) {
            throw new IllegalArgumentException("o chassi nao pode ser vazio");
        }
        this.chassi = chassi;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}

