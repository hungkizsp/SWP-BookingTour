# Danangbest - Separate Frontend

This folder contains the modern, premium frontend for the Danangbest Tour Booking application. It is separated from the Spring Boot backend to allow for dedicated UI development.

## Project Structure

- `assets/`: Contains global CSS, JS, and image assets.
- `pages/`: Contains HTML templates for all application screens.
- `package.json`: Manages frontend scripts and dependencies.

## Development Workflow

1. **Local Development**:
   You can open the HTML files directly or use a local dev server (like Live Server or BrowserSync).
   
2. **Synchronizing with Backend**:
   When you are ready to deploy changes to the Spring Boot application, you need to copy the files to `src/main/resources/static`.
   
   A synchronization script is provided in `./scripts/sync.ps1`.

## Future Improvements
- Move to a build-based system like **Vite**.
- Implement **TailwindCSS** for more flexible styling.
- Integrate with a modern framework like **React** or **Next.js**.
