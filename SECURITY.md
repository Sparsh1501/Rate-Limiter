# Security Policy

## Supported Versions

The latest release on the `main` branch receives security updates.

| Version | Supported |
| ------- | --------- |
| 1.0.x   | Yes       |

## Reporting a Vulnerability

Please **do not** open a public issue for security vulnerabilities.

Instead, report them privately using GitHub's
[private vulnerability reporting](https://github.com/Sparsh1501/Rate-Limiter/security/advisories/new)
(Security tab → "Report a vulnerability"), or email the maintainer at
**sparsh.indurkar@gmail.com**.

Please include:

- A description of the vulnerability and its impact
- Steps to reproduce (affected endpoint, algorithm, configuration)
- Any relevant logs or proof-of-concept

## Response

- We aim to acknowledge reports within **72 hours**.
- We will provide an estimated timeline for a fix after triage.
- Once a fix is released, we will credit the reporter (unless anonymity is requested).

## Scope

Examples of in-scope issues:

- Bypassing the rate limit (e.g. exceeding the configured limit under race conditions)
- Denial-of-service through crafted requests or configuration
- Information disclosure via error responses or metrics

Out of scope:

- Vulnerabilities in third-party dependencies (report upstream; Dependabot tracks updates here)
- Issues requiring physical access to the host or Redis instance
