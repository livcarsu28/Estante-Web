<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Acervo EstanteWeb</title>
</head>
<body>
    <#include "cabecalho.ftlh">
    <h2>Acervo EstanteWeb</h2>
    <p><a href="/livros/novo">Cadastrar livro</a></p>

    <nav aria-label="Ordenação do acervo">
        Ordenar por:
        <a href="/livros/html">Padrão</a> |
        <a href="/livros/html?ordem=titulo">Título</a> |
        <a href="/livros/html?ordem=autor">Autor</a> |
        <a href="/livros/html?ordem=ano">Ano</a> |
        <a href="/livros/html?ordem=preco">Preço</a>
    </nav>

    <#if livros?size == 0>
        <p>Nenhum livro no acervo no momento.</p>
    <#else>
        <ul>
            <#list livros as livro>
                <li><a href="/livros/${livro.id?c}">${livro.titulo}</a>, de ${livro.autor} (R$ ${livro.preco?c})</li>
            </#list>
        </ul>
    </#if>
</body>
</html>
