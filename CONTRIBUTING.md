# Contributing to GitHub Access Analyzer

First off, thanks for taking the time to contribute!

All types of contributions are welcome, from bug fixes to new features. 

## How can I contribute?

1. **Fork** the repository and clone it to your machine.
2. **Create a branch** for your feature or fix.
3. **Make your changes** and add tests if possible.
4. **Push your branch** to your fork.
5. **Open a Pull Request** with a description of your changes.

## Development Setup

We use Java 17 and Maven. To get started:

```bash
mvn clean install
mvn spring-boot:run
```

Make sure to set your `GITHUB_TOKEN` as an environment variable before running.

## Code Style

- Follow standard Java CamelCase naming conventions.
- Keep methods short and focused.
- Add logging where appropriate.
