# SRE and Operations

Operating a production API at scale requires more than code. You need defined reliability targets, monitoring, alerting, and operational procedures.

## Goals

- Define SLOs and SLIs
- Build alerting and runbooks
- Reduce mean time to recovery

## SLO and SLI Basics

Examples of SLIs:

- Availability percentage
- p95 and p99 latency
- Error rate

A sample SLO could be: 99.9% availability and p95 latency below 300ms.

## Error Budgets

Use error budgets to balance reliability with feature delivery. When the budget is exhausted, focus on stability work.

## Incident Response

1. Detect via alerts and dashboards.
2. Triage and mitigate customer impact.
3. Communicate status updates.
4. Perform a postmortem with root cause analysis.

## Runbooks

Create runbooks for high-frequency incidents. Each runbook should include:

- Symptom description
- Immediate actions
- Rollback steps
- Escalation path

## Release Strategies

- Rolling deployments for normal changes.
- Canary releases for risky changes.
- Blue/green for near-zero downtime cutovers.

## Operational Dashboards

Dashboards should include:

- Request rate and error rate
- Latency distribution
- Resource utilization
- Queue depth and job failures

## Tips

- Keep on-call rotations sustainable.
- Automate common recovery tasks.
- Review incidents regularly and track follow-up work.

---

[Previous: Infrastructure as Code](./24-infrastructure-iac.md) | [Back to Index](./README.md) | [Next: Security Hardening ->](./26-security-hardening.md)
