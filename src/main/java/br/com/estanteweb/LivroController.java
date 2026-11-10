package br.com.estanteweb;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LivroController {
    private final LivroRepository repository = new LivroRepository();

    @GetMapping(value = "/livros", produces = "text/plain;charset=UTF-8")
    public String listar() {
        StringBuilder resposta = new StringBuilder("Acervo EstanteWeb: ");
        boolean primeiro = true;
        for (Livro livro : repository.listarTodos()) {
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

    @GetMapping(value = "/livros/total", produces = "text/plain;charset=UTF-8")
    public String total() {
        return "O acervo tem " + repository.contarLivros() + " livros.";
    }

    @GetMapping(value = "/livros/html", produces = "text/html;charset=UTF-8")
    public String listarHtml() {
        StringBuilder html = new StringBuilder("<!DOCTYPE html><html lang='pt-BR'>"
                + "<head><meta charset='UTF-8'><title>Acervo EstanteWeb</title></head>"
                + "<body><h1>Acervo EstanteWeb</h1><ul>");
        // Títulos fixos do repositório: a tag didática é interpretada nesta rota.
        for (Livro livro : repository.listarTodos()) {
            html.append("<li>").append(livro.getTitulo())
                    .append(", de ").append(livro.getAutor())
                    .append(" (R$ ").append(livro.getPreco()).append(")</li>");
        }
        return html.append("</ul></body></html>").toString();
    }
}
