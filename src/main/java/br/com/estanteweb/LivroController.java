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
}
