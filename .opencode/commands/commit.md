---
description: Create a safe git commit
---
Create a git commit for the current repository changes using the repository conventions in @AGENTS.md.

Commit request arguments, if any: `$ARGUMENTS`

Follow this workflow exactly:

1. Inspect the commit context before staging anything:
   - Run `git status --short` to see tracked and untracked files.
   - Run `git diff` and `git diff --staged` to inspect unstaged and staged changes.
   - Run `git log --oneline -10` to infer the local commit message style.

2. Analyze what should be committed:
   - Include only relevant files for the requested commit.
   - Do not include secrets or local environment files such as `.env`, credentials, tokens, or private keys.
   - If unrelated user changes exist, leave them untouched and do not revert them.
   - If the requested scope is ambiguous, ask one short clarification question before staging.

3. Draft a concise commit message:
   - Prefer this project style when applicable: `[HU-05] add redis service configuration`.
   - Use an accurate verb: `add` for new behavior, `update` for enhancement, `fix` for bugs, `refactor` for internal-only changes, `docs` for documentation-only changes.
   - Focus on why the change exists, not a file-by-file list.

4. Stage and commit safely:
   - Stage only the intended files with non-interactive `git add` commands.
   - Run `git commit -m "<message>"`.
   - Never use interactive git commands.
   - Never amend unless explicitly requested by the user.
   - Never run destructive git commands such as `git reset --hard` or `git checkout --`.
   - Never skip hooks unless explicitly requested by the user.

5. Verify the result:
   - Run `git status --short` after the commit.
   - Report the commit hash and message.
   - Mention any remaining uncommitted files without modifying them.

Do not push to any remote unless the user explicitly requests it.
