# Secure API (Spring Boot + JWT)

Учебный проект для лабораторной по ИБ: REST API с JWT-аутентификацией,
защитой от SQLi и XSS, и CI/CD с SAST/SCA.

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
# 1. Логин
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password123"}' | jq -r .token)

# 2. Без токена → 401
curl -i http://localhost:8080/api/data

# 3. С токеном → 200
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/data

# 4. XSS-инъекция: тег <script> будет вырезан
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"x","content":"<script>alert(1)</script>hello"}'
```

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