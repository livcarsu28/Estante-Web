package br.com.estanteweb;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaginaController {

    @GetMapping(value = "/", produces = "text/plain;charset=UTF-8")
    public String inicio() {
        return "EstanteWeb: livros usados com história";
    }

    @GetMapping(value = "/sobre", produces = "text/plain;charset=UTF-8")
    public String sobre() {
        return "A EstanteWeb é um sebo de bairro que dá novas histórias aos livros usados.";
    }
}
