# Production Checklist

This chapter provides a final checklist before going live and a short post-release plan.

## Pre-Release Checklist

- All secrets stored outside the repo
- HTTPS enabled and HSTS configured
- Rate limiting enabled on public endpoints
- DB migrations tested
- CI pipeline green
- Monitoring and alerts configured
- Logs and traces visible in your observability backend

## Deployment Checklist

- Deploy to staging first
- Smoke test critical endpoints
- Run migrations
- Deploy new version
- Verify health checks and logs

## Post-Release Checklist

- Track error rates and latency
- Verify background jobs are processing
- Monitor database connections and pool usage
- Confirm audit logs are flowing

## Incident Readiness

- Define rollback steps
- Keep backups and restore procedures
- Document on-call and escalation paths

## Next Steps

- Iterate and document as your system grows

---

[Previous: CI/CD](./24-ci-cd.md) | [Back to Index](./README.md)
