# Security Hardening for FastAPI

This chapter covers API security fundamentals, HTTP headers, CSRF strategies, rate limiting, secrets management, and operational safeguards for production systems.

## Security Baseline

A secure FastAPI app needs defense in depth:

- Strong authentication and authorization
- Strict input validation and output filtering
- Protected transport (HTTPS)
- Abuse controls (rate limiting, quotas)
- Auditing and incident visibility

Use the OWASP API Security Top 10 as a checklist for risk modeling and prioritization.

## Transport Security and Headers

Serve only over HTTPS in production. Add security headers to reduce common attacks like clickjacking and content sniffing.

### Recommended Headers

| Header | Purpose | Notes |
|--------|---------|-------|
| `Strict-Transport-Security` | Enforce HTTPS | Only when HTTPS is enabled |
| `Content-Security-Policy` | Control content sources | Use `frame-ancestors` for clickjacking protection |
| `X-Content-Type-Options` | Disable MIME sniffing | Use `nosniff` |
| `Referrer-Policy` | Limit referrer leakage | Use `no-referrer` or `strict-origin-when-cross-origin` |
| `Permissions-Policy` | Restrict browser features | Disable unused features |

Note: Prefer CSP `frame-ancestors`. You can still add `X-Frame-Options` for legacy browser support.

### Security Headers Middleware

```python
from fastapi import FastAPI, Request

app = FastAPI()

SECURITY_HEADERS = {
    "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
    "Content-Security-Policy": "default-src 'none'; frame-ancestors 'none'",
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "strict-origin-when-cross-origin",
    "Permissions-Policy": "geolocation=(), microphone=(), camera=()",
}


@app.middleware("http")
async def add_security_headers(request: Request, call_next):
    response = await call_next(request)
    for key, value in SECURITY_HEADERS.items():
        response.headers.setdefault(key, value)
    return response
```

## CORS and CSRF

CORS controls which origins can access your API. CSRF is relevant when using cookies for authentication.

- For bearer tokens in headers, CSRF risk is low.
- For cookie-based auth, require a CSRF token.

### Simple CSRF Check

```python
from fastapi import Depends, HTTPException, Request


def verify_csrf(request: Request) -> None:
    csrf_cookie = request.cookies.get("csrf_token")
    csrf_header = request.headers.get("x-csrf-token")
    if not csrf_cookie or csrf_cookie != csrf_header:
        raise HTTPException(status_code=403, detail="CSRF validation failed")


@app.post("/payments", dependencies=[Depends(verify_csrf)])
async def create_payment():
    return {"ok": True}
```

## Authentication Hardening

- Hash passwords with Argon2 or bcrypt.
- Keep access tokens short-lived and use refresh tokens.
- Add rate limits to login and password reset endpoints.
- Rotate credentials and invalidate tokens after critical changes.

## Authorization Controls

- Enforce object-level checks on every resource access.
- Prefer query scoping to avoid accidental data leaks.
- Use scopes or permissions for fine-grained access.

## Input Validation and File Uploads

- Validate all input with Pydantic constraints.
- Enforce size limits on file uploads.
- Reject unexpected fields (use `extra="forbid"` in Pydantic models).

## Abuse Protection

- Add rate limiting and quotas on public endpoints.
- Use IP-based and user-based throttles.
- Add captcha or email verification for sensitive flows.

## Secrets Management

- Store secrets in environment variables or a secrets manager.
- Do not log secrets or tokens.
- Use different credentials for dev, staging, and production.

## Dependency and Supply Chain Security

- Pin dependencies with lock files.
- Run dependency audits in CI.
- Use minimal base images for Docker.

## Audit Logging

Track sensitive actions for incident response.

```python
logger.info(
    "security_event",
    action="user.password.reset",
    user_id=current_user.id,
    ip=request.client.host,
)
```

## Security Checklist

- Enforce HTTPS and HSTS
- Add security headers
- Lock down CORS
- Use short-lived tokens and rotate secrets
- Validate input and constrain uploads
- Apply rate limiting and abuse protection
- Log security events
- Review against OWASP API Top 10

## References

- [FastAPI Security](https://fastapi.tiangolo.com/tutorial/security/)
- [OWASP API Security Top 10](https://owasp.org/API-Security/editions/2023/en/0x11-t10/)
- [OWASP HTTP Security Headers Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html)

## Next Steps

- [Observability](./19-observability.md) - Traces, metrics, and logs
- [Performance](./20-performance.md) - Profiling and optimization

---

[Previous: GenAI Integration](./17-genai-integration.md) | [Back to Index](./README.md) | [Next: Observability](./19-observability.md)
