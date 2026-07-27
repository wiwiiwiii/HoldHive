# Use of GenAI

## Introduction

This document establishes mandatory guidelines for the appropriate use of Generative AI tools (such as GitHub Copilot, ChatGPT, Claude, etc.) during this training project. These guidelines reflect professional standards required in banking software development.

**Why These Rules Exist:**

Generative AI is a powerful tool that can accelerate development when used correctly. However, in a banking environment, code quality, security, and maintainability are paramount. More importantly, **this is a learning program** - the primary goal is for you to develop your skills and understanding, not simply to produce working code as quickly as possible.

**The Core Principle:**

AI should augment your abilities, not replace your thinking. You must understand every line of code in your application. In a real banking environment, you will be responsible for maintaining, debugging, and explaining code to auditors, security teams, and regulators. Code you cannot explain is code you cannot defend.

**Consequences of Non-Compliance:**

Teams or individuals who fail to follow these guidelines may face:
- Mandatory code rewrites
- Removal of unexplainable code during reviews
- Additional code review sessions
- In a real banking environment, this could result in failed audits, security vulnerabilities, or project failures

Remember: The goal is not just to complete the project, but to **learn how to build professional-grade software** that meets banking standards.

---

## Guidelines

1. **Agile Methodology Must Be Followed**
   - Daily stand-up (Yesterday / Today / Blockers)
   - Sprint retrospective is mandatory
   - Discussion & reflection > speed of coding
2. **Repository Hygiene**
   - Proper branching strategy: prod / main, qa / release, feature/*
   - Commit regularly (small, meaningful commits)
   - Clear commit messages (why, not just what)
   - Pull Request mandatory
   - Code review by a human, not AI
3. **Jira (or Similar Tool or Post it notes) Usage Is Mandatory**
   - Work must start from some kind of requirement such as a User Story
   - Each requirement must have: Priority, Clear acceptance criteria
   - No direct coding without a tracked story
4. **Unit Testing Discipline**
   - Backend unit tests are mandatory
   - Minimum 70% line coverage
   - Tests should validate business logic, not just happy paths
5. **Design Artifacts Must Be Updated**
   - Appropriate diagrams should exist and stay updated
   - Changes in code â†’ changes in diagram - C4 diagrams could be good
   - Copilot can't be the source of architecture decisions
6. **Problem Understanding Before Prompting**
   - Developer must explain the problem in their own words
   - If you can't explain it, you can't prompt Copilot
   - Copilot comes after clarity, not before
7. **Human-Driven Code Structure**
   - Developers decide: Package structure, Class responsibilities, Method names
   - Copilot suggestions can be refined, not blindly accepted
8. **Acceptance Criteria Validation by Developer**
   - Code must be manually checked against acceptance criteria
   - "Works on my machine" is not completion
   - AI output â‰  Done
9. **Debugging by Human First**
   - First attempt: logs, breakpoints, reasoning
   - Copilot used only after understanding the issue
   - AI is a second brain, not the first reaction
10. **Explain-Your-Code Rule**
    - Any Copilot-generated code must be explainable by the developer
    - If the developer can't explain it line by line: Rewrite it Or don't merge it


If nobody in your team can explain a block of code to the satisfaction of your instructor, then your instructor **will probably delete** that code. Code that is not understood by team members will not be acceptable in any new banking application in the firm.


