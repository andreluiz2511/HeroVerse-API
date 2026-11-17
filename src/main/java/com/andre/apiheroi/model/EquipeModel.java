package com.andre.apiheroi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
public class EquipeModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da equipe é obrigatório")
    @Size(max = 100, message = "O nome da equipe deve ter no máximo 100 caracteres")
    private String nome;

    @NotNull(message = "O perfil é obrigatório")
    @ManyToOne(optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilModel perfil;

    @ManyToMany
    @JoinTable(
            name = "equipe_heroi",
            joinColumns = @JoinColumn(name = "equipe_id"),
            inverseJoinColumns = @JoinColumn(name = "heroi_id")
    )
    private List<HeroiModel> herois = new ArrayList<>();

    public EquipeModel() {
    }

    public EquipeModel(String nome, PerfilModel perfil) {
        this.nome = nome;
        this.perfil = perfil;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public PerfilModel getPerfil() { return perfil; }
    public void setPerfil(PerfilModel perfil) { this.perfil = perfil; }
    public List<HeroiModel> getHerois() { return herois; }
    public void setHerois(List<HeroiModel> herois) { this.herois = herois; }
}
