package br.com.estanteweb;

import java.util.List;
import java.util.ArrayList;

public class LivroRepository {
    private final List<Livro> livros = List.of(
            new Livro("O Mapa da Rua Antiga", "Helena Duarte", 2001, 24.9),
            new Livro("Cartas de uma Livraria", "Marcos Azevedo", 2010, 22.0),
            new Livro("O Jardim das Memórias", "Clara Nogueira", 1998, 18.5),
            new Livro("Viagem ao Último Capítulo", "Rafael Prado", 2015, 30.0),
            new Livro("Contos da Chuva", "Lia Monteiro", 2007, 16.9),
            // Sexto livro acrescentado na missão 4.3 para comparar texto e HTML.
            new Livro("O <b>Grande</b> Gatsby", "F. Scott Fitzgerald", 1925, 28.0)
    );

    public List<Livro> listarTodos() {
        return livros;
    }

    public List<Livro> listarAtePreco(double precoMaximo) {
        List<Livro> encontrados = new ArrayList<>();
        for (Livro livro : livros) {
            if (livro.getPreco() <= precoMaximo) {
                encontrados.add(livro);
            }
        }
        return encontrados;
    }

    public int contarLivros() {
        return livros.size();
    }
}
