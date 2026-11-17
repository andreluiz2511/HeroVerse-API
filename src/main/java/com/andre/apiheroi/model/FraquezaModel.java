package com.andre.apiheroi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class FraquezaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da fraqueza é obrigatório")
    @Size(max = 100, message = "O nome da fraqueza deve ter no máximo 100 caracteres")
    private String nome;

    @NotBlank(message = "A descrição da fraqueza é obrigatória")
    @Size(max = 500, message = "A descrição da fraqueza deve ter no máximo 500 caracteres")
    private String descricao;

    @NotNull(message = "O herói é obrigatório")
    @OneToOne(optional = false)
    @JoinColumn(name = "heroi_id", nullable = false, unique = true)
    private HeroiModel heroi;

    public FraquezaModel() {
    }

    public FraquezaModel(String nome, String descricao, HeroiModel heroi) {
        this.nome = nome;
        this.descricao = descricao;
        this.heroi = heroi;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public HeroiModel getHeroi() { return heroi; }
    public void setHeroi(HeroiModel heroi) { this.heroi = heroi; }
}
