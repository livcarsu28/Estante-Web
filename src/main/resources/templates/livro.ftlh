<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>${livro.titulo} | EstanteWeb</title>
</head>
<body>
    <#include "cabecalho.ftlh">
    <h1>${livro.titulo}</h1>
    <dl>
        <dt>Autor</dt><dd>${livro.autor}</dd>
        <dt>Ano</dt><dd>${livro.ano?c}</dd>
        <dt>Preço</dt><dd>R$ ${livro.preco?string["0.00"]}</dd>
    </dl>
    <p><a href="/livros/html">Voltar para o acervo</a></p>
    <#if outrosLivros?has_content>
        <section>
            <h2>Outros títulos do mesmo autor</h2>
            <ul>
                <#list outrosLivros as outro>
                    <li><a href="/livros/${outro.id?c}">${outro.titulo}</a></li>
                </#list>
            </ul>
        </section>
    </#if>
</body>
</html>
