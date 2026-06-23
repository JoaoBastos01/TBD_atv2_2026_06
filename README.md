# Sistema de Gerenciamento de Eventos

Trabalho 2 - sistema em Java para gerenciar eventos, participantes e relacionamentos usando MongoDB, Neo4j e PostgreSQL.

## Objetivo

O sistema atende aos requisitos do trabalho:

1. Dashboard em Java com interface grafica.
2. MongoDB para armazenar eventos e participantes.
3. Neo4j para representar relacoes entre participantes e eventos.
4. Consulta de participantes, organizadores e pessoas com duplo papel.
5. Migracao de participante para organizador em um evento.
6. Filtros dinamicos por local, data e palavra-chave.
7. Exportacao JSON.
8. Exportacao/sincronizacao para base SQL.

## Arquitetura

```text
Java Swing
  |-- MongoDB
  |     |-- events
  |     |-- participants
  |
  |-- Neo4j
  |     |-- (:Pessoa)
  |     |-- (:Evento)
  |     |-- [:PARTICIPOU]
  |     |-- [:ORGANIZOU]
  |
  |-- PostgreSQL
        |-- eventos
        |-- participantes
        |-- participacoes
```

## Bancos utilizados

| Banco | Responsabilidade |
| --- | --- |
| MongoDB | Fonte principal de eventos e participantes; filtros dinamicos; exportacao JSON |
| Neo4j | Relacionamentos entre pessoas e eventos |
| PostgreSQL | Espelho SQL, estatisticas do dashboard e exportacao SQL |

## Pre-requisitos

- Java 17
- Maven
- Docker com Docker Compose
- Ambiente grafico para abrir a interface Java Swing

Observacoes:

- No Windows ou macOS, Docker Desktop ja inclui Docker Compose.
- No Linux, instale Docker Engine e o plugin Docker Compose.
- Em servidores Linux sem ambiente grafico, a aplicacao Swing nao abre sem configuracao adicional de display.

## Portas usadas

| Servico | Porta |
| --- | --- |
| MongoDB | `localhost:27017` |
| PostgreSQL | `localhost:5433` |
| Neo4j Browser | `http://localhost:7475` |
| Neo4j Bolt | `bolt://localhost:7688` |

Credenciais:

| Banco | Usuario | Senha |
| --- | --- | --- |
| PostgreSQL | `postgres` | `1234` |
| Neo4j | `neo4j` | `senha123` |

## Como executar

Subir os bancos:

```bash
docker compose up -d
```

Verificar se os containers estao rodando:

```bash
docker compose ps
```

Compilar o projeto:

```bash
mvn clean package
```

Executar a aplicacao no Linux/macOS:

```bash
java -jar ./target/event-system-jar-with-dependencies.jar
```

Executar a aplicacao no Windows PowerShell:

```powershell
java -jar .\target\event-system-jar-with-dependencies.jar
```

## Como zerar os dados

Se aparecerem eventos ou participantes antigos, apague os volumes do Docker:

```bash
docker compose down -v
docker compose up -d
```

Depois compile e rode novamente:

```bash
mvn clean package
java -jar ./target/event-system-jar-with-dependencies.jar
```

## Testes de conexao

Com Docker:

```bash
docker compose ps
docker logs events-mongo
docker logs events-postgres
docker logs events-neo4j
```

No Linux/macOS, se tiver `nc` instalado:

```bash
nc -zv localhost 27017
nc -zv localhost 5433
nc -zv localhost 7475
nc -zv localhost 7688
```

No Windows PowerShell:

```powershell
Test-NetConnection localhost -Port 27017
Test-NetConnection localhost -Port 5433
Test-NetConnection localhost -Port 7475
Test-NetConnection localhost -Port 7688
```

As portas devem estar abertas. No PowerShell, o retorno esperado e:

```text
TcpTestSucceeded : True
```

## Como abrir e verificar os bancos

### MongoDB pelo terminal

Abrir o shell do MongoDB:

```bash
docker exec -it events-mongo mongosh events_db
```

Comandos dentro do `mongosh`:

```javascript
show collections
db.events.find().pretty()
db.participants.find().pretty()
db.events.countDocuments()
db.participants.countDocuments()
```

Tambem e possivel consultar sem entrar no shell:

```bash
docker exec events-mongo mongosh --quiet events_db --eval "db.events.find().toArray()"
docker exec events-mongo mongosh --quiet events_db --eval "db.participants.find().toArray()"
```

### Neo4j pelo navegador

Abra no navegador:

```text
http://localhost:7475
```

Login:

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

### Neo4j pelo terminal

```bash
docker exec -it events-neo4j cypher-shell -u neo4j -p senha123
```

Dentro do `cypher-shell`:

```cypher
MATCH (n) RETURN n;
MATCH (p:Pessoa)-[r]->(e:Evento) RETURN p.nome, type(r), e.nome;
```

### PostgreSQL pelo terminal

```bash
docker exec -it events-postgres psql -U postgres -d events_sql
```

Dentro do `psql`:

```sql
\dt
SELECT * FROM eventos;
SELECT * FROM participantes;
SELECT * FROM participacoes;
```

