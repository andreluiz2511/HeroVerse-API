package com.andre.apiheroi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
public class PerfilModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do perfil é obrigatório")
    @Size(min = 3, max = 60, message = "O nome do perfil deve ter entre 3 e 60 caracteres")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Informe um e-mail válido")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    private String senha;

    @Column(name = "api_key", unique = true)
    private String apiKey;

    @OneToMany(mappedBy = "perfil")
    private List<HeroiModel> herois = new ArrayList<>();

    @OneToMany(mappedBy = "perfil")
    private List<EquipeModel> equipes = new ArrayList<>();

    public PerfilModel() {
    }

    public PerfilModel(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public List<HeroiModel> getHerois() { return herois; }
    public void setHerois(List<HeroiModel> herois) { this.herois = herois; }
    public List<EquipeModel> getEquipes() { return equipes; }
    public void setEquipes(List<EquipeModel> equipes) { this.equipes = equipes; }
}
