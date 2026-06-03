# Danangbest - Mono-Repo Structure

This project is organized into separate Backend and Frontend modules for better maintainability and professional standards.

## Project Layout

- `/backend`: Spring Boot application (Java, Maven, JPA).
- `/frontend`: Modern static frontend (HTML, CSS, JS).

## Getting Started

### 1. Backend Development
To run the backend, navigate to the `backend/` directory and use Maven:
```bash
cd backend
mvn spring-boot:run
```

### 2. Frontend Development
You can modify the UI directly in the `frontend/` directory. To sync changes to the backend's static folder, use the provided script:
```powershell
./frontend/scripts/sync.ps1
```

## Deployment
The project is configured such that building the backend (`mvn package` inside `backend/`) will automatically bundle the latest frontend into the final executable.
