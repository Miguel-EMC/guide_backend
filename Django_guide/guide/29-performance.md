# Performance and Profiling

This chapter covers practical performance tuning for Django + DRF.

## Step 1: Measure First

- Enable query logging in development.
- Track p95 latency and error rates.
- Profile slow endpoints.

## Step 2: Avoid N+1 Queries

```python
# Use select_related for FK and prefetch_related for M2M
appointments = Appointment.objects.select_related("doctor", "patient")
```

## Step 3: Use Database Indexes

Add indexes for common filters.

```python
class Doctor(models.Model):
    last_name = models.CharField(max_length=100, db_index=True)
```

## Step 4: Cache Read-Heavy Endpoints

Use view-level caching or low-level caching (see caching chapter).

## Step 5: Pagination and Limits

Always paginate list endpoints and set `max_page_size`.

## Step 6: Optimize Serialization

- Avoid nested serializers in large lists.
- Use `only()` or `defer()` for heavy fields.

## Step 7: Profiling Tools

- Django Debug Toolbar (local dev)
- `cProfile` for hotspots
- Database EXPLAIN for slow queries

## Tips

- Keep ORM queries small and explicit.
- Add tests that assert query counts for critical endpoints.

## Next Steps

- [CI/CD](./30-ci-cd.md)

---

[Previous: Observability](./28-observability.md) | [Back to Index](./README.md) | [Next: CI/CD](./30-ci-cd.md)
