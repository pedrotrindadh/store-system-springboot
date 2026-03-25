# 🤖 Projeto `company` (Spring Boot)

API REST para gerenciar um fluxo simples de e-commerce: `User` (clientes), `Category` (categorias), `Product` (produtos) e `Order` (pedidos), com itens (`OrderItem`), e pagamento (`Payment`).

O projeto usa Spring Boot + Spring Data JPA (Hibernate) para persistir os dados e expõe endpoints via controllers em `resources`.

## 🧱 Como o projeto foi feito

Arquitetura em camadas (na prática):

 - 🧩 `resources` (REST Controllers): recebem requisições HTTP e retornam `ResponseEntity`.
 - ⚙️ `services`: encapsulam regras de negócio simples e acesso ao repositório.
 - 🗃️ `repositories`: `JpaRepository` para persistência (CRUD).
 - 🧬 `entities` / `enums` / `pk`: modelam o domínio e o mapeamento JPA (relacionamentos e chaves).
 - 🧯 `exceptions`:
  - 🔥 exceções lançadas em `services/exceptions`.
  - 🧰 um handler global em `resources/exceptions/ResourceExceptionHandler` transforma exceções em respostas JSON padronizadas (`StandardError`).

### 🧪 Seed de dados (perfil `test`)

Em `application.properties` o perfil ativo é `test`. Assim, em toda execução local (sem alterar perfil), o `config/TestConfig` carrega dados de exemplo no banco em memória (`H2`).

## ⚙️ Tecnologias

- ☕ Java
- 🚀 Spring Boot
- 🌐 Spring Web MVC (`spring-boot-starter-webmvc`)
- 🗃️ Spring Data JPA (`spring-boot-starter-data-jpa`)
- 🧊 H2 (em memória) no perfil `test`
- 🐘 PostgreSQL como dependência runtime (mas o perfil `test` usa H2)

## 🏗️ Classes de bootstrap e ambiente

### `CompanyApplication`

Classe de entrada da aplicacao:

- anotada com `@SpringBootApplication`
- contem o metodo `main` que chama `SpringApplication.run(...)`
- inicializa o contexto Spring e passa a atender as rotas (endpoints) declaradas em `resources`.

### `config/TestConfig`

Carregador de dados de exemplo quando o perfil ativo e `test`:

- anotado com `@Configuration` e `@Profile("test")`
- implementa `CommandLineRunner`, entao o metodo `run(...)` e executado na inicializacao
- cria entidades (`User`, `Category`, `Product`, `Order`, `OrderItem`, `Payment`) e as persiste usando os `repositories`
- relacionamentos importantes criados no seed:
  - `User` -> `Order` via `Order.client`
  - `Category` <-> `Product` via `Product.categories` (tabela `tb_product_category`)
  - `Order` <-> `Product` via `OrderItem` (chave composta `OrderItemPK`)
  - `Order` -> `Payment` via relacao `@OneToOne` e `@MapsId` (pagamento compartilha a chave com o pedido)

### `CompanyApplicationTests`

Teste simples de integracao:

- anotado com `@SpringBootTest`
- unico teste `contextLoads()` valida que o contexto Spring inicializa corretamente.

## 🚀 Como usar

### ✅ Pré-requisitos

- Java (compatível com o projeto)
- Maven

### ▶️ Executar

O projeto já configura o perfil `test` por padrão.

Opção 1 (Maven wrapper):

```powershell
.\mvnw.cmd spring-boot:run
```

Opção 2 (Maven):

```powershell
mvn spring-boot:run
```

Depois de iniciar, acesse:

- Endpoints REST: normalmente em `http://localhost:8080`
- Console H2: `http://localhost:8080/h2-console`

### 🔁 Rotas (endpoints)

Controllers disponíveis:

- `GET /users`
- `GET /users/{id}`
- `POST /users`
- `PUT /users/{id}`
- `DELETE /users/{id}`

- `GET /categories`
- `GET /categories/{id}`

- `GET /products`
- `GET /products/{id}`

- `GET /orders`
- `GET /orders/{id}`

Exemplo rápido (listar usuários):

