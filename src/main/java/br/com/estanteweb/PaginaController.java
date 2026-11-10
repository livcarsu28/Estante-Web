package br.com.estanteweb;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @GetMapping(value = "/contato", produces = "text/html;charset=UTF-8")
    public String contato() {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><title>Contato | EstanteWeb</title></head>
                <body>
                    <h1>Contato</h1>
                    <form method="post" action="/contato">
                        <p><label>Nome: <input type="text" name="nome"></label></p>
                        <p><label>Mensagem: <textarea name="mensagem"></textarea></label></p>
                        <button type="submit">Enviar mensagem</button>
                    </form>
                    <p><a href="/">Início</a></p>
                </body>
                </html>
                """;
    }

    @PostMapping(value = "/contato", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> receberContato(@RequestParam("nome") String nome,
            @RequestParam("mensagem") String mensagem) {
        if (mensagem.isBlank()) {
            return ResponseEntity.badRequest().body("A mensagem não pode estar em branco.");
        }
        return ResponseEntity.ok("Obrigado, " + nome + "! Sua mensagem foi recebida.");
    }
}
