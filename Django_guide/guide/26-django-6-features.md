# Django 6.0 Notes and Upgrade Checklist

This chapter helps you evaluate and adopt new Django major versions safely.

## Step 1: Check Compatibility

- Verify DRF and third-party packages support your Django version.
- Read release notes for removals and deprecations.

## Step 2: Upgrade Strategy

1. Upgrade dependencies in a branch.
2. Run tests and fix warnings.
3. Update settings for deprecated options.
4. Re-run migrations and check DB compatibility.

## Step 3: Test Critical Paths

- Authentication and permissions
- Background jobs
- Admin workflows
- API schema generation

## Step 4: Production Rollout

- Deploy to staging first
- Monitor error rates and logs
- Roll back if necessary

## Tips

- Prefer LTS versions for DRF-heavy projects.
- Keep dependencies pinned for reproducibility.

## Next Steps

- [Architecture and Diagrams](./27-architecture-diagrams.md)
- [Observability](./28-observability.md)
- [Performance](./29-performance.md)

---

[Previous: Frontend Integration](./25-frontend-integration.md) | [Back to Index](./README.md) | [Next: Architecture and Diagrams](./27-architecture-diagrams.md)
