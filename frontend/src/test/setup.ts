import "@testing-library/jest-dom";
import { setupServer } from "msw/node";

// Capture originals before overriding to avoid infinite recursion
const originalError = console.error.bind(console);
const originalWarn = console.warn.bind(console);

// Silence console warnings in tests
global.console = {
  ...console,
  error: (...args: any[]) => {
    if (
      typeof args[0] === "string" &&
      (args[0].includes("Not implemented: HTMLFormElement.prototype.submit") ||
        args[0].includes("Not implemented: navigation"))
    ) {
      return;
    }
    originalError(...args);
  },
  warn: (...args: any[]) => {
    if (typeof args[0] === "string" && args[0].includes("act(")) {
      return;
    }
    originalWarn(...args);
  },
};

// MSW server setup
export const server = setupServer();

// Establish API mocking before all tests
// beforeAll(() => server.listen({ onUnhandledRequest: "error" }));

// Reset any request handlers that we may add during the tests,
// so they don't affect other tests.
// afterEach(() => server.resetHandlers());

// Clean up after the tests are finished.
// afterAll(() => server.close());
