package br.com.santoandreplacas.apisantoandreplacas.veiculo;

import br.com.santoandreplacas.apisantoandreplacas.cliente.Cliente;
import br.com.santoandreplacas.apisantoandreplacas.usuario.Usuario;
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
    private Integer anoFabricacao;
    private Integer anoModelo;
    private String chassi;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private  Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_usuario_id")
    private Usuario criadoPorUsuario;

    private String criadoPor; // snapshot do nome na hora da ação

    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por_usuario_id")
    private Usuario atualizadoPorUsuario;

    private String atualizadoPor;

    private LocalDateTime atualizadoEm;


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
        this.placa = placa.trim().toUpperCase();
    }

    public String getMarcaModelo() {
        return marcaModelo;
    }
    public void setMarcaModelo(String marcaModelo) {
        this.marcaModelo = marcaModelo;
    }

    public Integer getAnoFabricacao() {
        return anoFabricacao;
    }

    public void setAnoFabricacao(Integer anoFabricacao) {
        if (anoFabricacao != null && anoFabricacao < 1900) {
            throw new IllegalArgumentException("Ano de fabricação inválido.");
        }
        this.anoFabricacao = anoFabricacao;
    }

    public Integer getAnoModelo() {
        return anoModelo;
    }

    public void setAnoModelo(Integer anoModelo) {
        if (anoModelo != null && anoModelo < 1900) {
            throw new IllegalArgumentException("Ano do modelo inválido.");
        }
        this.anoModelo = anoModelo;
    }

    public String getChassi() {
        return chassi;
    }

    public void setChassi(String chassi) {
        this.chassi = chassi;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Usuario getCriadoPorUsuario() {
        return criadoPorUsuario;
    }

    public void setCriadoPorUsuario(Usuario criadoPorUsuario) {
        this.criadoPorUsuario = criadoPorUsuario;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(String criadoPor) {
        this.criadoPor = criadoPor;
    }

    public Usuario getAtualizadoPorUsuario() {
        return atualizadoPorUsuario;
    }

    public void setAtualizadoPorUsuario(Usuario atualizadoPorUsuario) {
        this.atualizadoPorUsuario = atualizadoPorUsuario;
    }

    public String getAtualizadoPor() {
        return atualizadoPor;
    }

    public void setAtualizadoPor(String atualizadoPor) {
        this.atualizadoPor = atualizadoPor;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}

