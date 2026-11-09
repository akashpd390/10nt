---
name: git-commit-workflow
description: Enforce atomic, descriptive, phase-oriented git commits after every task, session, or chat instruction.
---

# Git Commit Workflow & Phasing Skill

This skill enforces strict version control practices on the 10nts project.

## Commit Frequency
- Perform commits after finishing each session, phase milestone, or user instruction.
- Never bundle unrelated changes across backend and android into a single ambiguous commit.

## Commit Message Format & Structure
Every commit must provide a structured narrative explaining:
1. **Title / Subject**: `[<phase/scope>] <concise imperative summary>` (e.g., `[Backend/Auth] Add JWT refresh token rotation with Drizzle schema`)
2. **What was done**: Bulleted list of additions, deletions, and refactors.
3. **Why changes were made**: The architectural rationale or user requirement addressed.
4. **Problems encountered & solutions**: Edge cases, bug fixes, or performance hurdles discovered during implementation.

### Example Template:
```
[Backend/Billing] Implement signed balance calculation and ledger entries

WHAT WAS DONE:
- Added billing calculation service for rent and electricity units.
- Created ledger entry triggers on bill generation and manual payment receipt.
- Added migration for signed running balance (`current_balance`).

WHY:
- Needed to support partial payments and advance rent credits without separate disjoint balance columns.

PROBLEMS & RESOLUTIONS:
- Problem: Negative balance floating-point inaccuracies when calculating per-unit electricity bills.
- Resolution: Used decimal/numeric SQL types in Drizzle and rounded values using financial precision utilities.
```
