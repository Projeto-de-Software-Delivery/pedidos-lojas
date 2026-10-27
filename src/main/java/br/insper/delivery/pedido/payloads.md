Os 5 tópicos da arquitetura são pedido.criado, pedido.validado, entrega.aceita, pedido.retirado e pedido.entregue. Para cada um, a task define:

Payload: quais campos a mensagem carrega, com tipo e se são obrigatórios.
Versão: um número no evento que permite mudar o formato no futuro sem quebrar quem já consome a versão antiga.

{
"eventId": "b1f3...",
"eventType": "pedido.criado",
"version": 1,
"occurredAt": "2026-09-30T14:00:00Z",
"data": {
"pedidoId": "123",
"clienteId": "45",
"lojaId": "7",
"itens": [{ "produtoId": "9", "quantidade": 2, "precoUnitario": 15.90 }],
"total": 31.80,
"enderecoEntrega": { "rua": "...", "lat": -23.5, "lng": -46.6 }
}
}

pedido.validado (Loja aceitou e baixou o estoque; o Entregador consome para ofertar a corrida)

json
{
"pedidoId": "123",
"lojaId": "7",
"enderecoRetirada": { "rua": "...", "lat": -23.56, "lng": -46.65 },
"enderecoEntrega": { "rua": "...", "lat": -23.50, "lng": -46.60 },
"valorFrete": 8.00,
"tempoPreparoMin": 20
}

entrega.aceita (um entregador pegou a corrida)

json
{
"pedidoId": "123",
"entregaId": "88",
"entregadorId": "31",
"nomeEntregador": "João",
"veiculo": "moto",
"etaRetiradaMin": 12
}

pedido.retirado (entregador saiu da loja com o pedido)

json
{
"pedidoId": "123",
"entregaId": "88",
"entregadorId": "31",
"retiradoEm": "2026-09-30T14:25:00Z",
"etaEntregaMin": 18,
"clienteId": "45",
"lojaId": "7"
}

clienteId e lojaId foram acrescentados (sem mudar a versão, campos novos não quebram quem já consome) para o serviço de notificações saber quem avisar. Publicado por POST /pedidos/{id}/retirar; no fallback /eventos/pedido-retirado eles são opcionais.

pedido.entregue (PIN validado e entrega finalizada)

json
{
"pedidoId": "123",
"entregaId": "88",
"entregadorId": "31",
"entregueEm": "2026-09-30T14:45:00Z",
"pinValidado": true
}