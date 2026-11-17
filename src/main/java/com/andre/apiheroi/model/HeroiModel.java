package com.andre.apiheroi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class HeroiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String nome;

    @NotBlank(message = "O nome heroico é obrigatório")
    @Size(max = 100, message = "O nome heroico deve ter no máximo 100 caracteres")
    private String nomeHeroico;

    @NotNull(message = "O nível de força é obrigatório")
    @Enumerated(EnumType.STRING)
    private NivelForca nivelForca;

    @Min(value = 0, message = "A força deve ser de 0 a 100")
    @Max(value = 100, message = "A força deve ser de 0 a 100")
    private Integer forca;

    @NotNull(message = "O perfil é obrigatório")
    @ManyToOne(optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilModel perfil;

    @ManyToMany(mappedBy = "herois")
    private List<EquipeModel> equipes = new ArrayList<>();

    @OneToOne(mappedBy = "heroi")
    private PoderModel poder;

    @OneToOne(mappedBy = "heroi")
    private FraquezaModel fraqueza;

    public HeroiModel() {
    }

    public HeroiModel(String nome, String nomeHeroico, NivelForca nivelForca, Integer forca, PerfilModel perfil) {
        this.nome = nome;
        this.nomeHeroico = nomeHeroico;
        this.nivelForca = nivelForca;
        this.forca = forca;
        this.perfil = perfil;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNomeHeroico() { return nomeHeroico; }
    public void setNomeHeroico(String nomeHeroico) { this.nomeHeroico = nomeHeroico; }
    public NivelForca getNivelForca() { return nivelForca; }
    public void setNivelForca(NivelForca nivelForca) { this.nivelForca = nivelForca; }
    public Integer getForca() { return forca; }
    public void setForca(Integer forca) { this.forca = forca; }
    public PerfilModel getPerfil() { return perfil; }
    public void setPerfil(PerfilModel perfil) { this.perfil = perfil; }
    public List<EquipeModel> getEquipes() { return equipes; }
    public void setEquipes(List<EquipeModel> equipes) { this.equipes = equipes; }
    public PoderModel getPoder() { return poder; }
    public void setPoder(PoderModel poder) { this.poder = poder; }
    public FraquezaModel getFraqueza() { return fraqueza; }
    public void setFraqueza(FraquezaModel fraqueza) { this.fraqueza = fraqueza; }
}
