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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
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
    public String listarHtml(@RequestParam(name = "ordem", required = false) String ordem, Model model) {
        model.addAttribute("livros", repository.listarOrdenadoPor(ordem));
        return "livros";
    }
    @GetMapping("/livros/novo")
    public String novoLivro() {
        return "livro-novo";
    }

    @PostMapping("/livros")
    public String salvarLivro(@RequestParam("titulo") String titulo,
            @RequestParam("autor") String autor, @RequestParam("ano") String ano,
            @RequestParam("preco") String preco, Model model) {
        Livro livro = validarFormulario(titulo, autor, ano, preco, model);
        if (livro == null) {
            return "livro-novo";
        }
        repository.salvar(livro);
        return "redirect:/livros/html";
    }

    // Cadastro e edição usam a mesma validação. Dados retornam ao Model somente no erro.
    private Livro validarFormulario(String titulo, String autor, String ano, String preco, Model model) {
        String erro = null;
        int anoNumero = 0;
        double precoNumero = 0;
        if (titulo.isBlank()) {
            erro = "O título é obrigatório.";
        } else if (autor.isBlank()) {
            erro = "O autor é obrigatório.";
        } else {
            try {
                anoNumero = Integer.parseInt(ano.trim());
            } catch (NumberFormatException e) {
                erro = "Informe um ano inteiro válido.";
            }
            if (erro == null) {
                try {
                    precoNumero = Double.parseDouble(preco.trim().replace(',', '.'));
                    if (!Double.isFinite(precoNumero) || precoNumero <= 0) {
                        erro = "O preço deve ser maior que zero.";
                    }
                } catch (NumberFormatException e) {
                    erro = "Informe um preço válido.";
                }
            }
        }
        if (erro != null) {
            model.addAttribute("erro", erro);
            model.addAttribute("titulo", titulo);
            model.addAttribute("autor", autor);
            model.addAttribute("ano", ano);
            model.addAttribute("preco", preco);
            return null;
        }
        Livro livro = new Livro();
        livro.setTitulo(titulo);
        livro.setAutor(autor);
        livro.setAno(anoNumero);
        livro.setPreco(precoNumero);
        return livro;
    }

    private Livro exigirLivro(Long id) {
        Livro livro = repository.buscarPorId(id);
        if (livro == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return livro;
    }

    @GetMapping("/livros/{id}/editar")
    public String editarLivro(@PathVariable("id") Long id, Model model) {
        Livro livro = exigirLivro(id);
        model.addAttribute("livro", livro);
        model.addAttribute("titulo", livro.getTitulo());
        model.addAttribute("autor", livro.getAutor());
        model.addAttribute("ano", Integer.toString(livro.getAno()));
        model.addAttribute("preco", Double.toString(livro.getPreco()));
        return "livro-editar";
    }

    @PostMapping("/livros/{id}")
    public String salvarEdicao(@PathVariable("id") Long id,
            @RequestParam("titulo") String titulo, @RequestParam("autor") String autor,
            @RequestParam("ano") String ano, @RequestParam("preco") String preco, Model model) {
        model.addAttribute("livro", exigirLivro(id));
        Livro livro = validarFormulario(titulo, autor, ano, preco, model);
        if (livro == null) {
            return "livro-editar";
        }
        livro.setId(id);
        if (repository.atualizar(livro) == 0 && repository.buscarPorId(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/livros/" + id;
    }

    @GetMapping("/livros/{id}/excluir")
    public String confirmarExclusao(@PathVariable("id") Long id, Model model) {
        model.addAttribute("livro", exigirLivro(id));
        return "livro-excluir";
    }

    @PostMapping("/livros/{id}/excluir")
    public String excluirLivro(@PathVariable("id") Long id) {
        if (repository.excluir(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/livros/html";
    }

    @GetMapping("/livros/{id}")
    public String detalharLivro(@PathVariable("id") Long id, Model model) {
        Livro livro = repository.buscarPorId(id);
        if (livro == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("livro", livro);
        model.addAttribute("outrosLivros", repository.listarDoMesmoAutor(livro.getAutor(), id));
        return "livro";
    }
}
