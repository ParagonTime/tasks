### Быстрый старт

```cmd
git clone git@github.com:ParagonTime/tasks.git
cd tasks
```

```cmd
mvn clean package -DskipTests
```

```cmd
docker-compose up -d --build
```

### После запуска контенеров
можно посмотреть OpenAPI
http://localhost:8080/swagger-ui.html

### Краткое описание EP

```declarative
POST /tasks – создать задачу

GET /tasks?page=0&size=20 – список задач

GET /tasks/{id} – получить задачу по ID

PATCH /tasks/{id}/executor/{userId} – назначить исполнителя

PATCH /tasks/{id}/status/{status} – сменить статус

POST /users – создать пользователя
```
