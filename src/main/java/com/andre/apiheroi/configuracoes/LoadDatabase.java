package com.andre.apiheroi.configuracoes;

import com.andre.apiheroi.model.*;
import com.andre.apiheroi.repositorio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(RepositoryPerfil perfis, RepositoryHeroi herois, RepositoryPoder poderes,
                                   RepositoryFraqueza fraquezas, RepositoryEquipe equipes, PasswordEncoder passwordEncoder) {
        return args -> {
            String senha = passwordEncoder.encode("senha123");
            PerfilModel alfa = perfis.save(new PerfilModel("Líder Alfa", "lider.alfa@exemplo.com", senha));
            PerfilModel beta = perfis.save(new PerfilModel("Guardiã Beta", "guardia.beta@exemplo.com", senha));
            perfis.save(new PerfilModel("Recruta Gama", "recruta.gama@exemplo.com", senha));

            HeroiModel faisca = herois.save(new HeroiModel("Lia Moreira", "Faísca Azul", NivelForca.MUTANTE, 78, alfa));
            HeroiModel muralha = herois.save(new HeroiModel("Tomás Rocha", "Muralha", NivelForca.SUPERHUMANO, 91, alfa));
            HeroiModel eco = herois.save(new HeroiModel("Nara Vey", "Eco Estelar", NivelForca.ALIENIGENA, 85, alfa));
            HeroiModel sombra = herois.save(new HeroiModel("Iara Lopes", "Sombra Rubra", NivelForca.HUMANA, 55, beta));
            HeroiModel vento = herois.save(new HeroiModel("Caio Brandão", "Vento Norte", NivelForca.MUTANTE, 70, beta));

            poderes.save(new PoderModel("Rajada Elétrica", "Dispara descargas elétricas pelas mãos.", faisca));
            poderes.save(new PoderModel("Pele de Pedra", "Fica quase indestrutível por alguns minutos.", muralha));
            poderes.save(new PoderModel("Voo Cósmico", "Voa em altíssima velocidade, inclusive fora da atmosfera.", eco));
            poderes.save(new PoderModel("Camuflagem", "Se mistura às sombras e fica invisível no escuro.", sombra));
            poderes.save(new PoderModel("Controle do Vento", "Cria rajadas e correntes de ar.", vento));

            fraquezas.save(new FraquezaModel("Água Salgada", "Perde os poderes quando molhada com água do mar.", faisca));
            fraquezas.save(new FraquezaModel("Lentidão", "É muito lento quando está na forma de pedra.", muralha));
            fraquezas.save(new FraquezaModel("Campo Magnético", "Fica desorientada perto de ímãs fortes.", eco));
            fraquezas.save(new FraquezaModel("Ar Rarefeito", "Os poderes enfraquecem em grandes altitudes.", vento));

            EquipeModel liga = new EquipeModel("Liga do Amanhecer", alfa);
            liga.getHerois().addAll(List.of(faisca, muralha, eco));
            equipes.save(liga);
            EquipeModel esquadrao = new EquipeModel("Esquadrão Noturno", beta);
            esquadrao.getHerois().addAll(List.of(sombra, vento));
            equipes.save(esquadrao);

            log.info("HeroVerse carregado: {} perfis, {} heróis, {} poderes, {} fraquezas, {} equipes",
                    perfis.count(), herois.count(), poderes.count(), fraquezas.count(), equipes.count());
            log.info("Login de teste: lider.alfa@exemplo.com / senha123 (também guardia.beta@ e recruta.gama@, mesma senha)");
        };
    }
}
