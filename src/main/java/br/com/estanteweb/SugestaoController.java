package br.com.estanteweb;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SugestaoController {
    private final SugestaoRepository repository = new SugestaoRepository();

    @GetMapping(value = "/sugestoes/nova", produces = "text/html;charset=UTF-8")
    @ResponseBody
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

    @GetMapping("/sugestoes")
    public String listar(Model model) {
        model.addAttribute("sugestoes", repository.listarTodas());
        return "sugestoes";
    }
}
