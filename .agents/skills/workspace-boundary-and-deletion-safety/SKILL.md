---
name: workspace-boundary-and-deletion-safety
description: Strictly enforces workspace boundaries (no modifications/deletions outside the current project), allows read-only shell tools without friction, and requires explicit user permission in BIG CAPITAL LETTERS before deleting anything inside the directory.
---

# Workspace Boundary and Deletion Safety Skill

This skill governs tool execution, file boundaries, and deletion safety protocols.

## 1. Directory Boundaries
- **Project Directory (`/home/koin/Projects/10nts`)**: The agent is allowed to create, edit, and read files and directories within this path.
- **Outside Project Directory**:
  - **READ**: Allowed (e.g., inspecting global configs, system specs, or reference files).
  - **MODIFY / UPDATE**: **STRICTLY FORBIDDEN** unless explicit user permission is requested and granted.
  - **DELETE**: **PERMANENTLY FORBIDDEN**. The agent must never delete or alter anything outside the project directory.

## 2. Command & Tool Execution Rules
- Safe, non-destructive read operations (`ls`, `cat`, `echo`, `grep`, `find`, `pwd`, reading files) are allowed and encouraged for fast inspection.
- Any operation that mutates state outside the project directory requires asking the user first.

## 3. Mandatory Deletion Protocol
Whenever any file or directory needs to be deleted or removed inside the project:
1. **DO NOT** execute the deletion immediately.
2. Stop execution immediately and ask the user for confirmation in **BIG CAPITAL LETTERS**:
   ```
   ATTENTION: PERMISSION REQUIRED TO DELETE:
   [PATH_TO_FILE_OR_DIRECTORY]

   DO YOU CONFIRM THE DELETION OF THIS FILE/DIRECTORY? (REPLY YES TO PROCEED)
   ```
3. Halt tool calls and wait for the user's explicit `YES` before performing any deletion.
