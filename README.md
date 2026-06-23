# Sistema de Gerenciamento de Eventos

Trabalho 2 - sistema em Java para gerenciar eventos, participantes e relacionamentos usando MongoDB, Neo4j e PostgreSQL.

## Funcionalidades

- Dashboard em Java Swing.
- Cadastro e consulta de eventos.
- Cadastro e consulta de participantes.
- Filtros dinamicos por local, data e palavra-chave.
- Relacionamentos entre participantes e eventos.
- Migracao de participante para organizador.
- Suporte a pessoa com duplo papel: participante e organizador no mesmo evento.
- Exportacao JSON.
- Exportacao SQL com schema e dados.

## Arquitetura

```text
Java Swing
  |-- MongoDB: eventos, participantes e filtros
  |-- Neo4j: relacionamentos PARTICIPOU e ORGANIZOU
  |-- PostgreSQL: espelho SQL, estatisticas e dump.sql
```

## Bancos utilizados

| Banco | Uso no projeto |
| --- | --- |
| MongoDB | Fonte principal de eventos e participantes |
| Neo4j | Grafo de relacoes entre pessoas e eventos |
| PostgreSQL | Base SQL sincronizada e exportacao SQL |

## Pre-requisitos

- Java 17
- Maven
- Docker com Docker Compose
- Ambiente grafico para abrir a interface Java Swing

No Windows e macOS, o Docker Desktop ja inclui o Docker Compose. No Linux, instale o Docker Engine e o plugin Docker Compose.

Em servidores Linux sem interface grafica, a aplicacao Swing nao abre sem configuracao adicional de display.

## Portas e credenciais

| Servico | Endereco |
| --- | --- |
| MongoDB | `localhost:27017` |
| PostgreSQL | `localhost:5433` |
| Neo4j Browser | `http://localhost:7475` |
| Neo4j Bolt | `bolt://localhost:7688` |

| Banco | Usuario | Senha |
| --- | --- | --- |
| PostgreSQL | `postgres` | `1234` |
| Neo4j | `neo4j` | `senha123` |

## Como rodar

Suba os bancos:

```bash
docker compose up -d
```

Confira se os containers estao rodando:

```bash
docker compose ps
```

Compile o projeto:

```bash
mvn clean package
```

Execute no Linux/macOS:

```bash
java -jar ./target/event-system-jar-with-dependencies.jar
```

Execute no Windows PowerShell:

```powershell
java -jar .\target\event-system-jar-with-dependencies.jar
```

## Como testar pelo sistema

1. Abra a aplicacao.
2. Na aba **Eventos**, crie um evento com nome, data, local, descricao e palavras-chave.
3. Na aba **Participantes**, crie um participante com nome e e-mail.
4. Copie o ID do evento e o ID da pessoa.
5. Na aba **Relacionamentos**, adicione a pessoa como `PARTICIPANTE`.
6. Consulte o evento para verificar a relacao.
7. Teste a migracao para organizador.
8. Teste a opcao de adicionar organizador sem remover participante.
9. Na aba **Exportacoes**, gere os arquivos JSON e SQL.

## Como verificar os bancos

### MongoDB

Abrir o terminal do MongoDB:

```bash
docker exec -it events-mongo mongosh events_db
```

Comandos uteis dentro do `mongosh`:

```javascript
show collections
db.events.find().pretty()
db.participants.find().pretty()
db.events.countDocuments()
db.participants.countDocuments()
```

Tambem e possivel consultar direto pelo terminal:

```bash
docker exec events-mongo mongosh --quiet events_db --eval "db.events.find().toArray()"
docker exec events-mongo mongosh --quiet events_db --eval "db.participants.find().toArray()"
```

### Neo4j

Abra no navegador:

```text
http://localhost:7475
```

Use:

```text
usuario: neo4j
senha: senha123
```

Consultas uteis no Neo4j Browser:

```cypher
MATCH (n) RETURN n;
MATCH (p:Pessoa)-[r]->(e:Evento) RETURN p, r, e;
MATCH (p:Pessoa)-[r]->(e:Evento) RETURN p.id, p.nome, type(r), e.id, e.nome;
```

Tambem e possivel consultar pelo terminal:

```bash
docker exec events-neo4j cypher-shell -u neo4j -p senha123 "MATCH (p:Pessoa)-[r]->(e:Evento) RETURN p.nome, type(r), e.nome;"
```

### PostgreSQL

Abrir o terminal do PostgreSQL:

```bash
docker exec -it events-postgres psql -U postgres -d events_sql
```

Comandos uteis dentro do `psql`:

```sql
\dt
SELECT * FROM eventos;
SELECT * FROM participantes;
SELECT * FROM participacoes;
```

Tambem e possivel consultar direto pelo terminal:

```bash
docker exec events-postgres psql -U postgres -d events_sql -c "SELECT * FROM eventos;"
docker exec events-postgres psql -U postgres -d events_sql -c "SELECT * FROM participantes;"
docker exec events-postgres psql -U postgres -d events_sql -c "SELECT * FROM participacoes;"
```

## Exportacoes

Os arquivos sao gerados na pasta `exports/`:

```text
exports/eventos.json
exports/participantes.json
exports/export_completo.json
exports/dump.sql
```

O arquivo `dump.sql` contem o schema e os `INSERTs` de eventos, participantes e participacoes.

## Problemas comuns

Se aparecerem dados antigos, apague os volumes do Docker e suba os bancos novamente:

```bash
docker compose down -v
docker compose up -d
```

Se alguma porta estiver ocupada, pare o servico local que esta usando a porta ou altere a porta externa no `docker-compose.yml`.

Se a aplicacao acusar erro de conexao, confira:

```bash
docker compose ps
docker logs events-mongo
docker logs events-postgres
docker logs events-neo4j
```

No Windows PowerShell, as portas podem ser testadas com:

```powershell
Test-NetConnection localhost -Port 27017
Test-NetConnection localhost -Port 5433
Test-NetConnection localhost -Port 7475
Test-NetConnection localhost -Port 7688
```
