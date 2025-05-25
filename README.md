# core-api - Módulo Base Comum para Microsserviços

O módulo `core-api` centraliza funcionalidades reutilizáveis e genéricas que podem ser compartilhadas entre os microsserviços do ecossistema da corretora. Um dos principais recursos implementados nesse módulo é o sistema de cache com Redis, utilizando serialização via Jackson (`ObjectMapper`) e suporte genérico para qualquer tipo de dado.

## RedisCacheService

### Localização

```
com.sob.CoreApi.cache.RedisCacheService
```

### Objetivo

A classe `RedisCacheService<T>` fornece uma abstração reutilizável para cacheamento com Redis. Ela é construída para:

* Evitar consultas repetidas ao banco de dados.
* Armazenar objetos serializados em cache com tempo de expiração (TTL).
* Retornar os dados em cache caso estejam disponíveis (cache hit).
* Buscar os dados no provider (ex: banco de dados) em caso de cache miss, armazená-los no cache e retorná-los.

## Componentes Envolvidos

* `StringRedisTemplate`: Cliente Spring para manipulação de valores do Redis usando `String` como tipo de chave/valor.
* `ObjectMapper`: Responsável por serializar e desserializar os objetos para JSON.
* `DataProvider<T>`: Interface funcional responsável por buscar dados "fallback" quando não encontrados no cache.
* `Class<T> typeClass`: Tipo genérico do objeto que será armazenado e recuperado.
* `Duration ttl`: Tempo de vida (Time To Live) de cada item armazenado no cache.

## Métodos

### `Optional<T> get(String key)`

Busca um valor no cache Redis usando a chave fornecida. Se encontrado (cache hit), o JSON é convertido de volta para o tipo esperado (`T`). Se não encontrado (cache miss), o `dataProvider.load(key)` é chamado, o resultado é salvo no cache e retornado.

### `void put(String key, T value)`

Serializa o valor do tipo `T` para JSON e armazena no Redis com a TTL definida.

### `void evict(String key)`

Remove explicitamente uma chave do Redis.

## Interface Esperada

### CacheService

```java
public interface CacheService<T> {
    Optional<T> get(String key);
    void put(String key, T value);
    void evict(String key);
}
```

### DataProvider

```java
@FunctionalInterface
public interface DataProvider<T> {
    T load(String key);
}
```

## Como Usar nos Microsserviços

### 1. Dependência no `pom.xml`

```xml
<dependency>
    <groupId>com.sob</groupId>
    <artifactId>core-api</artifactId>
    <version>1.0.0</version> <!-- substitua pela versão correta -->
</dependency>
```

### 2. Configuração do Redis no `application.yml`

```yaml
spring:
  redis:
    host: localhost
    port: 6379
```

### 3. Criação do CacheService no microserviço

```java
@Bean
public CacheService<UsuarioDTO> usuarioCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    return new RedisCacheService<>(
        redisTemplate,
        objectMapper,
        usuarioId -> usuarioRepository.buscarPorId(usuarioId),
        UsuarioDTO.class,
        Duration.ofMinutes(30)
    );
}
```

### 4. Uso no código

```java
@Autowired
private CacheService<UsuarioDTO> usuarioCacheService;

public UsuarioDTO obterUsuario(String id) {
    return usuarioCacheService.get(id)
        .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
}
```

## Logs

* `[CACHE HIT] Key {}`: Indica que a chave foi encontrada no cache.
* `[CACHE MISS] Key {}`: Chave não encontrada, dados buscados do banco.
* `[CACHE PUT] Key {}`: Um novo valor foi armazenado com sucesso no cache.
* `[CACHE EVICT] Key {}`: A chave foi removida manualmente do cache.
* `Erro ao serializar/desserializar`: Indica falha na transformação JSON ↔ objeto.

## Vantagens

* Reutilização: Basta injetar um `DataProvider` e definir o tipo e TTL para usar o mesmo cache.
* Flexível: Suporta qualquer classe genérica com JSON serializável.
* Desacoplamento: A lógica de fallback fica isolada no `DataProvider`, facilitando testes e manutenção.

---

Para quaisquer novos microsserviços que precisarem de cache com Redis, recomenda-se utilizar diretamente o `RedisCacheService` definido neste módulo, reaproveitando a lógica genérica implementada no core.
