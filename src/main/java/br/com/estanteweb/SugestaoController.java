package br.com.estanteweb;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SugestaoController {
    @Autowired
    private SugestaoRepository repository;

    @GetMapping("/sugestoes/nova")
    public String nova(Model model) {
        return "sugestao-nova";
    }

    @PostMapping("/sugestoes")
    public String adicionar(@RequestParam("nomeCliente") String nomeCliente,
            @RequestParam("livroSugerido") String livroSugerido, Model model) {
        if (nomeCliente.isBlank() || livroSugerido.isBlank()) {
            model.addAttribute("erro", "Preencha seu nome e o livro sugerido.");
            model.addAttribute("nomeCliente", nomeCliente);
            model.addAttribute("livroSugerido", livroSugerido);
            return "sugestao-nova";
        }
        repository.salvar(new Sugestao(nomeCliente, livroSugerido));
        return "redirect:/sugestoes?enviada=true";
    }

    @GetMapping("/sugestoes")
    public String listar(@RequestParam(name = "enviada", required = false) Boolean enviada, Model model) {
        model.addAttribute("sugestoes", repository.listarTodas());
        model.addAttribute("enviada", Boolean.TRUE.equals(enviada));
        return "sugestoes";
    }
}