```bash
curl http://localhost:8080/users
```

Exemplo (criar usuário):

```bash
curl -X POST http://localhost:8080/users ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Fulano\",\"email\":\"fulano@email.com\",\"phone\":\"999999999\",\"password\":\"123456\"}"
```

### 🧾 Observações de comportamento

- Tratamento global de erros existe para:
  - `ResourceNotFoundException` (retorna `404`)
  - `DatabaseException` (retorna `400`)
- Serviços de `Product`, `Order` e `Category` usam `Optional.get()` no `findById`. Se o recurso não existir, isso pode resultar em exceção não mapeada (erro 500). No projeto, o tratamento customizado está aplicado com mais consistência no `UserService`.

## 🗃️ Modelo de dados (entities, enums e PK)

### `entities/enums/OrderStatus`

Enum que representa o status do pedido:

- `WAITING_PAYMENT` (código `1`)
- `PAID` (código `2`)
- `SHIPPED` (código `3`)
- `DELIVERED` (código `4`)
- `CANCELED` (código `5`)

Nota: o metodo `OrderStatus.valueOf(int code)` no codigo atual ignora o parametro `code` e retorna sempre o primeiro valor do enum. Como `Order.getOrderStatus()` usa esse metodo, a serializacao pode nao refletir o valor armazenado em `orderStatus`.

No `Order`, o status é persistido como um `int` (`orderStatus`):

- `setOrderStatus(OrderStatus)` grava `orderStatus.getCode()`
- `getOrderStatus()` converte o `int` de volta para `OrderStatus`

### `entities/pk/OrderItemPK`

Chave composta (embeddable) de `OrderItem` formada por:

- `Order order` (associada via `order_id`)
- `Product product` (associada via `product_id`)

É usada em `OrderItem` através de `@EmbeddedId`.

### `entities/User`

Entidade de cliente (`tb_user`):

- Campos: `id`, `name`, `email`, `phone`, `password`
- Relacionamento:
  - `@OneToMany(mappedBy = "client")` para `Order`
- Serialização:
  - `order` é `@JsonIgnore` para evitar recursão em respostas JSON.

### `entities/Category`

Entidade de categoria (`/tb_category`):

- Campos: `id`, `name`
- Relacionamento:
  - `@ManyToMany(mappedBy = "categories")` com `Product`

O lado que define a tabela de associação é `Product` (em `tb_product_category`).

### `entities/Product`

Entidade de produto (`/tb_product`):

- Campos: `id`, `name`, `description`, `price`, `imgUrl`
- Relacionamentos:
  - `@ManyToMany` com `Category`, mapeado via `@JoinTable(name = "tb_product_category", ...)`
  - `@OneToMany(mappedBy = "id.product")` com `OrderItem`
- Serialização:
  - `getOrders()` é `@JsonIgnore` e retorna os pedidos deduzidos a partir dos `OrderItem`.

### `entities/Order`

Entidade de pedido (`/tb_order`):

- Campos: `id`, `moment` (timestamp `Instant`), `orderStatus` (persistido como `int`)
- Relacionamentos:
  - `@ManyToOne` com `User` (`client`, coluna `client_id`)
  - `@OneToMany(mappedBy = "id.order")` com `OrderItem` (`items`)
  - `@OneToOne(mappedBy = "order", cascade = CascadeType.ALL)` com `Payment` (`payment`)
- Comportamento:
  - `getTotal()` soma `subTotal` de cada `OrderItem` (`price * quantity`).
- Serialização:
  - `items` é exposto via `getOrderItem()` (não está marcado com `@JsonIgnore`).

### `entities/OrderItem`

Entidade do item do pedido (`tb_order_item`) com chave composta:

- `@EmbeddedId OrderItemPK id`
- Campos: `quantity`, `price`
- Métodos utilitários:
  - `getSubTotal()` retorna `price * quantity`
- Serialização:
  - `getOrder()` é `@JsonIgnore` para evitar recursão; o `Product` do item costuma ser retornado.

### `entities/Payment`

Entidade de pagamento (`tb_payment`):

