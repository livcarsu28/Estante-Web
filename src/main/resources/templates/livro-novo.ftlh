
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Novo livro - EstanteWeb</title>
</head>
<body>
   <#include "cabecalho.ftlh">

<h1>Novo Livro</h1>

<#if erro??>
    <p>${erro}</p>
</#if>

<form method="post" action="/livros">

    <label for="titulo">Título:</label>
    <input type="text" id="titulo" name="titulo" value="${titulo!}">
    <br><br>

    <label for="autor">Autor:</label>
    <input type="text" id="autor" name="autor" value="${autor!}">
    <br><br>

    <label for="ano">Ano:</label>
    <input type="text" id="ano" name="ano" value="${ano!}">
    <br><br>

    <label for="preco">Preço:</label>
    <input type="text" id="preco" name="preco" value="${preco!}">
    <br><br>

    <button type="submit">Cadastrar</button>

</form>
</body>
</html>