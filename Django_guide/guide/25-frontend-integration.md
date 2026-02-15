# Frontend Integration

This chapter shows how to connect a frontend (React/Vue/etc.) to your DRF API.

## Step 1: Configure CORS

```python
# config/settings.py
INSTALLED_APPS = ["corsheaders", *INSTALLED_APPS]
MIDDLEWARE = ["corsheaders.middleware.CorsMiddleware", *MIDDLEWARE]

CORS_ALLOWED_ORIGINS = [
    "http://localhost:3000",
    "https://app.example.com",
]

CORS_ALLOW_CREDENTIALS = True
```

## Step 2: Decide Auth Strategy

- Token auth: send `Authorization: Token <token>`
- Session auth: use cookies + CSRF token

## Step 3: Fetch Data (Token Auth)

```javascript
async function getDoctors() {
  const res = await fetch("http://localhost:8000/api/doctors/", {
    headers: { Authorization: `Token ${localStorage.getItem("token")}` },
  });
  return res.json();
}
```

## Step 4: POST Data

```javascript
async function createDoctor(payload) {
  const res = await fetch("http://localhost:8000/api/doctors/", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Token ${localStorage.getItem("token")}`,
    },
    body: JSON.stringify(payload),
  });
  return res.json();
}
```

## Step 5: Session + CSRF

If you use cookie auth, send credentials and CSRF token.

```javascript
async function csrfFetch(url, options = {}) {
  const csrftoken = document.cookie
    .split(";")
    .find((c) => c.trim().startsWith("csrftoken="))
    ?.split("=")[1];

  return fetch(url, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "X-CSRFToken": csrftoken,
      ...(options.headers || {}),
    },
  });
}
```

## Tips

- Keep CORS strict in production.
- Use HTTPS for cookies and tokens.
- Store tokens in memory or secure storage.

## Next Steps

- [Django 6.0 Notes](./26-django-6-features.md)
- [Architecture and Diagrams](./27-architecture-diagrams.md)

---

[Previous: API Schema Generation](./24-api-schema-generation.md) | [Back to Index](./README.md) | [Next: Django 6.0 Notes](./26-django-6-features.md)