- Campos: `id`, `moment`
- Relacionamento:
  - `@OneToOne` com `Order` usando `@MapsId` (compartilha a chave com o pedido)
  - `order` é `@JsonIgnore` para evitar recursão.

## 🗄️ Repositories

Interfaces `JpaRepository` que dão acesso a CRUD.

### `repositories/UserRepository`

`JpaRepository<User, Long>` para persistir `User`.

### `repositories/CategoryRepository`

`JpaRepository<Category, Long>` para persistir `Category`.

### `repositories/ProductRepository`

`JpaRepository<Product, Long>` para persistir `Product`.

### `repositories/OrderRepository`

`JpaRepository<Order, Long>` para persistir `Order`.

### `repositories/OrderItemRepository`

`JpaRepository<OrderItem, Long>` (conforme implementado).  
Como `OrderItem` usa uma chave embeddada (`OrderItemPK`), a tipagem do ID no repositório deve ser observada caso você evolua o projeto.

Observacao: em uma versao corrigida, o repositirio poderia ser `JpaRepository<OrderItem, OrderItemPK>` para refletir corretamente a chave composta.

## 🧰 Services (regras e acesso)

### `services/UserService`

Encapsula regras de `User`:

- `findAll()` -> `repository.findAll()`
- `findById(Long id)` -> `repository.findById(id).orElseThrow(...)` lançando `ResourceNotFoundException`
- `insert(User obj)` -> `repository.save(obj)`
- `delete(Long id)`:
  - mapeia `EmptyResultDataAccessException` para `ResourceNotFoundException`
  - mapeia `DataIntegrityViolationException` para `DatabaseException`
- `update(Long id, User obj)`:
  - usa `repository.getReferenceById(id)` e atualiza campos `name/email/phone`
  - mapeia `EntityNotFoundException` para `ResourceNotFoundException`

### `services/ProductService`

- `findAll()` -> lista produtos
- `findById(Long id)` -> usa `Optional.get()` (erro pode virar 500 se não existir)

### `services/OrderService`

- `findAll()` -> lista pedidos
- `findById(Long id)` -> usa `Optional.get()` (erro pode virar 500 se não existir)

### `services/CategoryService`

- `findAll()` -> lista categorias
- `findById(Long id)` -> usa `Optional.get()` (erro pode virar 500 se não existir)

## 🧯 Exceptions

### 🧷 `services/exceptions/ResourceNotFoundException`

Runtime exception lançada quando um recurso não é encontrado.

Mensagem: `Resource not found. Id <id>`.

É tratada pelo `ResourceExceptionHandler` em `resources/exceptions`.

### 💾 `services/exceptions/DatabaseException`

Runtime exception para falhas relacionadas a integridade de dados.

É tratada pelo `ResourceExceptionHandler` em `resources/exceptions`.

### 🛡️ `resources/exceptions/ResourceExceptionHandler`

`@ControllerAdvice` que mapeia exceções para respostas JSON:

- `ResourceNotFoundException` -> HTTP `404 Not Found`
- `DatabaseException` -> HTTP `400 Bad Request`

Retorna um `StandardError`.

### 🧾 `resources/exceptions/StandardError`

Modelo JSON da resposta de erro:

- `timestamp`
- `status`
- `error`
- `message`
- `path`

## 🌐 Resources (endpoints)

### 👤 `resources/UserResource`

Base: `/users`

- `GET /users` -> lista todos os usuários
- `GET /users/{id}` -> busca por id
- `POST /users` -> cria usuário (retorna `201 Created`)
- `PUT /users/{id}` -> atualiza `name/email/phone`
- `DELETE /users/{id}` -> remove usuário

### 🏷️ `resources/CategoryResource`

Base: `/categories`

- `GET /categories` -> lista categorias
- `GET /categories/{id}` -> busca por id

### 🧰 `resources/ProductResource`

Base: `/products`

- `GET /products` -> lista produtos
- `GET /products/{id}` -> busca por id

### 📦 `resources/OrderResource`

Base: `/orders`

- `GET /orders` -> lista pedidos
- `GET /orders/{id}` -> busca por id

