package br.com.estanteweb;

// Modelo do domínio: não depende de Spring, HTTP ou apresentação.
public class Livro {
    private final String titulo;
    private final String autor;
    private final int ano;
    private final double preco;

    public Livro(String titulo, String autor, int ano, double preco) {
        this.titulo = titulo;
        this.autor = autor;
        this.ano = ano;
        this.preco = preco;
    }

    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public int getAno() { return ano; }
    public double getPreco() { return preco; }
}
