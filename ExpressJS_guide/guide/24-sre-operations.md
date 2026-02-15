# 24 - SRE and Operations

SRE focuses on reliability, fast recovery, and predictable operations.

## Goals

- Define SLIs and SLOs
- Build alerting and runbooks
- Improve incident response

## 1. SLIs and SLOs

Example SLIs:

- Availability
- p95 and p99 latency
- Error rate
- Saturation (CPU, memory, queue depth)

Example SLOs:

- 99.9% availability per month
- p95 latency under 300ms
- Error rate under 0.1%

## 2. Error Budgets

Use error budgets to balance reliability and feature delivery.

- If the budget is healthy, ship features.
- If the budget is exhausted, focus on stability work.

## 3. Runbooks

Each runbook should include:

- Symptoms
- Immediate mitigation steps
- Rollback instructions
- Escalation path
- Links to dashboards

## 4. Alert Design

Alert on symptoms, not causes.

- High error rate
- Latency spikes
- Queue backlogs
- Saturation for a sustained period

## 5. Example Alert Thresholds

- p95 latency > 500ms for 10 minutes
- error rate > 1% for 5 minutes
- queue depth > 1000 for 15 minutes

## 6. Incident Process

1. Detect
2. Triage
3. Mitigate
4. Communicate
5. Postmortem

## 7. Postmortems

- Make them blameless and specific.
- Record timeline, impact, and action items.
- Track follow ups to completion.

## 8. Disaster Recovery

- Test backups regularly.
- Verify restore procedures.
- Define RTO and RPO for each service.

## Tips

- Keep on-call rotations sustainable.
- Automate recovery steps where possible.
- Review incidents regularly and track action items.

---

[Previous: OAuth/OIDC, RBAC, and Rate Limiting](./23-oauth-oidc-rbac-rate-limiting.md) | [Back to Index](./README.md) | [Next: High-Scale Architecture ->](./25-high-scale-architecture.md)
