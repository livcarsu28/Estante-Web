package br.com.estanteweb;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PaginaController {
    @GetMapping(value = "/", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String inicio() {
        return "EstanteWeb: livros usados com história";
    }

    @GetMapping(value = "/sobre", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String sobre() {
        return "A EstanteWeb é um sebo de bairro que dá novas histórias aos livros usados.";
    }

    @GetMapping("/contato")
    public String contato(Model model) {
        return "contato";
    }

    @PostMapping("/contato")
    public String receberContato(@RequestParam("nome") String nome,
            @RequestParam("mensagem") String mensagem, Model model) {
        if (mensagem.isBlank()) {
            model.addAttribute("erro", "A mensagem não pode estar em branco.");
            model.addAttribute("nome", nome);
            model.addAttribute("mensagem", mensagem);
            return "contato";
        }
        return "redirect:/contato/sucesso";
    }

    @GetMapping("/contato/sucesso")
    public String contatoSucesso() {
        return "contato-sucesso";
    }
}
