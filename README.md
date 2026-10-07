# Secure API (Spring Boot + JWT)

Учебный проект для лабораторной по ИБ: REST API с JWT-аутентификацией,
защитой от SQLi и XSS, и CI/CD с SAST/SCA.

Автор Юдин Георгий Дмитриевич P3413

## Эндпоинты

### POST /auth/login
Аутентификация. Тело:
```json
{ "username": "alice", "password": "password123" }
```
Ответ 200:
```json
{ "token": "<JWT>", "type": "Bearer" }
```
Ответ 401 при неверных данных.

### GET /api/data
Требует заголовок `Authorization: Bearer <JWT>`.
Возвращает список постов.

### POST /api/posts
Требует JWT. Тело:
```json
{ "title": "Hi", "content": "Hello <script>alert(1)</script>" }
```
Ответ — сохранённый пост с очищенным содержимым.

## Тестирование через curl

```bash
# 1. Успешная аутентификация
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'

# 2. Неверный пароль
curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"wrong"}'

# 3. Доступ без токена
curl -i http://localhost:8080/api/data

# 4. Доступ с JWT
TOKEN="<вставить_токен_из_шага_1>"
curl -i http://localhost:8080/api/data -H "Authorization: Bearer $TOKEN"

# 5. Проверка XSS-защиты
  curl -X POST http://localhost:8080/api/posts -H "Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhbGljZSIsInJvbGUiOiJVU0VSIiwiaWF0IjoxNzkxMzkzNTI3LCJleHAiOjE3OTEzOTcxMjd9.0v1kQtQa9dAR4TEwV49ymryepITTFsCbcAlPjUQQhf9bFzbEeZ2aBRBPWfbcxhtR" -H "Content-Type: application/json" -d @post.json
  
# 6. Проверка SQLi-защиты
  curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice'"'"' OR '"'"'1'"'"'='"'"'1","password":"x"}'
```
![Первый тест](docs/test_1.jpg)

![Второй тест](docs/test_2.jpg)

![Третий тест](docs/test_3.jpg)

![Четвертый тест](docs/test_4.jpg)

![Пятый тест](docs/test_5.jpg)

![Шестой тест](docs/test_6.jpg)

## Реализованные меры защиты

| Угроза | Мера |
|---|---|
| **SQL Injection (A03:2021)** | Все запросы — через Spring Data JPA / Hibernate. Hibernate использует PreparedStatement и параметризацию. Конкатенация SQL в коде отсутствует. |
| **XSS (A03:2021)** | OWASP Java HTML Sanitizer (`HtmlSanitizer.sanitize`) очищает title/content перед сохранением. Jackson при отдаче JSON экранирует спецсимволы. |
| **Broken Authentication (A07:2021)** | Пароли хранятся как BCrypt-хэши (`BCryptPasswordEncoder(12)`), JWT подписывается HMAC-SHA256, срок жизни 1 час, кастомный `JwtAuthFilter` валидирует токен на защищённых эндпоинтах. |
| **Broken Access Control (A01:2021)** | Spring Security: `/auth/login` — публичный, всё остальное — `authenticated()`. |

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`) при каждом push/PR:
1. Сборка и unit-тесты Maven.
2. **SAST** — SpotBugs + find-sec-bugs.
3. **SCA** — OWASP Dependency-Check (порог CVSS 8).
4. Отчёты публикуются как artifacts.

## Скриншоты CI/CD и отчётов сканеров

### Успешные запуски pipeline (GitHub Actions)

Все проверки запускаются автоматически при каждом push в `main`
и при создании pull request.

![Список запусков](docs/main.jpg)

### Детали последнего успешного запуска

Pipeline включает 4 шага:
1. Сборка проекта и unit-тесты (`mvn clean verify`).
2. **SAST** — SpotBugs + find-sec-bugs.
3. **SCA** — Snyk (`snyk test`).
4. Публикация отчётов как artifacts.

![Pipeline](docs/pre_main.jpg)
![Детали pipeline](docs/fixed_all.jpg)

### Отчёт SAST — SpotBugs

SpotBugs с плагином find-sec-bugs анализирует байт-код проекта
на типовые уязвимости (SQL injection, XSS, слабая криптография и т.д.).
SpotBugs + find-sec-bugs находит 5 замечаний (2 Medium, 3 Low).
Все проанализированы и признаны неприменимыми:

| Finding | Severity | Обоснование |
|---|---|---|
| `EI_EXPOSE_REP2` в `SecurityConfig` | Medium | Стандартный паттерн Spring DI. SpotBugs не распознаёт внедрение зависимостей и трактует передачу ссылки на бин как утечку. Не является уязвимостью. |
| `SPRING_ENDPOINT` × 3 | Low | Информационные маркеры «это Spring-контроллер». Не уязвимости — рекомендация проверять входные данные вручную (что и сделано через `@Valid`). |
| `CT_CONSTRUCTOR_THROW` в `JwtService` | Medium | Теоретическая finalizer-атака. В Java 17+ `finalizer()` deprecated, в 18+ удалён. Вектор отсутствует. |

Критических (High/Critical) findings **нет**.

![SpotBugs](docs/SAST_fixed.jpg)

### Отчёт SCA — Snyk

Snyk проверяет зависимости из `pom.xml` на известные CVE
(например, уязвимости в Spring, Jackson, H2, jjwt и т.д.).
В первый раз (предпоследний коммит) было обнаружено 60+ уязвимостей, связанных с транзитивными зависимостями spring-a.

![Snyk](docs/sca_first_attempt.jpg)

После обновил версию до предложенной минорной, но некоторые ошибки остались. Решил оставить все как есть, так как
решение этих проблем - переход на новую мажорную версию spring, отчего могут появиться множество проблем

![Snyk](docs/sca_fixed.jpg)

### Ссылка на последний успешный запуск

🔗 https://github.com/electropaskyda228/secure-api/actions/runs/37655288861