package br.com.estanteweb;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LivroController {
    @Autowired
    private LivroRepository repository;

    @GetMapping(value = "/livros", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String listar() {
        return formatarAcervo(repository.listarTodos());
    }

    private String formatarAcervo(List<Livro> livros) {
        StringBuilder resposta = new StringBuilder("Acervo EstanteWeb: ");
        boolean primeiro = true;
        for (Livro livro : livros) {
            if (!primeiro) {
                resposta.append(" | ");
            }
            resposta.append(livro.getTitulo())
                    .append(", de ").append(livro.getAutor())
                    .append(" (R$ ").append(livro.getPreco()).append(")");
            primeiro = false;
        }
        return resposta.toString();
    }

    @GetMapping(value = "/livros/busca", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> buscar(
            @RequestParam(name = "precoMaximo", defaultValue = "999") double precoMaximo) {
        List<Livro> encontrados = repository.listarAtePreco(precoMaximo);
        if (encontrados.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nenhum livro encontrado até o preço de R$ " + precoMaximo + ".");
        }
        return ResponseEntity.ok()
                .header("X-Total-Encontrados", Integer.toString(encontrados.size()))
                .body(formatarAcervo(encontrados));
    }

    @GetMapping(value = "/livros/quem-acessa", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String quemAcessa(
            @RequestHeader(value = "User-Agent", defaultValue = "Desconhecido") String userAgent) {
        return "Você está acessando a partir de: " + userAgent;
    }

    @GetMapping(value = "/livros/total", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String total() {
        return "O acervo tem " + repository.contarLivros() + " livros.";
    }

    @GetMapping(value = "/livros/valor-total", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String valorTotal() {
        return "O valor total do acervo é R$ " + repository.somarValorAcervo();
    }

    @GetMapping("/livros/html")
    public String listarHtml(Model model) {
        model.addAttribute("livros", repository.listarTodos());
        return "livros";
    }
    @GetMapping("/livros/novo")
    public String novoLivro() {
        return "livro-novo";
    }

    @PostMapping("/livros")
    public String salvarLivro(@RequestParam("titulo") String titulo,
            @RequestParam("autor") String autor, @RequestParam("ano") int ano,
            @RequestParam("preco") double preco, Model model) {
        if (titulo.isBlank()) {
            model.addAttribute("erro", "O título é obrigatório.");
        } else if (autor.isBlank()) {
            model.addAttribute("erro", "O autor é obrigatório.");
        } else if (!Double.isFinite(preco) || preco <= 0) {
            model.addAttribute("erro", "O preço deve ser maior que zero e finito.");
        }
        if (model.containsAttribute("erro")) {
            model.addAttribute("titulo", titulo);
            model.addAttribute("autor", autor);
            model.addAttribute("ano", ano);
            model.addAttribute("preco", preco);
            return "livro-novo";
        }
        Livro livro = new Livro();
        livro.setTitulo(titulo);
        livro.setAutor(autor);
        livro.setAno(ano);
        livro.setPreco(preco);
        repository.salvar(livro);
        return "redirect:/livros/html";
    }
}
