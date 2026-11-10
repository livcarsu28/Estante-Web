package br.com.estanteweb;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class SugestaoController {
    private final SugestaoRepository repository = new SugestaoRepository();

    @GetMapping(value = "/sugestoes/nova", produces = "text/html;charset=UTF-8")
    public String nova() {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><title>Sugestão de leitura | EstanteWeb</title></head>
                <body>
                    <h1>Sugestão de leitura</h1>
                    <form method="post" action="/sugestoes">
                        <p><label>Nome: <input type="text" name="nomeCliente"></label></p>
                        <p><label>Livro sugerido: <input type="text" name="livroSugerido"></label></p>
                        <button type="submit">Enviar sugestão</button>
                    </form>
                    <p><a href="/sugestoes">Ver sugestões recebidas</a></p>
                </body>
                </html>
                """;
    }

    @PostMapping(value = "/sugestoes", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> adicionar(@RequestParam("nomeCliente") String nomeCliente,
            @RequestParam("livroSugerido") String livroSugerido) {
        repository.adicionar(new Sugestao(nomeCliente, livroSugerido));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Total-Sugestoes", Integer.toString(repository.contarSugestoes()))
                .body("Obrigado, " + nomeCliente + "! Sua sugestão de leitura foi recebida: " + livroSugerido);
    }

    @GetMapping(value = "/sugestoes", produces = "text/html;charset=UTF-8")
    public String listar() {
        StringBuilder html = new StringBuilder("""
                <!DOCTYPE html><html lang="pt-BR">
                <head><meta charset="UTF-8"><title>Sugestões | EstanteWeb</title></head>
                <body><h1>Sugestões recebidas</h1><ul>
                """);
        for (Sugestao sugestao : repository.listarTodas()) {
            // Dados enviados por visitantes são exibidos como texto, sem executar tags.
            html.append("<li>").append(HtmlUtils.htmlEscape(sugestao.getLivroSugerido()))
                    .append(" - sugerido por ").append(HtmlUtils.htmlEscape(sugestao.getNomeCliente()))
                    .append("</li>");
        }
        return html.append("</ul><p><a href='/sugestoes/nova'>Enviar uma sugestão</a></p></body></html>")
                .toString();
    }
}