## Roteiro de teste funcional

### 1. Testar MongoDB: eventos, participantes e filtros

Na aplicacao:

1. Abra a aba **Eventos**.
2. Crie um novo evento.
3. Preencha nome, data, local, descricao e palavras-chave.
4. Clique em atualizar.
5. Teste os filtros por local, data e palavra-chave.
6. Abra a aba **Participantes**.
7. Crie um novo participante.

Verificar no MongoDB:

```bash
docker exec events-mongo mongosh --quiet events_db --eval "db.events.find().toArray()"
docker exec events-mongo mongosh --quiet events_db --eval "db.participants.find().toArray()"
```

### 2. Testar PostgreSQL: espelho SQL e estatisticas

Depois de criar eventos e participantes, verificar no PostgreSQL:

```bash
docker exec events-postgres psql -U postgres -d events_sql -c "SELECT * FROM eventos;"
docker exec events-postgres psql -U postgres -d events_sql -c "SELECT * FROM participantes;"
docker exec events-postgres psql -U postgres -d events_sql -c "SELECT * FROM participacoes;"
```

A aba **Visao Geral** mostra eventos e participantes vindos do MongoDB, organizadores vindos do Neo4j e participacoes vindas do PostgreSQL.

### 3. Testar Neo4j: relacionamentos

Na aplicacao:

1. Crie um evento.
2. Crie um participante.
3. Copie o ID do evento e o ID do participante.
4. Abra a aba **Relacionamentos**.
5. Informe os dois IDs.
6. Escolha `PARTICIPANTE`.
7. Clique em adicionar relacao.
8. Clique em consultar evento.

Verificar no Neo4j:

```bash
docker exec events-neo4j cypher-shell -u neo4j -p senha123 "MATCH (p:Pessoa)-[r]->(e:Evento) RETURN p.nome, type(r), e.nome;"
```

### 4. Testar migracao para organizador

Na aba **Relacionamentos**:

1. Informe o ID do evento.
2. Informe o ID da pessoa.
3. Clique em migrar para organizador.
4. Clique em consultar evento.

Verificar no Neo4j:

```bash
docker exec events-neo4j cypher-shell -u neo4j -p senha123 "MATCH (p:Pessoa)-[r]->(e:Evento) RETURN p.nome, type(r), e.nome;"
```

### 5. Testar duplo papel

Na aba **Relacionamentos**:

1. Adicione uma pessoa como `PARTICIPANTE`.
2. Depois clique em adicionar organizador sem remover participante.
3. Consulte o evento.

O sistema deve mostrar a pessoa como participante e organizadora no mesmo evento.

### 6. Testar exportacoes

Na aba **Exportacoes**:

1. Clique em exportar eventos para JSON.
2. Clique em exportar participantes para JSON.
3. Clique em exportar tudo para JSON.
4. Clique em gerar script SQL.
5. Clique em sincronizar para PostgreSQL.

Os arquivos sao gerados na pasta:

```text
exports/
```

Arquivos esperados:

```text
exports/eventos.json
exports/participantes.json
exports/export_completo.json
exports/dump.sql
```

O arquivo `dump.sql` contem o schema e os `INSERTs` de eventos, participantes e participacoes.

## Consultas uteis para demonstracao

Contar documentos no MongoDB:

```bash
docker exec events-mongo mongosh --quiet events_db --eval "JSON.stringify({events: db.events.countDocuments(), participants: db.participants.countDocuments()})"
```

Contar registros no PostgreSQL:

```bash
docker exec events-postgres psql -U postgres -d events_sql -c "SELECT 'eventos' AS tabela, COUNT(*) FROM eventos UNION ALL SELECT 'participantes', COUNT(*) FROM participantes UNION ALL SELECT 'participacoes', COUNT(*) FROM participacoes;"
```

Contar nos no Neo4j:

```bash
docker exec events-neo4j cypher-shell -u neo4j -p senha123 "MATCH (n) RETURN labels(n) AS tipo, count(n) AS total;"
```

## Observacao sobre dados antigos

O Docker usa volumes para persistir os dados dos bancos. Por isso, se um evento foi criado em uma execucao anterior, ele pode continuar aparecendo depois que a aplicacao for reiniciada.

Para iniciar uma demonstracao limpa:

```bash
docker compose down -v
docker compose up -d
```

## Estrutura principal do projeto

```text
src/main/java/com/events/config       Configuracoes dos bancos
src/main/java/com/events/model        Modelos Evento, Participante e Papel
src/main/java/com/events/repository   Repositorios MongoDB, Neo4j e PostgreSQL
src/main/java/com/events/service      Camada de servico
src/main/java/com/events/ui           Dashboard Swing
src/main/java/com/events/export       Exportadores JSON e SQL
```

## Comando rapido para demonstracao

Linux/macOS:

```bash
docker compose down -v
docker compose up -d
mvn clean package
java -jar ./target/event-system-jar-with-dependencies.jar
```

Windows PowerShell:

```powershell
docker compose down -v
docker compose up -d
mvn clean package
java -jar .\target\event-system-jar-with-dependencies.jar
```
