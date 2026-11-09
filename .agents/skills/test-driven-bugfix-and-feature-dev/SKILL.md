---
name: test-driven-bugfix-and-feature-dev
description: Strictly enforces Test-Driven Development (TDD). Whenever writing a new service method, class, or fixing a bug, write the tests first, verify they fail/reproduce the bug, implement the fix until all tests pass, submit for review, and commit to git.
---

# Test-Driven Development (TDD) & Bugfix Skill

This skill enforces a rigorous test-first methodology for all new service methods, classes, and bug fixes across both backend and mobile codebases.

## 1. Core Workflow (Red -> Green -> Refactor -> Commit)
Whenever a new service method/class is requested or a bug is reported:

1. **Step 1: Write Tests First (Red Phase)**
   - Before writing or altering implementation code, write a comprehensive test suite covering the new feature or reproducing the exact bug described.
   - For bugs: The test must reliably reproduce the bug (i.e. fail initially).
   - For new methods: The test must define expected input/output contracts, boundary checks, and error cases.

2. **Step 2: Implement Code (Green Phase)**
   - Implement the minimum required code in the service/class to satisfy the test specifications.
   - Run the tests continuously until **all test cases pass**.

3. **Step 3: Refactor & Polish**
   - Clean up code structure, ensure documentation standards and typing constraints are met without breaking the passing test suite.

4. **Step 4: Submission & Git Commit**
   - Present the passing test verification and implementation summary to the user.
   - Create a structured phase git commit following the `git-commit-workflow` standard.
