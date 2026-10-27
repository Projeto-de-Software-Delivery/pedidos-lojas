-- Roda a cada subida (spring.sql.init.mode=always), antes do Hibernate.
-- O Hibernate cria um CHECK com os valores do enum PedidoStatus só quando cria a tabela, e o
-- ddl-auto=update nunca atualiza esse CHECK. Bancos criados antes de ENTREGA_ACEITA existir
-- rejeitavam a atribuição de entregador (500 no aceitar). O enum já é validado pela JPA.
ALTER TABLE IF EXISTS pedido DROP CONSTRAINT IF EXISTS pedido_status_check;
