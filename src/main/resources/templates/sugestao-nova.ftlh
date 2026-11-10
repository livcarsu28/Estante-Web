<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Sugestão de leitura | EstanteWeb</title>
</head>
<body>
    <#include "cabecalho.ftlh">
    <h2>Sugestão de leitura</h2>
    <#if erro??>
        <p role="alert">${erro}</p>
    </#if>
    <form method="post" action="/sugestoes">
        <p><label for="nomeCliente">Nome:</label>
            <input type="text" id="nomeCliente" name="nomeCliente" value="${nomeCliente!}"></p>
        <p><label for="livroSugerido">Livro sugerido:</label>
            <input type="text" id="livroSugerido" name="livroSugerido" value="${livroSugerido!}"></p>
        <button type="submit">Enviar sugestão</button>
    </form>
    <p><a href="/sugestoes">Ver sugestões recebidas</a></p>
</body>
</html>
