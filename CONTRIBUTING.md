# Contributing to BookStack Companion

Thank you for your interest in contributing to BookStack Companion!

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/Bookstack-app.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Run tests: `./gradlew test`
6. Run lint: `./gradlew lint`
7. Commit your changes with a clear message
8. Push to your fork and open a Pull Request

## Development Requirements

- **JDK 21**
- **Android Studio** Jellyfish or newer
- **Android SDK** with API 34

## Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Keep composables focused and reusable
- Place ViewModels in separate files from screen composables
- Use `stringResource()` for all user-facing strings (no hardcoded text)
- Follow Material 3 design guidelines

## Commit Messages

- Use clear, descriptive commit messages
- Start with a verb in imperative mood (e.g., "Add", "Fix", "Update")
- Reference issue numbers when applicable (e.g., "Fix #42")

## Pull Request Guidelines

- PR should target the `main` branch
- Include a clear description of what changed and why
- Ensure all tests pass
- Keep PRs focused on a single feature or fix
- Update documentation if needed

## Reporting Issues

- Use GitHub Issues to report bugs or request features
- Include steps to reproduce for bug reports
- Mention your device model and Android version

## License

By contributing, you agree that your contributions will be licensed under the GPL-3.0 License.
