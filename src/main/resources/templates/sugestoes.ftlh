<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Sugestões | EstanteWeb</title>
</head>
<body>
    <#include "cabecalho.ftlh">
    <h2>Sugestões recebidas</h2>
    <#if sugestoes?size == 0>
        <p>Nenhuma sugestão recebida no momento.</p>
    <#else>
        <ul>
            <#list sugestoes as sugestao>
                <li>${sugestao.livroSugerido} - sugerido por ${sugestao.nomeCliente}</li>
            </#list>
        </ul>
    </#if>
    <p><a href="/sugestoes/nova">Enviar uma sugestão</a></p>
</body>
</html>
